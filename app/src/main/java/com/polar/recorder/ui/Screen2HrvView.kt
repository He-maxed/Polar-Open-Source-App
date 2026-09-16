package com.polar.recorder.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.polar.recorder.R
import com.polar.recorder.analysis.EcgAnalysisResult

class Screen2HrvView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tvRmssd: TextView
    private val tvSdnn: TextView
    private val tvMeanRr: TextView
    private val tvAvgHr: TextView
    private val tvPnn50: TextView
    private val tvLnRmssd: TextView
    private val tvAvgResp: TextView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_screen2_hrv, this, true)

        tvRmssd = findViewById(R.id.tvRmssd)
        tvSdnn = findViewById(R.id.tvSdnn)
        tvMeanRr = findViewById(R.id.tvMeanRr)
        tvAvgHr = findViewById(R.id.tvAvgHr)
        tvPnn50 = findViewById(R.id.tvPnn50)
        tvLnRmssd = findViewById(R.id.tvLnRmssd)
        tvAvgResp = findViewById(R.id.tvAvgResp)
    }

    fun updateResults(res: EcgAnalysisResult) {
        tvRmssd.text = "RMSSD: ${res.rmssdMs.toInt()} ms"
        tvSdnn.text = "SDNN: ${res.sdnnMs.toInt()} ms"
        tvMeanRr.text = "Mean RR interval: ${res.meanRrMs.toInt()} ms"
        tvAvgHr.text = "Average heart rate: ${res.meanHr.toInt()} bpm"
        tvPnn50.text = "PNN50: ${res.pNN50.toInt()} %"
        tvLnRmssd.text = "ln(RMSSD): ${String.format("%.2f", res.lnRmssd)} ms"
        tvAvgResp.text = "Average respiratory rate:${String.format("%.2f", res.avgRespRate)}"
    }
}
