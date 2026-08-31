package com.kutluoglu.prayer_feature.home

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.components.HomeEmptyContent
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import com.kutluoglu.prayer_location.data.LocationsState
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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

    @Test
    fun `renders loading indicator when entries empty and state is loading`() {
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Loading,
                locationsState = LocationsState(),
                prayerDataByLocation = emptyMap(),
                activeLocationId = null,
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add location").assertDoesNotExist()
        composeTestRule.onNodeWithText("Use My Location").assertDoesNotExist()
    }

    @Test
    fun `renders permission denied hint when permissionDenied is true`() {
        composeTestRule.setContent {
            HomeEmptyContent(
                onAddLocation = {},
                onUseMyLocation = {},
                permissionDenied = true
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Location permission is off. Use My Location will open settings.").assertIsDisplayed()
    }

    @Test
    fun `use my location with permission granted fires OnUseMyLocation`() {
        shadowOf(composeTestRule.activity).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val events = mutableListOf<HomeEvent>()
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Empty,
                locationsState = LocationsState(),
                prayerDataByLocation = emptyMap(),
                activeLocationId = null,
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = { events.add(it) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Use My Location").performClick()
        composeTestRule.waitForIdle()
        assertThat(events).contains(HomeEvent.OnUseMyLocation)
    }

    @Test
    fun `use my location without permission requests permission and does not fire OnUseMyLocation`() {
        shadowOf(composeTestRule.activity).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val events = mutableListOf<HomeEvent>()
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Empty,
                locationsState = LocationsState(),
                prayerDataByLocation = emptyMap(),
                activeLocationId = null,
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = { events.add(it) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Use My Location").performClick()
        composeTestRule.waitForIdle()
        assertThat(events).doesNotContain(HomeEvent.OnUseMyLocation)
        assertThat(shadowOf(composeTestRule.activity).lastRequestedPermission.requestedPermissions)
            .asList()
            .contains(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Test
    fun `use my location without permission then granting permission fires OnUseMyLocation`() {
        shadowOf(composeTestRule.activity).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val events = mutableListOf<HomeEvent>()
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Empty,
                locationsState = LocationsState(),
                prayerDataByLocation = emptyMap(),
                activeLocationId = null,
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = { events.add(it) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Use My Location").performClick()
        composeTestRule.waitForIdle()

        shadowOf(composeTestRule.activity).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()

        assertThat(events).contains(HomeEvent.OnUseMyLocation)
    }

    @Test
    fun `verse detail sheet bookmark toggles saved state`() {
        val verse = com.kutluoglu.prayer.model.quran.AyahData(
            text = "Bismillah",
            surah = com.kutluoglu.prayer.model.quran.SurahInfo(
                englishName = "Al-Fatihah",
                name = "الفاتحة",
                number = 1,
                numberOfAyahs = 7
            ),
            numberInSurah = 1
        )
        var toggled = false
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Success(
                    locationState = com.kutluoglu.prayer_feature.common.states.LocationUiState(
                        locationData = istanbul.location,
                        locationInfoText = "Istanbul, Turkey"
                    ),
                    quranVerse = verse,
                    isVerseDetailSheetVisible = true,
                    isVerseSaved = false
                ),
                locationsState = LocationsState(entries = listOf(istanbul), selectedId = "loc-1"),
                prayerDataByLocation = emptyMap(),
                activeLocationId = "loc-1",
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = { if (it == HomeEvent.OnToggleVerseSaved) toggled = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Save Verse")
            // performClick() is swallowed by CustomBottomSheet's drag gestures under Robolectric,
            // so invoke the semantics OnClick action directly.
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertThat(toggled).isTrue()
    }

    @Test
    fun `verse detail sheet shows unsave state when verse is saved`() {
        val verse = com.kutluoglu.prayer.model.quran.AyahData(
            text = "Bismillah",
            surah = com.kutluoglu.prayer.model.quran.SurahInfo(
                englishName = "Al-Fatihah",
                name = "الفاتحة",
                number = 1,
                numberOfAyahs = 7
            ),
            numberInSurah = 1
        )
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Success(
                    locationState = com.kutluoglu.prayer_feature.common.states.LocationUiState(
                        locationData = istanbul.location,
                        locationInfoText = "Istanbul, Turkey"
                    ),
                    quranVerse = verse,
                    isVerseDetailSheetVisible = true,
                    isVerseSaved = true
                ),
                locationsState = LocationsState(entries = listOf(istanbul), selectedId = "loc-1"),
                prayerDataByLocation = emptyMap(),
                activeLocationId = "loc-1",
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Unsave Verse").assertIsDisplayed()
    }
}
