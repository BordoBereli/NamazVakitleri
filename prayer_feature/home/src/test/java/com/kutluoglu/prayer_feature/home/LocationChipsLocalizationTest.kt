package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_feature.home.components.LocationChipsRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocationChipsLocalizationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val istanbul = LocationEntry(
        id = "loc-1",
        location = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
        displayName = "Istanbul, Turkey",
        displayNameTr = "İstanbul, Türkiye",
        displayNameAr = "إسطنبول، تركيا"
    )

    @Test
    fun `chip renders localized name for turkish`() {
        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 1 })
            LocationChipsRow(
                entries = listOf(istanbul),
                selectedId = null,
                pagerState = pagerState,
                onLocationSelected = {},
                onAddLocation = {},
                languageCode = "tr"
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("İstanbul, Türkiye").assertIsDisplayed()
    }

    @Test
    fun `chip renders localized name for arabic`() {
        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 1 })
            LocationChipsRow(
                entries = listOf(istanbul),
                selectedId = null,
                pagerState = pagerState,
                onLocationSelected = {},
                onAddLocation = {},
                languageCode = "ar"
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("إسطنبول، تركيا").assertIsDisplayed()
    }

    @Test
    fun `chip renders english displayName as fallback`() {
        composeTestRule.setContent {
            val pagerState = rememberPagerState(pageCount = { 1 })
            LocationChipsRow(
                entries = listOf(istanbul),
                selectedId = null,
                pagerState = pagerState,
                onLocationSelected = {},
                onAddLocation = {},
                languageCode = "en"
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Istanbul, Turkey").assertIsDisplayed()
    }
}
