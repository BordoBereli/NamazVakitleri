package com.kutluoglu.prayer_settings.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.CityList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class PresetCitiesLocalizationTest {

    private lateinit var context: Context
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        context = Robolectric.buildActivity(android.app.Activity::class.java).create().get()
    }

    private fun loadCities(): CityList {
        val input = context.assets.open("cities.json")
        return json.decodeFromString<CityList>(input.bufferedReader().use { it.readText() })
    }

    @Test
    fun `every city has localized country fields`() {
        val cities = loadCities().cities
        assertThat(cities).isNotEmpty()
        cities.forEach { city ->
            assertThat(city.countryTr).isNotNull()
            assertThat(city.countryAr).isNotNull()
            assertThat(city.countryFa).isNotNull()
        }
    }

    @Test
    fun `every Turkey city has localized province fields`() {
        val cities = loadCities().cities.filter { it.country == "Turkey" }
        assertThat(cities).isNotEmpty()
        cities.forEach { city ->
            assertThat(city.cityTr).isNotNull()
            assertThat(city.cityAr).isNotNull()
            assertThat(city.cityFa).isNotNull()
        }
    }

    @Test
    fun `every non-Turkey city has localized name fields`() {
        val cities = loadCities().cities.filter { it.country != "Turkey" }
        assertThat(cities).isNotEmpty()
        cities.forEach { city ->
            assertThat(city.nameTr).isNotNull()
            assertThat(city.nameAr).isNotNull()
            assertThat(city.nameFa).isNotNull()
        }
    }

    @Test
    fun `Istanbul has Turkish province name`() {
        val istanbul = loadCities().cities.first { it.city == "Istanbul" && it.country == "Turkey" }
        assertThat(istanbul.cityTr).isEqualTo("İstanbul")
    }

    @Test
    fun `Turkey province centers have localized name fields`() {
        val centers = loadCities().cities.filter { it.country == "Turkey" && it.city == it.name }
        assertThat(centers).isNotEmpty()
        centers.forEach { city ->
            assertThat(city.nameTr).isNotNull()
            assertThat(city.nameAr).isNotNull()
            assertThat(city.nameFa).isNotNull()
        }
    }
}
