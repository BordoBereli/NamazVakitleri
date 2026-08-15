# Qibla Feature Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Qibla screen (Classic Brass compass, hero layout, accuracy ring + badge, aligned state) and rework the sensor pipeline with a gyroscope complementary filter for smooth, accurate orientation.

**Architecture:** Two phases. Phase 1 (sensor pipeline, `prayer_qibla`) adds a pure quaternion-based `OrientationFusion` filter (gyro integration + accel/mag correction), wires the gyroscope through `SensorService`/`OrientationProvider`, and cleans up perf issues (log spam, lifecycle). Phase 2 (screen, `prayer_feature/qibla`) implements the Classic Brass visuals, hero layout, accuracy ring + badge, fixed info semantics, and aligned state, with pure formatter logic extracted and tested.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin, JUnit 5, MockK, Turbine, Truth.

---

## File Structure

**Phase 1 — `prayer_qibla` module:**
- Create: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/OrientationFusion.kt` — pure quaternion complementary filter
- Modify: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/RawSensorState.kt` — add gyro + timestamp
- Modify: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/SensorService.kt` — register gyroscope, idempotent start/stop, `SENSOR_DELAY_GAME`
- Modify: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/OrientationProvider.kt` — feed fusion, extract azimuth from fused quaternion
- Modify: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/QiblaDataStoreImp.kt` — remove `Log.e` spam
- Create: `prayer_qibla/src/test/java/com/kutluoglu/prayer_qibla/OrientationFusionTest.kt`
- Modify: `prayer_qibla/src/test/java/com/kutluoglu/prayer_qibla/OrientationProviderTest.kt` — add gyro fusion tests

**Phase 2 — `prayer_feature/qibla` module:**
- Modify: `prayer_feature/qibla/src/main/res/drawable/ic_kaaba.xml` — proper Kaaba vector drawable
- Modify: `prayer_feature/qibla/src/main/res/values/strings.xml` + `values-tr/strings.xml` — all UI strings
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoFormatter.kt` — pure logic (accuracy level, distance label)
- Create: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoFormatterTest.kt`
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaCompass.kt` — Classic Brass, graphicsLayer, accuracy ring
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoSection.kt` — fixed semantics, aligned state
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` — hero layout, badge, wiring

---

## Phase 1: Sensor Pipeline

### Task 1: `OrientationFusion` pure quaternion filter

**Files:**
- Create: `prayer_qibla/src/test/java/com/kutluoglu/prayer_qibla/OrientationFusionTest.kt`
- Create: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/OrientationFusion.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_qibla:testDebugUnitTest --tests="com.kutluoglu.prayer_qibla.OrientationFusionTest"`
Expected: FAIL with "Unresolved reference: OrientationFusion"

- [ ] **Step 3: Write the implementation**

```kotlin
package com.kutluoglu.prayer_qibla

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
        val halfDt = 0.5f * dtSeconds
        val dqw = -halfDt * (qx * gx + qy * gy + qz * gz)
        val dqx = halfDt * (qw * gx + qy * gz - qz * gy)
        val dqy = halfDt * (qw * gy + qz * gx - qx * gz)
        val dqz = halfDt * (qw * gz + qx * gy - qy * gx)
        qw += dqw
        qx += dqx
        qy += dqy
        qz += dqz
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
                x = (m[7] - m[5]) / s,
                y = (m[2] - m[6]) / s,
                z = (m[3] - m[1]) / s
            )
        } else if (m[0] > m[4] && m[0] > m[8]) {
            val s = sqrt(1f + m[0] - m[4] - m[8]) * 2f
            Quat(
                w = (m[7] - m[5]) / s,
                x = 0.25f * s,
                y = (m[1] + m[3]) / s,
                z = (m[2] + m[6]) / s
            )
        } else if (m[4] > m[8]) {
            val s = sqrt(1f + m[4] - m[0] - m[8]) * 2f
            Quat(
                w = (m[2] - m[6]) / s,
                x = (m[1] + m[3]) / s,
                y = 0.25f * s,
                z = (m[5] + m[7]) / s
            )
        } else {
            val s = sqrt(1f + m[8] - m[0] - m[4]) * 2f
            Quat(
                w = (m[3] - m[1]) / s,
                x = (m[2] + m[6]) / s,
                y = (m[5] + m[7]) / s,
                z = 0.25f * s
            )
        }
    }

    private data class Quat(val w: Float, val x: Float, val y: Float, val z: Float)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_qibla:testDebugUnitTest --tests="com.kutluoglu.prayer_qibla.OrientationFusionTest"`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/OrientationFusion.kt prayer_qibla/src/test/java/com/kutluoglu/prayer_qibla/OrientationFusionTest.kt
git commit -m "feat(qibla): add pure quaternion complementary filter with tests"
```

