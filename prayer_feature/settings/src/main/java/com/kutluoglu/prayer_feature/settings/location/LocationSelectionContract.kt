package com.kutluoglu.prayer_feature.settings.location

import com.kutluoglu.prayer.model.location.City

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class LocationTab {
    PRESET,
    SEARCH,
    MAP
}

data class MapLocationState(
    val latitude: Double,
    val longitude: Double,
    val cityName: String? = null,
    val country: String? = null,
    val county: String? = null,
    val locationInfoText: String = ""
)

data class CountryInfo(
    val name: String,
    val key: String,
    val cityCount: Int,
    val isPriority: Boolean = false
)

data class ProvinceInfo(
    val name: String,
    val city: City,
    val districtCount: Int,
    val districts: List<City> = emptyList()
)

sealed class LocationSelectionUiState {
    data object Loading : LocationSelectionUiState()
    
    data class CountrySelection(
        val countries: List<CountryInfo>,
        val searchQuery: String = ""
    ) : LocationSelectionUiState()
    
    data class CitySelection(
        val country: String,
        val cities: List<City>,
        val citiesByProvince: Map<String, List<City>>,
        val selectedProvince: String? = null,
        val sortOrder: SortOrder = SortOrder.ASCENDING,
        val countries: List<CountryInfo> = emptyList()
    ) : LocationSelectionUiState()
    
    data class ProvinceSelection(
        val country: String,
        val province: String,
        val mainCity: City,
        val districts: List<City>,
        val sortOrder: SortOrder = SortOrder.ASCENDING,
        val countries: List<CountryInfo> = emptyList()
    ) : LocationSelectionUiState()
    
    data class SearchResults(
        val results: List<City>,
        val query: String,
        val searchHistory: List<City> = emptyList(),
        val errorMessage: String? = null
    ) : LocationSelectionUiState()
    
    data object Searching : LocationSelectionUiState()
    
    data class MapView(
        val selectedLocation: MapLocationState? = null,
        val isLocating: Boolean = false,
        val error: String? = null
    ) : LocationSelectionUiState()
    
    data class Error(val message: String) : LocationSelectionUiState()
}

sealed class LocationSelectionEvent {
    data object LoadCountries : LocationSelectionEvent()
    data class SearchCountry(val query: String) : LocationSelectionEvent()
    data class SelectCountry(val country: String) : LocationSelectionEvent()
    data object GoBack : LocationSelectionEvent()
    data object Retry : LocationSelectionEvent()
    data class Search(val query: String) : LocationSelectionEvent()
    data class SelectCity(val city: City) : LocationSelectionEvent()
    data class SelectProvince(val province: String, val mainCity: City) : LocationSelectionEvent()
    data class SelectDistrict(val district: City) : LocationSelectionEvent()
    data object ClearSearch : LocationSelectionEvent()
    data object ClearHistory : LocationSelectionEvent()
    data class ChangeSortOrder(val sortOrder: SortOrder) : LocationSelectionEvent()
    data object UseMyLocation : LocationSelectionEvent()
    data class SelectTab(val tab: LocationTab) : LocationSelectionEvent()
    data class UpdateMapLocation(val latitude: Double, val longitude: Double) : LocationSelectionEvent()
    data class ConfirmMapLocation(val location: MapLocationState) : LocationSelectionEvent()
    data object ClearMapLocation : LocationSelectionEvent()
}
