# Home Feature Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decompose the home feature's monoliths (`LocationChipsRow.kt` 241 lines, `HomeScreen.kt` 380 lines) into small single-purpose units, extract the pager/chips math into a testable pure object, and fix hot-path performance smells — with no behavior change.

**Architecture:** Extract pure geometry math into `ChipsSelectionGeometry` (TDD). Split `LocationChipsRow` into `LocationChip`/`AddLocationChip`/`SlidingChipIndicator` + a slim orchestrator. Extract `HomeResponsiveLayout` (shared Landscape/Portrait), `HomeErrorContent`, and `LocationPager` (pager + chips + scroll-settle + page content) out of `HomeScreen`, leaving it a thin wrapper. All ViewModel/domain/state code is untouched.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose (Material3, Foundation pager/lazy), JUnit 5, MockK, Truth, Gradle Kotlin DSL.

---

## File Structure

**New files (main):**
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/ChipsSelectionGeometry.kt` — pure geometry math + `ChipBounds`/`IndicatorBounds` data classes
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChip.kt` — single chip (text, GPS icon, color interpolation)
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/AddLocationChip.kt` — "+" chip
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/SlidingChipIndicator.kt` — animated indicator pill
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeErrorContent.kt` — error message + retry
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/layout/HomeResponsiveLayout.kt` — shared Landscape/Portrait dispatch
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt` — pager + chips + scroll-settle + page content

**New files (test):**
- `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/components/ChipsSelectionGeometryTest.kt`

**Modified files:**
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt` — slim to orchestration only
- `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt` — thin wrapper

**Unchanged:** `HomeViewModel`, `HomeEvent`, `HomeRoute`, `HomeGraph`, `PrayerCard`, `DailyPrayers`, `HomeTopContainer`, `BottomContainer`, all `state/` and `domain/` files.

---

### Task 1: Extract `ChipsSelectionGeometry` (pure logic, TDD)

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/ChipsSelectionGeometry.kt`
- Test: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/components/ChipsSelectionGeometryTest.kt`

- [ ] **Step 1: Write the failing test**

