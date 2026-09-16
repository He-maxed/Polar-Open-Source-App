package com.polar.recorder.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.polar.recorder.R

class Screen4InspectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val waveformCanvasView: CustomQrsCanvasView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_screen4_inspector, this, true)
        waveformCanvasView = findViewById(R.id.customQrsCanvas)
    }
}
