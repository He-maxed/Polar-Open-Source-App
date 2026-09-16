package com.polar.recorder.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
class PolarBleDeviceSource(private val context: Context) : PolarDeviceSource {

    companion object {
        private const val TAG = "PolarBleDeviceSource"
        val PMD_SERVICE_UUID: UUID = UUID.fromString("fb005c80-02e7-f38b-5d57-418465666501")
        val PMD_CONTROL_UUID: UUID = UUID.fromString("fb005c81-02e7-f38b-5d57-418465666501")
        val PMD_DATA_UUID: UUID = UUID.fromString("fb005c82-02e7-f38b-5d57-418465666501")

        val HR_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    override var connectionState: DeviceConnectionState = DeviceConnectionState.DISCONNECTED
        private set
    override var isStreaming: Boolean = false
        private set
    override var isRecording: Boolean = false
        private set

    private val listeners = mutableListOf<PolarDeviceSourceListener>()
    private var bluetoothGatt: BluetoothGatt? = null
    private var sampleCounter = 0L

    private var ecgWriter: FileWriter? = null
    private var rrWriter: FileWriter? = null
    private var accWriter: FileWriter? = null
    private var hrWriter: FileWriter? = null
    private var currentSessionDir: File? = null

    override fun registerListener(listener: PolarDeviceSourceListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    override fun unregisterListener(listener: PolarDeviceSourceListener) {
        listeners.remove(listener)
    }

    override fun startScanning() {
        connectionState = DeviceConnectionState.SCANNING
        notifyState(null)
    }

    override fun stopScanning() {
        if (connectionState == DeviceConnectionState.SCANNING) {
            connectionState = DeviceConnectionState.DISCONNECTED
            notifyState(null)
        }
    }

    override fun connect(device: BluetoothDevice?) {
        if (device == null) {
            listeners.forEach { it.onError("No Bluetooth Device provided to connect()") }
            return
        }
        connectionState = DeviceConnectionState.CONNECTING
        notifyState(device.name ?: device.address)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    override fun disconnect() {
        stopStreaming()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectionState = DeviceConnectionState.DISCONNECTED
        notifyState(null)
    }

    override fun startStreaming() {
        isStreaming = true
        connectionState = DeviceConnectionState.STREAMING
        notifyState(bluetoothGatt?.device?.name)
    }

    override fun stopStreaming() {
        isStreaming = false
        if (connectionState == DeviceConnectionState.STREAMING) {
            connectionState = DeviceConnectionState.CONNECTED
            notifyState(bluetoothGatt?.device?.name)
        }
    }

    override fun startRecording(sessionName: String): String {
        val storageDir = File(context.getExternalFilesDir(null), "PolarRecorder")
        if (!storageDir.exists()) storageDir.mkdirs()

        currentSessionDir = storageDir
        val timeStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val baseName = "Polar_H10_$timeStr"

        val ecgFile = File(storageDir, "${baseName}_ECG.txt")
        val rrFile = File(storageDir, "${baseName}_RR.txt")
        val accFile = File(storageDir, "${baseName}_ACC.txt")
        val hrFile = File(storageDir, "${baseName}_HR.txt")

        ecgWriter = FileWriter(ecgFile).apply { write("Phone timestamp;Timestamp [ms];Sample count;ECG [uV]\n") }
        rrWriter = FileWriter(rrFile).apply { write("Phone timestamp;RR interval [ms]\n") }
        accWriter = FileWriter(accFile).apply { write("Phone timestamp;Timestamp [ns];X [mg];Y [mg];Z [mg]\n") }
        hrWriter = FileWriter(hrFile).apply { write("Phone timestamp;HR [bpm]\n") }

        sampleCounter = 0
        isRecording = true
        return storageDir.absolutePath
    }

    override fun stopRecording(): Map<String, String> {
        isRecording = false
        try {
            ecgWriter?.flush(); ecgWriter?.close()
            rrWriter?.flush(); rrWriter?.close()
            accWriter?.flush(); accWriter?.close()
            hrWriter?.flush(); hrWriter?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing writers", e)
        }
        return mapOf("dir" to (currentSessionDir?.absolutePath ?: ""))
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = DeviceConnectionState.CONNECTED
                notifyState(gatt.device.name)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = DeviceConnectionState.DISCONNECTED
                notifyState(null)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                subscribeHeartRate(gatt)
                subscribePMDEcg(gatt)
                startStreaming()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date())
            val nowMs = System.currentTimeMillis()

            if (characteristic.uuid == HR_MEASUREMENT_UUID) {
                val data = characteristic.value ?: return
                val flags = data[0].toInt()
                val is16Bit = (flags and 0x01) != 0
                val hr = if (is16Bit) ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF) else (data[1].toInt() and 0xFF)

                val hrObj = HrSample(nowIso, hr)
                listeners.forEach { it.onHrSampleReceived(hrObj) }

                if (isRecording) {
                    hrWriter?.write("$nowIso;$hr\n")
                }

                val hasRr = (flags and 0x10) != 0
                if (hasRr) {
                    val offset = if (is16Bit) 3 else 2
                    if (data.size >= offset + 2) {
                        val rrRaw = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)
                        val rrMs = ((rrRaw / 1024.0) * 1000.0).toInt()
                        val rrObj = RrSample(nowIso, rrMs)
                        listeners.forEach { it.onRrSampleReceived(rrObj) }
                        if (isRecording) {
                            rrWriter?.write("$nowIso;$rrMs\n")
                        }
                    }
                }
            } else if (characteristic.uuid == PMD_DATA_UUID) {
                val data = characteristic.value ?: return
                if (data.isNotEmpty() && data[0] == 0x00.toByte()) {
                    var offset = 10
                    while (offset + 2 < data.size) {
                        val uV = (data[offset].toInt() and 0xFF) or
                                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                                (data[offset + 2].toInt() shl 16)
                        offset += 3
                        sampleCounter++

                        val ecgObj = EcgSample(nowIso, nowMs, sampleCounter, uV.toFloat())
                        listeners.forEach { it.onEcgSampleReceived(ecgObj) }

                        if (isRecording) {
                            ecgWriter?.write("$nowIso;$nowMs;$sampleCounter;$uV\n")
                        }
                    }
                }
            }
        }
    }

    private fun subscribeHeartRate(gatt: BluetoothGatt) {
        val service = gatt.getService(HR_SERVICE_UUID) ?: return
        val char = service.getCharacteristic(HR_MEASUREMENT_UUID) ?: return
        gatt.setCharacteristicNotification(char, true)
        val config = char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
        config?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(config)
    }

    private fun subscribePMDEcg(gatt: BluetoothGatt) {
        val service = gatt.getService(PMD_SERVICE_UUID) ?: return
        val dataChar = service.getCharacteristic(PMD_DATA_UUID) ?: return
        val controlChar = service.getCharacteristic(PMD_CONTROL_UUID) ?: return

        gatt.setCharacteristicNotification(dataChar, true)
        val config = dataChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
        config?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(config)

        val enableCmd = byteArrayOf(0x02, 0x00, 0x00, 0x01, 0x82.toByte(), 0x00, 0x01, 0x01, 0x0E, 0x00)
        controlChar.value = enableCmd
        gatt.writeCharacteristic(controlChar)
    }

    private fun notifyState(deviceName: String?) {
        listeners.forEach { it.onConnectionStateChanged(connectionState, deviceName) }
    }
}
