package com.kutluoglu.prayer_qibla

import com.kutluoglu.prayer.data.qibla.QiblaDataStore
import com.kutluoglu.prayer.model.qibla.QiblaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class QiblaDataStoreImp(
    private val sensorService: SensorService,
    private val orientationProvider: OrientationProvider
) : QiblaDataStore {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getQiblaDirection(
        latitude: Double,
        longitude: Double
    ): Flow<QiblaState> = channelFlow {
        sensorService.startCompass()
        val job = launch {
            sensorService.rawSensorState.collect {
                val finalState = orientationProvider.getOrientation(it, latitude, longitude)
                trySend(
                    QiblaState(
                        qiblaAngle = finalState.qiblaAngle,
                        deviceAzimuth = finalState.deviceAzimuth,
                        sensorAccuracy = finalState.sensorAccuracy,
                        qiblaBearing = finalState.qiblaBearing
                    )
                )
            }
        }
        awaitClose {
            job.cancel()
            sensorService.stopCompass()
        }
    }.flowOn(Dispatchers.Default)

    override fun start() {
        orientationProvider.reset()
        sensorService.startCompass()
    }

    override fun stop() = sensorService.stopCompass()
}
