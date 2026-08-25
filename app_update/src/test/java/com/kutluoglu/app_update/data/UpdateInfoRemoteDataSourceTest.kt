package com.kutluoglu.app_update.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UpdateInfoRemoteDataSourceTest {

    private val configSource = mockk<UpdateConfigSource>()
    private val dataSource = UpdateInfoRemoteDataSource(configSource)

    @Test
    fun `maps config values to UpdateInfo`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 100L
        every { configSource.getString("update_latest_version_name") } returns "2.0"
        every { configSource.getString("update_release_notes") } returns "New features"
        every { configSource.getString("update_direct_download_url") } returns "https://example.com/app.apk"

        val info = dataSource.fetchUpdateInfo()

        assertThat(info).isNotNull()
        assertThat(info!!.latestVersionCode).isEqualTo(200)
        assertThat(info.minVersionCode).isEqualTo(100)
        assertThat(info.latestVersionName).isEqualTo("2.0")
        assertThat(info.releaseNotes).isEqualTo("New features")
        assertThat(info.directDownloadUrl).isEqualTo("https://example.com/app.apk")
    }

    @Test
    fun `returns null when latest version code is missing`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { configSource.getLong("update_latest_version_code") } returns 0L
        every { configSource.getLong("update_min_version_code") } returns 100L

        assertThat(dataSource.fetchUpdateInfo()).isNull()
    }

    @Test
    fun `returns null when min version code is missing`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 0L

        assertThat(dataSource.fetchUpdateInfo()).isNull()
    }

    @Test
    fun `returns null when fetch fails`() = runTest {
        coEvery { configSource.fetchAndActivate() } throws RuntimeException("network error")

        assertThat(dataSource.fetchUpdateInfo()).isNull()
    }
}
