package com.kutluoglu.prayer_location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
@Single
class LocationService(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    // This is the main public function that will be called from the ViewModel
    @SuppressLint("MissingPermission") // Permissions are handled at the UI layer
    suspend fun getCurrentLocation(): LocationData? {
        return withContext(Dispatchers.IO) {
            // 1. Get Coordinates
            val coordinates: Location = awaitLastLocation() ?: return@withContext null

            // 2. Get Address from Coordinates (Reverse Geocoding)
            val address = getAddress(coordinates.latitude, coordinates.longitude)

            // 3. Combine into a single data object
            LocationData(
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                country = address?.countryName,
                countryCode = address?.countryCode,
                city = address?.adminArea, // Often the state/province
                county = address?.subAdminArea // Often the city/county
            )
        }
    }

    // A helper function to promisify the Google Play Services location API
    @SuppressLint("MissingPermission")
    private suspend fun awaitLastLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
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

    private suspend fun getAddress(lat: Double, lon: Double): Address? {
        // The entire function is now a suspend function
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use suspendCancellableCoroutine for the callback-based API
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()
                try {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        // When the callback is invoked, resume the coroutine
                        if(continuation.isActive) continuation.resume(addresses.firstOrNull())
                    }
                } catch (e: Exception) {
                    // In case of immediate error, resume with null
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        } else {
            // For older APIs, the call is synchronous and can be wrapped in withContext
            withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                } catch (e: Exception) {
                    // Geocoder can fail if there's no network or other issues
                    null
                }
            }
        }
    }
}