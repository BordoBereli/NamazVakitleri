package com.kutluoglu.prayer_feature.home.domain

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.usecases.quran.GetRandomVerseUseCase
import com.kutluoglu.prayer.usecases.quran.IsVerseSavedUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
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
    private val isVerseSavedUseCase: IsVerseSavedUseCase = mockk()
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase = mockk()
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
        coEvery { isVerseSavedUseCase.invoke(verse) } returns false

        val loader = QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)
        loader.loadVerse(scope = this, isScreenReady = { true })
        runCurrent()

        assertThat(loader.quranState.value.verse).isEqualTo(verse)
    }

    @Test
    fun `loadVerse does not fetch while screen not ready`() = runTest {
        coEvery { languageProvider.getLanguageCode() } returns "tr"
        coEvery { getRandomVerseUseCase.invoke("tr") } returns Result.failure(RuntimeException("x"))

        var ready = false
        val loader = QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)
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
        val loader = QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)
        loader.setSheetVisible(true)
        assertThat(loader.quranState.value.isSheetVisible).isTrue()
        loader.setSheetVisible(false)
        assertThat(loader.quranState.value.isSheetVisible).isFalse()
    }

    @Test
    fun `loadVerse sets isSaved from the store`() = runTest {
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
        coEvery { isVerseSavedUseCase.invoke(verse) } returns true

        val loader = QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)
        loader.loadVerse(scope = this, isScreenReady = { true })
        runCurrent()

        assertThat(loader.quranState.value.verse).isEqualTo(verse)
        assertThat(loader.quranState.value.isSaved).isTrue()
    }

    @Test
    fun `toggleSaved flips isSaved on success`() = runTest {
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
        coEvery { toggleSavedVerseUseCase.invoke(verse) } returns Result.success(Unit)

        val loader = QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)
        loader.toggleSaved(verse, scope = this)
        runCurrent()

        assertThat(loader.quranState.value.isSaved).isTrue()
    }

    @Test
    fun `toggleSaved keeps isSaved unchanged on failure`() = runTest {
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
        coEvery { toggleSavedVerseUseCase.invoke(verse) } returns Result.failure(RuntimeException("x"))

        val loader = QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)
        loader.toggleSaved(verse, scope = this)
        runCurrent()

        assertThat(loader.quranState.value.isSaved).isFalse()
    }
}
