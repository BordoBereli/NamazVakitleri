package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.kutluoglu.core.designsystem.prayerUtils.PrayerFormatter
import com.kutluoglu.core.designsystem.states.TimeUiState
import com.kutluoglu.prayer.domain.PrayerLogicEngine
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.states.LocationUiState
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
import java.io.File
import java.time.Duration
import java.time.ZoneId

/**
 * Verifies that the home pager renders a location's stored custom background
 * (via Coil's AsyncImage) when [LocationEntry.backgroundPath] points to an
 * existing file, and silently falls back to the default painter when the path
 * is missing or the file does not exist.
 *
 * The background image is identified by its content description, which
 * TopContainer sets from the same string resource as the default painter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationPagerBackgroundTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // R.string.home_page_fallback in the default (en) locale.
    private val backgroundDescription = "Mescid-i Nebevi"

    private val istanbul = LocationEntry(
        id = "loc-1",
        location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey"
    )

    private fun backgroundEntry(path: String?) = istanbul.copy(backgroundPath = path)

    private fun activeLocationPager(entry: LocationEntry) {
        composeTestRule.setContent {
            LocationPager(
                entries = listOf(entry),
                selectedId = entry.id,
                activeLocationId = entry.id,
                uiState = HomeUiState.Success(
                    timeState = TimeUiState(),
                    prayerState = PrayerUiState(
                        prayers = listOf(
                            Prayer("Imsak", "الإمساك", LocalTime(4, 50), LocalDate(2026, 10, 12), isImsak = true)
                        ),
                        currentPrayer = null,
                        nextPrayer = null
                    ),
                    locationState = LocationUiState(entry.location, entry.displayName)
                ),
                prayerDataByLocation = emptyMap(),
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onPrayerTimesClick = {},
                onAddLocation = {},
                onChooseLocation = {},
                onUseMyLocation = {},
                permissionDenied = false,
                onEvent = {},
                calculator = mockk<PrayerLogicEngine>(relaxed = true),
                formatter = mockk<PrayerFormatter>(relaxed = true),
                languageCode = "en"
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun previewLocationPager(entry: LocationEntry) {
        val calculator = mockk<PrayerLogicEngine>(relaxed = true)
        val formatter = mockk<PrayerFormatter>(relaxed = true)
        every { calculator.calculateTimeRemaining(any(), any()) } returns Duration.ofHours(2)
        every { formatter.formatTimeRemaining(any()) } returns "02:00:00"
        every { formatter.getFormattedCurrentTime(any()) } returns "10:30:00"

        val data = LoadedPrayerData(
            prayerState = PrayerUiState(
                nextPrayer = Prayer("Asr", "العصر", LocalTime(16, 0), LocalDate(2026, 10, 12))
            ),
            timeState = TimeUiState(),
            locationState = LocationUiState(entry.location, entry.displayName),
            zoneId = ZoneId.of("Europe/Istanbul")
        )

        composeTestRule.setContent {
            LocationPager(
                entries = listOf(entry),
                selectedId = entry.id,
                activeLocationId = "other-location",
                uiState = HomeUiState.Success(
                    locationState = LocationUiState(entry.location, entry.displayName)
                ),
                prayerDataByLocation = mapOf(entry.id to data),
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
    }

    @Test
    fun `active page renders stored background image when file exists`() {
        val file = File.createTempFile("background", ".jpg").apply { deleteOnExit() }
        activeLocationPager(backgroundEntry(file.absolutePath))
        composeTestRule.onNodeWithContentDescription(backgroundDescription).assertIsDisplayed()
        composeTestRule.onNodeWithTag("background_custom_image").assertIsDisplayed()
    }

    @Test
    fun `active page falls back to default painter when background file is missing`() {
        activeLocationPager(backgroundEntry("/data/user/0/missing/backgrounds/uuid.jpg"))
        composeTestRule.onNodeWithContentDescription(backgroundDescription).assertIsDisplayed()
        composeTestRule.onNodeWithTag("background_custom_image").assertDoesNotExist()
    }

    @Test
    fun `active page falls back to default painter when backgroundPath is null`() {
        activeLocationPager(backgroundEntry(null))
        composeTestRule.onNodeWithContentDescription(backgroundDescription).assertIsDisplayed()
        composeTestRule.onNodeWithTag("background_custom_image").assertDoesNotExist()
    }

    @Test
    fun `preview page renders stored background image when file exists`() {
        val file = File.createTempFile("background", ".jpg").apply { deleteOnExit() }
        previewLocationPager(backgroundEntry(file.absolutePath))
        composeTestRule.onNodeWithContentDescription(backgroundDescription).assertIsDisplayed()
        composeTestRule.onNodeWithTag("background_custom_image").assertIsDisplayed()
    }

    @Test
    fun `preview page falls back to default painter when background file is missing`() {
        previewLocationPager(backgroundEntry("/data/user/0/missing/backgrounds/uuid.jpg"))
        composeTestRule.onNodeWithContentDescription(backgroundDescription).assertIsDisplayed()
        composeTestRule.onNodeWithTag("background_custom_image").assertDoesNotExist()
    }
}
