package com.kutluoglu.prayer_settings.data.repository

import android.content.Context
import android.util.Log
import com.kutluoglu.prayer.model.location.City
import com.kutluoglu.prayer.model.location.CityList
import com.kutluoglu.prayer_settings.data.local.CityCacheDataStore
import com.kutluoglu.prayer_settings.domain.repository.LocationRepository
import com.kutluoglu.prayer_remote.location.CitySearchRemoteDataSource
import com.kutluoglu.prayer_remote.location.NetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class LocationRepositoryImpl(
    private val context: Context,
    private val citySearchRemoteDataSource: CitySearchRemoteDataSource,
    private val cacheDataStore: CityCacheDataStore? = null
) : LocationRepository {

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedCities: List<City>? = null

    override suspend fun getPresetCities(): List<City> {
        return cachedCities ?: loadCitiesFromAssets().also { cachedCities = it }
    }

    override suspend fun searchCities(query: String): List<City> {
        return withContext(Dispatchers.IO) {
            try {
                citySearchRemoteDataSource.searchCities(query).also { cities ->
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
                citySearchRemoteDataSource.reverseGeocode(latitude, longitude)?.also { city ->
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

    override suspend fun clearCache() {
        cachedCities = null
        cacheDataStore?.clearCache()
    }

    companion object {
        private const val TAG = "LocationRepository"
    }
}

private fun java.io.InputStream.readToString(): String = bufferedReader().use { it.readText() }
