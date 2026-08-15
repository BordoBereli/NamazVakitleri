package com.kutluoglu.prayer_feature.qibla.components

import android.hardware.SensorManager
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class QiblaInfoFormatterTest {

    @Test
    fun `accuracy level maps sensor accuracy`() {
        assertThat(accuracyLevel(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
            .isEqualTo(AccuracyLevel.HIGH)
        assertThat(accuracyLevel(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
            .isEqualTo(AccuracyLevel.MEDIUM)
        assertThat(accuracyLevel(SensorManager.SENSOR_STATUS_ACCURACY_LOW))
            .isEqualTo(AccuracyLevel.LOW)
        assertThat(accuracyLevel(SensorManager.SENSOR_STATUS_UNRELIABLE))
            .isEqualTo(AccuracyLevel.LOW)
    }

    @Test
    fun `small angle is aligned`() {
        assertThat(qiblaDistanceLabel(0f, 10f)).isEqualTo(QiblaDistanceLabel.Aligned)
        assertThat(qiblaDistanceLabel(9.9f, 10f)).isEqualTo(QiblaDistanceLabel.Aligned)
        assertThat(qiblaDistanceLabel(-9.9f, 10f)).isEqualTo(QiblaDistanceLabel.Aligned)
    }

    @Test
    fun `positive angle means turn right`() {
        val label = qiblaDistanceLabel(12f, 10f)
        assertThat(label).isEqualTo(
            QiblaDistanceLabel.Turn(12, TurnDirection.RIGHT)
        )
    }

    @Test
    fun `negative angle means turn left`() {
        val label = qiblaDistanceLabel(-25.6f, 10f)
        assertThat(label).isEqualTo(
            QiblaDistanceLabel.Turn(26, TurnDirection.LEFT)
        )
    }

    @Test
    fun `angle wraps around 180`() {
        // 350 degrees normalized is -10 -> aligned at threshold 10
        assertThat(qiblaDistanceLabel(350f, 10f)).isEqualTo(QiblaDistanceLabel.Aligned)
        // 200 degrees normalized is -160 -> turn left 160
        assertThat(qiblaDistanceLabel(200f, 10f)).isEqualTo(
            QiblaDistanceLabel.Turn(160, TurnDirection.LEFT)
        )
    }
}
