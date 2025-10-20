import com.kutluoglu.prayer.domain.PrayerTimeEngine
import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.JuristicMethod
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime


class PrayerCalculationServiceTest {

    @Test
    fun `calculatePrayerTimes fails because it does not exist yet`() {
        // GIVEN a specific, known set of parameters
        val service = PrayerTimeEngine()
        val date = LocalDate.of(2025, 9, 15)
        val latitude = 41.03648429460445 // Halkalı/Küçükçekmece/Istanbul
        val longitude = 28.79004556525033
        val method = CalculationMethod.TURKEY_DIYANET
        val juristic = JuristicMethod.STANDARD

        // WHEN we call the function calculatePrayerTimes
        val prayers = service.calculateDailyPrayerTimes(
            latitude,
            longitude,
            date,
            method,
            juristic
        )

        // THEN we expect a specific, verifiable result
        val fajrPrayer = prayers.first { it.name == "Fajr" }
        val sunrisePrayer = prayers.first { it.name == "Sunrise" }
        val dhuhrPrayer = prayers.first { it.name == "Dhuhr" }
        val asrPrayer = prayers.first { it.name == "Asr" }
        val maghribPrayer = prayers.first { it.name == "Maghrib" }
        val ishaPrayer = prayers.first { it.name == "Isha" }
        // NOTE: Replace this with a time from a trusted source for that exact date/location.
        assertThat(fajrPrayer.time).isEqualTo(LocalTime.of(5, 12))
        assertThat(sunrisePrayer.time).isEqualTo(LocalTime.of(6, 38))
        assertThat(dhuhrPrayer.time).isEqualTo(LocalTime.of(13, 5))
        assertThat(asrPrayer.time).isEqualTo(LocalTime.of(16, 34))
        assertThat(maghribPrayer.time).isEqualTo(LocalTime.of(19, 21))
        assertThat(ishaPrayer.time).isEqualTo(LocalTime.of(20, 42))
    }
}

