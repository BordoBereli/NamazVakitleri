package com.kutluoglu.prayer_remote.location

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CitySearchRemoteDataSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: CitySearchRemoteDataSource

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        dataSource = CitySearchRemoteDataSource(
            httpClient = OkHttpClient(),
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        )
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `reverseGeocode parses province as city and town as county (Bursa)`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                        "lat": "40.1826",
                        "lon": "29.0676",
                        "display_name": "Osmangazi, Bursa, Türkiye",
                        "address": {
                            "road": "Cumhuriyet Caddesi",
                            "town": "Osmangazi",
                            "province": "Bursa",
                            "ISO3166-2-lvl4": "TR-16",
                            "country": "Türkiye",
                            "country_code": "tr"
                        }
                    }
                    """.trimIndent()
                )
        )

        val city = dataSource.reverseGeocode(40.1826, 29.0676)

        assertNotNull(city)
        assertEquals("Bursa", city!!.name)
        assertEquals("Bursa", city.city)
        assertEquals("Osmangazi", city.county)
        assertEquals("Turkey", city.country)
    }

    @Test
    fun `reverseGeocode parses city and town as county (Istanbul)`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                        "lat": "41.0082",
                        "lon": "28.9784",
                        "display_name": "Fatih, İstanbul, Türkiye",
                        "address": {
                            "road": "Alemdar Caddesi",
                            "suburb": "Cankurtaran Mahallesi",
                            "city": "İstanbul",
                            "town": "Fatih",
                            "province": "İstanbul",
                            "ISO3166-2-lvl4": "TR-34",
                            "country": "Türkiye",
                            "country_code": "tr"
                        }
                    }
                    """.trimIndent()
                )
        )

        val city = dataSource.reverseGeocode(41.0082, 28.9784)

        assertNotNull(city)
        assertEquals("İstanbul", city!!.name)
        assertEquals("İstanbul", city.city)
        assertEquals("Fatih", city.county)
        assertEquals("Turkey", city.country)
    }

    @Test
    fun `searchCities parses province as city and town as county`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                        {
                            "lat": "41.0364",
                            "lon": "28.9603",
                            "display_name": "Fatih, İstanbul, Türkiye",
                            "address": {
                                "town": "Fatih",
                                "province": "İstanbul",
                                "ISO3166-2-lvl4": "TR-34",
                                "country": "Türkiye",
                                "country_code": "tr"
                            }
                        }
                    ]
                    """.trimIndent()
                )
        )

        val cities = dataSource.searchCities("Fatih")

        assertEquals(1, cities.size)
        val city = cities.first()
        assertEquals("İstanbul", city.name)
        assertEquals("İstanbul", city.city)
        assertEquals("Fatih", city.county)
        assertEquals("Turkey", city.country)
    }
}
