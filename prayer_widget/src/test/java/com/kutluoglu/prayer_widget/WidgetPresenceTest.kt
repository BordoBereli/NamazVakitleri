package com.kutluoglu.prayer_widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetPresenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `hasAnyWidget is true when any provider has instances`() {
        val manager = mockk<AppWidgetManager>(relaxed = true)
        every { manager.getAppWidgetIds(any()) } returns intArrayOf(1)

        assertThat(hasAnyWidget(context, manager)).isTrue()
    }

    @Test
    fun `hasAnyWidget is false when no provider has instances`() {
        val manager = mockk<AppWidgetManager>(relaxed = true)
        every { manager.getAppWidgetIds(any()) } returns intArrayOf()

        assertThat(hasAnyWidget(context, manager)).isFalse()
    }
}