Create `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/components/ChipsSelectionGeometryTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.unit.Size
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ChipsSelectionGeometryTest {

    // --- selectionProgressFor ---

    @Test
    fun `current page has progress 1 when not swiping`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(2, 2, 0f)).isEqualTo(1f)
    }

    @Test
    fun `current page progress decreases as user swipes forward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(2, 2, 0.4f)).isEqualTo(0.6f)
    }

    @Test
    fun `current page progress decreases as user swipes backward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(2, 2, -0.3f)).isEqualTo(0.7f)
    }

    @Test
    fun `next page progress increases when swiping forward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(3, 2, 0.4f)).isEqualTo(0.4f)
    }

    @Test
    fun `next page has zero progress when swiping backward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(3, 2, -0.4f)).isEqualTo(0f)
    }

    @Test
    fun `previous page progress increases when swiping backward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(1, 2, -0.4f)).isEqualTo(0.4f)
    }

    @Test
    fun `previous page has zero progress when swiping forward`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(1, 2, 0.4f)).isEqualTo(0f)
    }

    @Test
    fun `non adjacent page always has zero progress`() {
        assertThat(ChipsSelectionGeometry.selectionProgressFor(5, 2, 0.4f)).isEqualTo(0f)
        assertThat(ChipsSelectionGeometry.selectionProgressFor(5, 2, -0.4f)).isEqualTo(0f)
    }

    // --- indicatorBoundsFor ---

    private val bounds = mapOf(
        0 to ChipBounds(offsetX = 0f, width = 100f, height = 28f),
        1 to ChipBounds(offsetX = 120f, width = 80f, height = 28f)
    )

    @Test
    fun `indicator sits on from chip when not swiping`() {
        val result = ChipsSelectionGeometry.indicatorBoundsFor(0, 0f, bounds, lastIndex = 1)
        assertThat(result).isEqualTo(IndicatorBounds(offsetX = 0f, width = 100f, height = 28f))
    }

    @Test
    fun `indicator interpolates between from and to chips`() {
        val result = ChipsSelectionGeometry.indicatorBoundsFor(0, 0.5f, bounds, lastIndex = 1)
        assertThat(result).isEqualTo(IndicatorBounds(offsetX = 60f, width = 90f, height = 28f))
    }

    @Test
    fun `indicator clamps to last chip when swiping past end`() {
        val result = ChipsSelectionGeometry.indicatorBoundsFor(1, 0.5f, bounds, lastIndex = 1)
        assertThat(result).isEqualTo(IndicatorBounds(offsetX = 120f, width = 80f, height = 28f))
    }

    @Test
    fun `indicator returns null when from chip has no bounds`() {
        val result = ChipsSelectionGeometry.indicatorBoundsFor(0, 0f, emptyMap(), lastIndex = 1)
        assertThat(result).isNull()
    }

    // --- chipScrollTargetFor ---

    @Test
    fun `chip scroll target returns null when chip not visible`() {
        val layoutInfo = mockk<LazyListLayoutInfo> {
            every { visibleItemsInfo } returns emptyList()
        }
        assertThat(ChipsSelectionGeometry.chipScrollTargetFor(0, layoutInfo)).isNull()
    }

    @Test
    fun `chip scroll target returns centering delta when chip visible`() {
        val item = mockk<LazyListItemInfo> {
            every { index } returns 0
            every { offset } returns 50
            every { size } returns 100
        }
        val layoutInfo = mockk<LazyListLayoutInfo> {
            every { visibleItemsInfo } returns listOf(item)
            every { viewportSize } returns Size(400f, 100f)
        }
        assertThat(ChipsSelectionGeometry.chipScrollTargetFor(0, layoutInfo)).isEqualTo(-100f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.components.ChipsSelectionGeometryTest"`
Expected: FAIL — `ChipsSelectionGeometry` unresolved (class does not exist yet).

- [ ] **Step 3: Write minimal implementation**

Create `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/ChipsSelectionGeometry.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import kotlin.math.abs

data class ChipBounds(
    val offsetX: Float,
    val width: Float,
    val height: Float
)

data class IndicatorBounds(
    val offsetX: Float,
    val width: Float,
    val height: Float
)

object ChipsSelectionGeometry {

    fun selectionProgressFor(
        index: Int,
        currentPage: Int,
        currentPageOffsetFraction: Float
    ): Float {
        val fraction = currentPageOffsetFraction
        val absFraction = abs(fraction)
        return when (index) {
            currentPage -> 1f - absFraction
            currentPage + 1 -> if (fraction > 0) fraction else 0f
            currentPage - 1 -> if (fraction < 0) absFraction else 0f
            else -> 0f
        }
    }

    fun indicatorBoundsFor(
        currentPage: Int,
        currentPageOffsetFraction: Float,
        chipBounds: Map<Int, ChipBounds>,
        lastIndex: Int
    ): IndicatorBounds? {
        val fromIndex = currentPage.coerceIn(0, lastIndex)
        val toIndex = if (currentPageOffsetFraction > 0) {
            (currentPage + 1).coerceIn(0, lastIndex)
        } else {
            (currentPage - 1).coerceIn(0, lastIndex)
        }
        val fraction = abs(currentPageOffsetFraction).coerceIn(0f, 1f)

        val fromBounds = chipBounds[fromIndex] ?: return null
        val toBounds = chipBounds[toIndex]

        val offsetX = if (toBounds != null) {
            fromBounds.offsetX + (toBounds.offsetX - fromBounds.offsetX) * fraction
        } else {
            fromBounds.offsetX
        }
        val width = if (toBounds != null) {
            fromBounds.width + (toBounds.width - fromBounds.width) * fraction
        } else {
            fromBounds.width
        }
        return IndicatorBounds(offsetX = offsetX, width = width, height = fromBounds.height)
    }

    fun chipScrollTargetFor(page: Int, layoutInfo: LazyListLayoutInfo): Float? {
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == page } ?: return null
        return item.offset + item.size / 2 - layoutInfo.viewportSize.width / 2
    }
}
```

- [ ] **Step 4: Remove the duplicate private `ChipBounds` from `LocationChipsRow.kt`**

`ChipBounds` is now defined publicly in `ChipsSelectionGeometry.kt` (same package). The original `LocationChipsRow.kt` still declares its own `private data class ChipBounds` (currently lines 47-51) — two top-level classes with the same name in the same package will not compile. Delete those lines from `LocationChipsRow.kt`:

```kotlin
private data class ChipBounds(
    val offsetX: Float,
    val width: Float,
    val height: Float
)
```

`LocationChipsRow.kt` continues to compile because its `onGloballyPositioned` callback now resolves `ChipBounds` to the public one in `ChipsSelectionGeometry.kt` (same package, no import needed).

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.components.ChipsSelectionGeometryTest"`
Expected: PASS (all 15 tests).

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/ChipsSelectionGeometry.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/components/ChipsSelectionGeometryTest.kt
git commit -m "refactor(home): extract ChipsSelectionGeometry pure logic with tests"
```

---

### Task 2: Extract `LocationChip` + `AddLocationChip`

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChip.kt`
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/AddLocationChip.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt` (remove the two private composables + their now-unused imports)

- [ ] **Step 1: Create `LocationChip.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
internal fun LocationChip(
    text: String,
    selectionProgress: Float,
    isGps: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (isGps) {
        MaterialTheme.colorScheme.primary
    } else {
        lerp(
            start = MaterialTheme.colorScheme.onSurfaceVariant,
            stop = MaterialTheme.colorScheme.primary,
            fraction = selectionProgress.coerceIn(0f, 1f)
        )
    }
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isGps) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = textColor
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}
```

- [ ] **Step 2: Create `AddLocationChip.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
internal fun AddLocationChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add location",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 3: Remove the private copies from `LocationChipsRow.kt`**

Delete the `LocationChip` composable (currently lines 182-222) and the `AddLocationChip` composable (currently lines 224-241) from `LocationChipsRow.kt`. Then remove these now-unused imports from `LocationChipsRow.kt`:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.lerp
```

Keep `import androidx.compose.ui.Alignment` (still used by the outer `Box`'s `align`).

- [ ] **Step 4: Compile to verify**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChip.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/AddLocationChip.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt
git commit -m "refactor(home): extract LocationChip and AddLocationChip components"
```

---

### Task 3: Extract `SlidingChipIndicator`

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/SlidingChipIndicator.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt` (replace inline indicator `Box` with the new composable)

- [ ] **Step 1: Create `SlidingChipIndicator.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
internal fun BoxScope.SlidingChipIndicator(
    offsetX: Float,
    width: Float,
    height: Float
) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset(x = offsetX.dp + 4.dp)
            .width((width - 8f).dp)
            .height((height - 8f).dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    )
}
```

- [ ] **Step 2: Replace the inline indicator in `LocationChipsRow.kt`**

In `LocationChipsRow.kt`, replace the inline indicator `Box` (currently lines 140-150):

```kotlin
        if (fromBounds != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffsetX.value.dp + 4.dp)
                    .width((indicatorWidth.value - 8f).dp)
                    .height((fromBounds.height - 8f).dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            )
        }
