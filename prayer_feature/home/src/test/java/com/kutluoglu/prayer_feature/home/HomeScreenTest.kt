package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import com.kutluoglu.prayer_location.data.LocationsState
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val istanbul = LocationEntry(
        id = "loc-1",
        location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey"
    )

    @Test
    fun `renders error state with retry`() {
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Error("Something went wrong"),
                locationsState = LocationsState(entries = listOf(istanbul), selectedId = "loc-1"),
                prayerDataByLocation = emptyMap(),
                activeLocationId = "loc-1",
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `renders empty state with add location and use my location`() {
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Empty,
                locationsState = LocationsState(),
                prayerDataByLocation = emptyMap(),
                activeLocationId = null,
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add location").assertIsDisplayed()
        composeTestRule.onNodeWithText("Use My Location").assertIsDisplayed()
    }
}
