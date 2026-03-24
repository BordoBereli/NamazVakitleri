package com.kutluoglu.prayer_location.utils

import android.Manifest
import android.content.Context
import com.kutluoglu.core.designsystem.extensions.REQUIRED_LOCATION_PERMISSIONS
import com.kutluoglu.core.designsystem.extensions.hasAnyPermissionsOf
import com.kutluoglu.core.designsystem.extensions.hasPermissionOf
import com.kutluoglu.core.designsystem.extensions.hasPermissionsOf

/**
 * Created by F.K. on 30.10.2025.
 *
 */


fun Context.isLocationPermissionGranted() = hasPermissionsOf(REQUIRED_LOCATION_PERMISSIONS)
fun Context.isAnyLocationPermissionGranted() = hasAnyPermissionsOf(REQUIRED_LOCATION_PERMISSIONS)

fun Context.isAccessFinePermissionGranted() = hasPermissionOf(Manifest.permission.ACCESS_FINE_LOCATION)
fun Context.isAccessCoarsePermissionGranted() = hasPermissionOf(Manifest.permission.ACCESS_COARSE_LOCATION)
