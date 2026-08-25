package com.kutluoglu.app_update.domain.repository

import com.kutluoglu.app_update.domain.model.UpdateInfo

interface UpdateRepository {
    suspend fun getUpdateInfo(): UpdateInfo?
}