---

### Task 2: Extend `RawSensorState` with gyro + timestamp

**Files:**
- Modify: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/RawSensorState.kt`

- [ ] **Step 1: Update the data class**

Replace the entire file content with:

```kotlin
package com.kutluoglu.prayer_qibla

import android.hardware.SensorManager

// SensorService tarafından yayınlanan ham veri modeli
data class RawSensorState(
    val rotationMatrix: FloatArray? = null,
    val gyro: FloatArray? = null,
    val timestamp: Long = 0L,
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
        if (gyro != null) {
            if (other.gyro == null) return false
            if (!gyro.contentEquals(other.gyro)) return false
        } else if (other.gyro != null) return false
        if (timestamp != other.timestamp) return false
        if (accuracy != other.accuracy) return false
        return true
    }

    override fun hashCode(): Int {
        var result = rotationMatrix?.contentHashCode() ?: 0
        result = 31 * result + (gyro?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + accuracy
        return result
    }
}
```

- [ ] **Step 2: Verify existing tests still compile**

Run: `./gradlew :prayer_qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/RawSensorState.kt
git commit -m "feat(qibla): extend RawSensorState with gyro and timestamp"
```

---

### Task 3: `SensorService` — register gyroscope, idempotent lifecycle, `SENSOR_DELAY_GAME`

**Files:**
- Modify: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/SensorService.kt`

- [ ] **Step 1: Update the implementation**

Replace the file content with:

```kotlin
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

    fun startCompass() {
        if (isRegistered) return
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        isRegistered = true
    }

    fun stopCompass() {
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
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer_qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/SensorService.kt
git commit -m "feat(qibla): register gyroscope, idempotent lifecycle, SENSOR_DELAY_GAME"
```

---

### Task 4: `OrientationProvider` — feed the fusion filter

**Files:**
- Modify: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/OrientationProvider.kt`
- Modify: `prayer_qibla/src/test/java/com/kutluoglu/prayer_qibla/OrientationProviderTest.kt`

- [ ] **Step 1: Add a failing test for gyro fusion wiring**

Append these tests to `OrientationProviderTest.kt` (before the closing brace):

```kotlin
    @Test
    fun `gyro sample updates azimuth between rotation matrix samples`() {
        every { display.rotation } returns Surface.ROTATION_0

        // Initialize with device pointing north
        orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            41.0082, 28.9784
        )

        // Gyro rotates 90 deg/s around z for 1 second
        val gyroZ = Math.toRadians(90.0).toFloat()
        val state = orientationProvider.getOrientation(
            RawSensorState(gyro = floatArrayOf(0f, 0f, gyroZ), timestamp = 1_000_000_000L),
            41.0082, 28.9784
        )

        assertThat(state.deviceAzimuth).isWithin(1f).of(90f)
    }

    @Test
    fun `rotation matrix corrects gyro drift`() {
        every { display.rotation } returns Surface.ROTATION_0

        orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            41.0082, 28.9784
        )
        // Drift 10 degrees via gyro
        val gyroZ = Math.toRadians(10.0).toFloat()
        orientationProvider.getOrientation(
            RawSensorState(gyro = floatArrayOf(0f, 0f, gyroZ), timestamp = 1_000_000_000L),
            41.0082, 28.9784
        )
        // Correct back toward north reference
        val state = orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f), timestamp = 2_000_000_000L),
            41.0082, 28.9784
        )

        assertThat(state.deviceAzimuth).isLessThan(10f)
        assertThat(state.deviceAzimuth).isGreaterThan(0f)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_qibla:testDebugUnitTest --tests="com.kutluoglu.prayer_qibla.OrientationProviderTest"`
Expected: FAIL — the new tests fail because `getOrientation` ignores `gyro` (azimuth stays 0).

- [ ] **Step 3: Update the implementation**

Replace `OrientationProvider.kt` with:

```kotlin
package com.kutluoglu.prayer_qibla

