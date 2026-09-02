package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kutluoglu.prayer_notifications.domain.SpecialDay
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerNotificationManagerSpecialDayTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = PrayerNotificationManager(context)

    @Test
    fun `all special days have a non-empty localized title`() {
        SpecialDay.entries.forEach { day ->
            val title = manager.specialDayTitle(day)
            assertFalse("Missing title for $day", title.isBlank())
        }
    }
}
