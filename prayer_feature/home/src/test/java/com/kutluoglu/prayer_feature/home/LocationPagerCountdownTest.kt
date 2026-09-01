package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.prayerUtils.PrayerFormatter
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.domain.LoadedPrayerData
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationPagerCountdownTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val istanbul = LocationEntry(
        id = "loc-1",
        location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey"
    )
    private val ankara = LocationEntry(
        id = "loc-2",
        location = LocationData(39.9334, 32.8597, "Turkey", "TR", "Ankara", null),
        displayName = "Ankara, Turkey"
    )

    @Test
    fun `non-active location page shows a real countdown instead of placeholder`() {
        val calculator = mockk<PrayerLogicEngine>(relaxed = true)
        val formatter = mockk<PrayerFormatter>(relaxed = true)
        every { calculator.calculateTimeRemaining(any(), any()) } returns Duration.ofHours(2)
        every { formatter.formatTimeRemaining(any()) } returns "02:00:00"
        every { formatter.getFormattedCurrentTime(any()) } returns "10:30:00"

        val ankaraData = LoadedPrayerData(
            prayerState = PrayerUiState(
                nextPrayer = Prayer(
                    name = "Asr",
                    arabicName = "العصر",
                    time = LocalTime(16, 0),
                    date = LocalDate(2026, 8, 31)
                )
            ),
            timeState = TimeUiState(),
            locationState = LocationUiState(ankara.location, "Ankara, Turkey"),
            zoneId = ZoneId.of("Europe/Istanbul")
        )

        composeTestRule.setContent {
            LocationPager(
                entries = listOf(istanbul, ankara),
                selectedId = "loc-2",
                activeLocationId = "loc-1",
                uiState = HomeUiState.Success(
                    locationState = LocationUiState(istanbul.location, "Istanbul, Turkey")
                ),
                prayerDataByLocation = mapOf("loc-2" to ankaraData),
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onPrayerTimesClick = {},
                onAddLocation = {},
                onChooseLocation = {},
                onUseMyLocation = {},
                permissionDenied = false,
                onEvent = {},
                calculator = calculator,
                formatter = formatter,
                languageCode = "en"
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("02:00:00").assertIsDisplayed()
    }
}
