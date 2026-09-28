package com.kutluoglu.prayer_feature.settings.location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.City
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(value = ExecutionMode.SAME_THREAD)
class SearchAnalyticsMapperTest {

    private fun city(name: String, country: String, city: String? = null, county: String? = null) =
        City(name, country, 0.0, 0.0, "UTC", city, county)

    @Test
    fun `queryLengthBucket returns 2-3 for short queries`() {
        assertThat(SearchAnalyticsMapper.queryLengthBucket("ab")).isEqualTo("2-3")
        assertThat(SearchAnalyticsMapper.queryLengthBucket("abc")).isEqualTo("2-3")
    }

    @Test
    fun `queryLengthBucket returns 4-6 for medium queries`() {
        assertThat(SearchAnalyticsMapper.queryLengthBucket("abcd")).isEqualTo("4-6")
        assertThat(SearchAnalyticsMapper.queryLengthBucket("berlin")).isEqualTo("4-6")
    }

    @Test
    fun `queryLengthBucket returns 7-10 for long queries`() {
        assertThat(SearchAnalyticsMapper.queryLengthBucket("istanbul")).isEqualTo("7-10")
        assertThat(SearchAnalyticsMapper.queryLengthBucket("0123456789")).isEqualTo("7-10")
    }

    @Test
    fun `queryLengthBucket returns 11+ for very long queries`() {
        assertThat(SearchAnalyticsMapper.queryLengthBucket("kadikoy mosque")).isEqualTo("11+")
    }

    @Test
    fun `topResultCountry returns country of first result`() {
        val results = listOf(city("Berlin", "Germany"), city("Munich", "Germany"))
        assertThat(SearchAnalyticsMapper.topResultCountry(results)).isEqualTo("Germany")
    }

    @Test
    fun `topResultCountry returns none for empty results`() {
        assertThat(SearchAnalyticsMapper.topResultCountry(emptyList())).isEqualTo("none")
    }

    @Test
    fun `topResultAdminLevel returns district when county present`() {
        val results = listOf(city("Fatih", "Turkey", city = "Istanbul", county = "Fatih"))
        assertThat(SearchAnalyticsMapper.topResultAdminLevel(results)).isEqualTo("district")
    }

    @Test
    fun `topResultAdminLevel returns city when city present without county`() {
        val results = listOf(city("Berlin", "Germany", city = "Berlin"))
        assertThat(SearchAnalyticsMapper.topResultAdminLevel(results)).isEqualTo("city")
    }

    @Test
    fun `topResultAdminLevel returns locality when neither city nor county present`() {
        val results = listOf(city("Berlin", "Germany"))
        assertThat(SearchAnalyticsMapper.topResultAdminLevel(results)).isEqualTo("locality")
    }

    @Test
    fun `topResultAdminLevel returns none for empty results`() {
        assertThat(SearchAnalyticsMapper.topResultAdminLevel(emptyList())).isEqualTo("none")
    }
}