import android.hardware.SensorManager
import android.view.Surface
import com.kutluoglu.core.common.utils.AngleUtils
import com.kutluoglu.core.designsystem.utils.DisplayProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * A Factory that requires an Activity Context to calculate orientation
 * based on the current screen rotation.
 */
@Single
class OrientationProvider(private val displayProvider: DisplayProvider) {
    private val remappedRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val fusedMatrix = FloatArray(9)
    private val fusion = OrientationFusion()
    private var lastGyroTimestamp = 0L

    // Kaaba'nın koordinatları
    private val kaabaLatitude = 21.4225
    private val kaabaLongitude = 39.8262

    private var _sensorState = MutableStateFlow(SensorState())
    val sensorState = _sensorState.asStateFlow()

    /**
     * Calculates the final SensorState using the raw sensor data and location data.
     */
    fun getOrientation(rawState: RawSensorState, userLatitude: Double?, userLongitude: Double?): SensorState {
        if (rawState.gyro != null) {
            val dt = if (lastGyroTimestamp != 0L) {
                (rawState.timestamp - lastGyroTimestamp) / 1_000_000_000f
            } else {
                0f
            }
            lastGyroTimestamp = rawState.timestamp
            fusion.updateWithGyro(rawState.gyro[0], rawState.gyro[1], rawState.gyro[2], dt)
        }

        if (rawState.rotationMatrix != null) {
            fusion.correctWithRotationMatrix(rawState.rotationMatrix)
        }

        if (fusion.isInitialized) {
            fusion.toRotationMatrix(fusedMatrix)

            // 1. Ekran rotasyonuna göre koordinat sistemini yeniden haritala
            when (displayProvider.display().rotation) {
                Surface.ROTATION_0 -> remap(fusedMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y)
                Surface.ROTATION_90 -> remap(fusedMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X)
                Surface.ROTATION_180 -> remap(fusedMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y)
                Surface.ROTATION_270 -> remap(fusedMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X)
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

            val qiblaAngle = AngleUtils.normalizeDegrees((qiblaBearing - normalizedAzimuth).toFloat())

            _sensorState.update {
                it.copy(
                    deviceAzimuth = normalizedAzimuth,
                    qiblaBearing = qiblaBearing,
                    qiblaAngle = qiblaAngle,
                    sensorAccuracy = rawState.accuracy
                )
            }
        } else if (rawState.rotationMatrix == null && rawState.gyro == null) {
            _sensorState.update { it.copy(sensorAccuracy = rawState.accuracy) }
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
            atan2(
                sin(lambdaK - lambda),
                cos(phi) * tan(phiK) - sin(phi) * cos(lambdaK - lambda)
            )
        )
        return (psi + 360) % 360 // Normalleştir
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_qibla:testDebugUnitTest --tests="com.kutluoglu.prayer_qibla.OrientationProviderTest"`
Expected: PASS (all tests, including the 2 new ones)

- [ ] **Step 5: Commit**

```bash
git add prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/OrientationProvider.kt prayer_qibla/src/test/java/com/kutluoglu/prayer_qibla/OrientationProviderTest.kt
git commit -m "feat(qibla): feed gyro fusion through OrientationProvider"
```

---

### Task 5: `QiblaDataStoreImp` — remove `Log.e` spam

**Files:**
- Modify: `prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/QiblaDataStoreImp.kt`

- [ ] **Step 1: Update the implementation**

Replace the `getQiblaDirection` body to drop the per-emission logging:

```kotlin
package com.kutluoglu.prayer_qibla

import com.kutluoglu.prayer.data.qibla.QiblaDataStore
import com.kutluoglu.prayer.model.qibla.QiblaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class QiblaDataStoreImp(
    private val sensorService: SensorService,
    private val orientationProvider: OrientationProvider
) : QiblaDataStore {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getQiblaDirection(
        latitude: Double,
        longitude: Double
    ): Flow<QiblaState> = channelFlow {
        sensorService.startCompass()
        val job = launch {
            sensorService.rawSensorState.collect {
                val finalState = orientationProvider.getOrientation(it, latitude, longitude)
                trySend(
                    QiblaState(
                        qiblaAngle = finalState.qiblaAngle,
                        deviceAzimuth = finalState.deviceAzimuth,
                        sensorAccuracy = finalState.sensorAccuracy,
                        qiblaBearing = finalState.qiblaBearing
                    )
                )
            }
        }
        awaitClose {
            job.cancel()
            sensorService.stopCompass()
        }
    }.flowOn(Dispatchers.Default)

    override fun start() = sensorService.startCompass()

    override fun stop() = sensorService.stopCompass()
}
```

- [ ] **Step 2: Run the module tests**

Run: `./gradlew :prayer_qibla:testDebugUnitTest`
Expected: PASS (OrientationFusionTest, OrientationProviderTest, QiblaDataStoreImpTest)

- [ ] **Step 3: Commit**

```bash
git add prayer_qibla/src/main/java/com/kutluoglu/prayer_qibla/QiblaDataStoreImp.kt
git commit -m "perf(qibla): remove per-emission Log.e spam in QiblaDataStoreImp"
```

---

### Task 6: Phase 1 verification

- [ ] **Step 1: Run all `prayer_qibla` tests**

Run: `./gradlew :prayer_qibla:testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Run the Qibla ViewModel tests (unchanged, must stay green)**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest`
Expected: PASS

- [ ] **Step 3: Commit any remaining changes (none expected)**

```bash
git status --short
```

---

## Phase 2: Qibla Screen

### Task 7: Kaaba vector drawable + string resources

**Files:**
- Modify: `prayer_feature/qibla/src/main/res/drawable/ic_kaaba.xml`
- Modify: `prayer_feature/qibla/src/main/res/values/strings.xml`
- Modify: `prayer_feature/qibla/src/main/res/values-tr/strings.xml`

- [ ] **Step 1: Replace the Kaaba drawable**

Replace `ic_kaaba.xml` with a proper Kaaba silhouette (black cube + gold band + door):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    <!-- front face -->
    <path
        android:fillColor="#1A1A1A"
        android:pathData="M12,14 L36,14 L36,38 L12,38 Z"/>
    <!-- top face -->
    <path
        android:fillColor="#2A2A2A"
        android:pathData="M12,14 L20,8 L44,8 L36,14 Z"/>
    <!-- gold band (front) -->
    <path
        android:fillColor="#D4AF37"
        android:pathData="M12,26 L36,26 L36,30 L12,30 Z"/>
    <!-- gold band (top) -->
    <path
        android:fillColor="#C9A227"
        android:pathData="M20,8 L44,8 L44,12 L20,12 Z"/>
    <!-- gold door -->
    <path
        android:fillColor="#D4AF37"
        android:pathData="M22,30 L26,30 L26,38 L22,38 Z"/>
</vector>
```

- [ ] **Step 2: Add English strings**

Replace `values/strings.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="qibla_page_title">Qibla Compass</string>
    <string name="qibla_location">Location</string>
    <string name="qibla_direction">Direction</string>
    <string name="qibla_distance">Distance to Qibla</string>
    <string name="qibla_measurement">Measurement</string>
    <string name="qibla_accuracy_high">High</string>
    <string name="qibla_accuracy_medium">Medium</string>
    <string name="qibla_accuracy_low">Low</string>
    <string name="qibla_turn_right">Turn %1$d° right</string>
    <string name="qibla_turn_left">Turn %1$d° left</string>
    <string name="qibla_aligned">Facing Qibla!</string>
    <string name="qibla_calibrate">Move your phone in a figure-8 to calibrate</string>
    <string name="qibla_calibration_required">Calibration Required</string>
    <string name="qibla_accuracy_medium_badge">Medium Accuracy</string>
    <string name="qibla_accuracy_high_badge">High Accuracy</string>
    <string name="qibla_waiting_location">Waiting for location...</string>
    <string name="qibla_location_error">Error: Location unavailable. Please check your location services.</string>
    <string name="qibla_degrees_north">%1$d° North</string>
    <string name="qibla_compass_arrow">Qibla Direction</string>
    <string name="qibla_kaaba">Kaaba</string>
</resources>
```

- [ ] **Step 3: Add Turkish strings**

Replace `values-tr/strings.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="qibla_page_title">Kıble Pusulası</string>
    <string name="qibla_location">Konum</string>
    <string name="qibla_direction">Yön</string>
    <string name="qibla_distance">Kıbleye Uzaklık</string>
    <string name="qibla_measurement">Ölçüm</string>
    <string name="qibla_accuracy_high">Yüksek</string>
    <string name="qibla_accuracy_medium">Orta</string>
    <string name="qibla_accuracy_low">Düşük</string>
    <string name="qibla_turn_right">%1$d° sağa dön</string>
    <string name="qibla_turn_left">%1$d° sola dön</string>
    <string name="qibla_aligned">Kıbleye Dönük!</string>
    <string name="qibla_calibrate">8 şekli çizerek kalibre edin</string>
    <string name="qibla_calibration_required">Kalibrasyon Gerekli</string>
    <string name="qibla_accuracy_medium_badge">Orta Doğruluk</string>
    <string name="qibla_accuracy_high_badge">Yüksek Doğruluk</string>
    <string name="qibla_waiting_location">Konum bekleniyor...</string>
    <string name="qibla_location_error">Hata: Konum alınamadı. Lütfen konum servislerinizi kontrol edin.</string>
    <string name="qibla_degrees_north">%1$d° Kuzey</string>
    <string name="qibla_compass_arrow">Kıble Yönü</string>
    <string name="qibla_kaaba">Kabe</string>
</resources>
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/qibla/src/main/res/
git commit -m "feat(qibla): add Kaaba vector drawable and string resources"
```

---

### Task 8: `QiblaInfoFormatter` pure logic + tests

**Files:**
- Create: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoFormatterTest.kt`
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoFormatter.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.qibla.components.QiblaInfoFormatterTest"`
Expected: FAIL with "Unresolved reference: accuracyLevel"

- [ ] **Step 3: Write the implementation**

```kotlin
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
    return if (abs(normalized) < threshold) {
        QiblaDistanceLabel.Aligned
    } else if (normalized > 0) {
        QiblaDistanceLabel.Turn(normalized.roundToInt(), TurnDirection.RIGHT)
    } else {
        QiblaDistanceLabel.Turn(abs(normalized).roundToInt(), TurnDirection.LEFT)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.qibla.components.QiblaInfoFormatterTest"`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoFormatter.kt prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoFormatterTest.kt
git commit -m "feat(qibla): extract pure QiblaInfoFormatter logic with tests"
```

---

### Task 9: `QiblaCompass` — Classic Brass, graphicsLayer, accuracy ring

**Files:**
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaCompass.kt`

- [ ] **Step 1: Replace the implementation**

Replace the entire file with:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.kutluoglu.core.common.utils.AngleUtils
import com.kutluoglu.prayer_feature.qibla.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

const val QIBLA_ALIGNMENT_THRESHOLD = 10f

@Composable
fun QiblaCompass(
    deviceAzimuth: Float,
    qiblaAngle: Float,
    sensorAccuracy: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    var hasVibrated by remember { mutableStateOf(false) }

    val angleDifference = abs(AngleUtils.normalizeDegrees(qiblaAngle))
    val isAligned = angleDifference < QIBLA_ALIGNMENT_THRESHOLD

    val arrowColor by animateColorAsState(
        targetValue = if (isAligned) Color(0xFF1E7E34) else Color(0xFFB8860B),
        animationSpec = tween(durationMillis = 500),
        label = "arrow_color"
    )

    val arrowScale by animateFloatAsState(
        targetValue = if (isAligned) 1.1f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "arrow_scale"
    )

    // Yön hizalandığında titreşim efekti uygula
    LaunchedEffect(isAligned) {
        if (isAligned && !hasVibrated) {
            vibrator?.let { v ->
                val vibrationDuration = 200L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(
                        VibrationEffect.createOneShot(
                            vibrationDuration,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(vibrationDuration)
                }
                hasVibrated = true
            }
        } else if (!isAligned) {
            hasVibrated = false
        }
    }

    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        AccuracyRing(sensorAccuracy, Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDF6E3),
                            Color(0xFFF0E6C8),
                            Color(0xFFE4D5A8)
                        )
                    )
                )
        )

