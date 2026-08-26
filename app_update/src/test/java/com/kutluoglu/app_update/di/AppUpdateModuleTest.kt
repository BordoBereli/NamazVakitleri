package com.kutluoglu.app_update.di

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.data.VersionCodeProvider
import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.model.UpdateInfo
import com.kutluoglu.app_update.domain.repository.UpdateRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AppUpdateModuleTest {

    @Test
    fun `provideCheckForUpdateUseCase wires current version code`() = runTest {
        val repository = mockk<UpdateRepository>()
        val versionCodeProvider = mockk<VersionCodeProvider>()
        every { versionCodeProvider.getCurrentVersionCode() } returns 100

        val useCase = AppUpdateModule.provideCheckForUpdateUseCase(
            repository,
            versionCodeProvider,
        )

        coEvery { repository.getUpdateInfo() } returns UpdateInfo(
            latestVersionCode = 200,
            minVersionCode = 150,
            latestVersionName = "2.0",
            releaseNotes = "",
            directDownloadUrl = "",
        )

        val decision = useCase()

        assertThat(decision).isInstanceOf(UpdateDecision.ForceUpdate::class.java)
    }
}
