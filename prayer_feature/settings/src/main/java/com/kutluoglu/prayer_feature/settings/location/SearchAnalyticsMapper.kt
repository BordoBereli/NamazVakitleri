package com.kutluoglu.prayer_feature.settings.location

import com.kutluoglu.prayer.model.location.City

/**
 * Derives PII-safe analytics parameters from search input and results.
 *
 * Raw query text must never reach analytics; only these derived,
 * non-identifying signals are logged (see Architecture Review P0).
 */
object SearchAnalyticsMapper {

    fun queryLengthBucket(query: String): String = when (query.length) {
        in 2..3 -> "2-3"
        in 4..6 -> "4-6"
        in 7..10 -> "7-10"
        else -> "11+"
    }

    fun topResultCountry(results: List<City>): String =
        results.firstOrNull()?.country ?: "none"

    fun topResultAdminLevel(results: List<City>): String {
        val top = results.firstOrNull() ?: return "none"
        return when {
            top.county != null -> "district"
            top.city != null -> "city"
            else -> "locality"
        }
    }
}
