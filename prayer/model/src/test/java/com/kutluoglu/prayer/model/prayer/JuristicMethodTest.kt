package com.kutluoglu.prayer.model.prayer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JuristicMethodTest {

    @Test
    fun `fromSettingsId maps known ids`() {
        assertEquals(JuristicMethod.STANDARD, JuristicMethod.fromSettingsId("STANDARD"))
        assertEquals(JuristicMethod.HANAFI, JuristicMethod.fromSettingsId("HANAFI"))
    }

    @Test
    fun `fromSettingsId defaults to standard for unknown`() {
        assertEquals(JuristicMethod.STANDARD, JuristicMethod.fromSettingsId("BOGUS"))
    }
}
