package com.kutluoglu.namazvakitleri.locale

import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class AppLocaleControllerTest {

    private val controller = AppLocaleController()

    @Test
    fun `toLocaleList returns empty list for system`() {
        assertThat(controller.toLocaleList("system")).isEqualTo(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun `toLocaleList returns locale for explicit code`() {
        assertThat(controller.toLocaleList("tr")).isEqualTo(LocaleListCompat.forLanguageTags("tr"))
    }

    @Test
    fun `toLocaleList handles region-qualified code`() {
        assertThat(controller.toLocaleList("pt-BR")).isEqualTo(LocaleListCompat.forLanguageTags("pt-BR"))
    }

    @Test
    fun `toLanguageCode returns system for empty list`() {
        assertThat(controller.toLanguageCode(LocaleListCompat.getEmptyLocaleList())).isEqualTo("system")
    }

    @Test
    fun `toLanguageCode returns language tag for set locales`() {
        assertThat(controller.toLanguageCode(LocaleListCompat.forLanguageTags("ar"))).isEqualTo("ar")
    }

    @Test
    fun `round trip system stays system`() {
        val list = controller.toLocaleList("system")
        assertThat(controller.toLanguageCode(list)).isEqualTo("system")
    }

    @Test
    fun `round trip explicit code survives`() {
        val list = controller.toLocaleList("de")
        assertThat(controller.toLanguageCode(list)).isEqualTo("de")
    }
}
