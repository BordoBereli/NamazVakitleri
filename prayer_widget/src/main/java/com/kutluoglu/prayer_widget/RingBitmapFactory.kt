package com.kutluoglu.prayer_widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

object RingBitmapFactory {

    fun create(sizePx: Int, progress: Float, trackColor: Int, progressColor: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val strokeWidth = (size * 0.14f).coerceAtLeast(4f)
        val inset = strokeWidth / 2f
        val rect = RectF(inset, inset, size - inset, size - inset)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            isAntiAlias = true
        }
        paint.color = trackColor
        canvas.drawArc(rect, 0f, 360f, false, paint)
        paint.color = progressColor
        canvas.drawArc(rect, -90f, 360f * progress.coerceIn(0f, 1f), false, paint)

        if (progress > 0f && progress < 1f) {
            val centerX = size / 2f
            val centerY = size / 2f
            val radius = (size - strokeWidth) / 2f
            val startAngle = -90f
            val endAngle = -90f + 360f * progress
            val arrowHeight = strokeWidth * 1.5f
            val arrowBaseWidth = strokeWidth * 0.8f

            paint.style = Paint.Style.FILL
            drawInwardArrows(canvas, centerX, centerY, radius, startAngle, endAngle, arrowHeight, arrowBaseWidth, paint)
            paint.style = Paint.Style.STROKE
        }

        return bitmap
    }

    private fun drawInwardArrows(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngleDeg: Float,
        endAngleDeg: Float,
        arrowHeight: Float,
        arrowBaseWidth: Float,
        paint: Paint
    ) {
        val angles = listOf(startAngleDeg, endAngleDeg)
        for (angleDeg in angles) {
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val tipX = centerX + radius * Math.cos(angleRad.toDouble()).toFloat()
            val tipY = centerY + radius * Math.sin(angleRad.toDouble()).toFloat()

            val inwardX = centerX - tipX
            val inwardY = centerY - tipY
            val inwardLen = Math.sqrt((inwardX * inwardX + inwardY * inwardY).toDouble()).toFloat()
            val unitInwardX = inwardX / inwardLen
            val unitInwardY = inwardY / inwardLen

            val perpX = -unitInwardY
            val perpY = unitInwardX

            val baseCenterX = tipX + unitInwardX * arrowHeight
            val baseCenterY = tipY + unitInwardY * arrowHeight

            val path = Path().apply {
                moveTo(tipX, tipY)
                lineTo(
                    baseCenterX + perpX * arrowBaseWidth / 2f,
                    baseCenterY + perpY * arrowBaseWidth / 2f
                )
                lineTo(
                    baseCenterX - perpX * arrowBaseWidth / 2f,
                    baseCenterY - perpY * arrowBaseWidth / 2f
                )
                close()
            }
            canvas.drawPath(path, paint)
        }
    }
}
