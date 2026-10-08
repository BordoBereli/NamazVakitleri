package com.kutluoglu.prayer_auto.screen

import androidx.car.app.CarContext
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer.domain.SurfacePrayerData
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.Prayer
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PrayerTimesScreenTest {

    private val today = LocalDate(2026, 10, 5)
    private val tomorrow = LocalDate(2026, 10, 6)

    private fun surfaceData(tomorrowImsakTime: LocalTime?): SurfacePrayerData = SurfacePrayerData(
        location = LocationData(41.0082, 28.9784, "Türkiye", "TR", "İstanbul", "İstanbul", "Europe/Istanbul"),
        city = "İstanbul",
        district = null,
        nextPrayerName = "Imsak",
        nextPrayerTime = "05:48",
        nextPrayerLocalTime = LocalTime(5, 48),
        currentPrayerLocalTime = LocalTime(20, 0),
        nextPrayerEpochMillis = 0L,
        currentPrayerEpochMillis = 0L,
        isJumuah = false,
        hijriAdjustment = 0,
        prayers = listOf(
            Prayer("Imsak", "الإمساك", LocalTime(5, 47), today, isImsak = true),
            Prayer("Sunrise", "الشروق", LocalTime(7, 15), today),
            Prayer("Dhuhr", "الظهر", LocalTime(13, 0), today),
            Prayer("Asr", "العصر", LocalTime(16, 10), today),
            Prayer("Maghrib", "المغرب", LocalTime(18, 45), today),
            Prayer("Isha", "العشاء", LocalTime(20, 0), today),
        ),
        nextPrayerDate = tomorrow,
        tomorrowImsakTime = tomorrowImsakTime
    )

    private fun screenWith(data: SurfacePrayerData?): PrayerTimesScreen {
        val carContext = mockk<CarContext>(relaxed = true)
        every { carContext.getString(any()) } returns "Yarın"
        val screen = PrayerTimesScreen(
            carContext,
            mockk<PrayerSurfaceDataProvider>(relaxed = true),
            Clock.fixed(Instant.parse("2026-10-05T21:00:00Z"), ZoneOffset.UTC)
        )
        if (data != null) {
            PrayerTimesScreen::class.java
                .getDeclaredField("surface")
                .apply { isAccessible = true }
                .set(screen, data)
        }
        return screen
    }

    private fun rows(screen: PrayerTimesScreen): List<Row> {
        val template = screen.onGetTemplate()
        assertThat(template).isInstanceOf(ListTemplate::class.java)
        val itemList = (template as ListTemplate).singleList
        assertThat(itemList).isNotNull()
        return itemList!!.items.filterIsInstance<Row>()
    }

    @Test
    fun `after Isha appends tomorrow Imsak row with exact provider time`() {
        val rows = rows(screenWith(surfaceData(tomorrowImsakTime = LocalTime(5, 48))))

        val last = rows.last()
        assertThat(last.title.toString()).contains("Yarın")
        assertThat(last.texts.single().toString()).isEqualTo("05:48")
    }

    @Test
    fun `after Isha falls back to today Imsak when provider time is null`() {
        val rows = rows(screenWith(surfaceData(tomorrowImsakTime = null)))

        val last = rows.last()
        assertThat(last.title.toString()).contains("Yarın")
        assertThat(last.texts.single().toString()).isEqualTo("05:47")
    }
}
