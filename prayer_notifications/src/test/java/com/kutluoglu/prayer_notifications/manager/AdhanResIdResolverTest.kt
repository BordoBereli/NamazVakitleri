package com.kutluoglu.prayer_notifications.manager

import com.kutluoglu.prayer_notifications.R
import com.kutluoglu.prayer_notifications.domain.AdhanStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdhanResIdResolverTest {

    private val resolver = AdhanResIdResolver()

    @Test
    fun `default style resolves to default resource for each prayer`() {
        assertEquals(R.raw.adhan_fajr, resolver.resolve("Fajr", AdhanStyle.DEFAULT.id))
        assertEquals(R.raw.adhan_dhuhr, resolver.resolve("Dhuhr", AdhanStyle.DEFAULT.id))
        assertEquals(R.raw.adhan_asr, resolver.resolve("Asr", AdhanStyle.DEFAULT.id))
        assertEquals(R.raw.adhan_maghrib, resolver.resolve("Maghrib", AdhanStyle.DEFAULT.id))
        assertEquals(R.raw.adhan_isha, resolver.resolve("Isha", AdhanStyle.DEFAULT.id))
    }

    @Test
    fun `unknown style falls back to default`() {
        assertEquals(R.raw.adhan_fajr, resolver.resolve("Fajr", "bogus_style"))
    }

    @Test
    fun `unknown prayer falls back to fajr`() {
        assertEquals(R.raw.adhan_fajr, resolver.resolve("Unknown", AdhanStyle.DEFAULT.id))
    }
}
