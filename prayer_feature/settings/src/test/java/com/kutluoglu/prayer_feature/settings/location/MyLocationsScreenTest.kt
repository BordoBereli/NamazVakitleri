package com.kutluoglu.prayer_feature.settings.location

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.LocationsState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MyLocationsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val coordinator = mockk<LocationsCoordinator>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

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

    private val gps = LocationEntry(
        id = "gps",
        location = LocationData(41.0, 29.0, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey",
        isAutoGps = true
    )

    private fun setState(entries: List<LocationEntry>, gpsEnabled: Boolean = false, selectedId: String? = null) {
        coEvery { coordinator.observeState() } returns MutableStateFlow(
            LocationsState(entries = entries, gpsEnabled = gpsEnabled, selectedId = selectedId)
        )
    }

    private fun launchScreen() {
        val viewModel = MyLocationsViewModel(coordinator, analyticsTracker)
        composeTestRule.setContent {
            MyLocationsRoute(
                onNavigateBack = {},
                onAddLocation = {},
                viewModel = viewModel
            )
        }
    }

    @Test
    fun `renders empty state with location icon and message`() {
        setState(emptyList())
        launchScreen()

        composeTestRule.onNodeWithText("No location selected. Please choose a location to see prayer times.")
            .assertIsDisplayed()
    }

    @Test
    fun `renders manual locations with delete and drag handle`() {
        setState(listOf(istanbul, ankara), selectedId = "loc-1")
        launchScreen()

        composeTestRule.onNodeWithText("Istanbul, Turkey").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ankara, Turkey").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("Delete").assertCountEquals(2)
        composeTestRule.onAllNodesWithContentDescription("Reorder").assertCountEquals(2)
    }

    @Test
    fun `renders gps entry without delete or drag handle`() {
        setState(listOf(gps, istanbul), gpsEnabled = true, selectedId = "gps")
        launchScreen()

        composeTestRule.onAllNodesWithContentDescription("Delete").assertCountEquals(1)
        composeTestRule.onAllNodesWithContentDescription("Reorder").assertCountEquals(1)
    }

    @Test
    fun `drag reorder persists new order to coordinator`() {
        setState(listOf(istanbul, ankara), selectedId = "loc-1")
        launchScreen()

        composeTestRule.onAllNodesWithContentDescription("Reorder")[0]
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveTo(Offset(centerX, centerY + 60f))
                moveTo(Offset(centerX, centerY + 120f))
                moveTo(Offset(centerX, centerY + 180f))
                advanceEventTime(100)
                up()
            }

        composeTestRule.waitForIdle()
        coVerify { coordinator.reorderLocations(listOf("loc-2", "loc-1")) }
    }

    @Test
    fun `toggle is off when location permission is missing even if gps is enabled`() {
        shadowOf(composeTestRule.activity).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        setState(listOf(istanbul), gpsEnabled = true, selectedId = "loc-1")
        launchScreen()

        composeTestRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun `toggling on with permission granted enables gps`() {
        shadowOf(composeTestRule.activity).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        setState(listOf(istanbul), gpsEnabled = false, selectedId = "loc-1")
        launchScreen()

        composeTestRule.onNode(isToggleable()).performClick()

        composeTestRule.waitForIdle()
        coVerify { coordinator.setGpsEnabled(true) }
    }

    @Test
    fun `toggling on without permission requests permission and does not enable gps`() {
        shadowOf(composeTestRule.activity).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        setState(listOf(istanbul), gpsEnabled = false, selectedId = "loc-1")
        launchScreen()

        composeTestRule.onNode(isToggleable()).performClick()

        composeTestRule.waitForIdle()
        coVerify(exactly = 0) { coordinator.setGpsEnabled(any()) }
        assertThat(shadowOf(composeTestRule.activity).lastRequestedPermission.requestedPermissions)
            .asList()
            .contains(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
