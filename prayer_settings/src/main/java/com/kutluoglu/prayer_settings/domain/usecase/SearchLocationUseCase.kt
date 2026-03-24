package com.kutluoglu.prayer_settings.domain.usecase

import android.util.Log
import com.kutluoglu.prayer_settings.domain.model.City
import com.kutluoglu.prayer_settings.domain.repository.LocationRepository
import org.koin.core.annotation.Factory
import java.text.Normalizer

@Factory
class SearchLocationUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(query: String): List<City> {
        if (query.isBlank()) {
            return emptyList()
        }

        val presetResults = try {
            repository.getPresetCities().filter { city ->
                matchesTurkish(city.name, query) || matchesTurkish(city.country, query)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading preset cities", e)
            emptyList()
        }

        if (presetResults.size >= MIN_PRESET_RESULTS) {
            return presetResults.take(MAX_RESULTS)
        }

        val apiResults = try {
            repository.searchCities(query)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching cities", e)
            emptyList()
        }

        val combined = mutableListOf<City>()
        combined.addAll(presetResults)
        
        apiResults.forEach { apiCity ->
            if (combined.none { it.name == apiCity.name && it.country == apiCity.country }) {
                combined.add(apiCity)
            }
        }

        return combined.take(MAX_RESULTS)
    }

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

    companion object {
        private const val TAG = "SearchLocationUseCase"
        private const val MIN_PRESET_RESULTS = 5
        private const val MAX_RESULTS = 20
    }
}
