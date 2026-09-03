package com.kutluoglu.prayer_widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetReceiver::class.java)) } returns intArrayOf(1)
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetMediumReceiver::class.java)) } returns intArrayOf(2, 3)
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetLargeReceiver::class.java)) } returns intArrayOf(4)

        assertThat(hasAnyWidget(context, manager)).isTrue()
        verify(exactly = 1) { manager.getAppWidgetIds(any()) }
    }

    @Test
    fun `hasAnyWidget is true when only one provider has instances`() {
        val manager = mockk<AppWidgetManager>(relaxed = true)
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetReceiver::class.java)) } returns intArrayOf()
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetMediumReceiver::class.java)) } returns intArrayOf(1)
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetLargeReceiver::class.java)) } returns intArrayOf()

        assertThat(hasAnyWidget(context, manager)).isTrue()
        verify(exactly = 2) { manager.getAppWidgetIds(any()) }
    }

    @Test
    fun `hasAnyWidget is false when no provider has instances`() {
        val manager = mockk<AppWidgetManager>(relaxed = true)
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetReceiver::class.java)) } returns intArrayOf()
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetMediumReceiver::class.java)) } returns intArrayOf()
        every { manager.getAppWidgetIds(ComponentName(context, PrayerWidgetLargeReceiver::class.java)) } returns intArrayOf()

        assertThat(hasAnyWidget(context, manager)).isFalse()
        verify(exactly = 3) { manager.getAppWidgetIds(any()) }
    }
}
