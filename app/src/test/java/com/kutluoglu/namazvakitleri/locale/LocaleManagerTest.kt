package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import android.content.res.Configuration
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import com.kutluoglu.prayer_settings.domain.model.Settings
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.Locale

class LocaleManagerTest {

    private val manager = LocaleManager()
    private val originalLocale = Locale.getDefault()

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `default language is system`() {
        assertThat(manager.languageCode).isEqualTo("system")
    }

    @Test
    fun `setLanguage updates the holder synchronously`() {
        manager.setLanguage("ar")
        assertThat(manager.languageCode).isEqualTo("ar")
    }

    @Test
    fun `resolveLocale returns device locale for system`() {
        val deviceLocale = Locale("fr")
        assertThat(manager.resolveLocale(deviceLocale)).isEqualTo(deviceLocale)
    }

    @Test
    fun `resolveLocale returns explicit locale for override`() {
        manager.setLanguage("de")
        assertThat(manager.resolveLocale(Locale("fr")).language).isEqualTo("de")
    }

    @Test
    fun `applyLocale returns context unchanged for system`() {
        val context = mockk<Context>()
        assertThat(manager.applyLocale(context)).isSameInstanceAs(context)
    }

    @Test
    fun `applyLocale wraps context for explicit language`() {
        manager.setLanguage("ar")
        val context = mockk<Context>()
        val config = Configuration()
        every { context.resources } returns mockk()
        every { context.resources.configuration } returns config
        every { context.createConfigurationContext(any()) } returns mockk()
        val result = manager.applyLocale(context)
        assertThat(result).isNotNull()
    }

    @Test
    fun `setLanguage updates Locale default for explicit language`() {
        manager.setLanguage("de")
        assertThat(Locale.getDefault().language).isEqualTo("de")
    }

    @Test
    fun `setLanguage system restores device locale`() {
        manager.setLanguage("de")
        manager.setLanguage("system")
        assertThat(Locale.getDefault().language).isEqualTo(originalLocale.language)
    }

    @Test
    fun `applyPersistedLocale sets language from data store and applies it`() = runTest {
        val dataStore = mockk<SettingsDataStore>()
        coEvery { dataStore.getSettings() } returns Settings(language = "de")
        val context = mockk<Context>()
        val config = Configuration()
        every { context.resources } returns mockk()
        every { context.resources.configuration } returns config
        every { context.createConfigurationContext(any()) } returns mockk()

        val result = manager.applyPersistedLocale(context, dataStore)

        assertThat(manager.languageCode).isEqualTo("de")
        assertThat(result).isNotNull()
    }
}
