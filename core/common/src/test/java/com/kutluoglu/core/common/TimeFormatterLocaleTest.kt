package com.kutluoglu.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Locale

class TimeFormatterLocaleTest {

    private val originalLocale = Locale.getDefault()

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `gregorian formatters use current locale at format time`() {
        Locale.setDefault(Locale.ENGLISH)
        val date = LocalDate.of(2026, 8, 19)

        // First access initializes the formatter under English.
        assertThat(date.format(gregorianFullFormatter())).contains("August")

        // Simulate a runtime language change (LocaleManager.setLanguage + recreate).
        Locale.setDefault(Locale.forLanguageTag("tr"))

        // The formatter must now resolve the current locale.
        assertThat(date.format(gregorianFullFormatter())).contains("Ağustos")
    }
}
