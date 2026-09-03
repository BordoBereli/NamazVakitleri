package com.kutluoglu.prayer_widget.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WidgetCountdownFormatterTest {

    @Test
    fun `hours and minutes both present`() {
        assertEquals("2h 15m", formatCompact(2, 15, "h", "m"))
    }

    @Test
    fun `hours only`() {
        assertEquals("2h", formatCompact(2, 0, "h", "m"))
    }

    @Test
    fun `minutes only`() {
        assertEquals("15m", formatCompact(0, 15, "h", "m"))
    }

    @Test
    fun `zero countdown shows zero minutes`() {
        assertEquals("0m", formatCompact(0, 0, "h", "m"))
    }

    @Test
    fun `negative values are clamped to zero`() {
        assertEquals("0m", formatCompact(-1, -5, "h", "m"))
    }

    @Test
    fun `uses localized unit strings`() {
        assertEquals("2s 15d", formatCompact(2, 15, "s", "d"))
    }
}
