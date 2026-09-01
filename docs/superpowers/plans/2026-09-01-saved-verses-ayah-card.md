# Saved Verses Ayah Card Distinction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the saved ayah card visually distinct from the surah header — elevated white card with a primary accent bar, a soft circle medallion showing the ayah number, centered larger text — and add a filled circle surah-index badge to the header.

**Architecture:** Pure presentation change confined to two private composables (`SurahHeader`, `VerseRow`) in `SavedVersesScreen.kt`. No data, state, event, or navigation changes. The surah header already has its elevated `surfaceContainerHigh` card treatment (in-progress work in the working tree); this plan adds the index badge to it and rebuilds the verse row.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose (Material3), Robolectric + Compose UI test for the screen.

**Note on working tree:** `SavedVersesScreen.kt` and `core/designsystem/.../Theme.kt` have uncommitted changes (surah header elevation + `surfaceContainerHigh` theme colors). This plan builds on top of that state — do not revert those changes.

---

## File Structure

- **Modify:** `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt`
  - `SurahHeader` (lines ~431-477): add filled circle surah-index badge between chevron and name.
  - `VerseRow` (lines ~479-528): rebuild as elevated white card with accent bar, medallion, centered text; remove `(Surah - n:m)` caption.
  - Call site of `VerseRow` (lines ~339-356): drop the now-unused `verseFormatter` argument.
- **Modify:** `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt`
  - Add two tests: surah index badge displayed, ayah number medallion displayed.

---

### Task 1: Surah index badge in SurahHeader

**Files:**
- Modify: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt`

- [ ] **Step 1: Add the failing test**

Add these imports to `SavedVersesScreenTest.kt` (alphabetical, after the existing `androidx.compose.ui.test.*` imports):

```kotlin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
```

Add this test method to the `SavedVersesScreenTest` class (after `renders group headers and verses`):

```kotlin
@Test
fun `surah header shows its index badge`() {
    setContent(
        SavedVersesUiState.Success(
            groups = listOf(group(1, 1)),
            filteredGroups = listOf(group(1, 1)),
            collapsedSurahs = emptySet()
        )
    )
    composeTestRule.onNodeWithTag("surah_index_1").assertIsDisplayed()
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.SavedVersesScreenTest.surah header shows its index badge"`

Expected: FAIL — no node with tag `surah_index_1` exists.

- [ ] **Step 3: Implement the badge**

In `SavedVersesScreen.kt`, add this import (alphabetical, with the other `androidx.compose.foundation.*` imports):

```kotlin
import androidx.compose.foundation.shape.CircleShape
```

and this import (with the other `androidx.compose.ui.platform.*` imports):

```kotlin
import androidx.compose.ui.platform.testTag
```

In `SurahHeader`, insert the badge between the chevron `Spacer` and the surah-name `Text`. Replace this block:

```kotlin
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = verseFormatter.getLocalizedNameOf(group.surah, context),
```

with:

```kotlin
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("surah_index_${group.surah.number}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${group.surah.number}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = verseFormatter.getLocalizedNameOf(group.surah, context),
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.SavedVersesScreenTest"`

Expected: PASS — the new test passes and all existing `SavedVersesScreenTest` tests still pass (the `headerNode` helper matches on name + count, unaffected by the new badge).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt
git commit -m "feat(home): show surah index badge in saved verses header"
```

---

### Task 2: Redesign VerseRow

**Files:**
- Modify: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt`

- [ ] **Step 1: Add the failing test**

Add this test method to `SavedVersesScreenTest` (after `surah header shows its index badge`):

```kotlin
@Test
fun `ayah card shows its ayah number medallion`() {
    setContent(
        SavedVersesUiState.Success(
            groups = listOf(group(1, 1)),
            filteredGroups = listOf(group(1, 1)),
            collapsedSurahs = emptySet()
        )
    )
    composeTestRule.onNodeWithTag("ayah_index_1_1").assertIsDisplayed()
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.SavedVersesScreenTest.ayah card shows its ayah number medallion"`

Expected: FAIL — no node with tag `ayah_index_1_1` exists.

- [ ] **Step 3: Implement the redesigned VerseRow**

In `SavedVersesScreen.kt`, add these imports (alphabetical):

```kotlin
import androidx.compose.ui.text.style.TextAlign
```

Remove this import (no longer used after the caption is dropped):

```kotlin
import androidx.compose.ui.text.style.TextOverflow
```

Replace the entire `VerseRow` composable (from `@Composable\nprivate fun VerseRow(` through the closing brace of the function) with:

```kotlin
@Composable
private fun VerseRow(
    verse: AyahData,
    isDragging: Boolean = false,
    onSelect: () -> Unit,
    onShare: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .width(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("ayah_index_${verse.surah.number}_${verse.numberInSurah}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${verse.numberInSurah}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_verse)
                        )
                    }
                    dragHandle?.invoke()
                }
                Text(
                    text = verse.text,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 4: Update the VerseRow call site**

In the `LazyColumn` (the `is SavedRow.Verse ->` branch), remove the now-unused `verseFormatter` argument. Replace:

```kotlin
                                                    VerseRow(
                                                        verse = row.verse,
                                                        verseFormatter = verseFormatter,
                                                        isDragging = isDragging,
```

with:

```kotlin
                                                    VerseRow(
                                                        verse = row.verse,
                                                        isDragging = isDragging,
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.SavedVersesScreenTest"`

Expected: PASS — the new medallion test passes and all existing tests still pass. The swipe/select/reorder tests target the ayah text node (`onNodeWithText("Verse 1:1")`), which is still present and centered; the container color/elevation change does not affect them.

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/SavedVersesScreen.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/SavedVersesScreenTest.kt
git commit -m "feat(home): redesign saved verse card with accent bar and ayah medallion"
```

---

### Task 3: Full regression

**Files:** none (verification only)

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew assembleDebug testDebugUnitTest`

Expected: BUILD SUCCESSFUL — all unit tests pass, including `SavedVersesScreenTest`, `SavedVersesEndToEndTest`, `SavedVersesScreenWipeTest`, and the home feature tests.

- [ ] **Step 2: Verify change scope with GitNexus**

Run: `gitnexus_detect_changes()`

Expected: only `SurahHeader` and `VerseRow` in `SavedVersesScreen.kt` are reported as changed symbols; no execution flows outside the Saved Verses screen are affected.

- [ ] **Step 3: Manual visual check (optional)**

Run the app (`./gradlew installDebug` or Android Studio) and open Saved Verses with at least one saved verse. Confirm:
- Surah header shows a filled circle with the surah number before the name.
- Ayah card is a white elevated card with a primary accent bar on the left, a soft circle medallion (ayah number) top-left, share + drag handle top-right, and centered ayah text.
- Tap, share, drag reorder, swipe-to-delete, search, and collapse all still work.

---

## Acceptance Criteria

- Surah header shows its surah number in a filled circle badge before the name.
- Ayah card is visually distinct from the header: elevated white card, primary accent bar, soft circle medallion with the ayah number, centered larger text.
- The `(Surah - n:m)` caption is gone; surah context comes from the header, ayah number from the medallion.
- All existing Saved Verses behavior (select, share, drag reorder, swipe-to-delete, search, collapse) is unchanged and all tests pass.
