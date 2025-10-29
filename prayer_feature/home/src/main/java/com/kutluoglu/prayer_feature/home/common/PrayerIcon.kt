package com.kutluoglu.prayer_feature.home.common

import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.kutluoglu.core.ui.R.array
import com.kutluoglu.prayer_feature.home.R

/**
 * Created by F.K. on 28.10.2025.
 *
 * A composable function that efficiently maps a prayer name to its corresponding drawable resource ID.
 * It uses `remember` to avoid re-calculating the mapping on every recomposition.
 *
 * @param prayerName The name of the prayer (e.g., "Fajr", "Dhuhr").
 * @return The drawable resource ID for the prayer's icon, or a default/invalid ID (-1) if not found.
 */
@Composable
fun getPrayerDrawableIdFrom(prayerName: String): Int {
    val prayerNames = stringArrayResource(array.prayers)
    // Use 'remember' to create and cache the map of prayer names to drawable IDs.
    // This map is created only once and reused across recompositions, improving performance.
    val prayerIconMap = remember(prayerNames) {
        // Defensive check to ensure we have the expected number of prayer names.
        if (prayerNames.size < 6) {
            emptyMap() // Return an empty map if resources are not as expected.
        } else {
            // Create an immutable map for efficient lookups.
            mapOf(
                prayerNames[0] to R.drawable.facr,
                prayerNames[1] to R.drawable.sunrise,
                prayerNames[2] to R.drawable.dhuhr,
                prayerNames[3] to R.drawable.asr,
                prayerNames[4] to R.drawable.magrip,
                prayerNames[5] to R.drawable.isha
            )
        }
    }

    // Return the icon from the map, or a default value of "R.drawable.facr" if the key doesn't exist.
    return prayerIconMap[prayerName] ?: R.drawable.facr
}
