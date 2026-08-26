package com.kutluoglu.app_update.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateUrlOpenerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `open returns false when no activity can handle the url`() {
        val opener = UpdateUrlOpener(context)

        val result = opener.open("market://details?id=com.kutluoglu.namazvakitleri")

        assertThat(result).isFalse()
    }

    @Test
    fun `open returns true when an activity can handle the url`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.example.browser"
                name = "com.example.browser.BrowserActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        val opener = UpdateUrlOpener(context)

        val result = opener.open("https://example.com")

        assertThat(result).isTrue()
    }
}
