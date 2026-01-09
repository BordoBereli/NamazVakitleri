package com.kutluoglu.prayer_navigation.core

/**
 * Created by F.K. on 23.10.2025.
 *
 */

enum class Destination(
        val graph: String,
        val label: String,
        val iconDrawable: Int,
        val contentDescription: String
) {
    HOME(
        PrayerNestedGraph.HOME,
        "Home",
        R.drawable.home,
        "Home Page"
    ),
    PRAYER_TIMES(
        PrayerNestedGraph.PRAYER_TIMES,
        "Prayer Times",
        R.drawable.prayertimes,
        "PrayerTimes Page"
    ),
    QIBLA(
        PrayerNestedGraph.QIBLA,
        "Qibla",
        R.drawable.qibla_compass,
        "Qibla Page"
    ),
    SETTINGS(
    PrayerNestedGraph.SETTINGS,
    "Settings",
    R.drawable.settings,
    "Settings Page"
    )
}