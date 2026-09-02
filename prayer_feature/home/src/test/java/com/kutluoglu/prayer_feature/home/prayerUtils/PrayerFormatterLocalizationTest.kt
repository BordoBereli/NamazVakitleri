package com.kutluoglu.prayer_feature.home.prayerUtils

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.prayerUtils.ResourcesProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "tr")
class PrayerFormatterLocalizationTest {

    @Test
    fun withLocalizedNamesLocalizesSevenPrayersIncludingImsak() {
        val formatter = PrayerFormatter(ResourcesProvider(RuntimeEnvironment.getApplication()))
        val prayers = listOf(
            Prayer("Imsak", "الإمساك", kotlinx.datetime.LocalTime(4, 50), kotlinx.datetime.LocalDate(2026, 9, 2), isImsak = true),
            Prayer("Imsak", "الإمساك", kotlinx.datetime.LocalTime(5, 0), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Sunrise", "الشروق", kotlinx.datetime.LocalTime(6, 30), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Dhuhr", "الظهر", kotlinx.datetime.LocalTime(12, 30), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Asr", "العصر", kotlinx.datetime.LocalTime(15, 30), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Maghrib", "المغرب", kotlinx.datetime.LocalTime(18, 30), kotlinx.datetime.LocalDate(2026, 9, 2)),
            Prayer("Isha", "العشاء", kotlinx.datetime.LocalTime(20, 30), kotlinx.datetime.LocalDate(2026, 9, 2))
        )

        val localized = formatter.withLocalizedNames(prayers)

        assertThat(localized.map { it.name })
            .containsExactly("İmsak", "Fecir", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı")
            .inOrder()
    }
}
