package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@OptIn(ExperimentalCoroutinesApi::class)
@Execution(ExecutionMode.SAME_THREAD)
class SavedVersesViewModelTest {

    private val getSavedVersesUseCase: GetSavedVersesUseCase = mockk()
    private val reorderSavedVersesUseCase: ReorderSavedVersesUseCase = mockk()
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase = mockk()
    private val languageProvider: LanguageProvider = mockk()

    private fun verse(numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo(
            englishName = "Al-Fatihah",
            name = "الفاتحة",
            number = 1,
            numberOfAyahs = 7
        ),
        numberInSurah = numberInSurah
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { languageProvider.getLanguageCode() } returns "tr"
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads saved verses on init`() = runTest {
        val verses = listOf(verse(1), verse(2))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(verses)

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(verses))
    }

    @Test
    fun `emits error when load fails`() = runTest {
        coEvery { getSavedVersesUseCase(any()) } returns Result.failure(RuntimeException("boom"))

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(SavedVersesUiState.Error::class.java)
    }

    @Test
    fun `reload re-fetches saved verses`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returnsMany listOf(
            Result.success(emptyList()),
            Result.success(listOf(verse(1)))
        )

        val vm = viewModel()
        advanceUntilIdle()
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(emptyList()))

        vm.reload()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(1))))
    }

    @Test
    fun `loads saved verses in the current language`() = runTest {
        val localized = listOf(verse(1).copy(text = "English text"))
        every { languageProvider.getLanguageCode() } returns "en"
        coEvery { getSavedVersesUseCase("en") } returns Result.success(localized)

        val vm = viewModel()
        advanceUntilIdle()

        coVerify { getSavedVersesUseCase("en") }
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(localized))
    }

    @Test
    fun `remove toggles the verse and reloads`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returnsMany listOf(
            Result.success(listOf(verse(1), verse(2))),
            Result.success(listOf(verse(2)))
        )
        coEvery { toggleSavedVerseUseCase(verse(1)) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnRemove(verse(1)))
        advanceUntilIdle()

        coVerify { toggleSavedVerseUseCase(verse(1)) }
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(2))))
    }

    @Test
    fun `remove failure reloads the list`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returnsMany listOf(
            Result.success(listOf(verse(1), verse(2))),
            Result.success(listOf(verse(1), verse(2)))
        )
        coEvery { toggleSavedVerseUseCase(verse(1)) } returns Result.failure(RuntimeException("boom"))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnRemove(verse(1)))
        advanceUntilIdle()

        coVerify { toggleSavedVerseUseCase(verse(1)) }
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(1), verse(2))))
    }

    @Test
    fun `reorder persists the new order and updates state`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(verse(1), verse(2)))
        coEvery { reorderSavedVersesUseCase(any()) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnReorder(listOf(verse(2), verse(1))))
        advanceUntilIdle()

        coVerify { reorderSavedVersesUseCase(listOf(verse(2), verse(1))) }
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(2), verse(1))))
    }

    @Test
    fun `select opens the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(verse(1)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1)))

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isEqualTo(verse(1))
        assertThat(state.isDetailVisible).isTrue()
    }

    @Test
    fun `dismiss closes the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(verse(1)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1)))
        vm.onEvent(SavedVersesEvent.OnDismissDetail)

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isNull()
        assertThat(state.isDetailVisible).isFalse()
    }

    private fun viewModel() = SavedVersesViewModel(
        getSavedVersesUseCase,
        reorderSavedVersesUseCase,
        toggleSavedVerseUseCase,
        languageProvider
    )
}
