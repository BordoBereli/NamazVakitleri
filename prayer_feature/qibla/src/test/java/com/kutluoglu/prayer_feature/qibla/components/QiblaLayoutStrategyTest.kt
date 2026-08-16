package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class QiblaLayoutStrategyTest {

    @Test
    fun `landscape when width exceeds height`() {
        assertThat(qiblaLayoutStrategy(maxWidth = 320.dp, maxHeight = 240.dp))
            .isEqualTo(QiblaLayoutStrategy.LANDSCAPE)
    }

    @Test
    fun `portrait when height exceeds width`() {
        assertThat(qiblaLayoutStrategy(maxWidth = 240.dp, maxHeight = 320.dp))
            .isEqualTo(QiblaLayoutStrategy.PORTRAIT)
    }

    @Test
    fun `square defaults to portrait`() {
        assertThat(qiblaLayoutStrategy(maxWidth = 240.dp, maxHeight = 240.dp))
            .isEqualTo(QiblaLayoutStrategy.PORTRAIT)
    }
}