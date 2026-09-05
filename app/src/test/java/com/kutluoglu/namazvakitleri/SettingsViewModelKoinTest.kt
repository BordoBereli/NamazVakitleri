package com.kutluoglu.namazvakitleri

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer.usecases.prayer.ClearPrayerTimesCacheUseCase
import com.kutluoglu.prayer_feature.settings.SettingsViewModel
import com.kutluoglu.prayer_settings.domain.usecase.ClearLocationCacheUseCase
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCalculationMethodUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCompassAutoRotateUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateHijriAdjustmentUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLanguageUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLocationUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLockPortraitUseCase
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelKoinTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SettingsViewModel should not be registered as a singleton`() {
        val koin = koinApplication {
            modules(
                appModule,
                module {
                    single<AnalyticsTracker> { mockk(relaxed = true) }
                    single<GetSettingsUseCase> { mockk(relaxed = true) }
                    single<UpdateLocationUseCase> { mockk(relaxed = true) }
                    single<UpdateCalculationMethodUseCase> { mockk(relaxed = true) }
                    single<UpdateLanguageUseCase> { mockk(relaxed = true) }
                    single<UpdateHijriAdjustmentUseCase> { mockk(relaxed = true) }
                    single<UpdateLockPortraitUseCase> { mockk(relaxed = true) }
                    single<UpdateCompassAutoRotateUseCase> { mockk(relaxed = true) }
                    single<ClearLocationCacheUseCase> { mockk(relaxed = true) }
                    single<ClearPrayerTimesCacheUseCase> { mockk(relaxed = true) }
                }
            )
        }.koin

        val first = koin.get<SettingsViewModel>()
        val second = koin.get<SettingsViewModel>()

        assertThat(first).isNotSameInstanceAs(second)
    }
}
