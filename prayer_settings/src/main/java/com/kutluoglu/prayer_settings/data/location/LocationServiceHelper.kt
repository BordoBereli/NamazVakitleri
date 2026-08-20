package com.kutluoglu.prayer_settings.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Single
class LocationServiceHelper(
    private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationData? {
        if (!hasLocationPermission()) return null
        return withContext(Dispatchers.IO) {
            val coordinates = awaitLastLocation() ?: return@withContext null
            val address = getAddress(coordinates.latitude, coordinates.longitude)

            LocationData(
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                country = address?.countryName,
                countryCode = address?.countryCode,
                city = address?.adminArea,
                county = address?.subAdminArea
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitLastLocation(): android.location.Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: android.location.Location? ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun getAddress(lat: Double, lon: Double): Address? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()
                try {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val countryCode: String?,
    val city: String?,
    val county: String?
)
