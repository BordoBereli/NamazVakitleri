package com.kutluoglu.prayer.model.qibla

/**
 * Created by F.K. on 1.01.2026.
 *
 */
data class QiblaState(
    val accuracy: Int = -1,
    val deviceAzimuth: Float = 0.0f,
    val qiblaAngle: Float = 0.0f,
    val qiblaBearing: Double = 0.0
)
