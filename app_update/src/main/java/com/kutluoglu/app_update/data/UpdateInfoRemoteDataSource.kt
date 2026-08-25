package com.kutluoglu.app_update.data

import com.kutluoglu.app_update.domain.model.UpdateInfo
import org.koin.core.annotation.Single

@Single
class UpdateInfoRemoteDataSource(
    private val configSource: UpdateConfigSource,
) {

    suspend fun fetchUpdateInfo(): UpdateInfo? {
        return runCatching {
            configSource.fetchAndActivate()
            val latest = configSource.getLong(KEY_LATEST_VERSION_CODE)
            val min = configSource.getLong(KEY_MIN_VERSION_CODE)
            if (latest <= 0 || min <= 0) return null
            UpdateInfo(
                latestVersionCode = latest.toInt(),
                minVersionCode = min.toInt(),
                latestVersionName = configSource.getString(KEY_LATEST_VERSION_NAME),
                releaseNotes = configSource.getString(KEY_RELEASE_NOTES),
                directDownloadUrl = configSource.getString(KEY_DIRECT_DOWNLOAD_URL),
            )
        }.getOrNull()
    }

    private companion object {
        const val KEY_LATEST_VERSION_CODE = "update_latest_version_code"
        const val KEY_MIN_VERSION_CODE = "update_min_version_code"
        const val KEY_LATEST_VERSION_NAME = "update_latest_version_name"
        const val KEY_RELEASE_NOTES = "update_release_notes"
        const val KEY_DIRECT_DOWNLOAD_URL = "update_direct_download_url"
    }
}