        // Dial drawn once, rotated on the GPU
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .graphicsLayer { rotationZ = -deviceAzimuth }
        ) {
            drawCompassDial()
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_qibla_arrow),
            contentDescription = stringResource(R.string.qibla_compass_arrow),
            modifier = Modifier
                .fillMaxSize(0.75f)
                .graphicsLayer { rotationZ = qiblaAngle }
                .scale(arrowScale),
            tint = arrowColor
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_kaaba),
            contentDescription = stringResource(R.string.qibla_kaaba),
            modifier = Modifier.size(44.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun AccuracyRing(sensorAccuracy: Int, modifier: Modifier = Modifier) {
    val level = accuracyLevel(sensorAccuracy)
    val color = when (level) {
        AccuracyLevel.HIGH -> Color(0xFF1E7E34)
        AccuracyLevel.MEDIUM -> Color(0xFFB26A00)
        AccuracyLevel.LOW -> Color(0xFFB3261E)
    }
    val isLow = level == AccuracyLevel.LOW
    val transition = rememberInfiniteTransition(label = "ring_rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation_value"
    )

    Canvas(
        modifier = modifier.graphicsLayer { rotationZ = if (isLow) rotation else 0f }
    ) {
        val strokeWidth = 6.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val style = if (isLow) {
            Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f))
            )
        } else {
            Stroke(width = strokeWidth)
        }
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = style
        )
    }
}

