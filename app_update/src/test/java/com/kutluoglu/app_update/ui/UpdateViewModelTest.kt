package com.kutluoglu.app_update.ui

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.data.InstallSourceDetector
import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.model.UpdateInfo
import com.kutluoglu.app_update.domain.usecase.CheckForUpdateUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    private val checkForUpdateUseCase = mockk<CheckForUpdateUseCase>()
    private val installSourceDetector = mockk<InstallSourceDetector>()
    private val updateUrlOpener = mockk<UpdateUrlOpener>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun info() = UpdateInfo(
        latestVersionCode = 200,
        minVersionCode = 150,
        latestVersionName = "2.0",
        releaseNotes = "notes",
        directDownloadUrl = "https://example.com/app.apk",
    )

    private fun viewModel() = UpdateViewModel(
        checkForUpdateUseCase,
        installSourceDetector,
        updateUrlOpener,
    )

    @Test
    fun `checkForUpdate emits ForceUpdate when decision is force`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.ForceUpdate(info())
        val vm = viewModel()

        vm.checkForUpdate()

        assertThat(vm.uiState.value).isEqualTo(UpdateUiState.ForceUpdate(info()))
    }

    @Test
    fun `checkForUpdate emits OptionalUpdate when decision is optional`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.OptionalUpdate(info())
        val vm = viewModel()

        vm.checkForUpdate()

        assertThat(vm.uiState.value).isEqualTo(UpdateUiState.OptionalUpdate(info()))
    }

    @Test
    fun `checkForUpdate emits NoUpdate when decision is none`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.NoUpdate
        val vm = viewModel()

        vm.checkForUpdate()

        assertThat(vm.uiState.value).isEqualTo(UpdateUiState.NoUpdate)
    }

    @Test
    fun `onOptionalUpdateDismissed resets to NoUpdate`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.OptionalUpdate(info())
        val vm = viewModel()
        vm.checkForUpdate()
        assertThat(vm.uiState.value).isInstanceOf(UpdateUiState.OptionalUpdate::class.java)

        vm.onOptionalUpdateDismissed()

        assertThat(vm.uiState.value).isEqualTo(UpdateUiState.NoUpdate)
    }

    @Test
    fun `onUpdateClicked opens play store url for play install`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.ForceUpdate(info())
        every { installSourceDetector.isPlayStoreInstall() } returns true
        every { installSourceDetector.getPlayStoreUrl() } returns "market://details?id=com.kutluoglu.namazvakitleri"
        every { updateUrlOpener.open(any()) } returns true
        val vm = viewModel()
        vm.checkForUpdate()

        vm.onUpdateClicked()

        verify { updateUrlOpener.open("market://details?id=com.kutluoglu.namazvakitleri") }
    }

    @Test
    fun `onUpdateClicked falls back to web url when market url fails`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.ForceUpdate(info())
        every { installSourceDetector.isPlayStoreInstall() } returns true
        every { installSourceDetector.getPlayStoreUrl() } returns "market://details?id=com.kutluoglu.namazvakitleri"
        every { installSourceDetector.getPlayStoreWebUrl() } returns "https://play.google.com/store/apps/details?id=com.kutluoglu.namazvakitleri"
        every { updateUrlOpener.open(any()) } returns false
        val vm = viewModel()
        vm.checkForUpdate()

        vm.onUpdateClicked()

        verify { updateUrlOpener.open("market://details?id=com.kutluoglu.namazvakitleri") }
        verify { updateUrlOpener.open("https://play.google.com/store/apps/details?id=com.kutluoglu.namazvakitleri") }
    }

    @Test
    fun `onUpdateClicked opens direct url for sideload install`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.OptionalUpdate(info())
        every { installSourceDetector.isPlayStoreInstall() } returns false
        every { installSourceDetector.getDirectDownloadUrl(any()) } returns "https://example.com/app.apk"
        every { updateUrlOpener.open(any()) } returns true
        val vm = viewModel()
        vm.checkForUpdate()

        vm.onUpdateClicked()

        verify { updateUrlOpener.open("https://example.com/app.apk") }
    }

    @Test
    fun `onUpdateClicked sets urlOpenFailed when all opens fail`() = runTest {
        coEvery { checkForUpdateUseCase() } returns UpdateDecision.ForceUpdate(info())
        every { installSourceDetector.isPlayStoreInstall() } returns false
        every { installSourceDetector.getDirectDownloadUrl(any()) } returns "https://example.com/app.apk"
        every { updateUrlOpener.open(any()) } returns false
        val vm = viewModel()
        vm.checkForUpdate()

        vm.onUpdateClicked()

        val state = vm.uiState.value as UpdateUiState.ForceUpdate
        assertThat(state.urlOpenFailed).isTrue()
    }

    @Test
    fun `concurrent checkForUpdate calls are deduped`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { checkForUpdateUseCase() } coAnswers {
            delay(1000)
            UpdateDecision.NoUpdate
        }
        val vm = viewModel()

        vm.checkForUpdate()
        vm.checkForUpdate()

        advanceUntilIdle()

        coVerify(exactly = 1) { checkForUpdateUseCase() }
    }
}
