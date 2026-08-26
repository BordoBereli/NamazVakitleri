package com.kutluoglu.app_update.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UpdateModelTest {

    @Test
    fun `UpdateInfo holds update metadata`() {
        val info = UpdateInfo(
            latestVersionCode = 200,
            minVersionCode = 150,
            latestVersionName = "2.0",
            releaseNotes = "notes",
            directDownloadUrl = "https://example.com/app.apk",
        )
        assertThat(info.latestVersionCode).isEqualTo(200)
        assertThat(info.minVersionCode).isEqualTo(150)
        assertThat(info.latestVersionName).isEqualTo("2.0")
        assertThat(info.releaseNotes).isEqualTo("notes")
        assertThat(info.directDownloadUrl).isEqualTo("https://example.com/app.apk")
    }

    @Test
    fun `UpdateDecision ForceUpdate holds info`() {
        val info = UpdateInfo(200, 150, "2.0", "", "")
        val decision = UpdateDecision.ForceUpdate(info)
        assertThat(decision.info).isEqualTo(info)
    }

    @Test
    fun `UpdateDecision NoUpdate is a singleton`() {
        assertThat(UpdateDecision.NoUpdate).isSameInstanceAs(UpdateDecision.NoUpdate)
    }

    @Test
    fun `UpdateInfo defaults version code lists to empty`() {
        val info = UpdateInfo(200, 150, "2.0", "", "")
        assertThat(info.forceVersionCodes).isEmpty()
        assertThat(info.optionalVersionCodes).isEmpty()
    }

    @Test
    fun `UpdateInfo holds force and optional version code lists`() {
        val info = UpdateInfo(
            latestVersionCode = 200,
            minVersionCode = 150,
            latestVersionName = "2.0",
            releaseNotes = "",
            directDownloadUrl = "",
            forceVersionCodes = listOf(100, 150),
            optionalVersionCodes = listOf(175),
        )
        assertThat(info.forceVersionCodes).containsExactly(100, 150)
        assertThat(info.optionalVersionCodes).containsExactly(175)
    }
}
