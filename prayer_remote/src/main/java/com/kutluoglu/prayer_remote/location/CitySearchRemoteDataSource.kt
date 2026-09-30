package com.kutluoglu.prayer_remote.location

import com.kutluoglu.core.common.AppVersion
import com.kutluoglu.prayer.model.location.City
import com.kutluoglu.prayer.model.location.GeocodingResult
import com.kutluoglu.prayer.model.location.timeZoneIdFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

/**
 * Remote data source for city search / reverse geocoding via the Nominatim API.
 */
@Single
class CitySearchRemoteDataSource(
    private val httpClient: OkHttpClient,
    private val appVersion: AppVersion,
    private val baseUrl: String = NOMINATIM_BASE_URL
) {

    private val userAgent = "NamazVakitleri/${appVersion.name}"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun searchCities(query: String): List<City> = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("search")
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .addQueryParameter("limit", "10")
            .addQueryParameter("addressdetails", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
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
            val cityField = address?.getCityName()
            val countryCode = address?.country_code?.uppercase() ?: ""
            val countryName = getCountryNameFromCode(countryCode) ?: address?.country ?: ""
            val latitude = result.lat.toDoubleOrNull() ?: return@mapNotNull null
            val longitude = result.lon.toDoubleOrNull() ?: return@mapNotNull null
            City(
                name = cityName,
                city = cityField,
                country = countryName,
                latitude = latitude,
                longitude = longitude,
                timezone = timeZoneIdFor(latitude, longitude, countryCode) ?: "",
                county = countyName
            )
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): City? = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("reverse")
            .addQueryParameter("lat", latitude.toString())
            .addQueryParameter("lon", longitude.toString())
            .addQueryParameter("format", "json")
            .addQueryParameter("addressdetails", "1")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
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
        val cityField = address?.getCityName()
        val countryCode = address?.country_code?.uppercase() ?: ""
        val countryName = getCountryNameFromCode(countryCode) ?: address?.country ?: ""

        City(
            name = cityName,
            city = cityField,
            country = countryName,
            latitude = latitude,
            longitude = longitude,
            timezone = timeZoneIdFor(latitude, longitude, countryCode) ?: "",
            county = countyName
        )
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
