package com.kutluoglu.namazvakitleri.bottom_navigation

import com.kutluoglu.core.ui.theme.navigation.NestedGraph

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
        NestedGraph.HOME,
        "Home",
        com.kutluoglu.prayer_feature.home.R.drawable.home,
        "Home Page"
    ),
    PRAYER_TIMES(
        NestedGraph.PRAYER_TIMES,
        "Prayer Times",
        com.kutluoglu.prayer_feature.prayertimes.R.drawable.prayertimes,
        "PrayerTimes Page"
    )
}


