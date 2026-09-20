package com.kutluoglu.wear.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.koin.core.annotation.Factory
import java.time.ZoneId

data class WatchLocation(
    val latitude: Double,
    val longitude: Double,
    val zoneId: ZoneId,
    val locationName: String
)

/**
 * Supplies the watch's current location for on-device prayer-time computation.
 *
 * Defaults to Istanbul so the tile ALWAYS has data, and falls back to the last
 * known GPS/network fix when the watch has location permission. Uses the
 * platform [LocationManager] (not Google Play Services) to stay dependency-light
 * on devices where GMS is unreliable.
 *
 * Timezone note: the GPS path uses [ZoneId.systemDefault] (the watch's own
 * zone), while the default location pins `Europe/Istanbul`. The phone derives
 * the timezone from coordinates; `systemDefault` is defensible for a watch
 * whose clock follows the local zone, but the divergence is intentional and
 * should be revisited if the watch is expected to travel across zones.
 */
@Factory
class WatchLocationProvider(
    private val context: Context
) {

    fun getLocation(): WatchLocation =
        runCatching {
            val lastKnown = if (hasLocationPermission()) getLastKnownLocation() else null
            if (lastKnown != null) {
                Log.d(
                    TAG,
                    "getLocation: using last known location " +
                        "lat=${lastKnown.latitude} lon=${lastKnown.longitude}"
                )
                WatchLocation(
                    latitude = lastKnown.latitude,
                    longitude = lastKnown.longitude,
                    zoneId = ZoneId.systemDefault(),
                    locationName = GPS_LOCATION_NAME
                )
            } else {
                Log.d(TAG, "getLocation: no permission or no fix, using default Istanbul")
                DEFAULT_LOCATION
            }
        }.getOrElse { e ->
            Log.e(TAG, "getLocation failed -> ${e.message}")
            DEFAULT_LOCATION
        }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    // Permission is checked by the caller (hasLocationPermission) before this is invoked.
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    companion object {
        private const val TAG = "WatchLocationProvider"
        private const val GPS_LOCATION_NAME = "Konum"
        private val DEFAULT_LOCATION = WatchLocation(
            latitude = 41.0082,
            longitude = 28.9784,
            zoneId = ZoneId.of("Europe/Istanbul"),
            locationName = "İstanbul"
        )
    }
}
