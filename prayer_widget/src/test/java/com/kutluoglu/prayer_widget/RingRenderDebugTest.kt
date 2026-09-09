package com.kutluoglu.prayer_widget

import android.graphics.Bitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RingRenderDebugTest {

    @Test
    fun `dump chevron region as ascii`() {
        val trackColor = 0x7FFFFFFF
        val progressColor = 0xFFFFD700.toInt()
        val bitmap = RingBitmapFactory.create(256, 0.5f, trackColor, progressColor)

        val cx = 128
        val cy = 128
        val radius = (256 - 256 * 0.14f) / 2f
        val tipY = (cy + radius).toInt()

        val x0 = cx - 55
        val x1 = cx + 55
        val y0 = tipY - 40
        val y1 = minOf(tipY + 40, 255)
        for (y in y0..y1) {
            val sb = StringBuilder()
            for (x in x0..x1) {
                val p = bitmap.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val a = (p shr 24) and 0xFF
                sb.append(
                    when {
                        a < 10 -> '.'
                        r > 200 && g > 150 && b < 100 -> 'G' // gold
                        r > 200 && g > 200 && b > 200 -> 'W' // white-ish (track)
                        else -> '?'
                    }
                )
            }
            println("%3d %s".format(y, sb))
        }
    }
}
