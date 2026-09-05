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

        // The ring shows the REMAINING time: full at the start of the period,
        // depleting clockwise as time passes. The arc is anchored at the top
        // (next prayer) and extends to the right side. `progress` is the elapsed fraction.
        val clamped = progress.coerceIn(0f, 1f)
        val remainingStart = -90f
        val remainingSweep = 360f * (1f - clamped)

        paint.color = progressColor
        canvas.drawArc(rect, remainingStart, remainingSweep, false, paint)

        if (clamped > 0f && clamped < 1f) {
            val centerX = size / 2f
            val centerY = size / 2f
            val radius = (size - strokeWidth) / 2f
            val arrowHeight = strokeWidth * 0.65f
            val arrowBaseWidth = strokeWidth * 0.8f

            paint.style = Paint.Style.FILL
            drawPathArrows(
                canvas, centerX, centerY, radius,
                remainingStart, remainingStart + remainingSweep,
                arrowHeight, arrowBaseWidth, paint
            )
            paint.style = Paint.Style.STROKE
        }

        return bitmap
    }

    private fun drawPathArrows(
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
        drawPathArrow(canvas, centerX, centerY, radius, startAngleDeg, arrowHeight, arrowBaseWidth, paint, forward = false)
        drawPathArrow(canvas, centerX, centerY, radius, endAngleDeg, arrowHeight, arrowBaseWidth, paint, forward = true)
    }

    private fun drawPathArrow(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        angleDeg: Float,
        arrowHeight: Float,
        arrowBaseWidth: Float,
        paint: Paint,
        forward: Boolean
    ) {
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val tipX = centerX + radius * Math.cos(angleRad.toDouble()).toFloat()
        val tipY = centerY + radius * Math.sin(angleRad.toDouble()).toFloat()

        // Tangent direction of the arc at this angle (clockwise in canvas coords).
        val tanX = -Math.sin(angleRad.toDouble()).toFloat()
        val tanY = Math.cos(angleRad.toDouble()).toFloat()
        val dirX = if (forward) tanX else -tanX
        val dirY = if (forward) tanY else -tanY

        val perpX = -dirY
        val perpY = dirX

        val baseCenterX = tipX + dirX * arrowHeight
        val baseCenterY = tipY + dirY * arrowHeight

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
