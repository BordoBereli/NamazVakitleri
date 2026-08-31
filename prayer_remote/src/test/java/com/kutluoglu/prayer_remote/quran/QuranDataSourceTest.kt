package com.kutluoglu.prayer_remote.quran

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QuranDataSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: QuranDataSource

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        dataSource = QuranDataSource(
            httpClient = OkHttpClient(),
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        )
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getSurah parses all ayahs of the surah`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "code": 200,
                      "status": "OK",
                      "data": {
                        "number": 1,
                        "name": "سُورَةُ ٱلْفَاتِحَةِ",
                        "englishName": "Al-Faatiha",
                        "englishNameTranslation": "The Opening",
                        "revelationType": "Meccan",
                        "numberOfAyahs": 2,
                        "ayahs": [
                          {"number": 1, "text": "Bismillah", "numberInSurah": 1, "juz": 1, "page": 1},
                          {"number": 2, "text": "Alhamdulillah", "numberInSurah": 2, "juz": 1, "page": 1}
                        ],
                        "edition": {"identifier": "tr.diyanet", "language": "tr", "name": "Diyanet", "englishName": "Diyanet", "format": "text", "type": "translation", "direction": "ltr"}
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = dataSource.getSurah(surahNumber = 1, langCode = "tr")

        assertThat(result.isSuccess).isTrue()
        val verses = result.getOrThrow()
        assertThat(verses).hasSize(2)
        assertThat(verses[0].text).isEqualTo("Bismillah")
        assertThat(verses[0].surah.number).isEqualTo(1)
        assertThat(verses[0].surah.englishName).isEqualTo("Al-Faatiha")
        assertThat(verses[0].numberInSurah).isEqualTo(1)
        assertThat(verses[1].numberInSurah).isEqualTo(2)
    }

    @Test
    fun `getSurah returns failure on non-200 response`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

        val result = dataSource.getSurah(surahNumber = 1, langCode = "tr")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getSurah returns failure on malformed JSON`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("not json"))

        val result = dataSource.getSurah(surahNumber = 1, langCode = "tr")

        assertThat(result.isFailure).isTrue()
    }
}
