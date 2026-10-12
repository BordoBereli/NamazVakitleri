package com.kutluoglu.prayer_feature.home.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.home.domain.RamadanDateResolver
import com.kutluoglu.prayer_feature.home.state.CountdownUiState
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.home.state.PrayerUiState
import com.kutluoglu.core.designsystem.states.TimeUiState
import io.mockk.mockk
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that the home-screen next-prayer header resolves the "Sabah" label
 * from locale-independent prayer data (isImsak flag / arabicName), not from
 * hardcoded localized display strings like "İmsak"/"Güneş".
 *
 * Runs with the Turkish qualifier so the string resources used by the
 * composable resolve in Turkish (Robolectric's default locale is en-US).
 * Note: the merged semantics tree trims trailing whitespace, so expected
 * strings omit the trailing space present in the raw resources.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "tr")
class NextPrayerInfoLocalizationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun stateWithNextPrayer(next: Prayer): HomeUiState.Success {
        val prayers = listOf(
            next.copy(time = LocalTime(4, 50), isImsak = true),
            Prayer("Sunrise", "الشروق", LocalTime(6, 0), LocalDate(2026, 10, 12)),
            Prayer("Dhuhr", "الظهر", LocalTime(13, 0), LocalDate(2026, 10, 12))
        )
        return HomeUiState.Success(
            timeState = TimeUiState(),
            prayerState = PrayerUiState(
                prayers = prayers,
                currentPrayer = prayers[0],
                nextPrayer = next
            ),
            locationState = LocationUiState(
                locationData = com.kutluoglu.prayer.model.location.LocationData(
                    41.0082, 28.9784, "Turkey", "TR", "Istanbul", null
                ),
                locationInfoText = "Istanbul, TR"
            ),
            countdownState = CountdownUiState(timeRemaining = "01:23:45")
        )
    }

    private fun setContent(state: HomeUiState.Success) {
        composeTestRule.setContent {
            HomeTopContainer(
                successState = state,
                painter = ColorPainter(Color.White),
                ramadanDateResolver = mockk<RamadanDateResolver>(relaxed = true)
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `next prayer with English Imsak name shows Sabah label`() {
        // English-locale name "Imsak" — the old code compared against "İmsak" only,
        // so this fell through to the generic label.
        setContent(
            stateWithNextPrayer(
                Prayer("Imsak", "الإمساك", LocalTime(4, 50), LocalDate(2026, 10, 12), isImsak = true)
            )
        )
        composeTestRule.onNodeWithText("Sabah namazına kalan süre:").assertIsDisplayed()
    }

    @Test
    fun `next prayer with Turkish İmsak name shows Sabah label`() {
        setContent(
            stateWithNextPrayer(
                Prayer("İmsak", "الإمساك", LocalTime(4, 50), LocalDate(2026, 10, 12), isImsak = true)
            )
        )
        composeTestRule.onNodeWithText("Sabah namazına kalan süre:").assertIsDisplayed()
    }

    @Test
    fun `next prayer with any localized Sunrise name shows Sabah label`() {
        // German-locale Sunrise name — the old code compared against "Güneş" only.
        setContent(stateWithNextPrayer(Prayer("Sonnenaufgang", "الشروق", LocalTime(6, 0), LocalDate(2026, 10, 12))))
        composeTestRule.onNodeWithText("Sabah namazı için kalan süre:").assertIsDisplayed()
    }

    @Test
    fun `next prayer with other prayer name keeps generic label`() {
        // Asr is neither Imsak nor Sunrise — must keep the generic "%s namazına kalan süre" text.
        setContent(stateWithNextPrayer(Prayer("İkindi", "العصر", LocalTime(16, 0), LocalDate(2026, 10, 12))))
        composeTestRule.onNodeWithText("İkindi namazına kalan süre:").assertIsDisplayed()
    }
}