```

with:

```kotlin
        if (fromBounds != null) {
            SlidingChipIndicator(
                offsetX = indicatorOffsetX.value,
                width = indicatorWidth.value,
                height = fromBounds.height
            )
        }
```

Then remove these now-unused imports from `LocationChipsRow.kt`:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
```

- [ ] **Step 3: Compile to verify**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/SlidingChipIndicator.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt
git commit -m "refactor(home): extract SlidingChipIndicator component"
```

---

### Task 4: Slim `LocationChipsRow` to orchestration + perf fixes

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt` (full rewrite)

- [ ] **Step 1: Run impact analysis (AGENTS.md requirement)**

Run: `gitnexus_impact({target: "LocationChipsRow", direction: "upstream", repo: "NamazVakitleri"})`
Expected: LOW risk — single caller `HomeScreen` (soon `LocationPager`). Report to user before proceeding.

- [ ] **Step 2: Rewrite `LocationChipsRow.kt`**

Replace the entire file content with:

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer.model.location.LocationEntry

@Composable
fun LocationChipsRow(
    entries: List<LocationEntry>,
    selectedId: String?,
    pagerState: PagerState,
    onLocationSelected: (String) -> Unit,
    onAddLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val currentPage = pagerState.currentPage
    val currentPageOffsetFraction = pagerState.currentPageOffsetFraction

    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val chipBounds = remember { mutableStateMapOf<Int, ChipBounds>() }

    val indicatorOffsetX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }

    val indicatorBounds by remember(entries.size) {
        derivedStateOf {
            ChipsSelectionGeometry.indicatorBoundsFor(
                currentPage = pagerState.currentPage,
                currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                chipBounds = chipBounds,
                lastIndex = (entries.size - 1).coerceAtLeast(0)
            )
        }
    }

    LaunchedEffect(indicatorBounds) {
        if (indicatorBounds == null) return@LaunchedEffect
        indicatorOffsetX.snapTo(indicatorBounds.offsetX)
        indicatorWidth.snapTo(indicatorBounds.width)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .collect { (page, _) ->
                val index = page.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
                if (index < 0) return@collect
                if (listState.isScrollInProgress) return@collect
                val info = listState.layoutInfo
                val delta = ChipsSelectionGeometry.chipScrollTargetFor(index, info)
                if (delta != null) {
                    if (delta != 0f) listState.animateScrollBy(delta)
                } else {
                    listState.scrollToItem(index)
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> containerCoords = coords }
    ) {
        if (indicatorBounds != null) {
            SlidingChipIndicator(
                offsetX = indicatorOffsetX.value,
                width = indicatorWidth.value,
                height = indicatorBounds.height
            )
        }
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                LocationChip(
                    text = entry.displayName,
                    selectionProgress = ChipsSelectionGeometry.selectionProgressFor(
                        index = index,
                        currentPage = currentPage,
                        currentPageOffsetFraction = currentPageOffsetFraction
                    ),
                    isGps = entry.isAutoGps,
                    onClick = { onLocationSelected(entry.id) },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val container = containerCoords ?: return@onGloballyPositioned
                        val position = container.localPositionOf(coords, Offset.Zero)
                        val newBounds = ChipBounds(
                            offsetX = with(density) { position.x.toDp().value },
                            width = with(density) { coords.size.width.toDp().value },
                            height = with(density) { coords.size.height.toDp().value }
                        )
                        if (chipBounds[index] != newBounds) {
                            chipBounds[index] = newBounds
                        }
                    }
                )
            }
            item {
                AddLocationChip(onClick = onAddLocation)
            }
        }
    }
}
```

Perf fixes applied:
- Geometry derivation wrapped in `derivedStateOf` (reads `pagerState` + `chipBounds` snapshot state inside the lambda, keyed on `entries.size`) so only the indicator recomposes per scroll frame.
- `onGloballyPositioned` writes guarded with `if (chipBounds[index] != newBounds)` to avoid redundant map writes.
- Scroll-sync guarded with `if (listState.isScrollInProgress) return@collect` so the centering scroll never fights a user dragging the chips row.

- [ ] **Step 3: Compile**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run home tests**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (including the new `ChipsSelectionGeometryTest`).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/LocationChipsRow.kt
git commit -m "refactor(home): slim LocationChipsRow to orchestration with perf fixes"
```

