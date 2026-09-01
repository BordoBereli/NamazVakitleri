package com.kutluoglu.prayer.model.location

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocationNameLocalizerTest {

    private val entry = LocationEntry(
        id = "x",
        location = LocationData(1.0, 2.0, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey",
        displayNameTr = "İstanbul, Türkiye",
        displayNameAr = "إسطنبول، تركيا",
        displayNameFa = "استانبول، ترکیه"
    )

    @Test
    fun `returns english displayName when language is not localized`() {
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(entry, "en"))
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(entry, "de"))
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(entry, "fr"))
    }

    @Test
    fun `returns turkish displayName for tr`() {
        assertEquals("İstanbul, Türkiye", LocationNameLocalizer.localized(entry, "tr"))
    }

    @Test
    fun `returns arabic displayName for ar`() {
        assertEquals("إسطنبول، تركيا", LocationNameLocalizer.localized(entry, "ar"))
    }

    @Test
    fun `returns farsi displayName for fa`() {
        assertEquals("استانبول، ترکیه", LocationNameLocalizer.localized(entry, "fa"))
    }

    @Test
    fun `falls back to english when localized field missing`() {
        val noLocalized = entry.copy(displayNameTr = null, displayNameAr = null, displayNameFa = null)
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(noLocalized, "tr"))
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(noLocalized, "ar"))
        assertEquals("Istanbul, Turkey", LocationNameLocalizer.localized(noLocalized, "fa"))
    }
}