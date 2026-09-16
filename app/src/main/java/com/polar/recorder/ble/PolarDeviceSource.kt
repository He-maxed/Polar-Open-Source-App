package com.polar.recorder.ble

import android.bluetooth.BluetoothDevice

enum class DeviceConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    STREAMING
}

data class EcgSample(
    val isoTimestamp: String,
    val timestampMs: Long,
    val sampleCount: Long,
    val uV: Float
)

data class RrSample(
    val isoTimestamp: String,
    val rrMs: Int
)

data class HrSample(
    val isoTimestamp: String,
    val hrBpm: Int
)

data class AccSample(
    val isoTimestamp: String,
    val timestampNs: Long,
    val xMg: Float,
    val yMg: Float,
    val zMg: Float
)

interface PolarDeviceSourceListener {
    fun onConnectionStateChanged(state: DeviceConnectionState, deviceName: String?)
    fun onEcgSampleReceived(sample: EcgSample)
    fun onRrSampleReceived(sample: RrSample)
    fun onHrSampleReceived(sample: HrSample)
    fun onAccSampleReceived(sample: AccSample)
    fun onError(message: String)
}

interface PolarDeviceSource {
    val connectionState: DeviceConnectionState
    val isStreaming: Boolean
    val isRecording: Boolean

    fun registerListener(listener: PolarDeviceSourceListener)
    fun unregisterListener(listener: PolarDeviceSourceListener)

    fun startScanning()
    fun stopScanning()
    fun connect(device: BluetoothDevice?)
    fun disconnect()

    fun startStreaming()
    fun stopStreaming()

    fun startRecording(sessionName: String): String
    fun stopRecording(): Map<String, String> // returns file paths
}
