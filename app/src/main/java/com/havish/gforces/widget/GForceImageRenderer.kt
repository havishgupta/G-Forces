package com.havish.gforces.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import kotlin.math.sqrt

object GForceImageRenderer {
    fun render(width: Int, height: Int, xGs: Float, yGs: Float, maxG: Float = 1.5f): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background is handled by widget layout, or we can draw it here
        canvas.drawColor(Color.parseColor("#1A1A1A"))

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(width, height) / 2f * 0.9f // 90% of half width

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        // Draw concentric rings
        val ringCount = 3
        for (i in 1..ringCount) {
            val ringRadius = radius * (i / ringCount.toFloat())
            if (i < ringCount) {
                paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            } else {
                paint.pathEffect = null
            }
            canvas.drawCircle(centerX, centerY, ringRadius, paint)
        }

        // Crosshairs
        paint.pathEffect = null
        paint.strokeWidth = 2f
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, paint)
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint)

        // Calculate dot position
        val normalizedX = (xGs / maxG).coerceIn(-1f, 1f)
        val normalizedY = (yGs / maxG).coerceIn(-1f, 1f)
        
        val dotX = centerX + (normalizedX * radius)
        val dotY = centerY - (normalizedY * radius)

        // Draw Dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.FILL
        }
        canvas.drawCircle(dotX, dotY, 16f, dotPaint)

        // Text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val totalG = sqrt(xGs * xGs + yGs * yGs)
        val gText = String.format("%.2f G", totalG)
        canvas.drawText(gText, centerX, height - 10f, textPaint)

        return bitmap
    }
}
