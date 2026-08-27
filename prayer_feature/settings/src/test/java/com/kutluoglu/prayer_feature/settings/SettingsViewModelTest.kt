package com.kutluoglu.prayer_feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.AppVersion
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer.usecases.prayer.ClearPrayerTimesCacheUseCase
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.ClearLocationCacheUseCase
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCalculationMethodUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateHijriAdjustmentUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLanguageUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLocationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@ExperimentalCoroutinesApi
@Execution(value = ExecutionMode.SAME_THREAD)
@ExtendWith(MainCoroutineRule::class)
class SettingsViewModelTest {

    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateLocationUseCase: UpdateLocationUseCase
    private lateinit var updateCalculationMethodUseCase: UpdateCalculationMethodUseCase
    private lateinit var updateLanguageUseCase: UpdateLanguageUseCase
    private lateinit var updateHijriAdjustmentUseCase: UpdateHijriAdjustmentUseCase
    private lateinit var clearLocationCacheUseCase: ClearLocationCacheUseCase
    private lateinit var clearPrayerTimesCacheUseCase: ClearPrayerTimesCacheUseCase
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val appVersion = AppVersion(name = "2.0.0", code = 200)
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setUp() {
        getSettingsUseCase = mockk()
        updateLocationUseCase = mockk(relaxed = true)
        updateCalculationMethodUseCase = mockk(relaxed = true)
        updateLanguageUseCase = mockk(relaxed = true)
        updateHijriAdjustmentUseCase = mockk(relaxed = true)
        clearLocationCacheUseCase = mockk(relaxed = true)
        clearPrayerTimesCacheUseCase = mockk(relaxed = true)
        
        coEvery { getSettingsUseCase() } returns Settings()
        
        viewModel = SettingsViewModel(
            getSettingsUseCase,
            updateLocationUseCase,
            updateCalculationMethodUseCase,
            updateLanguageUseCase,
            updateHijriAdjustmentUseCase,
            clearLocationCacheUseCase,
            clearPrayerTimesCacheUseCase,
            analyticsTracker,
            appVersion
        )
    }

    @Test
    fun `UpdateLocation should call updateLocationUseCase`() = runTest {
        // Arrange
        val location = LocationSettings(
            latitude = 51.5074,
            longitude = -0.1278,
            cityName = "London",
            country = "United Kingdom",
            timeZone = "Europe/London"
        )

        // Act
        viewModel.onEvent(SettingsEvent.UpdateLocation(location))

        // Assert
        coVerify { updateLocationUseCase(location) }
    }

    @Test
    fun `UpdateCalculationMethod should call updateCalculationMethodUseCase`() = runTest {
        // Arrange
        coEvery { getSettingsUseCase() } returns Settings()

        // Act
        viewModel.onEvent(SettingsEvent.UpdateCalculationMethod("ISNA"))

        // Assert
        coVerify { updateCalculationMethodUseCase("ISNA") }
    }
    
    @Test
    fun `after updateLocation state should be Success with updated location`() = runTest {
        // Arrange
        val location = LocationSettings(
            latitude = 51.5074,
            longitude = -0.1278,
            cityName = "London",
            country = "United Kingdom",
            timeZone = "Europe/London"
        )
        
        val updatedSettings = Settings(location = location)
        coEvery { getSettingsUseCase() } returns updatedSettings

        // Act
        viewModel.onEvent(SettingsEvent.UpdateLocation(location))

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(SettingsUiState.Success::class.java)
            val successState = state as SettingsUiState.Success
            assertThat(successState.settings.location.cityName).isEqualTo("London")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UpdateLanguage should call updateLanguageUseCase with tr`() = runTest {
        // Act
        viewModel.onEvent(SettingsEvent.UpdateLanguage("tr"))

        // Assert
        coVerify { updateLanguageUseCase("tr") }
    }

    @Test
    fun `UpdateLanguage should call updateLanguageUseCase with en`() = runTest {
        // Act
        viewModel.onEvent(SettingsEvent.UpdateLanguage("en"))

        // Assert
        coVerify { updateLanguageUseCase("en") }
    }

    @Test
    fun `UpdateHijriAdjustment should call updateHijriAdjustmentUseCase with positive days`() = runTest {
        // Act
        viewModel.onEvent(SettingsEvent.UpdateHijriAdjustment(1))

        // Assert
        coVerify { updateHijriAdjustmentUseCase(1) }
    }

    @Test
    fun `UpdateHijriAdjustment should call updateHijriAdjustmentUseCase with negative days`() = runTest {
        // Act
        viewModel.onEvent(SettingsEvent.UpdateHijriAdjustment(-2))

        // Assert
        coVerify { updateHijriAdjustmentUseCase(-2) }
    }

    @Test
    fun `Success state should expose app version`() = runTest {
        // Act
        viewModel.uiState.test {
            val state = awaitItem()

            // Assert
            assertThat(state).isInstanceOf(SettingsUiState.Success::class.java)
            val successState = state as SettingsUiState.Success
            assertThat(successState.version.name).isEqualTo("2.0.0")
            assertThat(successState.version.code).isEqualTo(200)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
