package com.kutluoglu.prayer_qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Created by F.K. on 1.01.2026.
 *
 */

@Single
class SensorService(context: Context): SensorEventListener {
    private var sensorManager: SensorManager = context
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    // Kaaba'nın koordinatları
    private val kaabaLatitude = 21.4225
    private val kaabaLongitude = 39.8262

    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState = _sensorState.asStateFlow()

    fun startCompass() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopCompass() {
        sensorManager.unregisterListener(this)
    }

    fun calculateQiblaBearing(latitude: Double, longitude: Double) {
        val phiK = Math.toRadians(kaabaLatitude)
        val lambdaK = Math.toRadians(kaabaLongitude)
        val phi = Math.toRadians(latitude)
        val lambda = Math.toRadians(longitude)
        val psi = Math.toDegrees(
            atan2(
                sin(lambdaK - lambda),
                cos(phi) * tan(phiK) - sin(phi) * cos(lambdaK - lambda)
            )
        )
        val qiblaBearing = (psi + 360) % 360 // Normalleştir
        _sensorState.update {
            it.copy(qiblaBearing = qiblaBearing)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, event.values.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, event.values.size)
        }
        updateOrientation()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _sensorState.update { it.copy(accuracy = accuracy) }
    }

    private fun updateOrientation() {
        val rotationOK = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalizedAzimuth = (azimuth + 360) % 360

            _sensorState.update { currentState ->
                // Kıble yönü = Kıble Derecesi - Cihazın Azimut Açısı
                val qiblaAngle = (currentState.qiblaBearing - normalizedAzimuth).toFloat()
                currentState.copy(
                    deviceAzimuth = normalizedAzimuth,
                    qiblaAngle = qiblaAngle
                )
            }
        }
    }
}