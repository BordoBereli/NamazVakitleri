package com.kutluoglu.prayer_settings.data.local

import com.kutluoglu.prayer.model.location.City
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CityLocalDataSource(
    private val cacheDataStore: CityCacheDataStore
) {
    suspend fun saveCities(cities: List<City>) = withContext(Dispatchers.IO) {
        cacheDataStore.saveCities(cities)
    }

    suspend fun getPresetCities(): List<City> = withContext(Dispatchers.IO) {
        cacheDataStore.getCities()
    }

    fun getPresetCitiesFlow(): Flow<List<City>> = cacheDataStore.getCitiesFlow()

    suspend fun searchCities(query: String): List<City> = withContext(Dispatchers.IO) {
        val cities = cacheDataStore.getCities()
        cities.filter { city ->
            city.name.contains(query, ignoreCase = true) ||
            city.country.contains(query, ignoreCase = true)
        }.sortedBy { it.name }
    }

    suspend fun isCacheValid(): Boolean = withContext(Dispatchers.IO) {
        cacheDataStore.isCacheValid()
    }

    suspend fun clearOldCache() = withContext(Dispatchers.IO) {
        cacheDataStore.clearOldCache()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        cacheDataStore.clearCache()
    }

    suspend fun getCityCount(): Int = withContext(Dispatchers.IO) {
        cacheDataStore.getCities().size
    }
}
