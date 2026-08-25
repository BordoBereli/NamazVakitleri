package com.kutluoglu.app_update.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.domain.model.UpdateInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UpdateRepositoryImplTest {

    private val remoteDataSource = mockk<UpdateInfoRemoteDataSource>()
    private val repository = UpdateRepositoryImpl(remoteDataSource)

    @Test
    fun `getUpdateInfo delegates to remote data source`() = runTest {
        val info = UpdateInfo(200, 150, "2.0", "notes", "https://example.com/app.apk")
        coEvery { remoteDataSource.fetchUpdateInfo() } returns info

        assertThat(repository.getUpdateInfo()).isEqualTo(info)
        coVerify { remoteDataSource.fetchUpdateInfo() }
    }

    @Test
    fun `getUpdateInfo returns null when remote source returns null`() = runTest {
        coEvery { remoteDataSource.fetchUpdateInfo() } returns null

        assertThat(repository.getUpdateInfo()).isNull()
    }
}
