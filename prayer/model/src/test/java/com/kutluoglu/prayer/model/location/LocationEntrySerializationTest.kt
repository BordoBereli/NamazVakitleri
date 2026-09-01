package com.kutluoglu.prayer.model.location

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocationEntrySerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes legacy entry without localized fields as null`() {
        val legacy = """
            {"id":"x","location":{"latitude":1.0,"longitude":2.0,"country":"Turkey",
             "countryCode":"TR","city":"Istanbul","county":null},"isAutoGps":false,
             "displayName":"Istanbul, Turkey"}
        """.trimIndent()
        val entry = json.decodeFromString<LocationEntry>(legacy)
        assertNull(entry.displayNameTr)
        assertNull(entry.displayNameAr)
        assertNull(entry.displayNameFa)
    }

    @Test
    fun `encodes and decodes localized fields round trip`() {
        val entry = LocationEntry(
            id = "x",
            location = LocationData(1.0, 2.0, "Turkey", "TR", "Istanbul", null),
            displayName = "Istanbul, Turkey",
            displayNameTr = "İstanbul, Türkiye",
            displayNameAr = "إسطنبول، تركيا",
            displayNameFa = "استانبول، ترکیه"
        )
        val roundTripped = json.decodeFromString<LocationEntry>(json.encodeToString(LocationEntry.serializer(), entry))
        assertEquals("İstanbul, Türkiye", roundTripped.displayNameTr)
        assertEquals("إسطنبول، تركيا", roundTripped.displayNameAr)
        assertEquals("استانبول، ترکیه", roundTripped.displayNameFa)
    }
}
