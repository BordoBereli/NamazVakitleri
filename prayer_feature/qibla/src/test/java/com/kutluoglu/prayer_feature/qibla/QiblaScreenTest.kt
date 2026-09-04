package com.kutluoglu.prayer_feature.qibla

import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QiblaScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val successState = QiblaUiState(
        qiblaBearing = 156.0,
        deviceAzimuth = 10f,
        qiblaAngle = 5f,
        sensorAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
        isLocationAvailable = true,
        error = null,
        locationName = "Istanbul, TR"
    )

    @Test
    fun `renders compass and bearing text on success`() {
        composeTestRule.setContent {
            QiblaScreen(
                uiState = successState,
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("156° North").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Qibla Direction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Istanbul, TR").assertIsDisplayed()
    }
}
