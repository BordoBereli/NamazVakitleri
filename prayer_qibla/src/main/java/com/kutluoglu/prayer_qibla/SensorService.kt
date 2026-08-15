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

/**
 * Manages raw sensor data listening (Accelerometer, Magnetometer and Gyroscope).
 * It is safe to be a Singleton as it only uses ApplicationContext.
 * It has NO knowledge of screen rotation or display.
 */
@Single
class SensorService(context: Context) : SensorEventListener {
    private val sensorManager: SensorManager = context
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)

    private var isRegistered = false

    private val _rawSensorState = MutableStateFlow(RawSensorState())
    val rawSensorState = _rawSensorState.asStateFlow()

    fun startCompass(): Unit {
        if (isRegistered) return
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        var registered = 0
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            registered++
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            registered++
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            registered++
        }
        isRegistered = registered > 0
    }

    fun stopCompass(): Unit {
        if (!isRegistered) return
        sensorManager.unregisterListener(this)
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, event.values.size)
                updateRotationMatrix(event.timestamp)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, event.values.size)
                updateRotationMatrix(event.timestamp)
            }
            Sensor.TYPE_GYROSCOPE -> {
                _rawSensorState.update {
                    it.copy(
                        gyro = event.values.clone(),
                        timestamp = event.timestamp
                    )
                }
            }
        }
    }

    private fun updateRotationMatrix(timestamp: Long) {
        val rotationOK = SensorManager.getRotationMatrix(
            rotationMatrix, null, accelerometerReading, magnetometerReading
        )
        if (rotationOK) {
            _rawSensorState.update {
                it.copy(
                    rotationMatrix = rotationMatrix.clone(),
                    timestamp = timestamp
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _rawSensorState.update {
            it.copy(accuracy = accuracy)
        }
    }
}
