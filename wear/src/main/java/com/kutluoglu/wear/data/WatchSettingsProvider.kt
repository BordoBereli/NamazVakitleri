package com.kutluoglu.wear.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.prayer.CalculationMethod
import com.kutluoglu.prayer.model.prayer.JuristicMethod
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

data class WatchSettings(
    val calculationMethod: CalculationMethod,
    val juristicMethod: JuristicMethod
)

/**
 * Supplies the watch-local prayer settings used for on-device computation.
 * Backed by DataStore so values can be changed later; defaults to
 * TURKEY_DIYANET / STANDARD when unset.
 */
@Factory
class WatchSettingsProvider(
    private val dataStore: DataStore<Preferences>
) {

    suspend fun getSettings(): WatchSettings {
        val preferences = dataStore.data.first()
        return WatchSettings(
            calculationMethod = preferences[KEY_CALCULATION_METHOD]
                ?.let { CalculationMethod.fromSettingsId(it) }
                ?: CalculationMethod.TURKEY_DIYANET,
            juristicMethod = preferences[KEY_JURISTIC_METHOD]
                ?.let { JuristicMethod.fromSettingsId(it) }
                ?: JuristicMethod.STANDARD
        )
    }

    suspend fun updateSettings(settings: WatchSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_CALCULATION_METHOD] = settings.calculationMethod.name
            preferences[KEY_JURISTIC_METHOD] = settings.juristicMethod.name
        }
    }

    companion object {
        private val KEY_CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        private val KEY_JURISTIC_METHOD = stringPreferencesKey("juristic_method")
    }
}
