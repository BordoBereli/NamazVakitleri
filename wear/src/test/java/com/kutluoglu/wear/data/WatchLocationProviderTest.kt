package com.kutluoglu.wear.data

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WatchLocationProviderTest {

    private val application: Application = ApplicationProvider.getApplicationContext()
    private val provider = WatchLocationProvider(application)

    @Test
    fun `returns default Istanbul location when no permission granted`() {
        val location = provider.getLocation()

        assertThat(location.latitude).isEqualTo(41.0082)
        assertThat(location.longitude).isEqualTo(28.9784)
        assertThat(location.zoneId.id).isEqualTo("Europe/Istanbul")
        assertThat(location.locationName).isEqualTo("İstanbul")
    }

    @Test
    fun `returns default Istanbul location when permission granted but no fix`() {
        grantLocationPermissions()

        val location = provider.getLocation()

        assertThat(location.latitude).isEqualTo(41.0082)
        assertThat(location.longitude).isEqualTo(28.9784)
        assertThat(location.zoneId.id).isEqualTo("Europe/Istanbul")
        assertThat(location.locationName).isEqualTo("İstanbul")
    }

    @Test
    fun `returns GPS location when permission granted and fix available`() {
        grantLocationPermissions()
        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 39.9208
            longitude = 32.8541
        }
        shadowOf(locationManager).setLastKnownLocation(LocationManager.GPS_PROVIDER, gpsLocation)

        val location = provider.getLocation()

        assertThat(location.latitude).isEqualTo(39.9208)
        assertThat(location.longitude).isEqualTo(32.8541)
        assertThat(location.locationName).isEqualTo("Konum")
    }

    @Test
    fun `falls back to network location when GPS has no fix`() {
        grantLocationPermissions()
        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val networkLocation = Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = 39.9208
            longitude = 32.8541
        }
        shadowOf(locationManager).setLastKnownLocation(LocationManager.NETWORK_PROVIDER, networkLocation)

        val location = provider.getLocation()

        assertThat(location.latitude).isEqualTo(39.9208)
        assertThat(location.longitude).isEqualTo(32.8541)
        assertThat(location.locationName).isEqualTo("Konum")
    }

    private fun grantLocationPermissions() {
        shadowOf(application).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
