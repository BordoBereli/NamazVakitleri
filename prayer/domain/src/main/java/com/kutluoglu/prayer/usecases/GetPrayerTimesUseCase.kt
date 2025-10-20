package com.kutluoglu.prayer.usecases

import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer.repository.IPrayerRepository
import java.time.LocalDate

class GetPrayerTimesUseCase(
    private val prayerRepository: IPrayerRepository
) {
    suspend operator fun invoke(date: LocalDate, latitude: Double, longitude: Double): Result<List<Prayer>> {
        return try {
            val prayerTimes = prayerRepository.getPrayerTimes(date, latitude, longitude)
            Result.success(prayerTimes)
        } catch (e: Exception) {
            // You can wrap exceptions here for better error handling in the ViewModel
            Result.failure(e)
        }
    }
}