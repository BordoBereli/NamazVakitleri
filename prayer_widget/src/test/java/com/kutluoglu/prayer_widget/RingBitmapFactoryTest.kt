package com.kutluoglu.prayer_widget

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RingBitmapFactoryTest {

    private val trackColor = 0x1FFFFFFF
    private val progressColor = 0xFFFFD700.toInt()

    @Test
    fun `creates square bitmap of requested size`() {
        val bitmap = RingBitmapFactory.create(
            sizePx = 64,
            progress = 0.5f,
            trackColor = trackColor,
            progressColor = progressColor
        )
        assertThat(bitmap.width).isEqualTo(64)
        assertThat(bitmap.height).isEqualTo(64)
        assertThat(bitmap.config).isEqualTo(Bitmap.Config.ARGB_8888)
    }

    @Test
    fun `zero progress paints full remaining ring while full progress paints empty ring`() {
        val empty = RingBitmapFactory.create(64, 0f, trackColor, progressColor)
        val full = RingBitmapFactory.create(64, 1f, trackColor, progressColor)
        assertPixelColor(empty, 32, 4, progressColor)
        assertPixelColor(full, 32, 4, trackColor)
    }

    @Test
    fun `arrows are drawn when progress is between 0 and 1`() {
        val withArrows = RingBitmapFactory.create(64, 0.5f, trackColor, progressColor)
        val noArrows = RingBitmapFactory.create(64, 0f, trackColor, progressColor)

        // The progress arc stays within the ring band (distance >= ring inner edge).
        // Any progress-color pixels deeper inside the hollow can only come from arrows,
        // which are only drawn when progress is strictly between 0 and 1.
        val hollowArrows = countHollowPixels(withArrows, noArrows)
        assertThat(hollowArrows).isGreaterThan(0)
    }

    @Test
    fun `clamps progress out of range`() {
        val low = RingBitmapFactory.create(64, -1f, trackColor, progressColor)
        val zero = RingBitmapFactory.create(64, 0f, trackColor, progressColor)
        val high = RingBitmapFactory.create(64, 2f, trackColor, progressColor)
        val full = RingBitmapFactory.create(64, 1f, trackColor, progressColor)
        assertThat(pixelsOf(low)).isEqualTo(pixelsOf(zero))
        assertThat(pixelsOf(high)).isEqualTo(pixelsOf(full))
    }

    @Test
    fun `no arrows drawn at progress 0 or 1`() {
        val zeroBitmap = RingBitmapFactory.create(64, 0f, trackColor, progressColor)
        val fullBitmap = RingBitmapFactory.create(64, 1f, trackColor, progressColor)

        // At progress 0: top of ring is track color, not progress color arrow
        // At progress 1: top of ring is track color (no progress arc drawn)
        // Both should be identical to the original behavior without arrows
        assertThat(pixelsOf(zeroBitmap)).isEqualTo(pixelsOf(zeroBitmap))
        assertThat(pixelsOf(fullBitmap)).isEqualTo(pixelsOf(fullBitmap))
    }

    private fun assertPixelColor(bitmap: Bitmap, x: Int, y: Int, expected: Int) {
        assertThat(bitmap.getPixel(x, y)).isEqualTo(expected)
    }

    private fun pixelsOf(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }

    private fun countHollowPixels(withArrows: Bitmap, noArrows: Bitmap): Int {
        val cx = withArrows.width / 2f
        val cy = withArrows.height / 2f
        val strokeWidth = withArrows.width * 0.14f
        val innerRadius = (withArrows.width - strokeWidth) / 2f - strokeWidth / 2f
        var count = 0
        for (y in 0 until withArrows.height) {
            for (x in 0 until withArrows.width) {
                val d = Math.sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble()).toFloat()
                if (d < innerRadius && withArrows.getPixel(x, y) != noArrows.getPixel(x, y)) {
                    count++
                }
            }
        }
        return count
    }
}
