package com.kutluoglu.prayer_feature.prayertimes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.prayer.DailyPrayer
import com.kutluoglu.prayer.model.prayer.Prayer
import com.kutluoglu.prayer_feature.common.states.LocationUiState
import com.kutluoglu.prayer_feature.common.states.TimeUiState
import com.kutluoglu.prayer_feature.prayertimes.components.PrayerContainer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.junit.Rule
import org.junit.Test

class PrayerHeaderSmallScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val prayers = listOf(
        Prayer(name = "İmsak", arabicName = "الفجر", time = LocalTime(5, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Güneş", arabicName = "الشروق", time = LocalTime(7, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Öğle", arabicName = "الظهر", time = LocalTime(12, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "İkindi", arabicName = "العصر", time = LocalTime(15, 30), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Akşam", arabicName = "المغرب", time = LocalTime(18, 0), date = LocalDate(2026, 8, 1)),
        Prayer(name = "Yatsı", arabicName = "العشاء", time = LocalTime(19, 30), date = LocalDate(2026, 8, 1))
    )

    private val successState = PrayerTimesUiState.Success(
        monthlyPrayers = listOf(
            DailyPrayer(
                dayOfMonth = 1,
                gregorianDate = "1 August",
                hijriDate = "1 Muharram 1448",
                prayers = prayers
            )
        ),
        currentDayOfMonth = 1,
        selectedMonth = YearMonth(2026, 8),
        isCurrentMonth = true,
        timeState = TimeUiState(gregorianShortDate = "August 2026"),
        locationState = LocationUiState(
            locationData = LocationData(41.0082, 28.9784, "Turkey", "TR", "Istanbul", null),
            locationInfoText = "Istanbul, TR"
        )
    )

    @Test
    fun lastPrayerNameStaysOnSingleLineOnSmallScreen() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(600.dp)
            ) {
                PrayerContainer(successState) {}
            }
        }

        composeRule.waitForIdle()

        val node = composeRule.onNodeWithText("Yatsı").fetchSemanticsNode()
        val heightDp = with(composeRule.density) { node.size.height.toFloat().toDp() }
        assertThat(heightDp).isLessThan(30.dp)
    }

    @Test
    fun prayerTimesStayOnSingleLineOnSmallScreen() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(600.dp)
            ) {
                PrayerContainer(successState) {}
            }
        }

        composeRule.waitForIdle()

        prayers.forEach { prayer ->
            val node = composeRule.onNodeWithText(prayer.time.toString()).fetchSemanticsNode()
            val heightDp = with(composeRule.density) { node.size.height.toFloat().toDp() }
            assertWithMessage("${prayer.time} should stay on a single line")
                .that(heightDp)
                .isLessThan(30.dp)
        }
    }

    @Test
    fun dateInfoStaysOnSingleLineOnSmallScreen() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(600.dp)
            ) {
                PrayerContainer(successState) {}
            }
        }

        composeRule.waitForIdle()

        listOf("August", "1 Muharram 1448").forEach { text ->
            val node = composeRule.onNodeWithText(text).fetchSemanticsNode()
            val heightDp = with(composeRule.density) { node.size.height.toFloat().toDp() }
            assertWithMessage("'$text' should stay on a single line")
                .that(heightDp)
                .isLessThan(30.dp)
        }
    }

    @Test
    fun dateInfoAlignsWeekdayLeftAndHijriRight() {
        composeRule.setContent {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(600.dp)
            ) {
                PrayerContainer(successState) {}
            }
        }

        composeRule.waitForIdle()

        val rootRight = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.right
        val weekdayLeft = composeRule.onNodeWithText("August").fetchSemanticsNode().boundsInRoot.left
        val hijriRight = composeRule.onNodeWithText("1 Muharram 1448").fetchSemanticsNode().boundsInRoot.right

        assertWithMessage("weekday left edge $weekdayLeft should be on the left half")
            .that(weekdayLeft)
            .isLessThan(rootRight / 2f)
        assertWithMessage("hijri right edge $hijriRight should be near the right edge $rootRight")
            .that(hijriRight)
            .isGreaterThan(rootRight * 0.8f)
    }

    @Test
    fun prayerTimesFitInColumnsOnSmallScreen() {
        composeRule.setContent {
            Column {
                prayers.forEach { prayer ->
                    Text(
                        text = prayer.time.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("natural_time_${prayer.time}")
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val slotWidthDp = 48.dp
        prayers.forEach { prayer ->
            val naturalWidthDp = with(composeRule.density) {
                composeRule.onNodeWithTag("natural_time_${prayer.time}")
                    .fetchSemanticsNode().size.width.toFloat().toDp()
            }
            assertWithMessage("${prayer.time} natural width $naturalWidthDp should fit in $slotWidthDp")
                .that(naturalWidthDp)
                .isAtMost(slotWidthDp)
        }
    }

    @Test
    fun longestPrayerNameFitsInColumnOnSmallScreen() {
        composeRule.setContent {
            Column {
                prayers.forEach { prayer ->
                    Text(
                        text = prayer.name,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.testTag("natural_${prayer.name}")
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val columnContentWidthDp = 44.dp
        val widths = prayers.associate { prayer ->
            val naturalWidthDp = with(composeRule.density) {
                composeRule.onNodeWithTag("natural_${prayer.name}")
                    .fetchSemanticsNode().size.width.toFloat().toDp()
            }
            prayer.name to naturalWidthDp
        }
        val maxWidth = widths.values.max()
        assertWithMessage("Natural widths: $widths")
            .that(maxWidth)
            .isAtMost(columnContentWidthDp)
    }
}
