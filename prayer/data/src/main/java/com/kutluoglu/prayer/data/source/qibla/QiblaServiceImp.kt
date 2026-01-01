package com.kutluoglu.prayer.data.source.qibla

import com.kutluoglu.prayer.data.qibla.QiblaDataStore
import com.kutluoglu.prayer.model.qibla.QiblaState
import com.kutluoglu.prayer.services.QiblaService
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 1.01.2026.
 *
 */

@Single
class QiblaServiceImp(
    private val qiblaService: QiblaDataStore
): QiblaService {
    override suspend fun startCompass() = qiblaService.startCompass()

    override suspend fun stopCompass() = qiblaService.stopCompass()

    override suspend fun calculateQiblaBearing(latitude: Double, longitude: Double) =
        qiblaService.calculateQiblaBearing(latitude, longitude)

    override fun observeSensorStateAs(): Flow<QiblaState> = qiblaService.observeSensorStateAs()
}