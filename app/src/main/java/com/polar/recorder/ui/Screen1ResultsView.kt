package com.polar.recorder.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.polar.recorder.R
import com.polar.recorder.analysis.EcgAnalysisResult

class Screen1ResultsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tvMeanHR: TextView
    private val tvPNN50: TextView
    private val tvPNN200: TextView
    private val tvMeanQRS: TextView
    private val tvMeanQTc: TextView
    private val tvMeanTPositive: TextView
    private val tvMeanJPoint: TextView
    private val tvMean28Hz: TextView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_screen1_results, this, true)

        tvMeanHR = findViewById(R.id.tvMeanHR)
        tvPNN50 = findViewById(R.id.tvPNN50)
        tvPNN200 = findViewById(R.id.tvPNN200)
        tvMeanQRS = findViewById(R.id.tvMeanQRS)
        tvMeanQTc = findViewById(R.id.tvMeanQTc)
        tvMeanTPositive = findViewById(R.id.tvMeanTPositive)
        tvMeanJPoint = findViewById(R.id.tvMeanJPoint)
        tvMean28Hz = findViewById(R.id.tvMean28Hz)
    }

    fun updateResults(res: EcgAnalysisResult) {
        tvMeanHR.text = "${res.meanHr.toInt()} [35-180] bpm"
        tvPNN50.text = "${res.pNN50.toInt()} [0-45] %"
        tvPNN200.text = "${res.pNN200.toInt()} [0-15] %"
        tvMeanQRS.text = "${res.meanQRS.toInt()} [50-114] ms"
        tvMeanQTc.text = "${res.meanQTc.toInt()} [310-470] ms"
        tvMeanTPositive.text = "${res.meanTPositive.toInt()} [90-100] %"
        tvMeanJPoint.text = "${res.meanJPointUv.toInt()} [0-500] uV"
        tvMean28Hz.text = "${res.mean28Hz.toInt()} [0-90] %"
    }
}
