package com.kutluoglu.app_update.data

import android.content.Context
import org.koin.core.annotation.Factory

@Factory
class VersionCodeProvider(
    private val context: Context,
) {

    @Suppress("DEPRECATION")
    fun getCurrentVersionCode(): Int {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionCode
        }.getOrDefault(0)
    }
}
