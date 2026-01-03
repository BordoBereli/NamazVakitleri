package com.kutluoglu.prayer_qibla

/**
 * Created by F.K. on 1.01.2026.
 *
 */
data class SensorState(
    val sensorAccuracy: Int = -1,
    val deviceAzimuth: Float = 0.0f,
    val qiblaAngle: Float = 0.0f,
    val qiblaBearing: Double = 0.0
)
