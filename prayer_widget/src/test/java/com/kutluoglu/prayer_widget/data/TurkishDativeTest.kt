package com.kutluoglu.prayer_widget.data

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TurkishDativeTest {

    @Test
    fun `consonant ending with back vowel takes -a`() {
        assertThat("Akşam".toTurkishDative()).isEqualTo("Akşam'a")
        assertThat("İmsak".toTurkishDative()).isEqualTo("İmsak'a")
    }

    @Test
    fun `consonant ending with front vowel takes -e`() {
        assertThat("Güneş".toTurkishDative()).isEqualTo("Güneş'e")
    }

    @Test
    fun `vowel ending with front vowel takes -ye`() {
        assertThat("Öğle".toTurkishDative()).isEqualTo("Öğle'ye")
        assertThat("İkindi".toTurkishDative()).isEqualTo("İkindi'ye")
    }

    @Test
    fun `vowel ending with back vowel takes -ya`() {
        assertThat("Yatsı".toTurkishDative()).isEqualTo("Yatsı'ya")
    }

    @Test
    fun `empty string returns itself`() {
        assertThat("".toTurkishDative()).isEmpty()
    }
}
