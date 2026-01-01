package com.kutluoglu.prayer_qibla

import com.kutluoglu.prayer.data.qibla.QiblaDataStore
import com.kutluoglu.prayer.model.qibla.QiblaState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Single

/**
 * Created by F.K. on 1.01.2026.
 *
 */

@Single
class QiblaDataStoreImp(
    private val sensorService: SensorService
): QiblaDataStore {
    override suspend fun startCompass() = sensorService.startCompass()

    override suspend fun stopCompass() = sensorService.stopCompass()

    override suspend fun calculateQiblaBearing(latitude: Double, longitude: Double) =
        sensorService.calculateQiblaBearing(latitude, longitude)

    override fun observeSensorStateAs(): Flow<QiblaState> = flow {
        sensorService.sensorState.collect { sensorState ->
            emit(
                QiblaState(
                    accuracy = sensorState.accuracy,
                    deviceAzimuth = sensorState.deviceAzimuth,
                    qiblaAngle = sensorState.qiblaAngle,
                    qiblaBearing = sensorState.qiblaBearing
                )
            )
        }
    }
}