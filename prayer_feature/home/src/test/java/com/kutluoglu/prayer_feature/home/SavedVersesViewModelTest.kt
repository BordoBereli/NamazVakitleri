package com.kutluoglu.prayer_feature.home

import android.util.Log
import com.google.common.truth.Truth.assertThat
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@OptIn(ExperimentalCoroutinesApi::class)
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MainCoroutineRule::class)
class SavedVersesViewModelTest {

    private val getSavedVersesUseCase: GetSavedVersesUseCase = mockk()
    private val reorderSavedVersesUseCase: ReorderSavedVersesUseCase = mockk()
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase = mockk()

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
        mockkStatic(Log::class)
        every { Log.e(any<String>(), any<String>()) } returns 0
    }

    @Test
    fun `loads saved verses on init`() = runTest {
        val verses = listOf(verse(1), verse(2))
        coEvery { getSavedVersesUseCase() } returns Result.success(verses)

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)

        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(verses))
    }

    @Test
    fun `emits error when load fails`() = runTest {
        coEvery { getSavedVersesUseCase() } returns Result.failure(RuntimeException("boom"))

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)

        assertThat(vm.uiState.value).isInstanceOf(SavedVersesUiState.Error::class.java)
    }

    @Test
    fun `reload re-fetches saved verses`() = runTest {
        coEvery { getSavedVersesUseCase() } returnsMany listOf(
            Result.success(emptyList()),
            Result.success(listOf(verse(1)))
        )

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(emptyList()))

        vm.reload()

        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(1))))
    }

    @Test
    fun `remove toggles the verse and reloads`() = runTest {
        coEvery { getSavedVersesUseCase() } returnsMany listOf(
            Result.success(listOf(verse(1), verse(2))),
            Result.success(listOf(verse(2)))
        )
        coEvery { toggleSavedVerseUseCase(verse(1)) } returns Result.success(Unit)

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnRemove(verse(1)))

        coVerify { toggleSavedVerseUseCase(verse(1)) }
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(2))))
    }

    @Test
    fun `remove failure reloads the list`() = runTest {
        coEvery { getSavedVersesUseCase() } returnsMany listOf(
            Result.success(listOf(verse(1), verse(2))),
            Result.success(listOf(verse(1), verse(2)))
        )
        coEvery { toggleSavedVerseUseCase(verse(1)) } returns Result.failure(RuntimeException("boom"))

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnRemove(verse(1)))

        coVerify { toggleSavedVerseUseCase(verse(1)) }
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(1), verse(2))))
    }

    @Test
    fun `reorder persists the new order and updates state`() = runTest {
        coEvery { getSavedVersesUseCase() } returns Result.success(listOf(verse(1), verse(2)))
        coEvery { reorderSavedVersesUseCase(any()) } returns Result.success(Unit)

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnReorder(listOf(verse(2), verse(1))))

        coVerify { reorderSavedVersesUseCase(listOf(verse(2), verse(1))) }
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse(2), verse(1))))
    }

    @Test
    fun `select opens the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase() } returns Result.success(listOf(verse(1)))

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1)))

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isEqualTo(verse(1))
        assertThat(state.isDetailVisible).isTrue()
    }

    @Test
    fun `dismiss closes the detail sheet`() = runTest {
        coEvery { getSavedVersesUseCase() } returns Result.success(listOf(verse(1)))

        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)
        vm.onEvent(SavedVersesEvent.OnSelect(verse(1)))
        vm.onEvent(SavedVersesEvent.OnDismissDetail)

        val state = vm.uiState.value as SavedVersesUiState.Success
        assertThat(state.selectedVerse).isNull()
        assertThat(state.isDetailVisible).isFalse()
    }
}
