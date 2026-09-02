package com.kutluoglu.prayer_feature.common.prayerUtils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import com.kutluoglu.core.designsystem.R
import com.kutluoglu.prayer_feature.common.R as AppR


internal fun buildPrayerIconMap(prayerNames: List<String>): Map<String, Int> =
    if (prayerNames.size < 6) {
        emptyMap()
    } else {
        mapOf(
            prayerNames[0] to AppR.drawable.facr,
            prayerNames[1] to AppR.drawable.sunrise,
            prayerNames[2] to AppR.drawable.dhuhr,
            prayerNames[3] to AppR.drawable.asr,
            prayerNames[4] to AppR.drawable.magrip,
            prayerNames[5] to AppR.drawable.isha
        )
    }

/**
 * Created by F.K. on 28.10.2025.
 *
 * A composable function that efficiently maps a prayer name to its corresponding drawable resource ID.
 * It uses `remember` to avoid re-calculating the mapping on every recomposition.
 *
 * @param prayerName The name of the prayer (e.g., "Imsak", "Dhuhr").
 * @return The drawable resource ID for the prayer's icon, or a default/invalid ID (-1) if not found.
 */
@Composable
fun getPrayerDrawableIdFrom(prayerName: String): Int {
    val prayerNames = stringArrayResource(id = R.array.prayers)
    val prayerIconMap = remember { buildPrayerIconMap(prayerNames.toList()) }
    return prayerIconMap[prayerName] ?: AppR.drawable.facr
}
