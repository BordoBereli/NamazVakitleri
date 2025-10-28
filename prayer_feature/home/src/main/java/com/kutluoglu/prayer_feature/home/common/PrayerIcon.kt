package com.kutluoglu.prayer_feature.home.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.kutluoglu.core.ui.R.array
import com.kutluoglu.prayer_feature.home.R

/**
 * Created by F.K. on 28.10.2025.
 *
 */

@Composable
fun getPrayerDrawableIdFrom(drawableName: String): Int {
    println("drawableName: $drawableName")
    val prayerNames = stringArrayResource(array.prayers)
    return when(drawableName) {
        prayerNames[0] -> return R.drawable.facr
        prayerNames[1] -> return R.drawable.sunrise
        prayerNames[2] -> return R.drawable.dhuhr
        prayerNames[3] -> return R.drawable.asr
        prayerNames[4] -> return R.drawable.magrip
        prayerNames[5] -> return R.drawable.isha
        else -> -1
    }
}