private fun DrawScope.drawCompassDial() {
    val radius = size.minDimension / 2
    val center = this.center

    val majorColor = Color(0xFFB8860B)
    val minorColor = Color(0xFFC9A227).copy(alpha = 0.6f)
    val northColor = Color(0xFFB3261E)

    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 16.sp.toPx()
        color = majorColor.toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        )
    }

    (0 until 360 step 10).forEach { angle ->
        val isMajorLine = angle % 90 == 0
        val isMediumLine = angle % 30 == 0

        val lineLength = when {
            isMajorLine -> 28.dp.toPx()
            isMediumLine -> 18.dp.toPx()
            else -> 12.dp.toPx()
        }
        val strokeWidth = when {
            isMajorLine -> 3.dp.toPx()
            isMediumLine -> 2.dp.toPx()
            else -> 1.dp.toPx()
        }
        val color = if (isMajorLine) majorColor else minorColor

        val angleInRad = Math.toRadians(angle.toDouble() - 90)
        val lineStart = Offset(
            x = center.x + (radius - lineLength) * cos(angleInRad).toFloat(),
            y = center.y + (radius - lineLength) * sin(angleInRad).toFloat()
        )
        val lineEnd = Offset(
            x = center.x + radius * cos(angleInRad).toFloat(),
            y = center.y + radius * sin(angleInRad).toFloat()
        )
        drawLine(
            color = color,
            start = lineStart,
            end = lineEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        if (isMajorLine) {
            val text = when (angle) {
                0 -> "K"   // Kuzey (North)
                90 -> "D"  // Doğu (East)
                180 -> "G" // Güney (South)
                270 -> "B" // Batı (West)
                else -> ""
            }
            val textRadius = radius - lineLength - 14.dp.toPx()
            val textX = center.x + textRadius * cos(angleInRad).toFloat()
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val textY = center.y + textRadius * sin(angleInRad).toFloat() + textBounds.height() / 2f

            if (angle == 0) {
                val northPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 18.sp.toPx()
                    color = northColor.toArgb()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(text, textX, textY, northPaint)
            } else {
                drawContext.canvas.nativeCanvas.drawText(text, textX, textY, textPaint)
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaCompass.kt
git commit -m "feat(qibla): Classic Brass compass with graphicsLayer rotation and accuracy ring"
```

---

### Task 10: `QiblaInfoSection` — fixed semantics + aligned state

**Files:**
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoSection.kt`

- [ ] **Step 1: Replace the implementation**

Replace the entire file with:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.common.utils.AngleUtils
import com.kutluoglu.prayer_feature.qibla.QiblaUiState
import com.kutluoglu.prayer_feature.qibla.R
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun QiblaInfoSection(
    modifier: Modifier = Modifier,
    uiState: QiblaUiState,
    locationName: String?
) {
    val isAligned = abs(AngleUtils.normalizeDegrees(uiState.qiblaAngle)) < QIBLA_ALIGNMENT_THRESHOLD
    val distanceLabel = qiblaDistanceLabel(uiState.qiblaAngle, QIBLA_ALIGNMENT_THRESHOLD)
    val accuracyText = when (accuracyLevel(uiState.sensorAccuracy)) {
        AccuracyLevel.HIGH -> stringResource(R.string.qibla_accuracy_high)
        AccuracyLevel.MEDIUM -> stringResource(R.string.qibla_accuracy_medium)
        AccuracyLevel.LOW -> stringResource(R.string.qibla_accuracy_low)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        if (isAligned) {
            AlignedBanner()
        }

        locationName?.let {
            InfoRow(title = stringResource(R.string.qibla_location), value = it)
        }

        InfoRow(
            title = stringResource(R.string.qibla_direction),
            value = stringResource(
                R.string.qibla_degrees_north,
                uiState.qiblaBearing.roundToInt()
            )
        )

        InfoRow(
            title = stringResource(R.string.qibla_distance),
            value = when (distanceLabel) {
                is QiblaDistanceLabel.Aligned -> "0°"
                is QiblaDistanceLabel.Turn -> when (distanceLabel.direction) {
                    TurnDirection.RIGHT -> stringResource(
                        R.string.qibla_turn_right,
                        distanceLabel.degrees
                    )
                    TurnDirection.LEFT -> stringResource(
                        R.string.qibla_turn_left,
                        distanceLabel.degrees
                    )
                }
            }
        )

        InfoRow(
            title = stringResource(R.string.qibla_measurement),
            value = accuracyText
        )
    }
}

@Composable
private fun AlignedBanner() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE6F4EA)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_kaaba),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(R.string.qibla_aligned),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E7E34)
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoSection.kt
git commit -m "feat(qibla): fix info section semantics and add aligned state"
```

---

### Task 11: `QiblaScreen` — hero layout + badge wiring

**Files:**
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt`

- [ ] **Step 1: Replace the implementation**

Replace the entire file with:

```kotlin
package com.kutluoglu.prayer_feature.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.components.AccuracyLevel
import com.kutluoglu.prayer_feature.qibla.components.QiblaCompass
import com.kutluoglu.prayer_feature.qibla.components.QiblaInfoSection
import com.kutluoglu.prayer_feature.qibla.components.accuracyLevel

@Composable
fun QiblaScreen(
    uiState: QiblaUiState,
    locationName: String? = "Istanbul, TR",
    onEvent: (QiblaEvent) -> Unit
) {
    LaunchedEffect(Unit) {
        onEvent(QiblaEvent.OnStart)
    }

    DisposableEffect(Unit) {
        onDispose {
            onEvent(QiblaEvent.OnStop)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(0.78f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                uiState.error != null -> {
                    Text(stringResource(R.string.qibla_location_error))
                }
                !uiState.isLocationAvailable -> {
                    Text(stringResource(R.string.qibla_waiting_location))
                }
                else -> {
                    QiblaCompass(
                        deviceAzimuth = uiState.deviceAzimuth,
                        qiblaAngle = uiState.qiblaAngle,
                        sensorAccuracy = uiState.sensorAccuracy
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AccuracyBadge(sensorAccuracy = uiState.sensorAccuracy)
                    if (accuracyLevel(uiState.sensorAccuracy) == AccuracyLevel.LOW) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.qibla_calibrate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    locationName?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(0.22f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            QiblaInfoSection(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                uiState = uiState,
                locationName = locationName
            )
        }
    }
}

@Composable
private fun AccuracyBadge(sensorAccuracy: Int, modifier: Modifier = Modifier) {
    val level = accuracyLevel(sensorAccuracy)
    val (text, container, content) = when (level) {
        AccuracyLevel.HIGH -> Triple(
            stringResource(R.string.qibla_accuracy_high_badge),
            Color(0xFFE6F4EA),
            Color(0xFF1E7E34)
        )
        AccuracyLevel.MEDIUM -> Triple(
            stringResource(R.string.qibla_accuracy_medium_badge),
            Color(0xFFFFF4E0),
            Color(0xFFB26A00)
        )
        AccuracyLevel.LOW -> Triple(
            stringResource(R.string.qibla_calibration_required),
            Color(0xFFFDE8E8),
            Color(0xFFB3261E)
        )
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "feat(qibla): hero layout with accuracy badge and bottom info card"
```

---

### Task 12: Full build + test verification

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (all modules)

- [ ] **Step 2: Build the debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run GitNexus change detection**

Run: `gitnexus_detect_changes()` (via MCP) to confirm only expected symbols/flows are affected.

- [ ] **Step 4: Final commit if any stragglers**

```bash
git status --short
```

---

## Self-Review Notes

- **Spec coverage:** Visual (Tasks 7, 9, 10, 11), performance (Tasks 3, 5, 9), accuracy (Tasks 1, 2, 3, 4), tests (Tasks 1, 4, 8). All spec sections covered.
- **Type consistency:** `OrientationFusion` API (`updateWithGyro`, `correctWithRotationMatrix`, `toRotationMatrix`, `reset`, `isInitialized`) used consistently in Tasks 1 and 4. `RawSensorState` fields (`gyro`, `timestamp`) used consistently in Tasks 2, 3, 4. `accuracyLevel`/`qiblaDistanceLabel`/`QIBLA_ALIGNMENT_THRESHOLD` used consistently in Tasks 8, 9, 10, 11.
- **Sign convention:** positive `qiblaAngle` = turn right (device must rotate clockwise to reach qibla bearing), negative = turn left. Verified in Task 8 tests.
