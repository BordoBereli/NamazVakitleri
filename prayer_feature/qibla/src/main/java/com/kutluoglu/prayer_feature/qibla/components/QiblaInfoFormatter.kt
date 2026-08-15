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

/**
 * Maps a qibla angle to a user-facing label.
 *
 * The angle is normalized to [-180, 180]. A positive angle means the user must
 * turn RIGHT, a negative angle means turn LEFT. The threshold is inclusive:
 * an angle whose absolute value is exactly `threshold` is considered aligned.
 */
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
