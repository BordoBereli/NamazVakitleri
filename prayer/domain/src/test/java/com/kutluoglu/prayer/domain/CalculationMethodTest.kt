package com.kutluoglu.prayer.domain

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import org.junit.jupiter.api.Test

class CalculationMethodTest {

    @Test
    fun `fromSettingsId maps every settings id to the matching enum`() {
        assertThat(CalculationMethod.fromSettingsId("TURKEY_DIYANET")).isEqualTo(CalculationMethod.TURKEY_DIYANET)
        assertThat(CalculationMethod.fromSettingsId("MWL")).isEqualTo(CalculationMethod.MWL)
        assertThat(CalculationMethod.fromSettingsId("ISNA")).isEqualTo(CalculationMethod.ISNA)
        assertThat(CalculationMethod.fromSettingsId("EGYPT")).isEqualTo(CalculationMethod.EGYPT)
        assertThat(CalculationMethod.fromSettingsId("MAKKAH")).isEqualTo(CalculationMethod.MAKKAH)
        assertThat(CalculationMethod.fromSettingsId("KARACHI")).isEqualTo(CalculationMethod.KARACHI)
    }

    @Test
    fun `fromSettingsId falls back to TURKEY_DIYANET for unknown ids`() {
        assertThat(CalculationMethod.fromSettingsId("UNKNOWN")).isEqualTo(CalculationMethod.TURKEY_DIYANET)
    }
}
