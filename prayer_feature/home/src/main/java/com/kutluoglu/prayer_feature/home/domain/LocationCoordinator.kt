package com.kutluoglu.prayer_feature.home.domain

import android.util.Log
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.usecases.location.GetSavedLocationUseCase
import com.kutluoglu.prayer.usecases.location.ObserveLocationUseCase
import com.kutluoglu.prayer.usecases.location.SaveLocationUseCase
import com.kutluoglu.prayer_location.LocationService
import com.kutluoglu.prayer_settings.domain.model.LocationSettings
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

/**
 * Resolves the source-of-truth [LocationData] (settings -> saved -> GPS), exposes the
 * location/settings observer flows, and tracks whether GPS has drifted from the saved location.
 */
@OptIn(FlowPreview::class)
@Factory
class LocationCoordinator(
    private val settingsRepository: SettingsRepository,
    private val getSavedLocationUseCase: GetSavedLocationUseCase,
    private val saveLocationUseCase: SaveLocationUseCase,
    private val observeLocationUseCase: ObserveLocationUseCase,
    private val locationService: LocationService
) {
    private val _locationUpdatePrompt = MutableStateFlow(false)
    val locationUpdatePrompt: StateFlow<Boolean> = _locationUpdatePrompt

    /** Observes repository location pushes (relayed with debounce + distinct). */
    fun observeLocationChanges(): Flow<LocationData> =
        observeLocationUseCase()
            .debounce(500)
            .distinctUntilChanged()

    /** Observes settings changes and maps them to [LocationData]. */
    fun observeSettingsChanges(): Flow<LocationData> =
        settingsRepository.observeSettings()
            .debounce(500)
            .distinctUntilChanged()
            .map { setLocationDataFrom(it.location) }

    /** Settings -> saved location -> GPS fallback precedence. */
    suspend fun resolveInitial(): LocationData? {
        return try {
            setLocationDataFrom(settingsRepository.getSettings().location)
        } catch (e: Exception) {
            Log.e("LocationCoordinator", "Failed to load from settings: ${e.message}")
            getSavedLocationUseCase()
                .getOrNullWrapped()
                ?: refreshFromGps()
        }
    }

    /**
     * Returns the saved location (or GPS fallback). Sets [locationUpdatePrompt] when the
     * current GPS position differs from what was saved, mirroring the old refresh flow.
     */
    suspend fun resolveSavedAndDetectDrift(): LocationData? {
        val saved = getSavedLocationUseCase().getOrNullWrapped()
            ?: return refreshFromGps()
        val current = locationService.getCurrentLocation()
        if (current != null && locationService.isDifferentThen(saved)) {
            _locationUpdatePrompt.value = true
        }
        return saved
    }

    /** Gets the current GPS location and saves it. Returns null when GPS is unavailable. */
    suspend fun refreshFromGps(): LocationData? {
        return try {
            val gpsLocation = locationService.getCurrentLocation()
            if (gpsLocation != null) {
                saveLocationUseCase(gpsLocation)
                gpsLocation
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("LocationCoordinator", "GPS fallback failed: ${e.message}")
            null
        }
    }

    fun setLocationDataFrom(locationSettings: LocationSettings): LocationData {
        return LocationData(
            latitude = locationSettings.latitude,
            longitude = locationSettings.longitude,
            country = locationSettings.country,
            countryCode = getCountryCode(locationSettings.timeZone),
            city = locationSettings.cityName,
            county = locationSettings.district
        )
    }

    private fun getCountryCode(timeZone: String): String? {
        return when {
            timeZone.contains("Istanbul", ignoreCase = true) ||
                timeZone.contains("Europe/Istanbul", ignoreCase = true) -> "TR"
            timeZone.contains("Europe/Berlin", ignoreCase = true) -> "DE"
            timeZone.contains("Europe/London", ignoreCase = true) -> "GB"
            timeZone.contains("Europe/Paris", ignoreCase = true) -> "FR"
            timeZone.contains("Asia/Jakarta", ignoreCase = true) -> "ID"
            timeZone.contains("Asia/Riyadh", ignoreCase = true) -> "SA"
            else -> null
        }
    }

    /** Small helper so getSavedLocationUseCase().getOrNull() stays explicit about Result. */
    private suspend fun Result<LocationData>.getOrNullWrapped(): LocationData? = getOrNull()
}
