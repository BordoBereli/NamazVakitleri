package com.kutluoglu.prayer_feature.common.prayerUtils

import android.content.Context
import android.content.res.Configuration
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.Locale

class ResourcesProviderTest {

    @Test
    fun `getStringArray resolves from a locale-aware configuration context`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr"))
            val context = mockk<Context>()
            val baseConfig = mockk<Configuration>()
            every { context.resources } returns mockk()
            every { context.resources.configuration } returns baseConfig
            val localizedContext = mockk<Context>()
            every { localizedContext.resources } returns mockk()
            every { localizedContext.resources.getStringArray(any()) } returns arrayOf("İmsak")
            every { context.createConfigurationContext(any()) } returns localizedContext

            val provider = ResourcesProvider(context)
            val result = provider.getStringArray(123)

            assertThat(result).asList().containsExactly("İmsak")
            verify { context.createConfigurationContext(any()) }
        } finally {
            Locale.setDefault(original)
        }
    }
}
