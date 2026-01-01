package com.kutluoglu.prayer.data.qibla

import com.kutluoglu.prayer.model.qibla.QiblaState
import kotlinx.coroutines.flow.Flow

/**
 * Created by F.K. on 1.01.2026.
 *
 */

/**
 * Interface defining methods for the data operations related to Qibla.
 * This is to be implemented by external data source layers, setting the requirements for the
 * operations that need to be implemented
 */

interface QiblaDataStore {
    suspend fun startCompass()
    suspend fun stopCompass()
    suspend fun calculateQiblaBearing(latitude: Double, longitude: Double)
    fun observeSensorStateAs(): Flow<QiblaState>
}