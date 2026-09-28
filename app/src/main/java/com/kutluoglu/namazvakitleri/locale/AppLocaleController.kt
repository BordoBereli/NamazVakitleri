package com.kutluoglu.namazvakitleri.locale

import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate

/**
 * Maps the app's language codes to the Android per-app language API.
 *
 * "system" maps to an empty locale list (follow the device language); any other
 * value is treated as a BCP-47 language tag (e.g. "tr", "pt-BR").
 */
class AppLocaleController {

    fun toLocaleList(languageCode: String): LocaleListCompat =
        if (languageCode == SYSTEM_LANGUAGE) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }

    fun toLanguageCode(locales: LocaleListCompat): String =
        if (locales.isEmpty) SYSTEM_LANGUAGE else locales[0]!!.toLanguageTag()

    fun setApplicationLocales(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(toLocaleList(languageCode))
    }

    fun getApplicationLocales(): String =
        toLanguageCode(AppCompatDelegate.getApplicationLocales())

    companion object {
        const val SYSTEM_LANGUAGE = "system"
    }
}
