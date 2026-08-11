package com.kutluoglu.prayer_settings.domain.repository

import com.kutluoglu.prayer.model.location.City

interface LocationRepository {
    suspend fun getPresetCities(): List<City>
    suspend fun searchCities(query: String): List<City>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): City?
    suspend fun clearCache()
}
