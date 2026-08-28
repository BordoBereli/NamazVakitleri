package com.kutluoglu.prayer_notifications.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
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
        assertThat(settings.adhanEnabled).isFalse()
    }

    @Test
    fun `updateEnabled persists`() = runTest {
        val store = freshStore()
        store.updateEnabled(true)
        assertThat(store.getSettings().enabled).isTrue()
    }

    @Test
    fun `persists across store instances`() = runTest {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("test_notif_persist") }
        )
        val store = NotificationSettingsDataStore(dataStore)
        store.updateEnabled(true)

        val freshStore = NotificationSettingsDataStore(dataStore)
        assertThat(freshStore.getSettings().enabled).isTrue()
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
    fun `updatePrayerToggle round-trips disable then re-enable`() = runTest {
        val store = freshStore()
        store.updatePrayerToggle("Fajr", false)
        assertThat(store.getSettings().prayerToggles["Fajr"]).isFalse()
        assertThat(store.getSettings().prayerToggles["Dhuhr"]).isTrue()

        store.updatePrayerToggle("Fajr", true)
        val settings = store.getSettings()
        assertThat(settings.prayerToggles["Fajr"]).isTrue()
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

    @Test
    fun `adhan volume defaults to 100`() = runTest {
        val store = freshStore()
        assertThat(store.getSettings().adhanVolume).isEqualTo(100)
    }

    @Test
    fun `updateAdhanVolume persists`() = runTest {
        val store = freshStore()
        store.updateAdhanVolume(50)
        assertThat(store.getSettings().adhanVolume).isEqualTo(50)
    }
}
