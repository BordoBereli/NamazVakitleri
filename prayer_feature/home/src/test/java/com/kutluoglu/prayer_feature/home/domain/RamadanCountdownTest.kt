package com.kutluoglu.prayer_feature.home.domain

import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class RamadanCountdownTest {

    private val imsak = LocalTime(4, 50)
    private val maghrib = LocalTime(19, 30)
    private val nextImsak = LocalTime(4, 50)

    @Test
    fun `before imsak counts down to sahur end`() {
        val state = computeRamadanCountdown(LocalTime(4, 0), imsak, maghrib, nextImsak)
        assertEquals(RamadanCountdownState.SahurEndsIn(Duration.ofMinutes(50)), state)
    }

    @Test
    fun `between imsak and maghrib counts down to iftar`() {
        val state = computeRamadanCountdown(LocalTime(12, 0), imsak, maghrib, nextImsak)
        assertEquals(RamadanCountdownState.IftarIn(Duration.ofMinutes(450)), state)
    }

    @Test
    fun `after maghrib counts to next day imsak`() {
        val state = computeRamadanCountdown(LocalTime(20, 0), imsak, maghrib, nextImsak)
        assertEquals(RamadanCountdownState.SahurEndsIn(Duration.ofMinutes(530)), state)
    }
}
