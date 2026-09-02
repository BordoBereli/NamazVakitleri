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
class NotificationSettingsDataStoreRamadanTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun freshStore(): NotificationSettingsDataStore =
        NotificationSettingsDataStore.create(context, "ramadan_test_${System.nanoTime()}")

    @Test
    fun `ramadan enabled defaults to true`() = runTest {
        val store = freshStore()
        assertThat(store.getSettings().ramadanEnabled).isTrue()
    }

    @Test
    fun `ramadan enabled persists round trip`() = runTest {
        val store = freshStore()
        store.updateRamadanEnabled(false)
        assertThat(store.getSettings().ramadanEnabled).isFalse()
    }
}
