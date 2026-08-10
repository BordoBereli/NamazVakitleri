package com.kutluoglu.prayer_navigation.core

import androidx.annotation.StringRes

/**
 * Created by F.K. on 23.10.2025.
 *
 */

enum class Destination(
        val graph: String,
        @StringRes val label: Int,
        val iconDrawable: Int,
        val contentDescription: String
) {
    HOME(
        PrayerNestedGraph.HOME,
        R.string.navigation_home,
        R.drawable.home,
        "Home Page"
    ),
    PRAYER_TIMES(
        PrayerNestedGraph.PRAYER_TIMES,
        R.string.navigation_prayer_times,
        R.drawable.prayertimes,
        "PrayerTimes Page"
    ),
    QIBLA(
        PrayerNestedGraph.QIBLA,
        R.string.navigation_qibla,
        R.drawable.qibla_compass,
        "Qibla Page"
    ),
    SETTINGS(
        PrayerNestedGraph.SETTINGS,
        R.string.navigation_settings,
        R.drawable.settings,
        "Settings Page"
    )
}