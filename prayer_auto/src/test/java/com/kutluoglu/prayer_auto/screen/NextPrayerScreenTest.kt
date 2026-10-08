package com.kutluoglu.prayer_auto.screen

import androidx.car.app.CarContext
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.PaneTemplate
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
class NextPrayerScreenTest {

    private val today = LocalDate(2026, 10, 5)

    private fun surfaceData(currentPrayerName: String?): SurfacePrayerData = SurfacePrayerData(
        location = LocationData(41.0082, 28.9784, "Türkiye", "TR", "İstanbul", "İstanbul", "Europe/Istanbul"),
        city = "İstanbul",
        district = null,
        nextPrayerName = "Isha",
        nextPrayerTime = "20:00",
        nextPrayerLocalTime = LocalTime(20, 0),
        currentPrayerLocalTime = if (currentPrayerName != null) LocalTime(18, 45) else null,
        nextPrayerEpochMillis = 0L,
        currentPrayerEpochMillis = 0L,
        isJumuah = false,
        hijriAdjustment = 0,
        prayers = listOf(
            Prayer("Maghrib", "المغرب", LocalTime(18, 45), today),
            Prayer("Isha", "العشاء", LocalTime(20, 0), today),
        ),
        nextPrayerDate = today,
        currentPrayerName = currentPrayerName
    )

    private fun screenWith(data: SurfacePrayerData?): NextPrayerScreen {
        val carContext = mockk<CarContext>(relaxed = true)
        every { carContext.getString(any()) } returns "Isha"
        val screen = NextPrayerScreen(
            carContext,
            mockk<PrayerSurfaceDataProvider>(relaxed = true),
            Clock.fixed(Instant.parse("2026-10-05T19:00:00Z"), ZoneOffset.UTC)
        )
        if (data != null) {
            NextPrayerScreen::class.java
                .getDeclaredField("surface")
                .apply { isAccessible = true }
                .set(screen, data)
        }
        return screen
    }

    private fun rows(screen: NextPrayerScreen): List<Row> {
        val template = screen.onGetTemplate()
        assertThat(template).isInstanceOf(PaneTemplate::class.java)
        val pane = (template as PaneTemplate).pane
        assertThat(pane).isNotNull()
        return pane!!.rows.filterIsInstance<Row>()
    }

    @Test
    fun `pane shows current prayer row above next prayer row`() {
        val rows = rows(screenWith(surfaceData(currentPrayerName = "Maghrib")))

        assertThat(rows).hasSize(2)
        assertThat(rows[0].title.toString()).startsWith("●")
        assertThat(rows[0].title.toString()).contains("Maghrib")
        val span = rows[0].texts.single().spans.single().carSpan
        assertThat(span).isInstanceOf(ForegroundCarColorSpan::class.java)
        assertThat((span as ForegroundCarColorSpan).color).isEqualTo(CarColor.PRIMARY)
    }

    @Test
    fun `pane omits current prayer row when current prayer is unknown`() {
        val rows = rows(screenWith(surfaceData(currentPrayerName = null)))

        assertThat(rows).hasSize(1)
        assertThat(rows[0].title.toString()).doesNotContain("●")
    }
}
