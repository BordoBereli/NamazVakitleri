package com.kutluoglu.prayer_location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ActiveLocationProviderTest {

    private val provider = ActiveLocationProvider()

    @Test
    fun `starts with null location`() = runBlocking<Unit> {
        assertThat(provider.location.first()).isNull()
    }

    @Test
    fun `set updates the emitted location`() = runBlocking<Unit> {
        val location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null)
        provider.set(location)
        assertThat(provider.location.first()).isEqualTo(location)
    }

    @Test
    fun `set null clears the location`() = runBlocking<Unit> {
        provider.set(LocationData(1.0, 2.0, "A", "AA", "C", null))
        provider.set(null)
        assertThat(provider.location.first()).isNull()
    }
}
