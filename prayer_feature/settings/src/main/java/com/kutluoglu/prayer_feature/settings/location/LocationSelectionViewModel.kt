package com.kutluoglu.prayer_feature.settings.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer.model.location.City
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_remote.location.NetworkException
import com.kutluoglu.prayer_settings.data.location.LocationData as SettingsLocationData
import com.kutluoglu.prayer_settings.data.location.LocationServiceHelper
import com.kutluoglu.prayer_settings.domain.repository.LocationRepository
import com.kutluoglu.prayer_settings.domain.usecase.SearchLocationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.text.Normalizer
import java.util.UUID

@KoinViewModel
class LocationSelectionViewModel(
    private val locationRepository: LocationRepository,
    private val searchLocationUseCase: SearchLocationUseCase,
    private val locationServiceHelper: LocationServiceHelper,
    private val locationsCoordinator: LocationsCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationSelectionUiState>(LocationSelectionUiState.Loading)
    val uiState: StateFlow<LocationSelectionUiState> = _uiState.asStateFlow()

    private val _selectedCity = MutableSharedFlow<City>()
    val selectedCity: SharedFlow<City> = _selectedCity.asSharedFlow()

    private var searchJob: Job? = null
    private val searchHistory = mutableListOf<City>()
    private var allCities: List<City> = emptyList()
    private var currentSortOrder: SortOrder = SortOrder.ASCENDING
    private var selectedCountry: String? = null
    private var selectedProvince: String? = null
    private var currentTab: LocationTab = LocationTab.PRESET

    init {
        loadCountries()
    }

    fun onEvent(event: LocationSelectionEvent) {
        when (event) {
            is LocationSelectionEvent.LoadCountries -> loadCountries()
            is LocationSelectionEvent.SearchCountry -> searchCountries(event.query)
            is LocationSelectionEvent.SelectCountry -> selectCountry(event.country)
            is LocationSelectionEvent.GoBack -> goBack()
            is LocationSelectionEvent.Retry -> retry()
            is LocationSelectionEvent.Search -> search(event.query)
            is LocationSelectionEvent.SelectCity -> selectCity(event.city)
            is LocationSelectionEvent.SelectProvince -> selectProvince(event.province, event.mainCity)
            is LocationSelectionEvent.SelectDistrict -> selectDistrict(event.district)
            is LocationSelectionEvent.ClearSearch -> clearSearch()
            is LocationSelectionEvent.ClearHistory -> clearHistory()
            is LocationSelectionEvent.ChangeSortOrder -> changeSortOrder(event.sortOrder)
            is LocationSelectionEvent.UseMyLocation -> useMyLocation()
            is LocationSelectionEvent.SelectTab -> selectTab(event.tab)
            is LocationSelectionEvent.UpdateMapLocation -> updateMapLocation(event.latitude, event.longitude)
            is LocationSelectionEvent.ConfirmMapLocation -> confirmMapLocation(event.location)
            is LocationSelectionEvent.ClearMapLocation -> clearMapLocation()
        }
    }

    private fun loadCountries() {
        viewModelScope.launch {
            try {
                _uiState.value = LocationSelectionUiState.Loading
                allCities = locationRepository.getPresetCities()
                
                val countries = allCities
                    .groupBy { it.country }
                    .map { (country, cities) ->
                        CountryInfo(
                            name = country,
                            cityCount = cities.size,
                            isPriority = PRIORITY_COUNTRIES.contains(country)
                        )
                    }
                    .sortedWith(compareBy({ !it.isPriority }, { it.name }))
                
                _uiState.value = LocationSelectionUiState.CountrySelection(countries)
            } catch (e: Exception) {
                _uiState.value = LocationSelectionUiState.Error(getUserFriendlyErrorMessage(e))
            }
        }
    }

    private fun searchCountries(query: String) {
        val currentState = _uiState.value
        if (currentState !is LocationSelectionUiState.CountrySelection) return
        
        val filtered = if (query.isBlank()) {
            allCities
                .groupBy { it.country }
                .map { (country, cities) ->
                    CountryInfo(
                        name = country,
                        cityCount = cities.size,
                        isPriority = PRIORITY_COUNTRIES.contains(country)
                    )
                }
                .sortedWith(compareBy({ !it.isPriority }, { it.name }))
        } else {
            allCities
                .filter { matchesTurkish(it.name, query) || matchesTurkish(it.country, query) }
                .groupBy { it.country }
                .map { (country, cities) ->
                    CountryInfo(
                        name = country,
                        cityCount = cities.size,
                        isPriority = PRIORITY_COUNTRIES.contains(country)
                    )
                }
                .sortedWith(compareBy({ !it.isPriority }, { it.name }))
        }
        
        _uiState.value = currentState.copy(countries = filtered, searchQuery = query)
    }

    private fun selectCountry(country: String) {
        selectedCountry = country
        val citiesInCountry = allCities.filter { it.country == country }
        
        val citiesByProvince = citiesInCountry
            .groupBy { it.city ?: it.name }
            .toSortedMap()
        
        _uiState.value = LocationSelectionUiState.CitySelection(
            country = country,
            cities = citiesInCountry,
            citiesByProvince = citiesByProvince,
            selectedProvince = null,
            sortOrder = currentSortOrder
        )
    }

    private fun selectProvince(province: String, mainCity: City) {
        selectedProvince = province
        val citiesInCountry = allCities.filter { it.country == selectedCountry }
        
        val districts = if (province == mainCity.name) {
            citiesInCountry.filter { it.city == province && it.name != province }
        } else {
            citiesInCountry.filter { it.city == province }
        }
        
        _uiState.value = LocationSelectionUiState.ProvinceSelection(
            country = selectedCountry ?: "",
            province = province,
            mainCity = mainCity,
            districts = districts.sortedBy { it.name.lowercase() },
            sortOrder = currentSortOrder
        )
    }

    private fun goBack() {
        when (_uiState.value) {
            is LocationSelectionUiState.CitySelection -> loadCountries()
            is LocationSelectionUiState.ProvinceSelection -> {
                selectedCountry?.let { selectCountry(it) }
            }
            else -> loadCountries()
        }
    }

    private fun retry() {
        loadCountries()
    }

    private fun search(query: String) {
        searchJob?.cancel()
        
        if (query.isBlank()) {
            showSearchHistory()
            return
        }

        if (query.length < MIN_QUERY_LENGTH) {
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.value = LocationSelectionUiState.Searching
            delay(DEBOUNCE_DELAY_MS)
            
            try {
                val results = searchLocationUseCase(query)
                _uiState.value = LocationSelectionUiState.SearchResults(
                    results = results,
                    query = query,
                    searchHistory = searchHistory.toList()
                )
            } catch (e: NetworkException) {
                _uiState.value = LocationSelectionUiState.Error(getUserFriendlyErrorMessage(e))
            } catch (e: Exception) {
                _uiState.value = LocationSelectionUiState.Error(getUserFriendlyErrorMessage(e))
            }
        }
    }

    private fun getUserFriendlyErrorMessage(exception: Throwable?): String {
        return when (exception) {
            is NetworkException -> "No internet connection. Please check your network and try again."
            else -> exception?.message?.let {
                when {
                    it.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
                    it.contains("network", ignoreCase = true) -> "Network error. Please check your connection."
                    it.contains("empty", ignoreCase = true) -> "No data received from server."
                    else -> "Failed to load data. Please try again."
                }
            } ?: "An unexpected error occurred. Please try again."
        }
    }

    private fun changeSortOrder(sortOrder: SortOrder) {
        currentSortOrder = sortOrder
        val currentState = _uiState.value
        
        when (currentState) {
            is LocationSelectionUiState.CitySelection -> {
                val citiesInCountry = allCities.filter { it.country == currentState.country }
                val sorted = if (sortOrder == SortOrder.ASCENDING) {
                    citiesInCountry.sortedBy { it.name.lowercase() }
                } else {
                    citiesInCountry.sortedByDescending { it.name.lowercase() }
                }
                val citiesByProvince = sorted.groupBy { it.city ?: it.name }
                
                _uiState.value = currentState.copy(
                    citiesByProvince = citiesByProvince,
                    sortOrder = sortOrder
                )
            }
            is LocationSelectionUiState.ProvinceSelection -> {
                val sorted = if (sortOrder == SortOrder.ASCENDING) {
                    currentState.districts.sortedBy { it.name.lowercase() }
                } else {
                    currentState.districts.sortedByDescending { it.name.lowercase() }
                }
                
                _uiState.value = currentState.copy(
                    districts = sorted,
                    sortOrder = sortOrder
                )
            }
            else -> {}
        }
    }

    private fun selectCity(city: City) {
        viewModelScope.launch {
            addToHistory(city)
            saveLocation(city)
            _selectedCity.emit(city)
        }
    }

    private fun selectDistrict(district: City) {
        viewModelScope.launch {
            addToHistory(district)
            saveLocation(district)
            _selectedCity.emit(district)
        }
    }

    private suspend fun saveLocation(city: City) {
        val countryCode = getCountryCode(city.country)
        val location = LocationData(
            latitude = city.latitude,
            longitude = city.longitude,
            country = city.country,
            countryCode = countryCode,
            city = city.province,
            county = city.county?.takeIf { it.isNotBlank() }
                ?: city.name.takeIf { it != city.province }
        )
        locationsCoordinator.addLocation(
            LocationEntry(
                id = UUID.randomUUID().toString(),
                location = location,
                displayName = listOfNotNull(location.city, location.country).joinToString(", ")
            )
        )
    }

    private fun getCountryCode(country: String): String {
        return when (country.lowercase()) {
            "turkey", "türkiye", "turkiye" -> "TR"
            "saudi arabia", "suudi arabistan" -> "SA"
            "egypt", "mısır" -> "EG"
            "indonesia", "endonezya" -> "ID"
            "malaysia", "malezya" -> "MY"
            "pakistan" -> "PK"
            "india" -> "IN"
            "bangladesh" -> "BD"
            "nigeria" -> "NG"
            "morocco" -> "MA"
            "algeria" -> "DZ"
            "tunisia" -> "TN"
            "jordan" -> "JO"
            "united arab emirates", "uae" -> "AE"
            "kuwait" -> "KW"
            "qatar" -> "QA"
            "bahrain" -> "BH"
            "oman" -> "OM"
            else -> country.take(2).uppercase()
        }
    }

    private fun addToHistory(city: City) {
        searchHistory.removeAll { it.name == city.name && it.country == city.country }
        searchHistory.add(0, city)
        if (searchHistory.size > MAX_HISTORY_SIZE) {
            searchHistory.removeLast()
        }
    }

    private fun clearSearch() {
        searchJob?.cancel()
        loadCountries()
    }

    private fun clearHistory() {
        searchHistory.clear()
    }

    private fun useMyLocation() {
        viewModelScope.launch {
            _uiState.value = LocationSelectionUiState.MapView(
                selectedLocation = null,
                isLocating = true,
                error = null
            )
            
            try {
                val location: SettingsLocationData? = locationServiceHelper.getCurrentLocation()
                if (location != null) {
                    val city = locationRepository.reverseGeocode(
                        location.latitude,
                        location.longitude
                    )
                    
                    if (city != null) {
                        selectCity(city)
                    } else {
                        val mapLocation = MapLocationState(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            cityName = location.city,
                            country = location.country
                        )
                        _uiState.value = LocationSelectionUiState.MapView(
                            selectedLocation = mapLocation,
                            isLocating = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.value = LocationSelectionUiState.MapView(
                        selectedLocation = null,
                        isLocating = false,
                        error = "Could not get current location. Please check your GPS settings."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = LocationSelectionUiState.MapView(
                    selectedLocation = null,
                    isLocating = false,
                    error = "Failed to get location: ${e.message}"
                )
            }
        }
    }

    private fun selectTab(tab: LocationTab) {
        currentTab = tab
        when (tab) {
            LocationTab.PRESET -> loadCountries()
            LocationTab.SEARCH -> showSearchHistory()
            LocationTab.MAP -> {
                _uiState.value = LocationSelectionUiState.MapView(
                    selectedLocation = null,
                    isLocating = false,
                    error = null
                )
            }
        }
    }

    private fun showSearchHistory() {
        if (searchHistory.isNotEmpty()) {
            _uiState.value = LocationSelectionUiState.SearchResults(
                results = emptyList(),
                query = "",
                searchHistory = searchHistory.toList()
            )
        } else {
            _uiState.value = LocationSelectionUiState.SearchResults(
                results = emptyList(),
                query = "",
                searchHistory = emptyList()
            )
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val city = locationRepository.reverseGeocode(latitude, longitude)
            val cityName = city?.city ?: city?.name
            val county = city?.county
            val country = city?.country

            val locationInfoText = when {
                !county.isNullOrBlank() -> "$county, $cityName - $country"
                !cityName.isNullOrBlank() -> "$cityName, $country"
                else -> country ?: ""
            }

            val mapLocation = MapLocationState(
                latitude = latitude,
                longitude = longitude,
                cityName = cityName,
                country = country,
                county = county,
                locationInfoText = locationInfoText
            )
            _uiState.value = LocationSelectionUiState.MapView(
                selectedLocation = mapLocation,
                isLocating = false,
                error = null
            )
        }
    }

    private fun confirmMapLocation(location: MapLocationState) {
        viewModelScope.launch {
            val city = City(
                name = location.cityName ?: location.county ?: "Unknown",
                city = location.cityName,
                country = location.country ?: "Unknown",
                latitude = location.latitude,
                longitude = location.longitude,
                timezone = "UTC",
                county = location.county
            )
            selectCity(city)
        }
    }

    private fun clearMapLocation() {
        _uiState.value = LocationSelectionUiState.MapView(
            selectedLocation = null,
            isLocating = false,
            error = null
        )
    }

    companion object {
        private const val DEBOUNCE_DELAY_MS = 300L
        private const val MIN_QUERY_LENGTH = 2
        private const val MAX_HISTORY_SIZE = 5

        private val PRIORITY_COUNTRIES = listOf(
            "Turkey", "Saudi Arabia", "Egypt", "Indonesia", "Malaysia", "Pakistan",
            "India", "Bangladesh", "Nigeria", "Morocco", "Algeria", "Tunisia",
            "Jordan", "UAE", "Kuwait", "Qatar", "Bahrain", "Oman"
        )

        private fun String.normalizeForSearch(): String {
            return Normalizer.normalize(this, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
                .lowercase()
        }

        private fun matchesTurkish(text: String, query: String): Boolean {
            val normalizedText = text.normalizeForSearch()
            val normalizedQuery = query.normalizeForSearch()
            
            if (normalizedText.contains(normalizedQuery)) return true
            
            val turkishEquivalents = mapOf(
                'i' to listOf('i', 'ı', 'İ', 'I'),
                'ı' to listOf('ı', 'i', 'I', 'İ'),
                'u' to listOf('u', 'ü', 'Ü', 'U'),
                'ü' to listOf('ü', 'u', 'Ü', 'U'),
                'o' to listOf('o', 'ö', 'Ö', 'O'),
                'ö' to listOf('ö', 'o', 'Ö', 'O'),
                's' to listOf('s', 'ş', 'Ş', 'S'),
                'ş' to listOf('ş', 's', 'Ş', 'S'),
                'g' to listOf('g', 'ğ', 'Ğ', 'G'),
                'ğ' to listOf('ğ', 'g', 'Ğ', 'G'),
                'c' to listOf('c', 'ç', 'Ç', 'C'),
                'ç' to listOf('ç', 'c', 'Ç', 'C')
            )
            
            for ((asciiChar, turkishChars) in turkishEquivalents) {
                val queryChar = normalizedQuery.firstOrNull() ?: return false
                if (asciiChar == queryChar || turkishChars.contains(queryChar)) {
                    val pattern = "[" + turkishChars.joinToString("") + "]"
                    val replaced = normalizedQuery.replace(queryChar.toString(), pattern)
                    if (normalizedText.contains(Regex(replaced))) return true
                }
            }
            
            return false
        }

        private fun getTimeZoneForCountry(countryCode: String): String {
            return when (countryCode.uppercase()) {
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
                "GB", "UK" -> "Europe/London"
                "US" -> "America/New_York"
                "DE" -> "Europe/Berlin"
                "FR" -> "Europe/Paris"
                else -> "UTC"
            }
        }
    }
}