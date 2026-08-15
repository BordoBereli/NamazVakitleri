package com.kutluoglu.prayer_qibla

import android.hardware.SensorManager
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class QiblaDataStoreImpTest {

    private val sensorService = mockk<SensorService>()
    private val orientationProvider = mockk<OrientationProvider>()
    private val dataStore = QiblaDataStoreImp(sensorService, orientationProvider)

    @Test
    fun `getQiblaDirection starts compass and emits mapped QiblaState`() = runTest {
        val sensorState = MutableStateFlow(RawSensorState())
        every { sensorService.rawSensorState } returns sensorState
        every { sensorService.startCompass() } just Runs
        every { sensorService.stopCompass() } just Runs
        every { orientationProvider.reset() } just Runs
        every { orientationProvider.getOrientation(any(), any(), any()) } returns SensorState(
            sensorAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
            deviceAzimuth = 45f,
            qiblaAngle = 10f,
            qiblaBearing = 151.6
        )

        dataStore.getQiblaDirection(41.0082, 28.9784).test {
            sensorState.value = RawSensorState(rotationMatrix = FloatArray(9) { 1f })

            val state = awaitItem()

            assertThat(state.deviceAzimuth).isEqualTo(45f)
            assertThat(state.qiblaAngle).isEqualTo(10f)
            assertThat(state.qiblaBearing).isEqualTo(151.6)
            assertThat(state.sensorAccuracy).isEqualTo(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
            verify { sensorService.startCompass() }
            verify { orientationProvider.reset() }

            cancelAndIgnoreRemainingEvents()
        }
        verify(timeout = 2000) { sensorService.stopCompass() }
    }

    @Test
    fun `start and stop delegate to sensor service`() {
        every { sensorService.startCompass() } just Runs
        every { sensorService.stopCompass() } just Runs

        dataStore.start()
        dataStore.stop()

        verify { sensorService.startCompass() }
        verify { sensorService.stopCompass() }
    }
}