---

### Task 5: Extract `HomeResponsiveLayout` + `HomeErrorContent`

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/layout/HomeResponsiveLayout.kt`
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeErrorContent.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt` (use the new components in `PrayerContent`/`LocationPagePreview`, delete `LandscapeMode`/`PortraitMode`/`ErrorContent`)

- [ ] **Step 1: Create `layout/HomeResponsiveLayout.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.kutluoglu.prayer_feature.common.LocalIsLandscape

@Composable
fun HomeResponsiveLayout(
    innerPadding: PaddingValues,
    topContainer: @Composable (Modifier) -> Unit,
    dailyPrayers: @Composable (Modifier) -> Unit,
    bottomContainer: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val layoutDirection = LocalLayoutDirection.current
        CompositionLocalProvider(LocalIsLandscape provides isLandscape) {
            if (isLandscape) {
                LandscapeMode(innerPadding, topContainer, dailyPrayers, bottomContainer)
            } else {
                PortraitMode(innerPadding, layoutDirection, topContainer, dailyPrayers, bottomContainer)
            }
        }
    }
}

@Composable
private fun LandscapeMode(
    innerPadding: PaddingValues,
    topContainer: @Composable (Modifier) -> Unit,
    dailyPrayers: @Composable (Modifier) -> Unit,
    bottomContainer: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        topContainer(Modifier.weight(0.4f))
        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dailyPrayers(Modifier.weight(0.8f))
            bottomContainer(Modifier.weight(0.2f))
        }
    }
}

@Composable
private fun PortraitMode(
    innerPadding: PaddingValues,
    layoutDirection: LayoutDirection,
    topContainer: @Composable (Modifier) -> Unit,
    dailyPrayers: @Composable (Modifier) -> Unit,
    bottomContainer: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding()
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topContainer(Modifier.weight(0.37f))
        dailyPrayers(Modifier.weight(0.50f))
        bottomContainer(Modifier.weight(0.13f))
    }
}
```

