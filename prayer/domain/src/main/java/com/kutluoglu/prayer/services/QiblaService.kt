package com.kutluoglu.prayer.services

import com.kutluoglu.prayer.model.qibla.QiblaState
import kotlinx.coroutines.flow.Flow


/**
 * Created by F.K. on 1.01.2026.
 *
 */
interface QiblaService {
    suspend fun startCompass()
    suspend fun stopCompass()
    suspend fun calculateQiblaBearing(latitude: Double, longitude: Double)
    fun observeSensorStateAs(): Flow<QiblaState>
}