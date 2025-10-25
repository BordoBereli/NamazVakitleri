package com.kutluoglu.namazvakitleri.bottom_navigation

/**
 * Created by F.K. on 23.10.2025.
 *
 */

enum class Destination(
        val route: String,
        val label: String,
        val iconDrawable: Int,
        val contentDescription: String
) {
    HOME(
        "home",
        "Home",
        com.kutluoglu.prayer_feature.home.R.drawable.home,
        "Home Page"
    ),
    PRAYER_TIMES(
        "prayer_times",
        "Prayer Times",
        com.kutluoglu.prayer_feature.prayertimes.R.drawable.prayertimes,
        "PrayerTimes Page"
    )
}