- [ ] **Step 2: Create `components/HomeErrorContent.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kutluoglu.prayer_feature.home.R

@Composable
fun HomeErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
```

- [ ] **Step 3: Update `HomeScreen.kt` to use the new components**

In `HomeScreen.kt`:

1. Replace the `ErrorContent(...)` call inside `PrayerContent` (currently lines 225-228) with:

```kotlin
            HomeErrorContent(
                message = errorState.message,
                onRetry = { onEvent(HomeEvent.OnRefresh) }
            )
```

2. Replace the body of `LocationPagePreview` (currently lines 170-200, the `BoxWithConstraints { ... }` block) with a single call:

```kotlin
    HomeResponsiveLayout(
        innerPadding = PaddingValues(0.dp),
        topContainer = { modifier ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                HomeTopContainer(
                    painter = painterResource(id = R.drawable.home_page_fallback),
                    successState = successState
                )
            }
        },
        dailyPrayers = { modifier ->
            Box(modifier = modifier) {
                DailyPrayers(
                    prayerState = data.prayerState,
                    isRefreshing = false,
                    onRefresh = {},
                    onViewAllClicked = onViewAllClicked,
                    isAutoGps = isAutoGps
                )
            }
        },
        bottomContainer = { modifier -> Box(modifier = modifier) }
    )
```

3. Replace the `BoxWithConstraints { ... }` block inside `PrayerContent` (currently lines 235-293) with a single `HomeResponsiveLayout(...)` call:

```kotlin
        Box(modifier = Modifier.fillMaxSize()) {
            HomeResponsiveLayout(
                innerPadding = innerPadding,
                topContainer = { modifier ->
                    Box(modifier = modifier, contentAlignment = Alignment.Center) {
                        HomeTopContainer(
                            painter = painterResource(id = R.drawable.home_page_fallback),
                            successState = successState
                        )
                    }
                },
                dailyPrayers = { modifier ->
                    Box(modifier = modifier) {
                        DailyPrayers(
                            prayerState = prayerState,
                            isRefreshing = isRefreshing,
                            onRefresh = { onEvent(HomeEvent.OnRefresh) },
                            onViewAllClicked = onPrayerTimesClick,
                            isAutoGps = isAutoGps
                        )
                    }
                },
                bottomContainer = { modifier ->
                    Box(
                        modifier = modifier
                            .clickable(enabled = successState?.quranVerse != null) {
                                onEvent(HomeEvent.OnVerseClicked)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BottomContainer(
                            quranVerse = quranVerse,
                            verseFormatter = quranVerseFormatter
                        ) { onEvent(HomeEvent.OnLoadQuranVerse) }
                    }
                }
            )
            CustomBottomSheet(
                isVisible = isVerseSheetVisible,
                onDismiss = { onEvent(HomeEvent.OnVerseDetailDismissed) }
            ) {
                quranVerse?.let { verse ->
                    VerseDetailSheetContent(verse = verse, verseFormatter = quranVerseFormatter)
                }
            }
        }
```

4. Delete the now-unused `LandscapeMode`, `PortraitMode`, and `ErrorContent` composables (currently lines 306-380) from `HomeScreen.kt`.

5. Add these imports to `HomeScreen.kt`:

```kotlin
import com.kutluoglu.prayer_feature.home.components.HomeErrorContent
import com.kutluoglu.prayer_feature.home.layout.HomeResponsiveLayout
```

6. Remove these now-unused imports from `HomeScreen.kt`:

```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.kutluoglu.prayer_feature.common.LocalIsLandscape
```

- [ ] **Step 4: Compile to verify**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/layout/HomeResponsiveLayout.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/components/HomeErrorContent.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt
git commit -m "refactor(home): extract HomeResponsiveLayout and HomeErrorContent"
```

---

### Task 6: Extract `LocationPager` + slim `HomeScreen`

**Files:**
- Create: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt` (thin wrapper)

- [ ] **Step 1: Run impact analysis (AGENTS.md requirement)**

Run: `gitnexus_impact({target: "HomeScreen", direction: "upstream", repo: "NamazVakitleri"})`
Expected: LOW risk — single caller `HomeRoute`. Report to user before proceeding.

- [ ] **Step 2: Create `LocationPager.kt`**

