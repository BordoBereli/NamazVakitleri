package com.kutluoglu.prayer.usecases

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
    ): Result<List<Prayer>> {
        return try {
            val prayerTimes = prayerRepository.getPrayerTimes(
                date, latitude, longitude, zoneId
            )
            Result.success(prayerTimes)
        } catch (e: Exception) {
            // You can wrap exceptions here for better error handling in the ViewModel
            Result.failure(e)
        }
    }
}