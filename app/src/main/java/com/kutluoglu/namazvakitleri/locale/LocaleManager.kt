package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import android.content.res.Configuration
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class LocaleManager {

    @Volatile
    var languageCode: String = SYSTEM_LANGUAGE
        private set

    fun setLanguage(code: String) {
        languageCode = code
        Locale.setDefault(resolveLocale())
    }

    fun resolveLocale(deviceLocale: Locale = LocaleManager.deviceLocale): Locale {
        return if (languageCode == SYSTEM_LANGUAGE) {
            deviceLocale
        } else {
            Locale.forLanguageTag(languageCode)
        }
    }

    fun applyLocale(context: Context): Context {
        if (languageCode == SYSTEM_LANGUAGE) return context
        val locale = resolveLocale()
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyPersistedLocale(context: Context, settingsDataStore: SettingsDataStore): Context {
        val language = runCatching { runBlocking { settingsDataStore.getSettings().language } }
            .getOrDefault(SYSTEM_LANGUAGE)
        setLanguage(language)
        return applyLocale(context)
    }

    companion object {
        const val SYSTEM_LANGUAGE = "system"

        private val deviceLocale: Locale = Locale.getDefault()
    }
}
