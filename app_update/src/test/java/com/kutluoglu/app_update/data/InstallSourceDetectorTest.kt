package com.kutluoglu.app_update.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.domain.model.UpdateInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplicationPackageManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InstallSourceDetectorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun setInstallerPackageName(installer: String?) {
        val shadowPm = shadowOf(context.packageManager) as ShadowApplicationPackageManager
        val method = ShadowApplicationPackageManager::class.java
            .getDeclaredMethod("setInstallerPackageName", String::class.java, String::class.java)
        method.isAccessible = true
        method.invoke(shadowPm, context.packageName, installer)
    }

    @Test
    fun `isPlayStoreInstall returns true for vending installer`() {
        setInstallerPackageName("com.android.vending")

        val detector = InstallSourceDetector(context)

        assertThat(detector.isPlayStoreInstall()).isTrue()
    }

    @Test
    fun `isPlayStoreInstall returns false for null installer`() {
        setInstallerPackageName(null)

        val detector = InstallSourceDetector(context)

        assertThat(detector.isPlayStoreInstall()).isFalse()
    }

    @Test
    fun `getPlayStoreUrl returns market url for package`() {
        val detector = InstallSourceDetector(context)

        assertThat(detector.getPlayStoreUrl())
            .isEqualTo("market://details?id=${context.packageName}")
    }

    @Test
    fun `getPlayStoreWebUrl returns play web url for package`() {
        val detector = InstallSourceDetector(context)

        assertThat(detector.getPlayStoreWebUrl())
            .isEqualTo("https://play.google.com/store/apps/details?id=${context.packageName}")
    }

    @Test
    fun `getDirectDownloadUrl returns info url`() {
        val detector = InstallSourceDetector(context)
        val info = UpdateInfo(200, 150, "2.0", "", "https://example.com/app.apk")

        assertThat(detector.getDirectDownloadUrl(info)).isEqualTo("https://example.com/app.apk")
    }
}
