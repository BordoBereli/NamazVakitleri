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
class NotificationSettingsDataStoreAdhanStyleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun freshStore(): NotificationSettingsDataStore =
        NotificationSettingsDataStore.create(context, "adhan_style_test_${System.nanoTime()}")

    @Test
    fun `adhan styles default to empty map`() = runTest {
        val store = freshStore()
        assertThat(store.getSettings().adhanStyles).isEmpty()
    }

    @Test
    fun `adhan style persists per prayer`() = runTest {
        val store = freshStore()
        store.updateAdhanStyle("Fajr", "makkah")
        assertThat(store.getSettings().adhanStyles).isEqualTo(mapOf("Fajr" to "makkah"))
    }
}
