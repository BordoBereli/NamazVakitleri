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

    /**
     * True when the app was installed from a known app store (Play Store or a
     * major OEM store). Used to decide whether GMS routes messages to the
     * manifest-declared WearableListenerService, which it does for Play Store
     * and OEM apps but not for sideloaded installs.
     */
    fun isStoreInstall(): Boolean {
        return runCatching {
            val installer = context.packageManager.getInstallerPackageName(context.packageName)
            installer in STORE_INSTALLERS
        }.getOrDefault(false)
    }

    fun getPlayStoreUrl(): String = "market://details?id=${context.packageName}"

    fun getPlayStoreWebUrl(): String =
        "https://play.google.com/store/apps/details?id=${context.packageName}"

    fun getDirectDownloadUrl(info: UpdateInfo): String = info.directDownloadUrl

    private companion object {
        const val PLAY_STORE_INSTALLER = "com.android.vending"
        val STORE_INSTALLERS = setOf(
            "com.android.vending",             // Google Play Store
            "com.sec.android.app.samsungapps", // Samsung Galaxy Store
            "com.amazon.venezia",              // Amazon Appstore
            "com.huawei.appmarket"             // Huawei AppGallery
        )
    }
}
