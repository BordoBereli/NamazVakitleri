package com.kutluoglu.prayer_settings.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.settings.AppLocation
import com.kutluoglu.prayer.settings.AppSettings
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.model.Settings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsProviderImplTest {

    private lateinit var repository: SettingsRepository
    private lateinit var provider: SettingsProviderImpl

    @BeforeEach
    fun setUp() {
        repository = mockk(relaxed = true)
        provider = SettingsProviderImpl(repository)
    }

    @Test
    fun `getSettings delegates to repository and maps Settings to AppSettings`() = runTest {
        val settings = Settings(
            calculationMethod = "ISNA",
            juristicMethod = "HANAFI",
            hijriAdjustment = 2,
            language = "en",
            lockPortrait = false,
            compassAutoRotate = false,
            location = LocationSettings(
                latitude = 39.9,
                longitude = 32.8,
                cityName = "Ankara",
                district = "Cankaya",
                country = "Turkey",
                timeZone = "Europe/Istanbul"
            )
        )
        coEvery { repository.getSettings() } returns settings

        val result = provider.getSettings()

        assertThat(result).isEqualTo(
            AppSettings(
                calculationMethod = "ISNA",
                juristicMethod = "HANAFI",
                hijriAdjustment = 2,
                language = "en",
                lockPortrait = false,
                compassAutoRotate = false,
                location = AppLocation(
                    latitude = 39.9,
                    longitude = 32.8,
                    cityName = "Ankara",
                    district = "Cankaya",
                    country = "Turkey",
                    timeZone = "Europe/Istanbul"
                )
            )
        )
        coVerify { repository.getSettings() }
    }

    @Test
    fun `observeSettings maps each Settings emission to AppSettings`() = runTest {
        val settings = Settings(calculationMethod = "MWL", language = "tr")
        every { repository.observeSettings() } returns flowOf(settings)

        val result = provider.observeSettings().first()

        assertThat(result.calculationMethod).isEqualTo("MWL")
        assertThat(result.language).isEqualTo("tr")
        assertThat(result.location).isEqualTo(
            AppLocation(
                latitude = 41.0082,
                longitude = 28.9784,
                cityName = "Istanbul",
                district = null,
                country = "Turkey",
                timeZone = "Europe/Istanbul"
            )
        )
    }

    @Test
    fun `updateLockPortrait delegates to repository`() = runTest {
        provider.updateLockPortrait(true)

        coVerify { repository.updateLockPortrait(true) }
    }

    @Test
    fun `updateCompassAutoRotate delegates to repository`() = runTest {
        provider.updateCompassAutoRotate(false)

        coVerify { repository.updateCompassAutoRotate(false) }
    }
}
