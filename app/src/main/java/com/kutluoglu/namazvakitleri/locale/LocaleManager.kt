package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import org.koin.core.annotation.Single

@Single
class LocaleManager {

    @Volatile
    var languageCode: String = SYSTEM_LANGUAGE
        private set

    fun setLanguage(code: String) {
        languageCode = code
    }

    fun resolveLocale(deviceLocale: Locale = Locale.getDefault()): Locale {
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

    companion object {
        const val SYSTEM_LANGUAGE = "system"
    }
}
