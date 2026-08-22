package com.kutluoglu.prayer_notifications.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationSettingsDataStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun freshStore(): NotificationSettingsDataStore =
        NotificationSettingsDataStore.create(context, "test_notif_${System.nanoTime()}")

    @Test
    fun `defaults are sensible`() = runTest {
        val store = freshStore()
        val settings = store.getSettings()
        assertThat(settings.enabled).isFalse()
        assertThat(settings.prayerToggles["Fajr"]).isTrue()
        assertThat(settings.prePrayerMinutes).isEqualTo(15)
    }

    @Test
    fun `updateEnabled persists`() = runTest {
        val store = freshStore()
        store.updateEnabled(true)
        assertThat(store.getSettings().enabled).isTrue()
    }

    @Test
    fun `updatePrayerToggle persists per prayer`() = runTest {
        val store = freshStore()
        store.updatePrayerToggle("Fajr", false)
        val settings = store.getSettings()
        assertThat(settings.prayerToggles["Fajr"]).isFalse()
        assertThat(settings.prayerToggles["Dhuhr"]).isTrue()
    }

    @Test
    fun `updatePrePrayerReminder persists minutes`() = runTest {
        val store = freshStore()
        store.updatePrePrayerReminder(true, 30)
        val settings = store.getSettings()
        assertThat(settings.prePrayerReminderEnabled).isTrue()
        assertThat(settings.prePrayerMinutes).isEqualTo(30)
    }
}
