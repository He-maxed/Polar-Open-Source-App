package com.polar.recorder.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CustomActivityGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#2C333C")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val velocityPaint = Paint().apply {
        color = Color.parseColor("#0284C7") // Blue Velocity
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val cadencePaint = Paint().apply {
        color = Color.parseColor("#16A34A") // Green Cadence
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val legendPaint = Paint().apply {
        color = Color.parseColor("#7C8794")
        textSize = 22f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawColor(Color.parseColor("#14181D"))

        for (i in 1..4) {
            val y = h * (i / 5f)
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Velocity Curve
        val vPoints = floatArrayOf(3.8f, 6.8f, 6.9f, 4.2f, 6.5f, 4.5f, 0.2f)
        val stepX = w / (vPoints.size - 1)
        val vPath = Path()
        for (i in vPoints.indices) {
            val x = i * stepX
            val normY = 1f - (vPoints[i] / 8.0f)
            val y = normY * (h - 40f) + 20f
            if (i == 0) vPath.moveTo(x, y) else vPath.lineTo(x, y)
        }
        canvas.drawPath(vPath, velocityPaint)

        // Cadence Curve
        val cPoints = floatArrayOf(20f, 80f, 82f, 35f, 78f, 40f, 5f)
        val cPath = Path()
        for (i in cPoints.indices) {
            val x = i * stepX
            val normY = 1f - (cPoints[i] / 120f)
            val y = normY * (h - 40f) + 20f
            if (i == 0) cPath.moveTo(x, y) else cPath.lineTo(x, y)
        }
        canvas.drawPath(cPath, cadencePaint)

        canvas.drawText("Velocity (blue) km/h  ·  Cadence (green) /min", 20f, h - 10f, legendPaint)
    }
}
