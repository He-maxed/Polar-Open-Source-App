package com.polar.recorder.ble

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class PolarSimulatedDeviceSource(private val context: Context) : PolarDeviceSource {

    override var connectionState: DeviceConnectionState = DeviceConnectionState.DISCONNECTED
        private set
    override var isStreaming: Boolean = false
        private set
    override var isRecording: Boolean = false
        private set

    private val listeners = mutableListOf<PolarDeviceSourceListener>()

    private val handler = Handler(Looper.getMainLooper())
    private var simRunnable: Runnable? = null
    private var sampleCounter = 0L
    private var startTimeMs = 0L

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
        notifyState("Simulated Polar H10 Device Found")
    }

    override fun stopScanning() {
        if (connectionState == DeviceConnectionState.SCANNING) {
            connectionState = DeviceConnectionState.DISCONNECTED
            notifyState(null)
        }
    }

    override fun connect(device: BluetoothDevice?) {
        connectionState = DeviceConnectionState.CONNECTING
        notifyState("Simulated Polar H10 (H10 89A2BF)")

        handler.postDelayed({
            connectionState = DeviceConnectionState.CONNECTED
            notifyState("Simulated Polar H10 (H10 89A2BF)")
            startStreaming()
        }, 500)
    }

    override fun disconnect() {
        stopStreaming()
        connectionState = DeviceConnectionState.DISCONNECTED
        notifyState(null)
    }

    override fun startStreaming() {
        if (isStreaming) return
        isStreaming = true
        connectionState = DeviceConnectionState.STREAMING
        notifyState("Simulated Polar H10 (H10 89A2BF)")

        sampleCounter = 0
        startTimeMs = System.currentTimeMillis()

        val sampleRateHz = 130
        val intervalMs = 1000L / sampleRateHz

        simRunnable = object : Runnable {
            override fun run() {
                if (!isStreaming) return
                sampleCounter++
                val nowMs = System.currentTimeMillis()
                val elapsedSec = (nowMs - startTimeMs) / 1000.0
                val isoStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date(nowMs))

                // Synthesize ECG Waveform (P-QRS-T Model)
                val currentBpm = 68.0 + 10.0 * sin(elapsedSec / 8.0)
                val beatPeriodSec = 60.0 / currentBpm
                val phase = (elapsedSec % beatPeriodSec) / beatPeriodSec

                var uV = 0.0
                if (phase > 0.1 && phase < 0.2) uV += 120.0 * sin((phase - 0.1) / 0.1 * Math.PI)
                if (phase >= 0.38 && phase <= 0.44) {
                    val qrsP = (phase - 0.38) / 0.06
                    if (qrsP < 0.2) uV -= 250.0 * (qrsP / 0.2)
                    else if (qrsP < 0.7) uV += 1450.0 * sin((qrsP - 0.2) / 0.5 * Math.PI)
                    else uV -= 450.0 * ((qrsP - 0.7) / 0.3)
                }
                if (phase > 0.55 && phase < 0.75) uV += 280.0 * sin((phase - 0.55) / 0.2 * Math.PI)
                uV += 30.0 * sin(elapsedSec * 1.5) + (Math.random() - 0.5) * 15.0

                val ecgSample = EcgSample(isoStr, nowMs, sampleCounter, uV.toFloat())
                listeners.forEach { it.onEcgSampleReceived(ecgSample) }

                if (isRecording) {
                    ecgWriter?.write("$isoStr;$nowMs;$sampleCounter;${uV.toInt()}\n")
                }

                // Emit HR / RR every beat cycle
                if (phase < intervalMs / 1000.0 / beatPeriodSec) {
                    val hrObj = HrSample(isoStr, currentBpm.toInt())
                    val rrObj = RrSample(isoStr, (beatPeriodSec * 1000).toInt())

                    listeners.forEach {
                        it.onHrSampleReceived(hrObj)
                        it.onRrSampleReceived(rrObj)
                    }

                    if (isRecording) {
                        hrWriter?.write("$isoStr;${hrObj.hrBpm}\n")
                        rrWriter?.write("$isoStr;${rrObj.rrMs}\n")
                    }
                }

                // Accelerometer simulation (25Hz - 200Hz)
                val xAcc = (30.0 + 15.0 * sin(elapsedSec * 2.0)).toFloat()
                val yAcc = (12.0 + 10.0 * cos(elapsedSec * 2.0)).toFloat()
                val zAcc = (980.0 + (Math.random() - 0.5) * 15.0).toFloat()
                val accSample = AccSample(isoStr, nowMs * 1000000L, xAcc, yAcc, zAcc)
                listeners.forEach { it.onAccSampleReceived(accSample) }

                if (isRecording) {
                    accWriter?.write("$isoStr;${accSample.timestampNs};$xAcc;$yAcc;$zAcc\n")
                }

                handler.postDelayed(this, intervalMs)
            }
        }

        handler.post(simRunnable!!)
    }

    override fun stopStreaming() {
        isStreaming = false
        simRunnable?.let { handler.removeCallbacks(it) }
        simRunnable = null
        if (connectionState == DeviceConnectionState.STREAMING) {
            connectionState = DeviceConnectionState.CONNECTED
            notifyState("Simulated Polar H10 (H10 89A2BF)")
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
            e.printStackTrace()
        }
        return mapOf(
            "dir" to (currentSessionDir?.absolutePath ?: "")
        )
    }

    private fun notifyState(deviceName: String?) {
        listeners.forEach { it.onConnectionStateChanged(connectionState, deviceName) }
    }
}
