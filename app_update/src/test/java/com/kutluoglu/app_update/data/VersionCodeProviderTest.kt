package com.kutluoglu.app_update.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VersionCodeProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `getCurrentVersionCode returns a non-negative version code`() {
        val provider = VersionCodeProvider(context)

        assertThat(provider.getCurrentVersionCode()).isAtLeast(0)
    }
}
