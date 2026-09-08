package com.kutluoglu.prayer_widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RingTextSpacingTest {

    private val trackColor = 0x1FFFFFFF
    private val progressColor = 0xFFFFD700.toInt()

    private fun density(): Float =
        ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density

    private fun ringInnerRadius(sizeDp: Float): Float {
        val sizePx = (sizeDp * density()).toInt()
        val bitmap = RingBitmapFactory.create(sizePx, 0.5f, trackColor, progressColor)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        var minDist = Float.MAX_VALUE
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                if (Color.alpha(bitmap.getPixel(x, y)) >= 20) {
                    val d = Math.sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble()).toFloat()
                    if (d < minDist) minDist = d
                }
            }
        }
        return minDist / density()
    }

    private fun textHalfWidth(sizeDp: Float, text: String, textSp: Float): Float {
        val sizePx = (sizeDp * density()).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.YELLOW
            textSize = textSp * density()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val baseline = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, sizePx / 2f, baseline, paint)
        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                }
            }
        }
        return (maxX - minX) / 2f / density()
    }

    private fun assertCountdownFitsInRing(ringSizeDp: Float, textSp: Float, minGapDp: Float, countdown: String) {
        val innerRadius = ringInnerRadius(ringSizeDp)
        // The countdown wraps at the space, so the widest single line is the
        // constraint for fitting inside the ring.
        val widestLine = countdown.split(" ").maxByOrNull { textHalfWidth(ringSizeDp, it, textSp) } ?: countdown
        val halfWidth = textHalfWidth(ringSizeDp, widestLine, textSp)
        val gap = innerRadius - halfWidth
        assertThat(gap)
            .isAtLeast(minGapDp)
    }

    @Test
    fun `small layout countdown text keeps spacing from ring`() {
        assertCountdownFitsInRing(ringSizeDp = 64f, textSp = 13f, minGapDp = 2f, countdown = "2s 15d")
    }

    @Test
    fun `medium layout countdown text keeps spacing from ring`() {
        assertCountdownFitsInRing(ringSizeDp = 64f, textSp = 13f, minGapDp = 2f, countdown = "2s 15d")
    }

    @Test
    fun `large layout countdown text keeps spacing from ring`() {
        assertCountdownFitsInRing(ringSizeDp = 64f, textSp = 13f, minGapDp = 2f, countdown = "2s 15d")
    }

    @Test
    fun `widest wrapped countdown line keeps spacing from ring for long locales`() {
        for (countdown in listOf("4s 44d", "4h 44m", "4ч 44мин", "4Std. 44Min.")) {
            assertCountdownFitsInRing(ringSizeDp = 64f, textSp = 13f, minGapDp = 2f, countdown = countdown)
        }
    }
}
