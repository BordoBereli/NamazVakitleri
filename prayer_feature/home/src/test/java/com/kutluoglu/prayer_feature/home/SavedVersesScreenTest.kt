package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SavedVersesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun verse(numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo(
            englishName = "Al-Fatihah",
            name = "الفاتحة",
            number = 1,
            numberOfAyahs = 7
        ),
        numberInSurah = numberInSurah
    )

    @Test
    fun `renders empty state when no saved verses`() {
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(emptyList()),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No saved verses yet. Bookmark a verse from the home screen.")
            .assertIsDisplayed()
    }

    @Test
    fun `renders saved verses list`() {
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(listOf(verse(1), verse(2))),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Text 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Text 2").assertIsDisplayed()
    }

    @Test
    fun `tapping a verse fires OnSelect`() {
        val events = mutableListOf<SavedVersesEvent>()
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(listOf(verse(1))),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = { events.add(it) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Text 1").performClick()
        composeTestRule.waitForIdle()
        assertThat(events).contains(SavedVersesEvent.OnSelect(verse(1)))
    }

    @Test
    fun `swiping a verse left fires OnRemove`() {
        val events = mutableListOf<SavedVersesEvent>()
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(listOf(verse(1))),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = { events.add(it) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Text 1").performTouchInput { swipeLeft() }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        assertThat(events).contains(SavedVersesEvent.OnRemove(verse(1)))
    }

    @Test
    fun `detail sheet shows the selected verse`() {
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(
                    verses = listOf(verse(1)),
                    selectedVerse = verse(1),
                    isDetailVisible = true
                ),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Text 1").assertCountEquals(2)
        composeTestRule.onNodeWithContentDescription("Unsave Verse").assertIsDisplayed()
    }

    @Test
    fun `unsaving from the detail sheet fires OnRemove`() {
        val events = mutableListOf<SavedVersesEvent>()
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(
                    verses = listOf(verse(1)),
                    selectedVerse = verse(1),
                    isDetailVisible = true
                ),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = { events.add(it) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Unsave Verse")
            // performClick() is swallowed by CustomBottomSheet's drag gestures under Robolectric,
            // so invoke the semantics OnClick action directly (same as HomeScreenTest).
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        composeTestRule.waitForIdle()
        assertThat(events).contains(SavedVersesEvent.OnRemove(verse(1)))
    }

    @Test
    fun `dragging the reorder handle fires OnReorder`() {
        val events = mutableListOf<SavedVersesEvent>()
        composeTestRule.setContent {
            SavedVersesScreen(
                state = SavedVersesUiState.Success(listOf(verse(1), verse(2))),
                verseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onNavigateBack = {},
                onEvent = { events.add(it) }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithContentDescription("Reorder")[0].performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, 120f))
            up()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        assertThat(events.any { it is SavedVersesEvent.OnReorder }).isTrue()
    }
}
