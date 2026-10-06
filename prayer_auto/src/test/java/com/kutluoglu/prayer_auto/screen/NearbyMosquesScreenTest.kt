package com.kutluoglu.prayer_auto.screen

import androidx.car.app.CarContext
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.Row
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer_auto.data.Mosque
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class NearbyMosquesScreenTest {

    private fun screenWith(mosques: List<Mosque>): NearbyMosquesScreen {
        val carContext = mockk<CarContext>(relaxed = true)
        every { carContext.getString(any()) } returns "Camiler"
        val screen = NearbyMosquesScreen(
            carContext,
            mockk<PrayerSurfaceDataProvider>(relaxed = true)
        )
        if (mosques.isNotEmpty()) {
            NearbyMosquesScreen::class.java
                .getDeclaredField("mosques")
                .apply { isAccessible = true }
                .set(screen, mosques)
        }
        return screen
    }

    private fun rows(screen: NearbyMosquesScreen): List<Row> {
        val template = screen.onGetTemplate()
        assertThat(template).isInstanceOf(PlaceListMapTemplate::class.java)
        val itemList = (template as PlaceListMapTemplate).itemList
        assertThat(itemList).isNotNull()
        return itemList!!.items.filterIsInstance<Row>()
    }

    @Test
    fun `empty state builds template with browsable retry row`() {
        val rows = rows(screenWith(emptyList()))

        assertThat(rows).hasSize(1)
        assertThat(rows[0].isBrowsable).isTrue()
    }

    @Test
    fun `mosque rows build template - requires DistanceSpan per PlaceListMapTemplate`() {
        val rows = rows(
            screenWith(
                listOf(
                    Mosque("Blue Mosque", 41.0054, 28.9768, 1.25f),
                    Mosque("Hagia Sophia", 41.0086, 28.9802, 0.4f),
                )
            )
        )

        assertThat(rows).hasSize(2)
        assertThat(rows.all { !it.isBrowsable }).isTrue()
    }
}
