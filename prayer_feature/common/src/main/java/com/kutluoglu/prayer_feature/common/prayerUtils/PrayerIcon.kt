package com.kutluoglu.prayer_feature.common.prayerUtils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import com.kutluoglu.core.designsystem.R
import com.kutluoglu.prayer_feature.common.R as AppR


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
    val prayerNames = stringArrayResource(id = R.array.prayers)
    // Use 'remember' to create and cache the map of prayer names to drawable IDs.
    // This map is created only once and reused across recompositions, improving performance.
    val prayerIconMap = remember(prayerNames) {
        // Defensive check to ensure we have the expected number of prayer names.
        if (prayerNames.size < 6) {
            emptyMap() // Return an empty map if resources are not as expected.
        } else {
            // Create an immutable map for efficient lookups.
            mapOf(
                prayerNames[0] to AppR.drawable.facr,
                prayerNames[1] to AppR.drawable.sunrise,
                prayerNames[2] to AppR.drawable.dhuhr,
                prayerNames[3] to AppR.drawable.asr,
                prayerNames[4] to AppR.drawable.magrip,
                prayerNames[5] to AppR.drawable.isha
            )
        }
    }

    // Return the icon from the map, or a default value of "R.drawable.facr" if the key doesn't exist.
    return prayerIconMap[prayerName] ?: AppR.drawable.facr
}
