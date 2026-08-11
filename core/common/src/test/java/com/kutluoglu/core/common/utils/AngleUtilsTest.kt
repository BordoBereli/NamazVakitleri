package com.kutluoglu.core.common.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AngleUtilsTest {

    @Test
    fun `normalizeDegrees keeps angles within range unchanged`() {
        assertThat(AngleUtils.normalizeDegrees(0f)).isEqualTo(0f)
        assertThat(AngleUtils.normalizeDegrees(90f)).isEqualTo(90f)
        assertThat(AngleUtils.normalizeDegrees(-90f)).isEqualTo(-90f)
        assertThat(AngleUtils.normalizeDegrees(180f)).isEqualTo(180f)
        assertThat(AngleUtils.normalizeDegrees(-180f)).isEqualTo(180f)
    }

    @Test
    fun `normalizeDegrees wraps angles above 180 to negative`() {
        assertThat(AngleUtils.normalizeDegrees(350f)).isEqualTo(-10f)
        assertThat(AngleUtils.normalizeDegrees(190f)).isEqualTo(-170f)
        assertThat(AngleUtils.normalizeDegrees(360f)).isEqualTo(0f)
        assertThat(AngleUtils.normalizeDegrees(540f)).isEqualTo(180f)
    }

    @Test
    fun `normalizeDegrees wraps angles below -180 to positive`() {
        assertThat(AngleUtils.normalizeDegrees(-350f)).isEqualTo(10f)
        assertThat(AngleUtils.normalizeDegrees(-190f)).isEqualTo(170f)
        assertThat(AngleUtils.normalizeDegrees(-360f)).isEqualTo(0f)
    }

    @Test
    fun `normalizeDegrees handles fractional angles`() {
        assertThat(AngleUtils.normalizeDegrees(359.5f)).isEqualTo(-0.5f)
        assertThat(AngleUtils.normalizeDegrees(-179.5f)).isEqualTo(-179.5f)
    }
}
