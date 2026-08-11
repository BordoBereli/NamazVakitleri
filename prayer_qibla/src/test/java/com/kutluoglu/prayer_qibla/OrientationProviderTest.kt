package com.kutluoglu.prayer_qibla

import android.hardware.SensorManager
import android.view.Display
import android.view.Surface
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.DisplayProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Execution(ExecutionMode.SAME_THREAD)
class OrientationProviderTest {

    private val displayProvider = mockk<DisplayProvider>()
    private val display = mockk<Display>()
    private val orientationProvider = OrientationProvider(displayProvider)

    @BeforeEach
    fun setUp() {
        every { displayProvider.display() } returns display
        mockkStatic(SensorManager::class)
        every { SensorManager.remapCoordinateSystem(any(), any(), any(), any()) } answers {
            val outR = arg<FloatArray>(3)
            System.arraycopy(arg<FloatArray>(0), 0, outR, 0, 9)
            true
        }
        every { SensorManager.getOrientation(any(), any()) } answers {
            val r = arg<FloatArray>(0)
            val out = arg<FloatArray>(1)
            out[0] = atan2(r[1], r[4]).toFloat()
            out[1] = 0f
            out[2] = 0f
            out
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SensorManager::class)
    }

    @Test
    fun `ROTATION_0 remaps with AXIS_X and AXIS_Y`() {
        every { display.rotation } returns Surface.ROTATION_0
        val xSlot = slot<Int>()
        val ySlot = slot<Int>()
        every { SensorManager.remapCoordinateSystem(any(), capture(xSlot), capture(ySlot), any()) } answers {
            val outR = arg<FloatArray>(3)
            System.arraycopy(arg<FloatArray>(0), 0, outR, 0, 9)
            true
        }

        orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            41.0082, 28.9784
        )

        assertThat(xSlot.captured).isEqualTo(SensorManager.AXIS_X)
        assertThat(ySlot.captured).isEqualTo(SensorManager.AXIS_Y)
    }

    @Test
    fun `ROTATION_90 remaps with AXIS_Y and AXIS_MINUS_X`() {
        every { display.rotation } returns Surface.ROTATION_90
        val xSlot = slot<Int>()
        val ySlot = slot<Int>()
        every { SensorManager.remapCoordinateSystem(any(), capture(xSlot), capture(ySlot), any()) } answers {
            val outR = arg<FloatArray>(3)
            System.arraycopy(arg<FloatArray>(0), 0, outR, 0, 9)
            true
        }

        orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            41.0082, 28.9784
        )

        assertThat(xSlot.captured).isEqualTo(SensorManager.AXIS_Y)
        assertThat(ySlot.captured).isEqualTo(SensorManager.AXIS_MINUS_X)
    }

    @Test
    fun `ROTATION_180 remaps with AXIS_MINUS_X and AXIS_MINUS_Y`() {
        every { display.rotation } returns Surface.ROTATION_180
        val xSlot = slot<Int>()
        val ySlot = slot<Int>()
        every { SensorManager.remapCoordinateSystem(any(), capture(xSlot), capture(ySlot), any()) } answers {
            val outR = arg<FloatArray>(3)
            System.arraycopy(arg<FloatArray>(0), 0, outR, 0, 9)
            true
        }

        orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            41.0082, 28.9784
        )

        assertThat(xSlot.captured).isEqualTo(SensorManager.AXIS_MINUS_X)
        assertThat(ySlot.captured).isEqualTo(SensorManager.AXIS_MINUS_Y)
    }

    @Test
    fun `ROTATION_270 remaps with AXIS_MINUS_Y and AXIS_X`() {
        every { display.rotation } returns Surface.ROTATION_270
        val xSlot = slot<Int>()
        val ySlot = slot<Int>()
        every { SensorManager.remapCoordinateSystem(any(), capture(xSlot), capture(ySlot), any()) } answers {
            val outR = arg<FloatArray>(3)
            System.arraycopy(arg<FloatArray>(0), 0, outR, 0, 9)
            true
        }

        orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            41.0082, 28.9784
        )

        assertThat(xSlot.captured).isEqualTo(SensorManager.AXIS_MINUS_Y)
        assertThat(ySlot.captured).isEqualTo(SensorManager.AXIS_X)
    }

    @Test
    fun `azimuth is 0 when device points north in portrait`() {
        every { display.rotation } returns Surface.ROTATION_0

        val state = orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            41.0082, 28.9784
        )

        assertThat(state.deviceAzimuth).isWithin(0.5f).of(0f)
    }

    @Test
    fun `azimuth is 90 when device points east in portrait`() {
        every { display.rotation } returns Surface.ROTATION_0

        val state = orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(90f)),
            41.0082, 28.9784
        )

        assertThat(state.deviceAzimuth).isWithin(0.5f).of(90f)
    }

    @Test
    fun `qiblaBearing for Istanbul is about 152 degrees`() {
        every { display.rotation } returns Surface.ROTATION_0

        val state = orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            41.0082, 28.9784
        )

        assertThat(state.qiblaBearing).isWithin(0.5).of(151.62)
    }

    @Test
    fun `qiblaAngle is normalized to shortest signed angle`() {
        every { display.rotation } returns Surface.ROTATION_0
        // Tokyo bearing ~293, device pointing north (azimuth 0) -> raw diff 293 -> normalized -67
        val state = orientationProvider.getOrientation(
            RawSensorState(rotationMatrix = rotationMatrixForHeading(0f)),
            35.6762, 139.6503
        )

        assertThat(state.qiblaAngle).isWithin(0.5f).of(-67f)
    }

    private fun rotationMatrixForHeading(headingDegrees: Float): FloatArray {
        val h = Math.toRadians(headingDegrees.toDouble())
        val c = cos(h).toFloat()
        val s = sin(h).toFloat()
        return floatArrayOf(
            c, s, 0f,
            -s, c, 0f,
            0f, 0f, 1f
        )
    }
}
