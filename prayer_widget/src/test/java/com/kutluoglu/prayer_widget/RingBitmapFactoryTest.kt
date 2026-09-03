package com.kutluoglu.prayer_widget

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RingBitmapFactoryTest {

    @Test
    fun `creates square bitmap of requested size`() {
        val bitmap = RingBitmapFactory.create(
            sizePx = 64,
            progress = 0.5f,
            trackColor = 0x1FFFFFFF,
            progressColor = 0xFFFFD700.toInt()
        )
        assertThat(bitmap.width).isEqualTo(64)
        assertThat(bitmap.height).isEqualTo(64)
        assertThat(bitmap.config).isEqualTo(Bitmap.Config.ARGB_8888)
    }

    @Test
    fun `full progress differs from zero progress`() {
        val empty = RingBitmapFactory.create(64, 0f, 0x1FFFFFFF, 0xFFFFD700.toInt())
        val full = RingBitmapFactory.create(64, 1f, 0x1FFFFFFF, 0xFFFFD700.toInt())
        assertThat(empty).isNotEqualTo(full)
    }

    @Test
    fun `clamps progress out of range`() {
        val low = RingBitmapFactory.create(64, -1f, 0x1FFFFFFF, 0xFFFFD700.toInt())
        val high = RingBitmapFactory.create(64, 2f, 0x1FFFFFFF, 0xFFFFD700.toInt())
        assertThat(low).isNotNull()
        assertThat(high).isNotNull()
    }
}
