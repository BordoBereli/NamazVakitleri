package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.usecases.quran.GetRandomVerseUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuranVerseLoaderTest {

    private val getRandomVerseUseCase: GetRandomVerseUseCase = mockk()
    private val languageProvider: LanguageProvider = mockk()

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
    }

    @Test
    fun `loadVerse resolves verse when screen ready`() = runTest {
        val verse = AyahData(
            text = "Bismillah...",
            surah = SurahInfo(
                englishName = "Al-Fatihah",
                name = "الفاتحة",
                number = 1,
                numberOfAyahs = 7
            ),
            numberInSurah = 1
        )
        coEvery { languageProvider.getLanguageCode() } returns "tr"
        coEvery { getRandomVerseUseCase.invoke("tr") } returns Result.success(verse)

        val loader = QuranVerseLoader(getRandomVerseUseCase, languageProvider)
        loader.loadVerse(scope = this, isScreenReady = { true })
        runCurrent()

        assertThat(loader.quranState.value.verse).isEqualTo(verse)
    }

    @Test
    fun `loadVerse does not fetch while screen not ready`() = runTest {
        coEvery { languageProvider.getLanguageCode() } returns "tr"
        coEvery { getRandomVerseUseCase.invoke("tr") } returns Result.failure(RuntimeException("x"))

        var ready = false
        val loader = QuranVerseLoader(getRandomVerseUseCase, languageProvider)
        loader.loadVerse(scope = this, isScreenReady = { ready })

        advanceTimeBy(5_000)
        runCurrent()

        ready = true
        advanceTimeBy(60_000)
        runCurrent()

        assertThat(loader.quranState.value.verse).isNull()
    }

    @Test
    fun `setSheetVisible toggles the sheet flag`() = runTest {
        val loader = QuranVerseLoader(getRandomVerseUseCase, languageProvider)
        loader.setSheetVisible(true)
        assertThat(loader.quranState.value.isSheetVisible).isTrue()
        loader.setSheetVisible(false)
        assertThat(loader.quranState.value.isSheetVisible).isFalse()
    }
}
