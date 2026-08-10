package com.kutluoglu.prayer_settings.data.repository

import android.content.Context
import android.util.Log
import com.kutluoglu.prayer_settings.data.local.CityCacheDataStore
import com.kutluoglu.prayer_settings.domain.model.City
import com.kutluoglu.prayer_settings.domain.model.CityList
import com.kutluoglu.prayer_settings.domain.model.GeocodingResult
import com.kutluoglu.prayer_settings.domain.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

@Single
class LocationRepositoryImpl(
    private val context: Context,
    private val cacheDataStore: CityCacheDataStore? = null
) : LocationRepository {

    private val json = kotlinx.serialization.json.Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var cachedCities: List<City>? = null

    override suspend fun getPresetCities(): List<City> {
        return cachedCities ?: loadCitiesFromAssets().also { cachedCities = it }
    }

    override suspend fun searchCities(query: String): List<City> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$NOMINATIM_BASE_URL/search?q=$query&format=json&limit=10&addressdetails=1"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "NamazVakitleri/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.code != HTTP_OK) {
                    throw NetworkException("Search failed with code: ${response.code}")
                }

                val body = response.body?.string() ?: throw NetworkException("Empty response")
                val results = json.decodeFromString<List<GeocodingResult>>(body)
                
                results.mapNotNull { result ->
                    val cityName = result.address?.getCityName() ?: return@mapNotNull null
                    val countyName = result.address?.getCountyName()
                    val cityField = result.address?.city
                    val countryCode = result.address?.country_code?.uppercase() ?: ""
                    val countryName = getCountryNameFromCode(countryCode) ?: result.address.country ?: ""
                    City(
                        name = cityName,
                        city = cityField,
                        country = countryName,
                        latitude = result.lat.toDoubleOrNull() ?: return@mapNotNull null,
                        longitude = result.lon.toDoubleOrNull() ?: return@mapNotNull null,
                        timezone = calculateTimezone(result.lat.toDoubleOrNull() ?: 0.0, result.lon.toDoubleOrNull() ?: 0.0, countryCode),
                        county = countyName
                    )
                }.also { cities ->
                    cacheDataStore?.saveCities(cities)
                }
            } catch (e: NetworkException) {
                cacheDataStore?.let { ds ->
                    val cachedResults = searchCitiesFromCache(query)
                    if (cachedResults.isNotEmpty()) {
                        return@withContext cachedResults
                    }
                }
                throw e
            } catch (e: Exception) {
                cacheDataStore?.let { ds ->
                    val cachedResults = searchCitiesFromCache(query)
                    if (cachedResults.isNotEmpty()) {
                        return@withContext cachedResults
                    }
                }
                throw NetworkException("Network error: ${e.message}")
            }
        }
    }

    private suspend fun searchCitiesFromCache(query: String): List<City> {
        return cacheDataStore?.let { ds ->
            val cities = ds.getCities()
            cities.filter { city ->
                city.name.contains(query, ignoreCase = true) ||
                city.country.contains(query, ignoreCase = true) ||
                city.county?.contains(query, ignoreCase = true) == true
            }.sortedBy { it.name }
        } ?: emptyList()
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): City? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$NOMINATIM_BASE_URL/reverse?lat=$latitude&lon=$longitude&format=json&addressdetails=1"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "NamazVakitleri/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val result = json.decodeFromString<GeocodingResult>(body)
                val cityName = result.address?.getCityName() ?: return@withContext null
                val countyName = result.address?.getCountyName()
                val cityField = result.address?.city
                val countryCode = result.address?.country_code?.uppercase() ?: ""
                val countryName = getCountryNameFromCode(countryCode) ?: result.address.country ?: ""
                
                City(
                    name = cityName,
                    city = cityField,
                    country = countryName,
                    latitude = latitude,
                    longitude = longitude,
                    timezone = calculateTimezone(latitude, longitude, countryCode),
                    county = countyName
                ).also { city ->
                    cacheDataStore?.saveCities(listOf(city))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reverse geocode error: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun loadCitiesFromAssets(): List<City> {
        return try {
            val inputStream = context.assets.open("cities.json")
            val cityList = json.decodeFromString<CityList>(inputStream.readToString())
            Log.d(TAG, "Loaded ${cityList.cities.size} cities from assets")
            cityList.cities.also { cities ->
                cacheDataStore?.saveCities(cities)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cities from assets", e)
            emptyList()
        }
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

    override suspend fun clearCache() {
        cachedCities = null
        cacheDataStore?.clearCache()
    }

    companion object {
        private const val TAG = "LocationRepository"
        private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org"
        private const val HTTP_OK = 200
    }
}

class NetworkException(message: String) : Exception(message)

private fun java.io.InputStream.readToString(): String = bufferedReader().use { it.readText() }
