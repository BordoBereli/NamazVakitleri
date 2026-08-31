package com.kutluoglu.prayer.model.quran

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuranApiSurahResponse(
    @SerialName("code") val code: Int,
    @SerialName("status") val status: String,
    @SerialName("data") val data: SurahData
)

@Serializable
data class SurahData(
    @SerialName("number") val number: Int,
    @SerialName("name") val name: String,
    @SerialName("englishName") val englishName: String,
    @SerialName("numberOfAyahs") val numberOfAyahs: Int,
    @SerialName("ayahs") val ayahs: List<Ayah>
)

@Serializable
data class Ayah(
    @SerialName("number") val number: Int,
    @SerialName("numberInSurah") val numberInSurah: Int,
    @SerialName("text") val text: String
)

fun QuranApiSurahResponse.toQuranVerses(): List<AyahData> = data.ayahs.map { ayah ->
    AyahData(
        text = ayah.text,
        surah = SurahInfo(
            englishName = data.englishName,
            name = data.name,
            number = data.number,
            numberOfAyahs = data.numberOfAyahs
        ),
        numberInSurah = ayah.numberInSurah
    )
}
