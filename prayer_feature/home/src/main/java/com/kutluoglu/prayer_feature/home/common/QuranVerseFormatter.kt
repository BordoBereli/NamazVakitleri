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
        val surahNumber = quranVerse.surah.number
        return try {
            getLocalizedSurahName(context, surahNumber, quranVerse.surah.englishName)
        } catch(e: Exception) {
            Log.e("QuranVerseFormatter", "Surah $surahNumber is got error with ${e.message}")
            quranVerse.surah.englishName
        }
    }

    /**
     * A function that provides a localized name for a given Surah.
     * It looks up the string resource keyed by the language-agnostic Surah number
     * (e.g., "surah_name_1") and fetches the corresponding string for the current locale.
     *
     * @param surahNumber The Surah number (1-114) from the data model. Stable and independent
     *                    of any translation or transliteration, so it is language agnostic.
     * @param fallback The English name of the Surah, used when no resource is found.
     * @return The localized Surah name if a resource is found, otherwise falls back to the English name.
     */

    private fun getLocalizedSurahName(context: Context, surahNumber: Int, fallback: String): String {
        // 1. Build the resource key from the stable Surah number.
        // "1" -> "surah_name_1"
        val resourceKey = "surah_name_$surahNumber"

        // 2. Get the resource ID from the generated key.
        val resourceId = context.resources.getIdentifier(
            resourceKey,
            "string",
            context.packageName
        )

        // 3. If the resource ID is valid (not 0), return the localized string.
        //    Otherwise, fallback gracefully to the English name.
        return if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            fallback
        }
    }
}