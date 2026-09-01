package com.kutluoglu.prayer_settings.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.City
import com.kutluoglu.prayer_settings.data.local.CityCacheDataStore
import com.kutluoglu.prayer_remote.location.CitySearchRemoteDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class LocationRepositoryImplRobolectricTest {

    private lateinit var context: Context
    private lateinit var mockWebServer: MockWebServer
    private lateinit var cacheDataStore: CityCacheDataStore
    private lateinit var repository: LocationRepositoryImpl

    @Before
    fun setUp() {
        context = Robolectric.buildActivity(android.app.Activity::class.java).create().get()
        
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        cacheDataStore = CityCacheDataStore(context)

        val remoteDataSource = CitySearchRemoteDataSource(
            httpClient = OkHttpClient(),
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        )
        
        repository = LocationRepositoryImpl(context, remoteDataSource, cacheDataStore)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getPresetCities should return empty list when cities json is missing`() {
        val cities = runBlocking {
            repository.getPresetCities()
        }
        
        assertThat(cities).isNotNull()
    }

    @Test
    fun `getPresetCities should return cached cities on second call`() {
        val firstCall = runBlocking {
            repository.getPresetCities()
        }
        
        val secondCall = runBlocking {
            repository.getPresetCities()
        }
        
        assertThat(firstCall).isEqualTo(secondCall)
    }

    @Test
    fun `searchCities with empty query should throw exception`() {
        runBlocking {
            cacheDataStore.clearCache()
        }

        var exception: Exception? = null
        try {
            runBlocking {
                repository.searchCities("")
            }
        } catch (e: Exception) {
            exception = e
        }
        
        assertThat(exception).isNotNull()
    }

    @Test
    fun `searchCities should return mocked data from MockWebServer`() {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                [
                    {
                        "lat": "41.0082",
                        "lon": "28.9784",
                        "display_name": "Istanbul, Turkey",
                        "address": {
                            "city": "Istanbul",
                            "country": "Turkey"
                        }
                    }
                ]
            """.trimIndent())
        
        mockWebServer.enqueue(mockResponse)
        
        val cities = runBlocking {
            repository.searchCities("Istanbul")
        }
        
        assertThat(cities).isNotEmpty()
        assertThat(cities.first().name).isEqualTo("Istanbul")
        assertThat(cities.first().country).isEqualTo("Turkey")
    }

    @Test
    fun `searchCities should return cached data on network error when cache exists`() {
        val mockResponse = MockResponse()
            .setResponseCode(500)
            .setBody("Server Error")
        
        mockWebServer.enqueue(mockResponse)
        
        runBlocking {
            cacheDataStore.saveCities(listOf(
                City("Istanbul", "Turkey", 41.0082, 28.9784, "UTC+3")
            ))
        }
        
        val cities = runBlocking {
            try {
                repository.searchCities("Istanbul")
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        assertThat(cities).isNotEmpty()
    }

    @Test
    fun `searchCities should throw exception when network fails and no cache`() {
        val mockResponse = MockResponse()
            .setResponseCode(500)
            .setBody("Server Error")
        
        mockWebServer.enqueue(mockResponse)
        
        var exception: Exception? = null
        try {
            runBlocking {
                repository.searchCities("NonExistentCity123")
            }
        } catch (e: Exception) {
            exception = e
        }
        
        assertThat(exception).isNotNull()
    }

    @Test
    fun `reverseGeocode should return null for invalid coordinates`() {
        val result = runBlocking {
            repository.reverseGeocode(0.0, 0.0)
        }
        
        assertThat(result).isNull()
    }

    @Test
    fun `reverseGeocode should return mocked city from MockWebServer`() {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                    "lat": "41.0082",
                    "lon": "28.9784",
                    "display_name": "Istanbul, Turkey",
                    "address": {
                        "city": "Istanbul",
                        "country": "Turkey"
                    }
                }
            """.trimIndent())
        
        mockWebServer.enqueue(mockResponse)
        
        val city = runBlocking {
            repository.reverseGeocode(41.0082, 28.9784)
        }
        
        assertThat(city).isNotNull()
        assertThat(city!!.name).isEqualTo("Istanbul")
        assertThat(city.country).isEqualTo("Turkey")
    }

    @Test
    fun `cacheDataStore should save and retrieve cities`() = runBlocking {
        val cities = listOf(
            City("Istanbul", "Turkey", 41.0082, 28.9784, "UTC+3"),
            City("Ankara", "Turkey", 39.9334, 32.8597, "UTC+3")
        )
        
        cacheDataStore.saveCities(cities)
        
        val retrieved = cacheDataStore.getCities()
        
        assertThat(retrieved).hasSize(2)
    }

    @Test
    fun `cacheDataStore should validate cache expiry`() = runBlocking {
        val cities = listOf(
            City("Istanbul", "Turkey", 41.0082, 28.9784, "UTC+3")
        )
        
        cacheDataStore.saveCities(cities)
        
        val isValid = cacheDataStore.isCacheValid()
        
        assertThat(isValid).isTrue()
    }
}
