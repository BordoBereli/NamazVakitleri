package com.kutluoglu.prayer_widget.data

private val BACK_VOWELS = setOf('a', 'ı', 'o', 'u')
private val FRONT_VOWELS = setOf('e', 'i', 'ö', 'ü')
private val VOWELS = BACK_VOWELS + FRONT_VOWELS

/**
 * Returns the Turkish dative (yönelme) form of this word, e.g. "Akşam" -> "Akşam'a",
 * "Güneş" -> "Güneş'e", "Öğle" -> "Öğle'ye", "Yatsı" -> "Yatsı'ya".
 *
 * The suffix follows vowel harmony: back vowels (a, ı, o, u) take -a/-ya and
 * front vowels (e, i, ö, ü) take -e/-ye; words ending in a vowel take -ya/-ye.
 */
fun String.toTurkishDative(): String {
    if (isEmpty()) return this
    val lastVowel = lastOrNull { it in VOWELS } ?: return this
    val endsInVowel = last() in VOWELS
    val suffix = when {
        endsInVowel && lastVowel in BACK_VOWELS -> "'ya"
        endsInVowel -> "'ye"
        lastVowel in BACK_VOWELS -> "'a"
        else -> "'e"
    }
    return this + suffix
}
