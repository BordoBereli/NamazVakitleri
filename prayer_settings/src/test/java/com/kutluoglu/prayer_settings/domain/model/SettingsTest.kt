package com.kutluoglu.prayer_settings.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class SettingsTest {

    @Test
    fun `Settings should have default values`() {
        // Arrange & Act
        val settings = Settings()

        // Assert
        assertThat(settings.location.latitude).isEqualTo(41.0082)
        assertThat(settings.location.longitude).isEqualTo(28.9784)
        assertThat(settings.location.cityName).isEqualTo("Istanbul")
        assertThat(settings.location.country).isEqualTo("Turkey")
        assertThat(settings.calculationMethod).isEqualTo("TURKEY_DIYANET")
        assertThat(settings.language).isEqualTo("system")
        assertThat(settings.hijriAdjustment).isEqualTo(0)
    }

    @Test
    fun `Settings should allow custom values`() {
        // Arrange & Act
        val location = LocationSettings(
            latitude = 51.5074,
            longitude = -0.1278,
            cityName = "London",
            country = "United Kingdom",
            timeZone = "Europe/London"
        )
        val settings = Settings(
            location = location,
            calculationMethod = "ISNA",
            language = "en",
            hijriAdjustment = 1
        )

        // Assert
        assertThat(settings.location.latitude).isEqualTo(51.5074)
        assertThat(settings.location.longitude).isEqualTo(-0.1278)
        assertThat(settings.location.cityName).isEqualTo("London")
        assertThat(settings.location.country).isEqualTo("United Kingdom")
        assertThat(settings.calculationMethod).isEqualTo("ISNA")
        assertThat(settings.language).isEqualTo("en")
        assertThat(settings.hijriAdjustment).isEqualTo(1)
    }

    @Test
    fun `LocationSettings should have displayName`() {
        // Arrange
        val location = LocationSettings(
            latitude = 41.0082,
            longitude = 28.9784,
            cityName = "Istanbul",
            country = "Turkey",
            timeZone = "Europe/Istanbul"
        )

        // Act
        val displayName = location.displayName()

        // Assert
        assertThat(displayName).isEqualTo("Istanbul, Turkey")
    }
}
