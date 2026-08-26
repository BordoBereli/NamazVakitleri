package com.kutluoglu.app_update.di

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kutluoglu.app_update.data.VersionCodeProvider
import com.kutluoglu.app_update.domain.repository.UpdateRepository
import com.kutluoglu.app_update.domain.usecase.CheckForUpdateUseCase
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("com.kutluoglu.app_update**")
object AppUpdateModule {

    @Single
    fun provideCheckForUpdateUseCase(
        repository: UpdateRepository,
        versionCodeProvider: VersionCodeProvider,
    ): CheckForUpdateUseCase = CheckForUpdateUseCase(
        repository = repository,
        currentVersionCode = versionCodeProvider.getCurrentVersionCode(),
    )

    @Single
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
}
