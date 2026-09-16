package com.polar.recorder.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.polar.recorder.R
import com.polar.recorder.analysis.EcgAnalysisResult

class Screen3ActivityView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val tvSteps: TextView
    private val tvDistance: TextView
    private val tvSpeed: TextView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_screen3_activity, this, true)

        tvSteps = findViewById(R.id.tvSteps)
        tvDistance = findViewById(R.id.tvDistance)
        tvSpeed = findViewById(R.id.tvSpeed)
    }

    fun updateResults(res: EcgAnalysisResult) {
        tvSteps.text = "${res.steps} steps"
        tvDistance.text = "${res.distanceMeters} m"
        tvSpeed.text = "${String.format("%.2f", res.speedKmh)} km/h"
    }
}
