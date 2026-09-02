package com.kutluoglu.prayer_feature.common.prayerUtils

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_feature.common.R as AppR
import org.junit.jupiter.api.Test

class PrayerIconTest {

    private val sevenNames = listOf("Imsak", "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")

    @Test
    fun `builds a map for seven localized prayer names`() {
        val map = buildPrayerIconMap(sevenNames)
        assertThat(map).hasSize(7)
        assertThat(map["Imsak"]).isEqualTo(AppR.drawable.facr)
        assertThat(map["Fajr"]).isEqualTo(AppR.drawable.facr)
        assertThat(map["Sunrise"]).isEqualTo(AppR.drawable.sunrise)
        assertThat(map["Dhuhr"]).isEqualTo(AppR.drawable.dhuhr)
        assertThat(map["Asr"]).isEqualTo(AppR.drawable.asr)
        assertThat(map["Maghrib"]).isEqualTo(AppR.drawable.magrip)
        assertThat(map["Isha"]).isEqualTo(AppR.drawable.isha)
    }

    @Test
    fun `returns an empty map when fewer than seven names are provided`() {
        assertThat(buildPrayerIconMap(listOf("Fajr", "Sunrise"))).isEmpty()
    }

    @Test
    fun `returns an empty map when the list is empty`() {
        assertThat(buildPrayerIconMap(emptyList())).isEmpty()
    }
}
