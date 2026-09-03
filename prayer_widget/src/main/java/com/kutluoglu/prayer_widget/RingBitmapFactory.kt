package com.kutluoglu.prayer_widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
        return bitmap
    }
}
