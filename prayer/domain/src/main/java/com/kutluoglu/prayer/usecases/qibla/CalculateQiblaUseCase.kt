package com.kutluoglu.prayer.usecases.qibla

import com.kutluoglu.prayer.services.QiblaService
import org.koin.core.annotation.Factory

@Factory
class CalculateQiblaUseCase(
    private val qiblaService: QiblaService
) {

    suspend operator fun invoke() {
        qiblaService.startCompass()
    }

    suspend fun stopCompass() {
        qiblaService.stopCompass()
    }

    suspend fun calculateQiblaBearing(latitude: Double, longitude: Double) {
        qiblaService.calculateQiblaBearing(latitude, longitude)
    }

    fun observeSensorStateAs() = qiblaService.observeSensorStateAs()
}