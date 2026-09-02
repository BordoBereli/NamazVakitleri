package com.kutluoglu.prayer.usecases.prayer

import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer.repository.IPrayerRepository
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Factory
import java.time.ZoneId

@Factory
class GetPrayerTimesUseCase(
    private val prayerRepository: IPrayerRepository
) {
    suspend operator fun invoke(
            date: LocalDateTime,
            latitude: Double,
            longitude: Double,
            zoneId: ZoneId,
            calculationMethod: CalculationMethod = CalculationMethod.TURKEY_DIYANET,
            imsakOffsetMinutes: Int = 10,
            juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
            persistDailyCache: Boolean = true,
    ): Result<List<Prayer>> {
        return try {
            val prayerTimes = prayerRepository.getPrayerTimes(
                date, latitude, longitude, zoneId, calculationMethod, imsakOffsetMinutes, juristicMethod, persistDailyCache
            )
            Result.success(prayerTimes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}