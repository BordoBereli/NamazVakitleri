package com.kutluoglu.app_update.data

import com.kutluoglu.app_update.domain.model.UpdateInfo
import com.kutluoglu.app_update.domain.repository.UpdateRepository
import org.koin.core.annotation.Single

@Single
class UpdateRepositoryImpl(
    private val remoteDataSource: UpdateInfoRemoteDataSource,
) : UpdateRepository {

    override suspend fun getUpdateInfo(): UpdateInfo? = remoteDataSource.fetchUpdateInfo()
}
