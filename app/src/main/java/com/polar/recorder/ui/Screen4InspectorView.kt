package com.polar.recorder.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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

    class CustomQrsCanvasView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private val gridPaint = Paint().apply {
            color = Color.parseColor("#F2B8C6")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        private val normalBeatPaint = Paint().apply {
            color = Color.parseColor("#16A34A") // Green
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val ectopicBeatPaint = Paint().apply {
            color = Color.parseColor("#DC2626") // Red Couplet
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        private val normalDotPaint = Paint().apply {
            color = Color.parseColor("#16A34A")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val ectopicDotPaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val midY = h / 2f

            // Draw ECG paper grid
            canvas.drawColor(Color.parseColor("#FDF7F4"))
            var xG = 0f
            while (xG < w) { canvas.drawLine(xG, 0f, xG, h, gridPaint); xG += 25f }
            var yG = 0f
            while (yG < h) { canvas.drawLine(0f, yG, w, yG, gridPaint); yG += 25f }

            // Synthetic waveform matching Screenshot 4 (VEB - Couplet red highlight & green dots)
            val pathNormal1 = Path()
            val pathEctopic = Path()
            val pathNormal2 = Path()

            // Beat 1 (Normal Green)
            pathNormal1.moveTo(0f, midY)
            pathNormal1.lineTo(w * 0.05f, midY - 20f)
            pathNormal1.lineTo(w * 0.10f, midY + 120f) // S
            pathNormal1.lineTo(w * 0.14f, midY - 180f) // R Peak
            pathNormal1.lineTo(w * 0.18f, midY + 40f)
            pathNormal1.lineTo(w * 0.25f, midY)
            canvas.drawPath(pathNormal1, normalBeatPaint)
            canvas.drawCircle(w * 0.14f, midY - 190f, 10f, normalDotPaint)

            // Beat 2 (Normal Green)
            val p2 = Path()
            p2.moveTo(w * 0.25f, midY)
            p2.lineTo(w * 0.28f, midY - 20f)
            p2.lineTo(w * 0.32f, midY + 120f)
            p2.lineTo(w * 0.36f, midY - 180f)
            p2.lineTo(w * 0.40f, midY + 40f)
            p2.lineTo(w * 0.45f, midY)
            canvas.drawPath(p2, normalBeatPaint)
            canvas.drawCircle(w * 0.36f, midY - 190f, 10f, normalDotPaint)

            // Ectopic Couplet Beat 1 & 2 (Tall Red Highlights)
            pathEctopic.moveTo(w * 0.45f, midY)
            pathEctopic.lineTo(w * 0.48f, midY + 220f) // Deep dip
            pathEctopic.lineTo(w * 0.54f, midY - 320f) // Couplet R1
            pathEctopic.lineTo(w * 0.60f, midY + 140f)
            pathEctopic.lineTo(w * 0.65f, midY - 380f) // Couplet R2
            pathEctopic.lineTo(w * 0.72f, midY + 40f)
            pathEctopic.lineTo(w * 0.80f, midY)
            canvas.drawPath(pathEctopic, ectopicBeatPaint)
            canvas.drawCircle(w * 0.54f, midY - 335f, 12f, ectopicDotPaint)
            canvas.drawCircle(w * 0.65f, midY - 395f, 12f, ectopicDotPaint)

            // Beat 4 (Normal Green)
            pathNormal2.moveTo(w * 0.80f, midY)
            pathNormal2.lineTo(w * 0.84f, midY - 20f)
            pathNormal2.lineTo(w * 0.88f, midY + 120f)
            pathNormal2.lineTo(w * 0.92f, midY - 180f)
            pathNormal2.lineTo(w * 0.96f, midY + 40f)
            pathNormal2.lineTo(w, midY)
            canvas.drawPath(pathNormal2, normalBeatPaint)
            canvas.drawCircle(w * 0.92f, midY - 190f, 10f, normalDotPaint)
        }
    }
}
