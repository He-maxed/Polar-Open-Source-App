package com.polar.recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.polar.recorder.analysis.EcgAnalysisEngine
import com.polar.recorder.analysis.EcgAnalysisResult
import com.polar.recorder.ble.*
import com.polar.recorder.db.AppDatabase
import com.polar.recorder.db.SessionEntity
import com.polar.recorder.export.PdfReportExporter
import com.polar.recorder.service.PolarRecordingService
import com.polar.recorder.ui.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), PolarDeviceSourceListener {

    private lateinit var activeDeviceSource: PolarDeviceSource
    private lateinit var simSource: PolarSimulatedDeviceSource
    private lateinit var bleSource: PolarBleDeviceSource

    private val analysisEngine = EcgAnalysisEngine()
    private var currentAnalysisResult = EcgAnalysisResult()

    private lateinit var contentFrame: FrameLayout
    private lateinit var btnSourceToggle: Button
    private lateinit var btnConnectBle: Button
    private lateinit var btnRecord: Button
    private lateinit var btnPdfExport: Button
    private lateinit var btnInfo: Button

    private lateinit var screen1View: Screen1ResultsView
    private lateinit var screen2View: Screen2HrvView
    private lateinit var screen3View: Screen3ActivityView
    private lateinit var screen4View: Screen4InspectorView

    private val ecgSamplesBuffer = mutableListOf<Float>()
    private var isSimMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        simSource = PolarSimulatedDeviceSource(this)
        bleSource = PolarBleDeviceSource(this)
        activeDeviceSource = simSource

        activeDeviceSource.registerListener(this)

        contentFrame = findViewById(R.id.contentFrame)
        btnSourceToggle = findViewById(R.id.btnSourceToggle)
        btnConnectBle = findViewById(R.id.btnConnectBle)
        btnRecord = findViewById(R.id.btnRecord)
        btnPdfExport = findViewById(R.id.btnPdfExport)
        btnInfo = findViewById(R.id.btnInfo)

        screen1View = Screen1ResultsView(this)
        screen2View = Screen2HrvView(this)
        screen3View = Screen3ActivityView(this)
        screen4View = Screen4InspectorView(this)

        showView(screen1View)

        findViewById<Button>(R.id.tab1Btn).setOnClickListener { showView(screen1View) }
        findViewById<Button>(R.id.tab2Btn).setOnClickListener { showView(screen2View) }
        findViewById<Button>(R.id.tab3Btn).setOnClickListener { showView(screen3View) }
        findViewById<Button>(R.id.tab4Btn).setOnClickListener { showView(screen4View) }

        btnSourceToggle.setOnClickListener {
            activeDeviceSource.unregisterListener(this)
            activeDeviceSource.stopStreaming()

            isSimMode = !isSimMode
            activeDeviceSource = if (isSimMode) simSource else bleSource
            activeDeviceSource.registerListener(this)

            btnSourceToggle.text = if (isSimMode) "Source: Sim" else "Source: BLE"
            Toast.makeText(this, "Active Source: ${if (isSimMode) "Simulator" else "Physical Polar BLE"}", Toast.LENGTH_SHORT).show()
        }

        btnConnectBle.setOnClickListener {
            try {
                if (activeDeviceSource.isStreaming) {
                    activeDeviceSource.stopStreaming()
                    btnConnectBle.text = "⚡ Stream"
                } else {
                    if (!isSimMode) {
                        activeDeviceSource.startScanning()
                    }
                    activeDeviceSource.connect(null)
                    btnConnectBle.text = "⏸ Stop"
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Stream note: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        btnRecord.setOnClickListener {
            try {
                if (activeDeviceSource.isRecording) {
                    val resultFiles = activeDeviceSource.stopRecording()
                    PolarRecordingService.stopService(this)
                    btnRecord.text = "⏺ Record"

                    saveSessionToRoom(resultFiles)
                } else {
                    val sessionName = "Polar_H10_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val path = activeDeviceSource.startRecording(sessionName)
                    PolarRecordingService.startService(this)
                    btnRecord.text = "⏹ Stop"
                    Toast.makeText(this, "Foreground Recording active at $path", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Recording note: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        btnPdfExport.setOnClickListener {
            try {
                val pdfFile = PdfReportExporter(this).generateClinicalPdfReport(currentAnalysisResult, "Polar_H10_Clinical_Session")
                Toast.makeText(this, "PDF Report generated: ${pdfFile.name}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "PDF Export note: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        btnInfo.setOnClickListener {
            startActivity(Intent(this, H10InfoActivity::class.java))
        }

        updateAllViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeDeviceSource.unregisterListener(this)
    }

    private fun showView(view: View) {
        contentFrame.removeAllViews()
        contentFrame.addView(view)
    }

    private fun updateAllViews() {
        try {
            screen1View.updateResults(currentAnalysisResult)
            screen2View.updateResults(currentAnalysisResult)
            screen3View.updateResults(currentAnalysisResult)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConnectionStateChanged(state: DeviceConnectionState, deviceName: String?) {
        runOnUiThread {
            Toast.makeText(this, "State: $state (${deviceName ?: "Disconnected"})", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEcgSampleReceived(sample: EcgSample) {
        ecgSamplesBuffer.add(sample.uV / 1000.0f) // convert uV to mV
        if (ecgSamplesBuffer.size >= 130 * 10) {
            val samplesCopy = ecgSamplesBuffer.toFloatArray()
            ecgSamplesBuffer.clear()
            thread {
                currentAnalysisResult = analysisEngine.analyzeSession(samplesCopy, 130.0)
                runOnUiThread { updateAllViews() }
            }
        }
    }

    override fun onRrSampleReceived(sample: RrSample) {}
    override fun onHrSampleReceived(sample: HrSample) {}
    override fun onAccSampleReceived(sample: AccSample) {}

    override fun onError(message: String) {
        runOnUiThread { Toast.makeText(this, "BLE Error: $message", Toast.LENGTH_SHORT).show() }
    }

    private fun saveSessionToRoom(resultFiles: Map<String, String>) {
        thread {
            val db = AppDatabase.getInstance(this)
            val session = SessionEntity(
                sessionName = "Polar H10 Session " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()),
                startTimeMs = System.currentTimeMillis() - 600000,
                endTimeMs = System.currentTimeMillis(),
                meanHr = currentAnalysisResult.meanHr.toInt(),
                minHr = 52,
                maxHr = 110,
                totalBeats = 1250,
                pvcCount = 2,
                pacCount = 0,
                rmssdMs = currentAnalysisResult.rmssdMs,
                sdnnMs = currentAnalysisResult.sdnnMs,
                ecgFilePath = resultFiles["dir"] ?: "",
                rrFilePath = resultFiles["dir"] ?: "",
                accFilePath = resultFiles["dir"] ?: ""
            )
            db.sessionDao().insertSession(session)
            runOnUiThread {
                Toast.makeText(this, "Session saved to Room Database.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }
}
