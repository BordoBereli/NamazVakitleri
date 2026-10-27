package com.kutluoglu.prayer_feature.prayertimes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText

import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.states.TimeUiState
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerTimesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val selectedMonth = YearMonth(2026, 8)

    private val prayers = listOf(
        Prayer(name = "Imsak", arabicName = "الإمساك", time = LocalTime(5, 0), date = LocalDate(2026, 8, 1), isImsak = true),
        Prayer(name = "Sunrise", arabicName = "الشروق", time = LocalTime(7, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Dhuhr", arabicName = "الظهر", time = LocalTime(12, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Asr", arabicName = "العصر", time = LocalTime(15, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Maghrib", arabicName = "المغرب", time = LocalTime(18, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Isha", arabicName = "العشاء", time = LocalTime(19, 30), date = LocalDate(2026, 8, 1))
    )

    private val successState = PrayerTimesUiState.Success(
        monthlyPrayers = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 Monday",
                hijriDate = "1 Muharram 1448",
                prayers = prayers
            )
        ),
        currentDayOfMonth = 1,
        selectedMonth = selectedMonth,
        isCurrentMonth = true,
        timeState = TimeUiState(gregorianShortDate = "August 2026"),
        locationState = LocationUiState(
            locationData = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
            locationInfoText = "Istanbul, TR"
        )
    )

    @Test
    fun `renders prayer names and month header for success state`() {
        composeTestRule.setContent {
            PayerTimesScreen(
                uiState = successState,
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("month_header").assertIsDisplayed()
        composeTestRule.onNodeWithText("Imsak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dhuhr").assertIsDisplayed()
        composeTestRule.onNodeWithText("Isha").assertIsDisplayed()
    }

    @Test
    fun `prayer names are not clipped at large font scale`() {
        withFontScale(2f) {
            composeTestRule.onNodeWithText("Imsak").assertTextNotClipped()
            composeTestRule.onNodeWithText("Sunrise").assertTextNotClipped()
            composeTestRule.onNodeWithText("Dhuhr").assertTextNotClipped()
            composeTestRule.onNodeWithText("Asr").assertTextNotClipped()
            composeTestRule.onNodeWithText("Maghrib").assertTextNotClipped()
            composeTestRule.onNodeWithText("Isha").assertTextNotClipped()
        }
    }

    @Test
    fun `prayer times are not clipped at large font scale`() {
        withFontScale(2f) {
            composeTestRule.onNodeWithText("05:00").assertTextNotClipped()
            composeTestRule.onNodeWithText("07:00").assertTextNotClipped()
            composeTestRule.onNodeWithText("12:30").assertTextNotClipped()
            composeTestRule.onNodeWithText("15:30").assertTextNotClipped()
            composeTestRule.onNodeWithText("18:00").assertTextNotClipped()
            composeTestRule.onNodeWithText("19:30").assertTextNotClipped()
        }
    }

    private fun withFontScale(fontScale: Float, block: () -> Unit) {
        composeTestRule.setContent {
            val density = Density(
                density = LocalDensity.current.density,
                fontScale = fontScale
            )
            CompositionLocalProvider(LocalDensity provides density) {
                PayerTimesScreen(
                    uiState = successState,
                    onEvent = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        block()
    }

    private fun SemanticsNodeInteraction.assertTextNotClipped(): SemanticsNodeInteraction {
        val node = fetchSemanticsNode()
        val getLayoutResultAction = node.config[SemanticsActions.GetTextLayoutResult]
            .action ?: error("Node does not expose a text layout result")
        val layoutResult = mutableListOf<TextLayoutResult>()
        @Suppress("UNCHECKED_CAST")
        getLayoutResultAction.invoke(layoutResult as MutableList<TextLayoutResult>)
        val result = layoutResult.lastOrNull()
            ?: error("Text layout result was not provided by the node")
        assertThat(result.didOverflowWidth).isFalse()
        return this
    }
}
