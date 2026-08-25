package com.kutluoglu.app_update.data

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import org.koin.core.annotation.Single

@Single
class FirebaseUpdateConfigSource(
    private val remoteConfig: FirebaseRemoteConfig,
) : UpdateConfigSource {

    override suspend fun fetchAndActivate(): Boolean {
        remoteConfig.fetch(0).await()
        return remoteConfig.activate().await()
    }

    override fun getLong(key: String): Long = remoteConfig.getLong(key)

    override fun getString(key: String): String = remoteConfig.getString(key)
}
