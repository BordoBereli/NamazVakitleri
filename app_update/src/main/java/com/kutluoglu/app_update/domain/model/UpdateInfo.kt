package com.kutluoglu.app_update.domain.model

data class UpdateInfo(
    val latestVersionCode: Int,
    val minVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String,
    val directDownloadUrl: String,
)
