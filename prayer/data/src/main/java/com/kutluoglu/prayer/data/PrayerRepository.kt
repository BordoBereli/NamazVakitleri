package com.kutluoglu.prayer.data

import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.JuristicMethod
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer.repository.IPrayerRepository
import com.kutluoglu.prayer.services.PrayerCalculationService
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single


@Single
class PrayerRepository(
    private val prayerCalculationService: PrayerCalculationService
    // private val prayerDao: PrayerDao // We will add this later for caching
) : IPrayerRepository {
    override suspend fun getPrayerTimes(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double
    ): List<Prayer> {
        // TODO: Step 1 - Check Room database for cached prayer times for this date/location.
        // If found, return cached data.

        // Step 2 - If not cached, calculate new prayer times.
        // For now, we always calculate.
        val prayerTimes =  prayerCalculationService.calculateDailyPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            date = date,
            calculationMethod = CalculationMethod.TURKEY_DIYANET, // Using placeholder values
            juristicMethod = JuristicMethod.STANDARD
        )

        // TODO: Step 3 - Save the newly calculated times to the Room database.

        return prayerTimes
    }
}