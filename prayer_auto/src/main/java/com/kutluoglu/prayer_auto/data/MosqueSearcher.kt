package com.kutluoglu.prayer_auto.data

import com.kutluoglu.prayer_remote.location.CitySearchRemoteDataSource
import org.koin.core.annotation.Factory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Searches nearby mosques via the Nominatim free-text search
 * ("mosque near lat,lon") and computes each result's distance from the
 * current location. Best-effort: returns an empty list on any failure so
 * the car screen can show a "no mosques" message instead of an error.
 */
@Factory
class MosqueSearcher(
        private val remoteDataSource: CitySearchRemoteDataSource
) {
    suspend fun search(latitude: Double, longitude: Double): List<Mosque> {
        return runCatching {
            remoteDataSource.searchPlaces("mosque near $latitude,$longitude")
                .map { city ->
                    Mosque(
                        name = city.name,
                        latitude = city.latitude,
                        longitude = city.longitude,
                        distanceKm = haversineKm(latitude, longitude, city.latitude, city.longitude),
                    )
                }.sortedBy { it.distanceKm }
        }.getOrElse { emptyList() }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }
}
