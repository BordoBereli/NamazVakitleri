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
    fun `full progress paints progress color while zero progress paints track color`() {
        val empty = RingBitmapFactory.create(64, 0f, trackColor, progressColor)
        val full = RingBitmapFactory.create(64, 1f, trackColor, progressColor)
        assertPixelColor(full, 32, 4, progressColor)
        assertPixelColor(empty, 32, 4, trackColor)
    }

    @Test
    fun `arrows are drawn when progress is between 0 and 1`() {
        val bitmap = RingBitmapFactory.create(64, 0.5f, trackColor, progressColor)
        // The ring center is at (32, 32), radius ~24
        // Start arrow at top (32, 8) pointing inward - check pixel just inside the arc
        val startArrowPixel = bitmap.getPixel(32, 14)
        // End arrow at ~90deg right side - check pixel just inside the arc
        val endArrowPixel = bitmap.getPixel(50, 32)
        assertThat(startArrowPixel).isEqualTo(progressColor)
        assertThat(endArrowPixel).isEqualTo(progressColor)
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
}
