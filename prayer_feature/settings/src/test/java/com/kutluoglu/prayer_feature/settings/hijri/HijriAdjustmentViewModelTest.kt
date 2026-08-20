package com.kutluoglu.prayer_feature.settings.hijri

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateHijriAdjustmentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineRule::class)
class HijriAdjustmentViewModelTest {

    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateHijriAdjustmentUseCase: UpdateHijriAdjustmentUseCase
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private lateinit var viewModel: HijriAdjustmentViewModel

    @BeforeEach
    fun setUp() {
        getSettingsUseCase = mockk()
        updateHijriAdjustmentUseCase = mockk()
        coEvery { getSettingsUseCase() } returns Settings()
        coEvery { updateHijriAdjustmentUseCase(any()) } returns Unit
        viewModel = HijriAdjustmentViewModel(getSettingsUseCase, updateHijriAdjustmentUseCase, analyticsTracker)
    }

    @Test
    fun `init loads current adjustment from settings`() = runTest {
        coEvery { getSettingsUseCase() } returns Settings(hijriAdjustment = 5)

        val viewModel = HijriAdjustmentViewModel(getSettingsUseCase, updateHijriAdjustmentUseCase, analyticsTracker)

        assertThat(viewModel.currentAdjustment.value).isEqualTo(5)
    }

    @Test
    fun `init defaults adjustment to zero when settings has no adjustment`() {
        assertThat(viewModel.currentAdjustment.value).isEqualTo(0)
    }

    @Test
    fun `confirmAdjustment persists the adjustment`() = runTest {
        viewModel.confirmAdjustment(5)

        coVerify { updateHijriAdjustmentUseCase(5) }
    }

    @Test
    fun `confirmAdjustment with negative value persists it`() = runTest {
        viewModel.confirmAdjustment(-10)

        coVerify { updateHijriAdjustmentUseCase(-10) }
    }

    @Test
    fun `confirmAdjustment with zero persists it`() = runTest {
        viewModel.confirmAdjustment(0)

        coVerify { updateHijriAdjustmentUseCase(0) }
    }

    @Test
    fun `confirmAdjustment with max value persists it`() = runTest {
        viewModel.confirmAdjustment(30)

        coVerify { updateHijriAdjustmentUseCase(30) }
    }

    @Test
    fun `confirmAdjustment with min value persists it`() = runTest {
        viewModel.confirmAdjustment(-30)

        coVerify { updateHijriAdjustmentUseCase(-30) }
    }

    @Test
    fun `confirmedAdjustment flow should not be null`() {
        assertThat(viewModel.confirmedAdjustment).isNotNull()
    }
}
