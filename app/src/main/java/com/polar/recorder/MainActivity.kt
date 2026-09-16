package com.polar.recorder

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
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
import com.polar.recorder.ble.PolarBleManager
import com.polar.recorder.ui.Screen1ResultsView
import com.polar.recorder.ui.Screen2HrvView
import com.polar.recorder.ui.Screen3ActivityView
import com.polar.recorder.ui.Screen4InspectorView

class MainActivity : AppCompatActivity() {

    private lateinit var bleManager: PolarBleManager
    private val analysisEngine = EcgAnalysisEngine()
    private var currentAnalysisResult = EcgAnalysisResult()

    private lateinit var contentFrame: FrameLayout
    private lateinit var btnConnectBle: Button
    private lateinit var btnRecord: Button

    private lateinit var screen1View: Screen1ResultsView
    private lateinit var screen2View: Screen2HrvView
    private lateinit var screen3View: Screen3ActivityView
    private lateinit var screen4View: Screen4InspectorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        bleManager = PolarBleManager(this)

        contentFrame = findViewById(R.id.contentFrame)
        btnConnectBle = findViewById(R.id.btnConnectBle)
        btnRecord = findViewById(R.id.btnRecord)

        screen1View = Screen1ResultsView(this)
        screen2View = Screen2HrvView(this)
        screen3View = Screen3ActivityView(this)
        screen4View = Screen4InspectorView(this)

        showView(screen1View)

        findViewById<Button>(R.id.tab1Btn).setOnClickListener { showView(screen1View) }
        findViewById<Button>(R.id.tab2Btn).setOnClickListener { showView(screen2View) }
        findViewById<Button>(R.id.tab3Btn).setOnClickListener { showView(screen3View) }
        findViewById<Button>(R.id.tab4Btn).setOnClickListener { showView(screen4View) }

        btnConnectBle.setOnClickListener {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            if (adapter != null && adapter.isEnabled) {
                val pairedDevices = adapter.bondedDevices
                val polarDevice = pairedDevices.firstOrNull { it.name?.contains("Polar H10") == true }
                if (polarDevice != null) {
                    bleManager.connectDevice(polarDevice)
                } else {
                    Toast.makeText(this, "Polar H10 not paired in Bluetooth settings.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please enable Bluetooth.", Toast.LENGTH_SHORT).show()
            }
        }

        btnRecord.setOnClickListener {
            if (bleManager.isRecording) {
                bleManager.stopRecording()
                btnRecord.text = "⏺ Record"
            } else {
                val path = bleManager.startRecording()
                btnRecord.text = "⏹ Stop & Save"
                Toast.makeText(this, "Recording started to $path", Toast.LENGTH_LONG).show()
            }
        }

        bleManager.onStatusChanged = { status ->
            runOnUiThread {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            }
        }

        updateAllViews()
    }

    private fun showView(view: View) {
        contentFrame.removeAllViews()
        contentFrame.addView(view)
    }

    private fun updateAllViews() {
        screen1View.updateResults(currentAnalysisResult)
        screen2View.updateResults(currentAnalysisResult)
        screen3View.updateResults(currentAnalysisResult)
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
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }
}
