package com.kutluoglu.namazvakitleri.notifications

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class PrayerCalculationSettingsProviderImplTest {

    private val getSettingsUseCase = mockk<GetSettingsUseCase>(relaxed = true)

    @Test
    fun `maps app settings to prayer calculation settings`() = runTest {
        coEvery { getSettingsUseCase() } returns Settings(
            calculationMethod = "ISNA",
            juristicMethod = "HANAFI",
            hijriAdjustment = 2
        )

        val provider = PrayerCalculationSettingsProviderImpl(getSettingsUseCase)
        val result = provider.getSettings()

        assertThat(result.calculationMethod).isEqualTo("ISNA")
        assertThat(result.juristicMethod).isEqualTo("HANAFI")
        assertThat(result.hijriAdjustment).isEqualTo(2)
        coVerify { getSettingsUseCase() }
    }
}
