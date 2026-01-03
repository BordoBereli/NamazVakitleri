package com.kutluoglu.prayer_qibla

import android.util.Log
import com.kutluoglu.prayer.data.qibla.QiblaDataStore
import com.kutluoglu.prayer.model.qibla.QiblaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Single
import org.koin.java.KoinJavaComponent.inject

/**
 * Created by F.K. on 1.01.2026.
 *
 */

@Single
class QiblaDataStoreImp(
    private val sensorService: SensorService,
        private val orientationProvider: OrientationProvider
): QiblaDataStore {
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
                ).onSuccess {
                    Log.e("QiblaDataStoreImp", "getQiblaDirection: $finalState")
                }.onFailure {
                    Log.e(
                        "QiblaDataStoreImp",
                        "getQiblaDirection: $finalState, Error Msg: ${it?.message}"
                    )
                }
            }
        }
        awaitClose {
            Log.d("QiblaDataStoreImp", "Flow is closing. Stopping compass.")
            job.cancel()
            sensorService.stopCompass()
        }
    }.flowOn(Dispatchers.Default)

    override fun start() = sensorService.startCompass()

    override fun stop() = sensorService.stopCompass()
}