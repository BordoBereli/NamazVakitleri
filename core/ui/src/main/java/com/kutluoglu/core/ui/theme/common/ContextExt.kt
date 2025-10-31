package com.kutluoglu.core.ui.theme.common

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * Created by F.K. on 30.10.2025.
 *
 */

fun Context.hasPermissionOf(permission: String) =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasNotPermissionOf(permission: String) = !this.hasPermissionOf(permission)

fun Context.hasPermissionsOf(permissions: List<String>): Boolean {
    for (permission in permissions) {
        if (this.hasNotPermissionOf(permission)) return false
    }
    return true
}

fun Context.hasAnyPermissionsOf(permissions: List<String>): Boolean {
    for (permission in permissions) {
        if (this.hasPermissionOf(permission)) return true
    }
    return false
}