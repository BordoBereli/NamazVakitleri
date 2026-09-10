package com.kutluoglu.prayer_feature.settings.juristic

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer_feature.settings.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h1400dp")
class ShadowLengthDiagramTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val context: ComponentActivity
        get() = composeTestRule.activity

    @Test
    fun `standard diagram shows angle and caption`() {
        composeTestRule.setContent {
            ShadowLengthDiagram(
                shadowFactor = 1f,
                angleLabel = "45°",
                caption = context.getString(R.string.juristic_shadow_equals_height)
            )
        }

        composeTestRule.onNodeWithText("45°").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            context.getString(R.string.juristic_shadow_equals_height)
        ).assertIsDisplayed()
    }

    @Test
    fun `hanafi diagram shows angle and caption`() {
        composeTestRule.setContent {
            ShadowLengthDiagram(
                shadowFactor = 2f,
                angleLabel = "26.5°",
                caption = context.getString(R.string.juristic_shadow_twice_height)
            )
        }

        composeTestRule.onNodeWithText("26.5°").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            context.getString(R.string.juristic_shadow_twice_height)
        ).assertIsDisplayed()
    }
}
