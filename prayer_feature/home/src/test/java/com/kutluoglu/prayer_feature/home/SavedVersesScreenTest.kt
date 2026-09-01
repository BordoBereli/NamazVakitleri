package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h1200dp")
class SavedVersesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val formatter = QuranVerseFormatter()

    private fun verse(surahNumber: Int, numberInSurah: Int) = AyahData(
        text = "Verse $surahNumber:$numberInSurah",
        surah = SurahInfo("Surah $surahNumber", "سورة", surahNumber, 10),
        numberInSurah = numberInSurah
    )

    private fun group(surahNumber: Int, vararg numbers: Int) = SavedVerseGroup(
        surah = verse(surahNumber, 1).surah,
        verses = numbers.map { verse(surahNumber, it) }
    )

    private fun headerNode(surahName: String, count: Int) =
        composeTestRule.onNode(hasText(surahName) and hasText(count.toString()))

    private fun setContent(state: SavedVersesUiState, onEvent: (SavedVersesEvent) -> Unit = {}) {
        composeTestRule.setContent {
            SavedVersesScreen(
                state = state,
                verseFormatter = formatter,
                onNavigateBack = {},
                onEvent = onEvent
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `renders group headers and verses`() {
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1, 2), group(36, 1)),
                filteredGroups = listOf(group(1, 1, 2), group(36, 1)),
                collapsedSurahs = emptySet()
            )
        )
        headerNode("Al-Fatihah", 2).assertIsDisplayed()
        headerNode("Ya-Sin", 1).assertIsDisplayed()
        composeTestRule.onNodeWithText("Verse 1:1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Verse 36:1").assertIsDisplayed()
    }

    @Test
    fun `collapsed group hides its verses`() {
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1, 2)),
                filteredGroups = listOf(group(1, 1, 2)),
                collapsedSurahs = setOf(1)
            )
        )
        headerNode("Al-Fatihah", 2).assertIsDisplayed()
        composeTestRule.onNodeWithText("Verse 1:1").assertDoesNotExist()
    }

    @Test
    fun `search filters the displayed groups`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1), group(36, 1)),
                filteredGroups = listOf(group(36, 1)),
                collapsedSurahs = emptySet(),
                query = "36"
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onNodeWithText("Ya-Sin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Al-Fatihah").assertDoesNotExist()
    }

    @Test
    fun `typing in search emits OnSearch`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = listOf(group(1, 1)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onNode(hasSetTextAction()).performTextInput("36")
        assertThat(lastEvent).isEqualTo(SavedVersesEvent.OnSearch("36"))
    }

    @Test
    fun `tapping a header emits OnToggleCollapse`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = listOf(group(1, 1)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        headerNode("Al-Fatihah", 1).performClick()
        assertThat(lastEvent).isEqualTo(SavedVersesEvent.OnToggleCollapse(1))
    }

    @Test
    fun `shows empty state when there are no saved verses`() {
        setContent(
            SavedVersesUiState.Success(
                groups = emptyList(),
                filteredGroups = emptyList(),
                collapsedSurahs = emptySet()
            )
        )
        composeTestRule.onNodeWithText("No saved verses yet. Bookmark a verse from the home screen.")
            .assertIsDisplayed()
    }

    @Test
    fun `shows no-matches state when search finds nothing`() {
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = emptyList(),
                collapsedSurahs = emptySet(),
                query = "zzz"
            )
        )
        composeTestRule.onNodeWithText("No saved verses match your search.").assertIsDisplayed()
    }

    @Test
    fun `swiping a verse right fires OnRemove`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = listOf(group(1, 1)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onNodeWithText("Verse 1:1").performTouchInput { swipeRight() }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        assertThat(lastEvent).isEqualTo(SavedVersesEvent.OnRemove(verse(1, 1)))
    }

    @Test
    fun `swiping a verse left does not fire OnRemove`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = listOf(group(1, 1)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onNodeWithText("Verse 1:1").performTouchInput { swipeLeft() }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        assertThat(lastEvent).isNotEqualTo(SavedVersesEvent.OnRemove(verse(1, 1)))
    }

    @Test
    fun `tapping a verse fires OnSelect`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1)),
                filteredGroups = listOf(group(1, 1)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onNodeWithText("Verse 1:1").performClick()
        assertThat(lastEvent).isEqualTo(SavedVersesEvent.OnSelect(verse(1, 1)))
    }

    @Test
    fun `dragging a header reorder handle fires OnReorderGroups`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1), group(36, 1)),
                filteredGroups = listOf(group(1, 1), group(36, 1)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onAllNodesWithContentDescription("Reorder")[0].performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, 120f))
            up()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        assertThat(lastEvent).isInstanceOf(SavedVersesEvent.OnReorderGroups::class.java)
    }

    @Test
    fun `dragging a verse reorder handle fires OnReorderWithinGroup`() {
        var lastEvent: SavedVersesEvent? = null
        setContent(
            SavedVersesUiState.Success(
                groups = listOf(group(1, 1, 2)),
                filteredGroups = listOf(group(1, 1, 2)),
                collapsedSurahs = emptySet()
            ),
            onEvent = { lastEvent = it }
        )
        composeTestRule.onAllNodesWithContentDescription("Reorder")[1].performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, 120f))
            up()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        assertThat(lastEvent).isInstanceOf(SavedVersesEvent.OnReorderWithinGroup::class.java)
    }

    companion object {
        private fun assertThat(actual: Any?) = com.google.common.truth.Truth.assertThat(actual)
    }
}
