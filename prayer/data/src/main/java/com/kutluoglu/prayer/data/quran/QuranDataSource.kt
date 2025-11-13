package com.kutluoglu.prayer.data.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.QuranApiAyahResponse
import com.kutluoglu.prayer.model.quran.toQuranVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.IOException
import kotlin.random.Random

@Single
class QuranDataSource(private val httpClient: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getRandomVerse(): Result<AyahData> = withContext(Dispatchers.IO) {
        // The Quran has 6236 verses (ayahs)
        val randomAyahNumber = Random.Default.nextInt(1, 6237)
        val request = Request.Builder()
            // Fetches a single random ayah in Turkish (translation by Türkiye Diyanet Başkanlığı)
            .url("https://api.alquran.cloud/v1/ayah/$randomAyahNumber/tr.diyanet")
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val apiResponse = json.decodeFromString<QuranApiAyahResponse>(body)
                    Result.success(apiResponse.toQuranVerse())
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