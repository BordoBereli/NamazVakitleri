package com.kutluoglu.prayer_qibla

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure quaternion-based complementary filter that fuses gyroscope angular
 * velocity with an accelerometer+magnetometer reference orientation.
 *
 * No Android dependencies — fully unit-testable.
 *
 * @param gyroGain weight applied to the gyroscope integration (0..1)
 * @param referenceGain weight applied to the accel/mag reference (0..1)
 */
class OrientationFusion(
    private val gyroGain: Float = 0.98f,
    private val referenceGain: Float = 0.02f
) {
    private var qw = 1f
    private var qx = 0f
    private var qy = 0f
    private var qz = 0f
    private var initialized = false

    val isInitialized: Boolean get() = initialized

    /**
     * Integrates gyroscope angular velocity (rad/s) over [dtSeconds].
     */
    fun updateWithGyro(gx: Float, gy: Float, gz: Float, dtSeconds: Float) {
        if (dtSeconds <= 0f) return
        val magnitude = sqrt(gx * gx + gy * gy + gz * gz)
        val angle = magnitude * dtSeconds
        if (angle == 0f) return
        // Exact axis-angle integration: q_new = q ⊗ (cos(θ/2), ω̂·sin(θ/2))
        val halfAngle = 0.5f * angle
        val sinHalf = sin(halfAngle)
        val cosHalf = cos(halfAngle)
        val dw = cosHalf
        val dx = (gx / magnitude) * sinHalf
        val dy = (gy / magnitude) * sinHalf
        val dz = (gz / magnitude) * sinHalf
        val nw = qw * dw - qx * dx - qy * dy - qz * dz
        val nx = qw * dx + qx * dw + qy * dz - qz * dy
        val ny = qw * dy - qx * dz + qy * dw + qz * dx
        val nz = qw * dz + qx * dy - qy * dx + qz * dw
        qw = nw
        qx = nx
        qy = ny
        qz = nz
        normalize()
    }

    /**
     * Fuses the accelerometer+magnetometer reference rotation matrix into the
     * current orientation. The first call initializes the filter.
     */
    fun correctWithRotationMatrix(matrix: FloatArray) {
        val ref = quaternionFromRotationMatrix(matrix)
        if (!initialized) {
            qw = ref.w
            qx = ref.x
            qy = ref.y
            qz = ref.z
            initialized = true
            return
        }
        // nlerp (normalized linear interpolation) — sufficient for small angles
        val dot = qw * ref.w + qx * ref.x + qy * ref.y + qz * ref.z
        val sign = if (dot < 0f) -1f else 1f
        val alpha = referenceGain
        val beta = 1f - alpha
        qw = alpha * qw + beta * ref.w * sign
        qx = alpha * qx + beta * ref.x * sign
        qy = alpha * qy + beta * ref.y * sign
        qz = alpha * qz + beta * ref.z * sign
        normalize()
    }

    /**
     * Writes the current orientation as a 3x3 row-major rotation matrix
     * (device frame -> world frame) into [out].
     */
    fun toRotationMatrix(out: FloatArray) {
        val w = qw
        val x = qx
        val y = qy
        val z = qz
        out[0] = 1 - 2 * (y * y + z * z)
        out[1] = 2 * (x * y + w * z)
        out[2] = 2 * (x * z - w * y)
        out[3] = 2 * (x * y - w * z)
        out[4] = 1 - 2 * (x * x + z * z)
        out[5] = 2 * (y * z + w * x)
        out[6] = 2 * (x * z + w * y)
        out[7] = 2 * (y * z - w * x)
        out[8] = 1 - 2 * (x * x + y * y)
    }

    fun reset() {
        qw = 1f
        qx = 0f
        qy = 0f
        qz = 0f
        initialized = false
    }

    private fun normalize() {
        val norm = sqrt(qw * qw + qx * qx + qy * qy + qz * qz)
        if (norm == 0f) return
        qw /= norm
        qx /= norm
        qy /= norm
        qz /= norm
    }

    private fun quaternionFromRotationMatrix(m: FloatArray): Quat {
        val trace = m[0] + m[4] + m[8]
        return if (trace > 0f) {
            val s = sqrt(trace + 1f) * 2f
            Quat(
                w = 0.25f * s,
                x = (m[5] - m[7]) / s,
                y = (m[6] - m[2]) / s,
                z = (m[1] - m[3]) / s
            )
        } else if (m[0] > m[4] && m[0] > m[8]) {
            val s = sqrt(1f + m[0] - m[4] - m[8]) * 2f
            Quat(
                w = (m[5] - m[7]) / s,
                x = 0.25f * s,
                y = (m[1] + m[3]) / s,
                z = (m[2] + m[6]) / s
            )
        } else if (m[4] > m[8]) {
            val s = sqrt(1f + m[4] - m[0] - m[8]) * 2f
            Quat(
                w = (m[6] - m[2]) / s,
                x = (m[1] + m[3]) / s,
                y = 0.25f * s,
                z = (m[5] + m[7]) / s
            )
        } else {
            val s = sqrt(1f + m[8] - m[0] - m[4]) * 2f
            Quat(
                w = (m[1] - m[3]) / s,
                x = (m[2] + m[6]) / s,
                y = (m[5] + m[7]) / s,
                z = 0.25f * s
            )
        }
    }

    private data class Quat(val w: Float, val x: Float, val y: Float, val z: Float)
}
