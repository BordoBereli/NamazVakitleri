package com.kutluoglu.prayer_location

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationServiceTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val settingsClient = mockk<SettingsClient>()
    private val fusedClient = mockk<FusedLocationProviderClient>(relaxed = true)
    private lateinit var service: LocationService

    @Before
    fun setUp() {
        service = LocationService(application)
        service.settingsClientOverride = settingsClient
        service.fusedClientOverride = fusedClient
    }

    private fun satisfiedTask(): Task<LocationSettingsResponse> {
        val task = mockk<Task<LocationSettingsResponse>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<LocationSettingsResponse>>()) } answers {
            firstArg<OnSuccessListener<LocationSettingsResponse>>().onSuccess(mockk())
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private fun failedTask(exception: Exception): Task<LocationSettingsResponse> {
        val task = mockk<Task<LocationSettingsResponse>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<LocationSettingsResponse>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(exception)
            task
        }
        return task
    }

    @Test
    fun `checkLocationSettings returns Satisfied when settings check succeeds`() = runTest {
        every { settingsClient.checkLocationSettings(any()) } returns satisfiedTask()

        val result = service.checkLocationSettings()

        assertThat(result).isEqualTo(LocationSettingsResult.Satisfied)
    }

    @Test
    fun `checkLocationSettings returns ResolutionRequired when resolution is available`() = runTest {
        val status = Status(CommonStatusCodes.RESOLUTION_REQUIRED, "GPS off", null)
        every { settingsClient.checkLocationSettings(any()) } returns failedTask(ApiException(status))

        val result = service.checkLocationSettings()

        assertThat(result).isInstanceOf(LocationSettingsResult.ResolutionRequired::class.java)
    }

    @Test
    fun `checkLocationSettings returns Unavailable on other failures`() = runTest {
        val status = Status(CommonStatusCodes.ERROR, "boom", null)
        every { settingsClient.checkLocationSettings(any()) } returns failedTask(ApiException(status))

        val result = service.checkLocationSettings()

        assertThat(result).isEqualTo(LocationSettingsResult.Unavailable)
    }

    @Test
    fun `getCurrentLocation returns null without requesting updates when settings unsatisfied`() = runTest {
        shadowOf(application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val status = Status(CommonStatusCodes.RESOLUTION_REQUIRED, "GPS off", null)
        every { settingsClient.checkLocationSettings(any()) } returns failedTask(ApiException(status))

        val result = service.getCurrentLocation()

        assertThat(result).isNull()
        verify(exactly = 0) {
            fusedClient.requestLocationUpdates(any<LocationRequest>(), any<LocationCallback>(), any<Looper>())
        }
    }
}
