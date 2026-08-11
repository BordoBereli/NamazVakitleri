package com.kutluoglu.prayer_remote.location

import com.kutluoglu.prayer.model.location.City
import com.kutluoglu.prayer.model.location.GeocodingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

/**
 * Remote data source for city search / reverse geocoding via the Nominatim API.
 */
@Single
class CitySearchRemoteDataSource(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = NOMINATIM_BASE_URL
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun searchCities(query: String): List<City> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?q=$query&format=json&limit=10&addressdetails=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NamazVakitleri/1.0")
            .build()

        val response = httpClient.newCall(request).execute()
        if (response.code != HTTP_OK) {
            throw NetworkException("Search failed with code: ${response.code}")
        }

        val body = response.body?.string() ?: throw NetworkException("Empty response")
        val results = json.decodeFromString<List<GeocodingResult>>(body)

        results.mapNotNull { result ->
            val address = result.address
            val cityName = address?.getCityName() ?: return@mapNotNull null
            val countyName = address?.getCountyName()
            val cityField = address?.city
            val countryCode = address?.country_code?.uppercase() ?: ""
            val countryName = getCountryNameFromCode(countryCode) ?: address?.country ?: ""
            City(
                name = cityName,
                city = cityField,
                country = countryName,
                latitude = result.lat.toDoubleOrNull() ?: return@mapNotNull null,
                longitude = result.lon.toDoubleOrNull() ?: return@mapNotNull null,
                timezone = calculateTimezone(
                    result.lat.toDoubleOrNull() ?: 0.0,
                    result.lon.toDoubleOrNull() ?: 0.0,
                    countryCode
                ),
                county = countyName
            )
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): City? = withContext(Dispatchers.IO) {
        val url = "$baseUrl/reverse?lat=$latitude&lon=$longitude&format=json&addressdetails=1"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NamazVakitleri/1.0")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            return@withContext null
        }

        val body = response.body?.string() ?: return@withContext null
        val result = json.decodeFromString<GeocodingResult>(body)
        val address = result.address
        val cityName = address?.getCityName() ?: return@withContext null
        val countyName = address?.getCountyName()
        val cityField = address?.city
        val countryCode = address?.country_code?.uppercase() ?: ""
        val countryName = getCountryNameFromCode(countryCode) ?: address?.country ?: ""

        City(
            name = cityName,
            city = cityField,
            country = countryName,
            latitude = latitude,
            longitude = longitude,
            timezone = calculateTimezone(latitude, longitude, countryCode),
            county = countyName
        )
    }

    private fun calculateTimezone(latitude: Double, longitude: Double, countryCode: String): String {
        return when (countryCode) {
            "TR" -> "Europe/Istanbul"
            "SA" -> "Asia/Riyadh"
            "EG" -> "Africa/Cairo"
            "ID" -> "Asia/Jakarta"
            "MY" -> "Asia/Kuala_Lumpur"
            "PK" -> "Asia/Karachi"
            "IN" -> "Asia/Kolkata"
            "BD" -> "Asia/Dhaka"
            "NG" -> "Africa/Lagos"
            "MA" -> "Africa/Casablanca"
            "DZ" -> "Africa/Algiers"
            "TN" -> "Africa/Tunis"
            "JO" -> "Asia/Amman"
            "AE" -> "Asia/Dubai"
            "KW" -> "Asia/Kuwait"
            "QA" -> "Asia/Qatar"
            "BH" -> "Asia/Bahrain"
            "OM" -> "Asia/Muscat"
            "GB" -> "Europe/London"
            "US" -> "America/New_York"
            "DE" -> "Europe/Berlin"
            "FR" -> "Europe/Paris"
            else -> {
                val offset = ((longitude + 180) / 30).toInt().coerceIn(-12, 12)
                when {
                    offset == 0 -> "UTC"
                    offset > 0 -> "UTC+$offset"
                    else -> "UTC$offset"
                }
            }
        }
    }

    private fun getCountryNameFromCode(code: String): String? {
        return when (code) {
            "TR" -> "Turkey"
            "SA" -> "Saudi Arabia"
            "EG" -> "Egypt"
            "ID" -> "Indonesia"
            "MY" -> "Malaysia"
            "PK" -> "Pakistan"
            "IN" -> "India"
            "BD" -> "Bangladesh"
            "NG" -> "Nigeria"
            "MA" -> "Morocco"
            "DZ" -> "Algeria"
            "TN" -> "Tunisia"
            "JO" -> "Jordan"
            "AE" -> "United Arab Emirates"
            "KW" -> "Kuwait"
            "QA" -> "Qatar"
            "BH" -> "Bahrain"
            "OM" -> "Oman"
            "GB" -> "United Kingdom"
            "US" -> "United States"
            "DE" -> "Germany"
            "FR" -> "France"
            else -> null
        }
    }

    companion object {
        private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org"
        private const val HTTP_OK = 200
    }
}

class NetworkException(message: String) : Exception(message)
