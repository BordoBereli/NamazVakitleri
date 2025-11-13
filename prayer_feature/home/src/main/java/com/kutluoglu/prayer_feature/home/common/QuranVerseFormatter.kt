package com.kutluoglu.prayer_feature.home.common

import android.content.Context
import android.util.Log
import com.kutluoglu.prayer.model.quran.AyahData
import org.koin.core.annotation.Factory

/**
 * Created by F.K. on 13.11.2025.
 *
 */

@Factory
class QuranVerseFormatter {
    fun getLocalizedNameOf(quranVerse: AyahData, context: Context, ): String {
        val englishName = quranVerse.surah.englishName
        return try {
            getLocalizedSurahName(context, englishName)
        } catch(e: Exception) {
            Log.e("QuranVerseFormatter", "$englishName is got error with ${e.message}")
            englishName
        }
    }

    /**
     * A function that provides a localized name for a given Surah.
     * It converts the English name (e.g., "Al-Fatihah") into a resource identifier
     * (e.g., "surah_al_fatihah") and fetches the corresponding string for the current locale.
     *
     * @param englishName The English name of the Surah from the data model.
     * @return The localized Surah name if a resource is found, otherwise falls back to the original English name.
     */

    private fun getLocalizedSurahName(context: Context, englishName: String): String {
        // 1. Convert the English name to a valid resource key format.
        // "Al-Fatihah" -> "surah_al_fatihah"
        // "An-Nas" -> "surah_an_nas"
        val resourceKey = "surah_${englishName.replace('-', '_').lowercase()}"

        // 2. Get the resource ID from the generated key.
        val resourceId = context.resources.getIdentifier(
            resourceKey,
            "string",
            context.packageName
        )

        // 3. If the resource ID is valid (not 0), return the localized string.
        //    Otherwise, fallback gracefully to the original English name.
        return if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            englishName // Fallback
        }
    }
}