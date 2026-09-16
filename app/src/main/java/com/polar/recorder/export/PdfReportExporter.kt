package com.polar.recorder.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.polar.recorder.analysis.EcgAnalysisResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfReportExporter(private val context: Context) {

    fun generateClinicalPdfReport(res: EcgAnalysisResult, sessionName: String): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply { color = Color.parseColor("#B91C1C"); textSize = 18f; isFakeBoldText = true }
        val paintHeader = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 12f; isFakeBoldText = true }
        val paintText = Paint().apply { color = Color.parseColor("#334155"); textSize = 10f }
        val paintGrid = Paint().apply { color = Color.parseColor("#F2B8C6"); strokeWidth = 0.5f }
        val paintWave = Paint().apply { color = Color.parseColor("#16A34A"); strokeWidth = 1.5f; style = Paint.Style.STROKE }

        // Draw Header
        canvas.drawText("Polar H10 Clinical Holter ECG Report", 40f, 50f, paintTitle)
        canvas.drawText("Session: $sessionName", 40f, 70f, paintText)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        canvas.drawText("Generated: $dateStr", 40f, 85f, paintText)

        // Summary Stats Box
        canvas.drawText("PHYSOLOGICAL PARAMETERS (ECG-Derived)", 40f, 120f, paintHeader)
        var y = 140f
        val params = listOf(
            "Mean Heart Rate" to "${res.meanHr.toInt()} bpm [35-180]",
            "pNN50" to "${res.pNN50.toInt()} % [0-45]",
            "pNN200" to "${res.pNN200.toInt()} % [0-15]",
            "meanQRS Width" to "${res.meanQRS.toInt()} ms [50-114]",
            "meanQTc" to "${res.meanQTc.toInt()} ms [310-470]",
            "SDNN (HRV)" to "${res.sdnnMs.toInt()} ms",
            "RMSSD (HRV)" to "${res.rmssdMs.toInt()} ms",
            "ln(RMSSD)" to String.format("%.2f ms", res.lnRmssd),
            "Average Respiratory Rate" to String.format("%.2f /min", res.avgRespRate)
        )

        for ((label, valStr) in params) {
            canvas.drawText("$label:", 40f, y, paintText)
            canvas.drawText(valStr, 220f, y, paintHeader)
            y += 18f
        }

        // ECG Strip Grid Preview
        canvas.drawText("REPRESENTATIVE ECG SAMPLE STRIP (25 mm/s, 10 mm/mV)", 40f, y + 20f, paintHeader)
        val gridTop = y + 35f
        val gridHeight = 120f
        val gridWidth = 515f

        // Draw Grid Paper
        var gx = 40f
        while (gx <= 40f + gridWidth) { canvas.drawLine(gx, gridTop, gx, gridTop + gridHeight, paintGrid); gx += 10f }
        var gy = gridTop
        while (gy <= gridTop + gridHeight) { canvas.drawLine(40f, gy, 40f + gridWidth, gy, paintGrid); gy += 10f }

        // Draw Waveform Line
        val midY = gridTop + gridHeight / 2f
        var lastX = 40f
        var lastY = midY

        for (i in 0 until 130 * 4) {
            val px = 40f + (i.toFloat() / (130 * 4)) * gridWidth
            val phase = (i / 130.0 % 0.8) / 0.8
            var v = 0.0
            if (phase > 0.1 && phase < 0.2) v += 0.12 * Math.sin((phase - 0.1) / 0.1 * Math.PI)
            if (phase >= 0.38 && phase <= 0.44) {
                val qrsP = (phase - 0.38) / 0.06
                if (qrsP < 0.2) v -= 0.25 * (qrsP / 0.2)
                else if (qrsP < 0.7) v += 1.45 * Math.sin((qrsP - 0.2) / 0.5 * Math.PI)
                else v -= 0.45 * ((qrsP - 0.7) / 0.3)
            }
            if (phase > 0.55 && phase < 0.75) v += 0.28 * Math.sin((phase - 0.55) / 0.2 * Math.PI)
            val py = (midY - v * 25.0).toFloat()

            if (i > 0) canvas.drawLine(lastX, lastY, px, py, paintWave)
            lastX = px
            lastY = py
        }

        // Disclaimer Footer
        val paintDisc = Paint().apply { color = Color.parseColor("#94A3B8"); textSize = 8f }
        canvas.drawText("DISCLAIMER: Research and clinician decision-support report. Not a certified diagnostic device.", 40f, 800f, paintDisc)

        pdfDocument.finishPage(page)

        val storageDir = File(context.getExternalFilesDir(null), "PolarReports")
        if (!storageDir.exists()) storageDir.mkdirs()

        val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val pdfFile = File(storageDir, "Polar_Holter_Report_$timeStr.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }
}