```kotlin
package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_feature.home.components.BottomContainer
import com.kutluoglu.prayer_feature.home.components.DailyPrayers
import com.kutluoglu.prayer_feature.home.components.HomeErrorContent
import com.kutluoglu.prayer_feature.home.components.HomeTopContainer
import com.kutluoglu.prayer_feature.home.components.LocationChipsRow
import com.kutluoglu.prayer_feature.home.domain.LoadedPrayerData
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_feature.home.layout.HomeResponsiveLayout
import com.kutluoglu.prayer_feature.home.state.CountdownUiState
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun LocationPager(
    entries: List<LocationEntry>,
    selectedId: String?,
    activeLocationId: String?,
    uiState: HomeUiState,
    prayerDataByLocation: Map<String, LoadedPrayerData>,
    quranVerseFormatter: QuranVerseFormatter,
    onPrayerTimesClick: () -> Unit,
    onAddLocation: () -> Unit,
    onEvent: (HomeEvent) -> Unit
) {
    val selectedIndex = entries.indexOfFirst { it.id == selectedId }
        .coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { entries.size.coerceAtLeast(1) }
    )

    LaunchedEffect(selectedId) {
        val index = entries.indexOfFirst { it.id == selectedId }
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    val currentEntries by rememberUpdatedState(entries)
    val currentSelectedId by rememberUpdatedState(selectedId)
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .filter { (_, isScrolling) -> !isScrolling }
            .map { (page, _) -> page }
            .distinctUntilChanged()
            .collect { page ->
                val entry = currentEntries.getOrNull(page)
                if (entry != null && entry.id != currentSelectedId) {
                    onEvent(HomeEvent.OnLocationSelected(entry.id))
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LocationChipsRow(
            entries = entries,
            selectedId = selectedId,
            pagerState = pagerState,
            onLocationSelected = { id -> onEvent(HomeEvent.OnLocationSelected(id)) },
            onAddLocation = onAddLocation
        )
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1
        ) { page ->
            val entry = entries.getOrNull(page)
            if (entry == null) return@HorizontalPager
            val isActive = entry.id == activeLocationId
            val data = prayerDataByLocation[entry.id]
            when {
                isActive -> PrayerContent(
                    uiState = uiState,
                    quranVerseFormatter = quranVerseFormatter,
                    isAutoGps = entry.isAutoGps,
                    onPrayerTimesClick = onPrayerTimesClick,
                    onEvent = onEvent
                )
                data != null -> LocationPagePreview(
                    data = data,
                    isAutoGps = entry.isAutoGps,
                    onViewAllClicked = onPrayerTimesClick
                )
                else -> LocationPlaceholder(entry)
            }
        }
    }
}

@Composable
private fun LocationPlaceholder(entry: LocationEntry?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = entry?.displayName ?: "",
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun LocationPagePreview(
    data: LoadedPrayerData,
    isAutoGps: Boolean,
    onViewAllClicked: () -> Unit
) {
    val successState = HomeUiState.Success(
        timeState = data.timeState,
        prayerState = data.prayerState,
        locationState = data.locationState,
        countdownState = CountdownUiState(),
        quranVerse = null,
        isVerseDetailSheetVisible = false
    )
    HomeResponsiveLayout(
        innerPadding = PaddingValues(0.dp),
        topContainer = { modifier ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                HomeTopContainer(
                    painter = painterResource(id = R.drawable.home_page_fallback),
                    successState = successState
                )
            }
        },
        dailyPrayers = { modifier ->
            Box(modifier = modifier) {
                DailyPrayers(
                    prayerState = data.prayerState,
                    isRefreshing = false,
                    onRefresh = {},
                    onViewAllClicked = onViewAllClicked,
                    isAutoGps = isAutoGps
                )
            }
        },
        bottomContainer = { modifier -> Box(modifier = modifier) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerContent(
    uiState: HomeUiState,
    quranVerseFormatter: QuranVerseFormatter,
    isAutoGps: Boolean = false,
    onPrayerTimesClick: () -> Unit,
    onEvent: (HomeEvent) -> Unit
) {
    val successState = uiState as? HomeUiState.Success

    val prayerState = successState?.prayerState
    val quranVerse = successState?.quranVerse
    val isVerseSheetVisible = successState?.isVerseDetailSheetVisible ?: false

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        val errorState = uiState as? HomeUiState.Error
        if (errorState != null) {
            HomeErrorContent(
                message = errorState.message,
                onRetry = { onEvent(HomeEvent.OnRefresh) }
            )
            return@Scaffold
        }

        val isRefreshing = uiState is HomeUiState.Loading

        Box(modifier = Modifier.fillMaxSize()) {
            HomeResponsiveLayout(
                innerPadding = innerPadding,
                topContainer = { modifier ->
                    Box(modifier = modifier, contentAlignment = Alignment.Center) {
                        HomeTopContainer(
                            painter = painterResource(id = R.drawable.home_page_fallback),
                            successState = successState
                        )
                    }
                },
                dailyPrayers = { modifier ->
                    Box(modifier = modifier) {
                        DailyPrayers(
                            prayerState = prayerState,
                            isRefreshing = isRefreshing,
                            onRefresh = { onEvent(HomeEvent.OnRefresh) },
                            onViewAllClicked = onPrayerTimesClick,
                            isAutoGps = isAutoGps
                        )
                    }
                },
                bottomContainer = { modifier ->
                    Box(
                        modifier = modifier
                            .clickable(enabled = successState?.quranVerse != null) {
                                onEvent(HomeEvent.OnVerseClicked)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BottomContainer(
                            quranVerse = quranVerse,
                            verseFormatter = quranVerseFormatter
                        ) { onEvent(HomeEvent.OnLoadQuranVerse) }
                    }
                }
            )
            CustomBottomSheet(
                isVisible = isVerseSheetVisible,
                onDismiss = { onEvent(HomeEvent.OnVerseDetailDismissed) }
            ) {
                quranVerse?.let { verse ->
                    VerseDetailSheetContent(verse = verse, verseFormatter = quranVerseFormatter)
                }
            }
        }
    }
}
```

