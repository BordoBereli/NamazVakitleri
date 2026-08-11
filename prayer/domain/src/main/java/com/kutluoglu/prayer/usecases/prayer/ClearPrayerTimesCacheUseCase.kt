package com.kutluoglu.prayer.usecases.prayer

import com.kutluoglu.prayer.repository.IPrayerRepository
import org.koin.core.annotation.Factory

/**
 * Use case to clear the prayer times cache.
 */
@Factory
class ClearPrayerTimesCacheUseCase(
    private val prayerRepository: IPrayerRepository
) {
    suspend operator fun invoke() {
        prayerRepository.clearCache()
    }
}
