package com.kutluoglu.prayer_qibla

import android.hardware.SensorManager
import android.view.Surface
import com.kutluoglu.core.ui.theme.common.DisplayProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Single
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Created by F.K. on 1.01.2026.
 *
 */

/**
 * A Factory that requires an Activity Context to calculate orientation
 * based on the current screen rotation.
 */
@Single
class OrientationProvider(private val displayProvider: DisplayProvider) {
    private val remappedRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Kaaba'nın koordinatları
    private val kaabaLatitude = 21.4225
    private val kaabaLongitude = 39.8262

    private var _sensorState = MutableStateFlow(SensorState())
    val sensorState = _sensorState.asStateFlow()

    /**
     * Calculates the final SensorState using the raw rotation matrix and location data.
     */
    fun getOrientation(rawState: RawSensorState, userLatitude: Double?, userLongitude: Double?): SensorState {
        if (rawState.rotationMatrix == null) {
            _sensorState.update { it.copy(sensorAccuracy = rawState.accuracy) }
        } else {

            // 1. Ekran rotasyonuna göre koordinat sistemini yeniden haritala
            when (displayProvider.display().rotation) {
                Surface.ROTATION_0   -> remap(rawState.rotationMatrix, 1, 3) // AXIS_X, AXIS_Y
                Surface.ROTATION_90 -> remap(rawState.rotationMatrix, 3, -1) // AXIS_Y, AXIS_MINUS_X
                Surface.ROTATION_180 -> remap(rawState.rotationMatrix, -1, -3) // AXIS_MINUS_X, AXIS_MINUS_Y
                Surface.ROTATION_270 -> remap(rawState.rotationMatrix, -3, 1) // AXIS_MINUS_Y, AXIS_X
            }

            // 2. Yönelim açılarını hesapla
            SensorManager.getOrientation(remappedRotationMatrix, orientationAngles)
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalizedAzimuth = (azimuth + 360) % 360

            // 3. Kıble yönünü hesapla (eğer konum varsa)
            val qiblaBearing = if (userLatitude != null && userLongitude != null) {
                calculateBearing(userLatitude, userLongitude)
            } else {
                0.0
            }

            val qiblaAngle = (qiblaBearing - normalizedAzimuth).toFloat()

            _sensorState.update {
                it.copy(
                    deviceAzimuth = normalizedAzimuth,
                    qiblaBearing = qiblaBearing,
                    qiblaAngle = qiblaAngle,
                    sensorAccuracy = rawState.accuracy
                )
            }
        }
        return sensorState.value
    }

    private fun remap(matrix: FloatArray, x: Int, y: Int) {
        SensorManager.remapCoordinateSystem(matrix, x, y, remappedRotationMatrix)
    }

    private fun calculateBearing(latitude: Double, longitude: Double): Double {
        val phiK = Math.toRadians(kaabaLatitude)
        val lambdaK = Math.toRadians(kaabaLongitude)
        val phi = Math.toRadians(latitude)
        val lambda = Math.toRadians(longitude)
        val psi = Math.toDegrees(
            atan2(sin(lambdaK - lambda),
                cos(phi) * tan(phiK) - sin(phi) * cos(lambdaK - lambda)
            )
        )
        return (psi + 360) % 360 // Normalleştir
    }
}