Note: `PrayerContent` no longer takes `navController` — it was an unused parameter in the original `HomeScreen.kt`.

- [ ] **Step 3: Rewrite `HomeScreen.kt` as a thin wrapper**

Replace the entire file content with:

```kotlin
package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kutluoglu.core.designsystem.components.PermissionHandler
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.domain.LoadedPrayerData
import com.kutluoglu.prayer_feature.home.state.HomeUiState
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_navigation.core.PrayerNestedGraph
import com.kutluoglu.prayer_navigation.core.Screen

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    uiState: HomeUiState,
    locationsState: LocationsState,
    prayerDataByLocation: Map<String, LoadedPrayerData>,
    activeLocationId: String?,
    quranVerseFormatter: QuranVerseFormatter,
    onEvent: (HomeEvent) -> Unit
) {
    val onPrayerTimesClick = {
        navController.navigate(PrayerNestedGraph.PRAYER_TIMES) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val onAddLocation = {
        navController.navigate(Screen.SettingsScreen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PermissionHandler(
            onPermissionsGranted = { onEvent(HomeEvent.OnPermissionsGranted) }
        ) {
            LocationPager(
                entries = locationsState.entries,
                selectedId = locationsState.selectedId,
                activeLocationId = activeLocationId,
                uiState = uiState,
                prayerDataByLocation = prayerDataByLocation,
                quranVerseFormatter = quranVerseFormatter,
                onPrayerTimesClick = onPrayerTimesClick,
                onAddLocation = onAddLocation,
                onEvent = onEvent
            )
        }
    }
}
```

- [ ] **Step 4: Compile to verify**

Run: `./gradlew :prayer_feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run home tests**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeScreen.kt
git commit -m "refactor(home): extract LocationPager and slim HomeScreen to a wrapper"
```

---

### Task 7: Final verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full home test suite**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Run the full project test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all module tests pass.

- [ ] **Step 3: Run gitnexus change detection (AGENTS.md requirement)**

Run: `gitnexus_detect_changes({repo: "NamazVakitleri"})`
Expected: only the home feature UI files changed; no ViewModel/domain/state symbols affected. If the index is stale (7+ commits behind), note it and rely on the test suite as the source of truth.

- [ ] **Step 4: Confirm success criteria**

- `LocationChipsRow.kt` < ~100 lines (orchestration only).
- `HomeScreen.kt` < ~150 lines (thin wrapper).
- New `ChipsSelectionGeometryTest` covers the extracted math.
- All existing tests still pass.
- No behavior change: pager↔chips sync, indicator, color interpolation, and scroll-settle selection all work identically.

- [ ] **Step 5: Commit any remaining changes**

```bash
git status
git add -A
git commit -m "refactor(home): finalize home feature decomposition"
```
