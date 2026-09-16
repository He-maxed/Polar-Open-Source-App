package com.polar.recorder.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@SuppressLint("MissingPermission")
class PolarBleManager(private val context: Context) {

    companion object {
        private const val TAG = "PolarBleManager"

        val PMD_SERVICE_UUID: UUID = UUID.fromString("fb005c80-02e7-f38b-5d57-418465666501")
        val PMD_CONTROL_UUID: UUID = UUID.fromString("fb005c81-02e7-f38b-5d57-418465666501")
        val PMD_DATA_UUID: UUID = UUID.fromString("fb005c82-02e7-f38b-5d57-418465666501")

        val HR_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    var isConnected = false
        private set
    var isRecording = false
        private set

    private var bluetoothGatt: BluetoothGatt? = null
    private var ecgWriter: FileWriter? = null
    private var rrWriter: FileWriter? = null
    private var accWriter: FileWriter? = null
    private var hrWriter: FileWriter? = null

    private var sampleCount = 0L

    var onEcgSample: ((Float) -> Unit)? = null
    var onHrUpdate: ((Int, Double?) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null

    fun connectDevice(device: BluetoothDevice) {
        onStatusChanged?.invoke("Connecting to ${device.name ?: device.address}...")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun startRecording(): String {
        val storageDir = File(context.getExternalFilesDir(null), "PolarRecorder")
        if (!storageDir.exists()) storageDir.mkdirs()

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

        sampleCount = 0
        isRecording = true
        onStatusChanged?.invoke("Recording started to ${storageDir.absolutePath}")
        return storageDir.absolutePath
    }

    fun stopRecording() {
        isRecording = false
        try {
            ecgWriter?.flush(); ecgWriter?.close()
            rrWriter?.flush(); rrWriter?.close()
            accWriter?.flush(); accWriter?.close()
            hrWriter?.flush(); hrWriter?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing file writers", e)
        }
        onStatusChanged?.invoke("Recording saved successfully.")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true
                onStatusChanged?.invoke("Connected. Discovering services...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false
                onStatusChanged?.invoke("Disconnected from Polar H10")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                subscribeHeartRate(gatt)
                subscribePMDEcg(gatt)
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

                var rrMs: Double? = null
                val hasRr = (flags and 0x10) != 0
                if (hasRr) {
                    val offset = if (is16Bit) 3 else 2
                    if (data.size >= offset + 2) {
                        val rrRaw = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)
                        rrMs = (rrRaw / 1024.0) * 1000.0
                    }
                }

                if (isRecording) {
                    hrWriter?.write("$nowIso;$hr\n")
                    rrMs?.let { rrWriter?.write("$nowIso;${it.toInt()}\n") }
                }
                onHrUpdate?.invoke(hr, rrMs)
            } else if (characteristic.uuid == PMD_DATA_UUID) {
                val data = characteristic.value ?: return
                if (data.isNotEmpty() && data[0] == 0x00.toByte()) { // Raw 130Hz ECG
                    var offset = 10
                    while (offset + 2 < data.size) {
                        val uV = (data[offset].toInt() and 0xFF) or
                                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                                (data[offset + 2].toInt() shl 16)
                        offset += 3
                        sampleCount++

                        val sampleMv = uV / 1000.0f
                        if (isRecording) {
                            ecgWriter?.write("$nowIso;$nowMs;$sampleCount;$uV\n")
                        }
                        onEcgSample?.invoke(sampleMv)
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

        // Enable PMD Raw ECG 130Hz Command
        val enableCmd = byteArrayOf(0x02, 0x00, 0x00, 0x01, 0x82.toByte(), 0x00, 0x01, 0x01, 0x0E, 0x00)
        controlChar.value = enableCmd
        gatt.writeCharacteristic(controlChar)
    }
}
