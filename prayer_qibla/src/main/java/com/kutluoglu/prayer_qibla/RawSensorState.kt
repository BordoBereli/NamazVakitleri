package com.kutluoglu.prayer_qibla

import android.hardware.SensorManager

// SensorService tarafından yayınlanan ham veri modeli
data class RawSensorState(
    val rotationMatrix: FloatArray? = null,
    val accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
) {
    // equals ve hashCode'u içeriğe göre doğru çalışması için override et
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RawSensorState
        if (rotationMatrix != null) {
            if (other.rotationMatrix == null) return false
            if (!rotationMatrix.contentEquals(other.rotationMatrix)) return false
        } else if (other.rotationMatrix != null) return false
        if (accuracy != other.accuracy) return false
        return true
    }

    override fun hashCode(): Int {
        var result = rotationMatrix?.contentHashCode() ?: 0
        result = 31 * result + accuracy
        return result
    }
}