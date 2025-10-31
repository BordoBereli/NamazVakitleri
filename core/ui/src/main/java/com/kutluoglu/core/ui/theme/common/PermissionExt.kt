package com.kutluoglu.core.ui.theme.common

import android.Manifest

/**
 * Created by F.K. on 30.10.2025.
 *
 */

val REQUIRED_LOCATION_PERMISSIONS: List<String> by lazy {
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
}
