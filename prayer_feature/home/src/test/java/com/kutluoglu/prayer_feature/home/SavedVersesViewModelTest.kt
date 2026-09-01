package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.usecases.quran.GetCollapsedSurahsUseCase
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.SetCollapsedSurahsUseCase
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
    private val getCollapsedSurahsUseCase: GetCollapsedSurahsUseCase = mockk()
    private val setCollapsedSurahsUseCase: SetCollapsedSurahsUseCase = mockk()
    private val languageProvider: LanguageProvider = mockk()

    private fun verse(surahNumber: Int, numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo("Surah $surahNumber", "سورة", surahNumber, 10),
        numberInSurah = numberInSurah
    )

    private fun group(surahNumber: Int, vararg numbers: Int) = SavedVerseGroup(
        surah = verse(surahNumber, 1).surah,
        verses = numbers.map { verse(surahNumber, it) }
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { languageProvider.getLanguageCode() } returns "tr"
        coEvery { getCollapsedSurahsUseCase() } returns emptySet()
        coEvery { setCollapsedSurahsUseCase(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads groups on init`() = runTest {
        val groups = listOf(group(1, 1, 2), group(36, 1))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(groups)

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.groups).isEqualTo(groups)
        assertThat(state.filteredGroups).isEqualTo(groups)
    }

    @Test
    fun `emits error when load fails`() = runTest {
        coEvery { getSavedVersesUseCase(any()) } returns Result.failure(RuntimeException("boom"))

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value).isInstanceOf(SavedVersesUiState.Error::class.java)
    }

    @Test
    fun `search filters groups by surah name and verse text`() = runTest {
        val groups = listOf(group(1, 1), group(36, 1))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(groups)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnSearch("36"))
        advanceUntilIdle()

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.query).isEqualTo("36")
        assertThat(state.filteredGroups.map { it.surah.number }).containsExactly(36)
    }

    @Test
    fun `toggle collapse updates state and persists`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(group(1, 1)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnToggleCollapse(1))
        advanceUntilIdle()

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.collapsedSurahs).containsExactly(1)
        coVerify { setCollapsedSurahsUseCase(setOf(1)) }
    }

    @Test
    fun `reorder groups persists the new group order`() = runTest {
        val groups = listOf(group(1, 1), group(36, 1))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(groups)
        coEvery { reorderSavedVersesUseCase(any()) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()
        val reordered = listOf(group(36, 1), group(1, 1))
        vm.onEvent(SavedVersesEvent.OnReorderGroups(reordered))
        advanceUntilIdle()

        coVerify { reorderSavedVersesUseCase(reordered) }
        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.groups).isEqualTo(reordered)
    }

    @Test
    fun `reorder within group persists the new verse order`() = runTest {
        val groups = listOf(group(1, 1, 2))
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(groups)
        coEvery { reorderSavedVersesUseCase(any()) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnReorderWithinGroup(1, listOf(verse(1, 2), verse(1, 1))))
        advanceUntilIdle()

        coVerify { reorderSavedVersesUseCase(listOf(group(1, 2, 1))) }
    }

    @Test
    fun `remove toggles the verse and reloads`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returnsMany listOf(
            Result.success(listOf(group(1, 1, 2))),
            Result.success(listOf(group(1, 2)))
        )
        coEvery { toggleSavedVerseUseCase(verse(1, 1)) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnRemove(verse(1, 1)))
        advanceUntilIdle()

        coVerify { toggleSavedVerseUseCase(verse(1, 1)) }
        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.groups).isEqualTo(listOf(group(1, 2)))
    }

    @Test
    fun `select opens the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(group(1, 1)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1, 1)))

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isEqualTo(verse(1, 1))
        assertThat(state.isDetailVisible).isTrue()
    }

    @Test
    fun `dismiss closes the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase("tr") } returns Result.success(listOf(group(1, 1)))

        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1, 1)))
        vm.onEvent(SavedVersesEvent.OnDismissDetail)

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isNull()
        assertThat(state.isDetailVisible).isFalse()
    }

    private fun viewModel() = SavedVersesViewModel(
        getSavedVersesUseCase,
        reorderSavedVersesUseCase,
        toggleSavedVerseUseCase,
        getCollapsedSurahsUseCase,
        setCollapsedSurahsUseCase,
        languageProvider
    )
}
