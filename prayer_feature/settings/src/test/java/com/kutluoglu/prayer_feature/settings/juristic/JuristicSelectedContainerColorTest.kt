package com.kutluoglu.prayer_feature.settings.juristic

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class JuristicSelectedContainerColorTest {

    @Test
    fun `light theme selected card uses soft cream gold`() {
        assertThat(juristicSelectedContainerColor(isDarkTheme = false))
            .isEqualTo(Color(0xFFFFE9A8))
    }

    @Test
    fun `dark theme selected card uses olive gold`() {
        assertThat(juristicSelectedContainerColor(isDarkTheme = true))
            .isEqualTo(Color(0xFF5A501E))
    }
}
