package com.kutluoglu.prayer_qibla

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class OrientationFusionTest {

    private fun identityMatrix(): FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )

    private fun azimuthOf(matrix: FloatArray): Float {
        val degrees = Math.toDegrees(atan2(matrix[1].toDouble(), matrix[4].toDouble()))
        return ((degrees + 360) % 360).toFloat()
    }

    @Test
    fun `gyro integration produces expected rotation magnitude around z`() {
        val fusion = OrientationFusion()
        fusion.correctWithRotationMatrix(identityMatrix())

        val gyroZ = Math.toRadians(90.0).toFloat() // 90 deg/s
        fusion.updateWithGyro(0f, 0f, gyroZ, 1f)   // for 1 second

        val m = FloatArray(9)
        fusion.toRotationMatrix(m)
        // rotation around z by 90 degrees: m[1] = +/-sin(90) = +/-1, m[0] = cos(90) ~ 0
        assertThat(abs(m[1])).isWithin(0.05f).of(1f)
        assertThat(abs(m[0])).isWithin(0.05f).of(0f)
    }

    @Test
    fun `correction pulls orientation toward reference`() {
        val fusion = OrientationFusion()
        fusion.correctWithRotationMatrix(identityMatrix())

        val gyroZ = Math.toRadians(10.0).toFloat()
        fusion.updateWithGyro(0f, 0f, gyroZ, 1f) // drift 10 degrees

        val before = FloatArray(9)
        fusion.toRotationMatrix(before)
        val azimuthBefore = azimuthOf(before)
        assertThat(azimuthBefore).isGreaterThan(0f)

        fusion.correctWithRotationMatrix(identityMatrix()) // reference = north (0)

        val after = FloatArray(9)
        fusion.toRotationMatrix(after)
        val azimuthAfter = azimuthOf(after)
        assertThat(azimuthAfter).isLessThan(azimuthBefore)
        assertThat(azimuthAfter).isGreaterThan(0f)
    }

    @Test
    fun `first correction initializes orientation to reference`() {
        val fusion = OrientationFusion()
        val heading = 90f
        val h = Math.toRadians(heading.toDouble())
        val c = cos(h).toFloat()
        val s = sin(h).toFloat()
        val matrix = floatArrayOf(
            c, s, 0f,
            -s, c, 0f,
            0f, 0f, 1f
        )

        fusion.correctWithRotationMatrix(matrix)

        val m = FloatArray(9)
        fusion.toRotationMatrix(m)
        assertThat(azimuthOf(m)).isWithin(0.5f).of(90f)
    }

    @Test
    fun `reset returns to identity`() {
        val fusion = OrientationFusion()
        fusion.correctWithRotationMatrix(identityMatrix())
        val gyroZ = Math.toRadians(90.0).toFloat()
        fusion.updateWithGyro(0f, 0f, gyroZ, 1f)

        fusion.reset()

        val m = FloatArray(9)
        fusion.toRotationMatrix(m)
        assertThat(m[0]).isWithin(0.01f).of(1f)
        assertThat(m[1]).isWithin(0.01f).of(0f)
    }

    @Test
    fun `tilted device still yields correct azimuth`() {
        val fusion = OrientationFusion()
        // pitch tilt 30 degrees around x, still pointing north
        val tilt = Math.toRadians(30.0)
        val c = cos(tilt).toFloat()
        val s = sin(tilt).toFloat()
        val matrix = floatArrayOf(
            1f, 0f, 0f,
            0f, c, s,
            0f, -s, c
        )

        fusion.correctWithRotationMatrix(matrix)

        val m = FloatArray(9)
        fusion.toRotationMatrix(m)
        assertThat(azimuthOf(m)).isWithin(0.5f).of(0f)
    }

    @Test
    fun `zero dt gyro update is a no-op`() {
        val fusion = OrientationFusion()
        fusion.correctWithRotationMatrix(identityMatrix())

        fusion.updateWithGyro(1f, 1f, 1f, 0f)

        val m = FloatArray(9)
        fusion.toRotationMatrix(m)
        assertThat(m[0]).isWithin(0.01f).of(1f)
        assertThat(m[1]).isWithin(0.01f).of(0f)
    }
}
