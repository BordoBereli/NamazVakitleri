package com.kutluoglu.prayer.model.quran

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuranApiAyahResponse(
    @SerialName("code")
    val code: Int,
    @SerialName("data")
    val `data`: Data,
    @SerialName("status")
    val status: String
)

fun QuranApiAyahResponse.toQuranVerse() = AyahData(
    text = this.data.text,
    surah = SurahInfo(
        englishName = this.data.surah.englishName,
        name = this.data.surah.name,
        number = this.data.surah.number,
        numberOfAyahs = this.data.surah.numberOfAyahs
    ),
    numberInSurah = this.data.numberInSurah,
)

@Serializable
data class AyahData(
        val text: String,
        val surah: SurahInfo,
        val numberInSurah: Int
) {
    override fun toString(): String {
        return "${surah.numberOfAyahs}:$numberInSurah"
    }
}

@Serializable
data class SurahInfo(
        val englishName: String,
        val name: String,
        val number: Int,
        val numberOfAyahs: Int
)

@Serializable
data class Data(
        @SerialName("edition")
        val edition: Edition,
        @SerialName("hizbQuarter")
        val hizbQuarter: Int,
        @SerialName("juz")
        val juz: Int,
        @SerialName("manzil")
        val manzil: Int,
        @SerialName("number")
        val number: Int,
        @SerialName("numberInSurah")
        val numberInSurah: Int,
        @SerialName("page")
        val page: Int,
        @SerialName("ruku")
        val ruku: Int,
        @SerialName("sajda")
        val sajda: Boolean,
        @SerialName("surah")
        val surah: Surah,
        @SerialName("text")
        val text: String
)

@Serializable
data class Edition(
        @SerialName("direction")
        val direction: String,
        @SerialName("englishName")
        val englishName: String,
        @SerialName("format")
        val format: String,
        @SerialName("identifier")
        val identifier: String,
        @SerialName("language")
        val language: String,
        @SerialName("name")
        val name: String,
        @SerialName("type")
        val type: String
)

@Serializable
data class Surah(
        @SerialName("englishName")
        val englishName: String,
        @SerialName("englishNameTranslation")
        val englishNameTranslation: String,
        @SerialName("name")
        val name: String,
        @SerialName("number")
        val number: Int,
        @SerialName("numberOfAyahs")
        val numberOfAyahs: Int,
        @SerialName("revelationType")
        val revelationType: String
)