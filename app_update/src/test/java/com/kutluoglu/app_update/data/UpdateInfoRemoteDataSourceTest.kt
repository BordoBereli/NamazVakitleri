package com.kutluoglu.app_update.data

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UpdateInfoRemoteDataSourceTest {

    private val configSource = mockk<UpdateConfigSource>()
    private val languageProvider = mockk<LanguageProvider>()
    private val dataSource = UpdateInfoRemoteDataSource(configSource, languageProvider)

    @Test
    fun `maps config values to UpdateInfo`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { languageProvider.getLanguageCode() } returns "tr"
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 100L
        every { configSource.getString("update_latest_version_name") } returns "2.0"
        every { configSource.getString("update_release_notes_tr") } returns "Yeni özellikler"
        every { configSource.getString("update_release_notes") } returns "New features"
        every { configSource.getString("update_direct_download_url") } returns "https://example.com/app.apk"
        every { configSource.getString("update_force_version_codes") } returns "100, 150"
        every { configSource.getString("update_optional_version_codes") } returns "175,180"

        val info = dataSource.fetchUpdateInfo()

        assertThat(info).isNotNull()
        assertThat(info!!.latestVersionCode).isEqualTo(200)
        assertThat(info.minVersionCode).isEqualTo(100)
        assertThat(info.latestVersionName).isEqualTo("2.0")
        assertThat(info.releaseNotes).isEqualTo("Yeni özellikler")
        assertThat(info.directDownloadUrl).isEqualTo("https://example.com/app.apk")
        assertThat(info.forceVersionCodes).containsExactly(100, 150)
        assertThat(info.optionalVersionCodes).containsExactly(175, 180)
    }

    @Test
    fun `falls back to default release notes when localized key is missing`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { languageProvider.getLanguageCode() } returns "tr"
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 100L
        every { configSource.getString("update_latest_version_name") } returns "2.0"
        every { configSource.getString("update_release_notes_tr") } returns ""
        every { configSource.getString("update_release_notes") } returns "New features"
        every { configSource.getString("update_direct_download_url") } returns "https://example.com/app.apk"
        every { configSource.getString("update_force_version_codes") } returns ""
        every { configSource.getString("update_optional_version_codes") } returns ""

        val info = dataSource.fetchUpdateInfo()

        assertThat(info).isNotNull()
        assertThat(info!!.releaseNotes).isEqualTo("New features")
    }

    @Test
    fun `uses language specific release notes key for non default language`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { languageProvider.getLanguageCode() } returns "ar"
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 100L
        every { configSource.getString("update_latest_version_name") } returns "2.0"
        every { configSource.getString("update_release_notes_ar") } returns "إصلاحات وتحسينات"
        every { configSource.getString("update_release_notes") } returns "New features"
        every { configSource.getString("update_direct_download_url") } returns "https://example.com/app.apk"
        every { configSource.getString("update_force_version_codes") } returns ""
        every { configSource.getString("update_optional_version_codes") } returns ""

        val info = dataSource.fetchUpdateInfo()

        assertThat(info).isNotNull()
        assertThat(info!!.releaseNotes).isEqualTo("إصلاحات وتحسينات")
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

    @Test
    fun `parses force and optional version code lists`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { languageProvider.getLanguageCode() } returns "tr"
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 100L
        every { configSource.getString("update_latest_version_name") } returns "2.0"
        every { configSource.getString("update_release_notes_tr") } returns ""
        every { configSource.getString("update_release_notes") } returns "New features"
        every { configSource.getString("update_direct_download_url") } returns "https://example.com/app.apk"
        every { configSource.getString("update_force_version_codes") } returns "100, 150"
        every { configSource.getString("update_optional_version_codes") } returns "175,180"

        val info = dataSource.fetchUpdateInfo()

        assertThat(info).isNotNull()
        assertThat(info!!.forceVersionCodes).containsExactly(100, 150)
        assertThat(info.optionalVersionCodes).containsExactly(175, 180)
    }

    @Test
    fun `parses empty version code lists when keys are missing`() = runTest {
        coEvery { configSource.fetchAndActivate() } returns true
        every { languageProvider.getLanguageCode() } returns "tr"
        every { configSource.getLong("update_latest_version_code") } returns 200L
        every { configSource.getLong("update_min_version_code") } returns 100L
        every { configSource.getString("update_latest_version_name") } returns ""
        every { configSource.getString("update_release_notes_tr") } returns ""
        every { configSource.getString("update_release_notes") } returns ""
        every { configSource.getString("update_direct_download_url") } returns ""
        every { configSource.getString("update_force_version_codes") } returns ""
        every { configSource.getString("update_optional_version_codes") } returns ""

        val info = dataSource.fetchUpdateInfo()

        assertThat(info).isNotNull()
        assertThat(info!!.forceVersionCodes).isEmpty()
        assertThat(info.optionalVersionCodes).isEmpty()
    }
}
