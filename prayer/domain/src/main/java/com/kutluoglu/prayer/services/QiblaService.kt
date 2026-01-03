package com.kutluoglu.prayer.services

import com.kutluoglu.prayer.model.qibla.QiblaState
import kotlinx.coroutines.flow.Flow


/**
 * Created by F.K. on 1.01.2026.
 *
 */
interface QiblaService {
    fun getQiblaDirection(latitude: Double, longitude: Double): Flow<QiblaState>
    fun start()
    fun stop()
}