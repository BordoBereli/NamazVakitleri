package com.kutluoglu.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class WeekdayNameTest {

    @Test
    fun `extracts weekday from day and name string`() {
        assertThat(extractWeekdayName("17 Monday")).isEqualTo("Monday")
    }

    @Test
    fun `returns input when no space is present`() {
        assertThat(extractWeekdayName("Monday")).isEqualTo("Monday")
    }

    @Test
    fun `returns empty string for empty input`() {
        assertThat(extractWeekdayName("")).isEmpty()
    }
}
