package com.kutluoglu.core.common.utils

/**
 * Utility functions for angle math.
 */
object AngleUtils {

    /**
     * Normalizes an angle in degrees to the range [-180, 180).
     * E.g. 350 -> -10, -190 -> 170.
     */
    fun normalizeDegrees(angle: Float): Float {
        val normalized = ((angle % 360f) + 360f) % 360f
        return if (normalized > 180f) normalized - 360f else normalized
    }
}
