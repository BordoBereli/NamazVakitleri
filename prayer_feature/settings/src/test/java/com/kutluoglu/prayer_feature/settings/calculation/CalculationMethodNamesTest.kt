package com.kutluoglu.prayer_feature.settings.calculation

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import org.junit.jupiter.api.Test

class CalculationMethodNamesTest {

    @Test
    fun `every enum method has a display name resource`() {
        CalculationMethod.entries.forEach { method ->
            assertThat(method.displayNameRes()).isNotNull()
        }
    }

    @Test
    fun `unknown id falls back to TURKEY_DIYANET`() {
        assertThat(CalculationMethod.fromSettingsId("MUSLIM_WORLD_LEAGUE"))
            .isEqualTo(CalculationMethod.TURKEY_DIYANET)
    }
}
