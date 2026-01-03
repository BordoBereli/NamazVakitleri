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
    private val qiblaDataStore: QiblaDataStore
): QiblaService {
    override fun getQiblaDirection(
            latitude: Double,
            longitude: Double
    ): Flow<QiblaState> = qiblaDataStore.getQiblaDirection(latitude, longitude)

    override fun start() = qiblaDataStore.start()

    override fun stop() = qiblaDataStore.stop()

}