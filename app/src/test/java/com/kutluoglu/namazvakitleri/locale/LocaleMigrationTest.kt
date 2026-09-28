package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class LocaleMigrationTest {

    private val controller = mockk<AppLocaleController>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `migrates non-system language to system API`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "de")

        LocaleMigration(context, controller, dataStore).migrateIfNeeded()

        coVerify { controller.setApplicationLocales("de") }
    }

    @Test
    fun `skips system language`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "system")

        LocaleMigration(context, controller, dataStore).migrateIfNeeded()

        coVerify(exactly = 0) { controller.setApplicationLocales(any()) }
    }

    @Test
    fun `runs only once - flag prevents second migration`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "de")

        val migration = LocaleMigration(context, controller, dataStore)
        migration.migrateIfNeeded()
        migration.migrateIfNeeded()

        coVerify(exactly = 1) { controller.setApplicationLocales(any()) }
    }

    @Test
    fun `skips when system locales already match datastore language`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "tr")
        every { controller.getApplicationLocales() } returns "tr"

        LocaleMigration(context, controller, dataStore).migrateIfNeeded()

        coVerify(exactly = 0) { controller.setApplicationLocales(any()) }
    }

    @Test
    fun `never throws on datastore failure`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } throws RuntimeException("disk error")

        LocaleMigration(context, controller, dataStore).migrateIfNeeded()

        coVerify(exactly = 0) { controller.setApplicationLocales(any()) }
    }

    @Test
    fun `persists flag and skips on fresh instance after migration`() = runTest {
        val realContext = Robolectric.buildActivity(android.app.Activity::class.java).create().get()
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns settingsWith(language = "de")

        LocaleMigration(realContext, controller, dataStore).migrateIfNeeded()

        val prefs = realContext.getSharedPreferences("locale_migration", Context.MODE_PRIVATE)
        assertThat(prefs.getBoolean("locale_migrated", false)).isTrue()

        val freshController = mockk<AppLocaleController>(relaxed = true)
        LocaleMigration(realContext, freshController, dataStore).migrateIfNeeded()

        coVerify(exactly = 0) { freshController.setApplicationLocales(any()) }
    }

    private fun settingsWith(language: String): com.kutluoglu.prayer_settings.domain.model.Settings =
        com.kutluoglu.prayer_settings.domain.model.Settings(language = language)
}
