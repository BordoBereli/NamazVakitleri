package com.kutluoglu.namazvakitleri.location

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.namazvakitleri.AppModule
import com.kutluoglu.prayer.domain.SurfaceLocationSource
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer_location.LocationsCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.ksp.generated.module

class SurfaceLocationSourceAdapterTest {

    private val locationsCoordinator = mockk<LocationsCoordinator>(relaxed = true)

    @Test
    fun `delegates to locations coordinator`() = runTest {
        val location = LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", "Fatih")
        coEvery { locationsCoordinator.resolveSelected() } returns location

        val adapter = SurfaceLocationSourceAdapter(locationsCoordinator)
        val result = adapter.resolveSelected()

        assertThat(result).isEqualTo(location)
        coVerify { locationsCoordinator.resolveSelected() }
    }

    @Test
    fun `returns null when coordinator has no location`() = runTest {
        coEvery { locationsCoordinator.resolveSelected() } returns null

        val adapter = SurfaceLocationSourceAdapter(locationsCoordinator)

        assertThat(adapter.resolveSelected()).isNull()
    }

    @Test
    fun `SurfaceLocationSource resolves from the app module`() {
        val koin = koinApplication {
            modules(
                AppModule.module,
                module {
                    single<LocationsCoordinator> { mockk(relaxed = true) }
                }
            )
        }.koin

        val source: SurfaceLocationSource = koin.get()

        assertThat(source).isInstanceOf(SurfaceLocationSourceAdapter::class.java)
    }
}
