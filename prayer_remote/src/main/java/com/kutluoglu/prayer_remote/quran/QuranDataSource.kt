package com.kutluoglu.prayer_remote.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.QuranApiSurahResponse
import com.kutluoglu.prayer.model.quran.toQuranVerses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.IOException

@Single
class QuranDataSource(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = "https://api.alquran.cloud"
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val supportedTranslations = mapOf(
        "tr" to "tr.diyanet",
        "en" to "en.sahih"
    )

    suspend fun getSurah(surahNumber: Int, langCode: String): Result<List<AyahData>> =
        withContext(Dispatchers.IO) {
            val translationIdentifier = supportedTranslations[langCode] ?: supportedTranslations["tr"]!!
            val request = Request.Builder()
                .url("$baseUrl/v1/surah/$surahNumber/$translationIdentifier")
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val apiResponse = json.decodeFromString<QuranApiSurahResponse>(body)
                        Result.success(apiResponse.toQuranVerses())
                    } else {
                        Result.failure(IOException("API response body was null."))
                    }
                } else {
                    Result.failure(IOException("API request failed with code: ${response.code}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
}
