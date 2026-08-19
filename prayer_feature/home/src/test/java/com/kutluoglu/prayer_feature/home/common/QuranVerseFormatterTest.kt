package com.kutluoglu.prayer_feature.home.common

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QuranVerseFormatterTest {

    private val context: Context = mockk()
    private val resources: Resources = mockk()
    private val formatter = QuranVerseFormatter()

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { context.resources } returns resources
        every { context.packageName } returns "com.kutluoglu.prayer_feature.home"
    }

    @Test
    fun `getLocalizedNameOf returns localized name keyed by surah number`() {
        val verse = verseWith(englishName = "Al-Faatiha", number = 1)
        every { resources.getIdentifier("surah_name_1", "string", any()) } returns 123
        every { context.getString(123) } returns "Fâtiha"

        val result = formatter.getLocalizedNameOf(verse, context)

        assertThat(result).isEqualTo("Fâtiha")
    }

    @Test
    fun `getLocalizedNameOf falls back to english name when resource is missing`() {
        val verse = verseWith(englishName = "Al-Faatiha", number = 1)
        every { resources.getIdentifier("surah_name_1", "string", any()) } returns 0

        val result = formatter.getLocalizedNameOf(verse, context)

        assertThat(result).isEqualTo("Al-Faatiha")
    }

    @Test
    fun `getLocalizedNameOf is independent of the api english transliteration`() {
        val verse = verseWith(englishName = "Aal-i-Imraan", number = 3)
        every { resources.getIdentifier("surah_name_3", "string", any()) } returns 456
        every { context.getString(456) } returns "Âl-i İmrân"

        val result = formatter.getLocalizedNameOf(verse, context)

        assertThat(result).isEqualTo("Âl-i İmrân")
    }

    private fun verseWith(englishName: String, number: Int) = AyahData(
        text = "Bismillah...",
        surah = SurahInfo(
            englishName = englishName,
            name = "الفاتحة",
            number = number,
            numberOfAyahs = 7
        ),
        numberInSurah = 1
    )
}
