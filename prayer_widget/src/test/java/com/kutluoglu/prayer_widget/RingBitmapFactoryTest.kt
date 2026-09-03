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
    fun `clamps progress out of range`() {
        val low = RingBitmapFactory.create(64, -1f, trackColor, progressColor)
        val zero = RingBitmapFactory.create(64, 0f, trackColor, progressColor)
        val high = RingBitmapFactory.create(64, 2f, trackColor, progressColor)
        val full = RingBitmapFactory.create(64, 1f, trackColor, progressColor)
        assertThat(pixelsOf(low)).isEqualTo(pixelsOf(zero))
        assertThat(pixelsOf(high)).isEqualTo(pixelsOf(full))
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
