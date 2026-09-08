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
            val dotRadius = strokeWidth * 0.45f
            val arrowStrokeWidth = strokeWidth * 0.2f

            // Dot at the start of the ring (filled).
            paint.style = Paint.Style.FILL
            drawDot(canvas, centerX, centerY, radius, remainingStart, dotRadius, paint)

            // Arrow at the end of the ring (chevron of two lines, not a triangle).
            // Drawn in the progress color; it stays visible because the arms sit
            // over the track (not the gold arc) in this orientation.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = arrowStrokeWidth
            drawChevronArrow(
                canvas, centerX, centerY, radius,
                remainingStart + remainingSweep,
                arrowHeight, strokeWidth, paint
            )
            paint.strokeWidth = strokeWidth
        }

        return bitmap
    }

    private fun drawDot(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        angleDeg: Float,
        dotRadius: Float,
        paint: Paint
    ) {
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val dotX = centerX + radius * Math.cos(angleRad.toDouble()).toFloat()
        val dotY = centerY + radius * Math.sin(angleRad.toDouble()).toFloat()
        canvas.drawCircle(dotX, dotY, dotRadius, paint)
    }

    private fun drawChevronArrow(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        angleDeg: Float,
        arrowHeight: Float,
        ringStrokeWidth: Float,
        paint: Paint
    ) {
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val tipX = centerX + radius * Math.cos(angleRad.toDouble()).toFloat()
        val tipY = centerY + radius * Math.sin(angleRad.toDouble()).toFloat()

        // Tangent direction of the arc at this angle (clockwise in canvas coords).
        val tanX = -Math.sin(angleRad.toDouble()).toFloat()
        val tanY = Math.cos(angleRad.toDouble()).toFloat()

        val perpX = -tanY
        val perpY = tanX

        val backX = tipX + tanX * arrowHeight
        val backY = tipY + tanY * arrowHeight

        // Place the arm endpoints exactly on the arc band's inner and outer
        // edges so the arrow's points touch the ring path.
        val backRadius = Math.hypot((backX - centerX).toDouble(), (backY - centerY).toDouble()).toFloat()
        val innerOffset = backRadius - (radius - ringStrokeWidth / 2f)
        val outerOffset = (radius + ringStrokeWidth / 2f) - backRadius

        val innerX = backX + perpX * innerOffset
        val innerY = backY + perpY * innerOffset
        val outerX = backX - perpX * outerOffset
        val outerY = backY - perpY * outerOffset

        // Arc band edges at the tip's angle.
        val cosA = Math.cos(angleRad.toDouble()).toFloat()
        val sinA = Math.sin(angleRad.toDouble()).toFloat()
        val innerEdgeX = centerX + (radius - ringStrokeWidth / 2f) * cosA
        val innerEdgeY = centerY + (radius - ringStrokeWidth / 2f) * sinA
        val outerEdgeX = centerX + (radius + ringStrokeWidth / 2f) * cosA
        val outerEdgeY = centerY + (radius + ringStrokeWidth / 2f) * sinA

        // Fill the two right triangles between the ring path (arc band edges) and
        // the chevron arms' touch points so the arrow connects to the ring.
        val fillPaint = Paint(paint).apply {
            style = Paint.Style.FILL
            strokeWidth = 0f
        }
        val path = Path().apply {
            moveTo(tipX, tipY)
            lineTo(innerEdgeX, innerEdgeY)
            lineTo(innerX, innerY)
            close()
            moveTo(tipX, tipY)
            lineTo(outerEdgeX, outerEdgeY)
            lineTo(outerX, outerY)
            close()
        }
        canvas.drawPath(path, fillPaint)

        canvas.drawLine(tipX, tipY, innerX, innerY, paint)
        canvas.drawLine(tipX, tipY, outerX, outerY, paint)
    }
}
