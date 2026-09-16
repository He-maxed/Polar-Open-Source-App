package com.polar.recorder.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CustomRespGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#2C333C")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#16A34A") // Green
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#7C8794")
        textSize = 24f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawColor(Color.parseColor("#14181D"))

        // Grid Lines
        for (i in 1..4) {
            val y = h * (i / 5f)
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Draw Respiratory Rate curve (5 to 40 breaths/min range)
        val path = Path()
        val dataPoints = floatArrayOf(14f, 22f, 18f, 28f, 15f, 25f, 16f, 30f, 19f, 14f)
        val stepX = w / (dataPoints.size - 1)

        for (i in dataPoints.indices) {
            val x = i * stepX
            val normY = 1f - ((dataPoints[i] - 5f) / 35f)
            val y = normY * (h - 40f) + 20f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint)
        canvas.drawText("Breathes per minute (EDR / ACC)", 20f, h - 10f, textPaint)
    }
}
