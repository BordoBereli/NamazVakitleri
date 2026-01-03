package com.kutluoglu.prayer.usecases.qibla

import com.kutluoglu.prayer.services.QiblaService
import org.koin.core.annotation.Factory

@Factory
class CalculateQiblaUseCase(
    private val qiblaService: QiblaService
) {
    fun observeQiblaDirection(latitude: Double, longitude: Double) =
        qiblaService.getQiblaDirection(latitude, longitude)

    fun stop() = qiblaService.stop()
}