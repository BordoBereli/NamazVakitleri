package com.kutluoglu.app_update.data

import android.content.Context
import com.kutluoglu.app_update.domain.model.UpdateInfo
import org.koin.core.annotation.Factory

@Factory
class InstallSourceDetector(
    private val context: Context,
) {

    fun isPlayStoreInstall(): Boolean {
        return runCatching {
            val installer = context.packageManager.getInstallerPackageName(context.packageName)
            installer == PLAY_STORE_INSTALLER
        }.getOrDefault(false)
    }

    fun getPlayStoreUrl(): String = "market://details?id=${context.packageName}"

    fun getPlayStoreWebUrl(): String =
        "https://play.google.com/store/apps/details?id=${context.packageName}"

    fun getDirectDownloadUrl(info: UpdateInfo): String = info.directDownloadUrl

    private companion object {
        const val PLAY_STORE_INSTALLER = "com.android.vending"
    }
}
