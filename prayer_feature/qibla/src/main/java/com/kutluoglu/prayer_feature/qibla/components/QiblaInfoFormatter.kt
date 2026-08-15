package com.kutluoglu.prayer_feature.qibla.components

import android.hardware.SensorManager
import com.kutluoglu.core.common.utils.AngleUtils
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AccuracyLevel { HIGH, MEDIUM, LOW }

fun accuracyLevel(sensorAccuracy: Int): AccuracyLevel = when {
    sensorAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> AccuracyLevel.HIGH
    sensorAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> AccuracyLevel.MEDIUM
    else -> AccuracyLevel.LOW
}

sealed interface QiblaDistanceLabel {
    data object Aligned : QiblaDistanceLabel
    data class Turn(val degrees: Int, val direction: TurnDirection) : QiblaDistanceLabel
}

enum class TurnDirection { LEFT, RIGHT }

fun qiblaDistanceLabel(qiblaAngle: Float, threshold: Float): QiblaDistanceLabel {
    val normalized = AngleUtils.normalizeDegrees(qiblaAngle)
    return if (abs(normalized) <= threshold) {
        QiblaDistanceLabel.Aligned
    } else if (normalized > 0) {
        QiblaDistanceLabel.Turn(normalized.roundToInt(), TurnDirection.RIGHT)
    } else {
        QiblaDistanceLabel.Turn(abs(normalized).roundToInt(), TurnDirection.LEFT)
    }
}
