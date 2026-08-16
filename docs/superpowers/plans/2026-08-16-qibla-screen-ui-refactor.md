# Qibla Screen UI Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Per AGENTS.md:** Before each edit run `gitnexus_impact` on the symbol being moved/extracted; run `gitnexus_detect_changes` before each commit. All modified symbols here are `@Composable` private functions in `QiblaScreen.kt` — verify blast radius before extracting.

**Goal:** Refactor the Qibla screen UI layer for maintainability — extract duplicated layout code into reusable `components/` composables, deduplicate the repeated status block, and unify portrait/landscape layouts behind a testable strategy seam. Visual output stays pixel-identical.

**Architecture:** One `QiblaScreen` composable keeps state gating and orientation detection (`BoxWithConstraints`), then delegates to a single adaptive `QiblaLayout` that switches on a `QiblaLayoutStrategy` (PORTRAIT/LANDSCAPE). The four inline private composables (`LocationChip`, `BearingBadge`, `TurnPill`, `AccuracyBadge`) move to `prayer_feature/qibla/.../components/` as public composables, and a new `QiblaStatusBlock` replaces the copy-pasted `TurnPill`+`AccuracyBadge`+calibrate block shared by both layouts.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), JUnit 5 (Jupiter) + Truth for the new unit test. No new dependencies.

**Spec:** `docs/superpowers/specs/2026-08-16-qibla-screen-ui-refactor-design.md`

---

## File Structure

| File | Responsibility |
|---|---|
| `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayoutStrategy.kt` | **New** — `QiblaLayoutStrategy` enum + `qiblaLayoutStrategy(maxWidth, maxHeight)` pure function |
| `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/LocationChip.kt` | **New** — extracted `LocationChip` (public) |
| `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/BearingBadge.kt` | **New** — extracted `BearingBadge` (public) |
| `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/TurnPill.kt` | **New** — extracted `TurnPill` (public) |
| `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/AccuracyBadge.kt` | **New** — extracted `AccuracyBadge` (public) |
| `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaStatusBlock.kt` | **New** — deduplicated `TurnPill`+`AccuracyBadge`+calibrate column |
| `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayout.kt` | **New** — unified adaptive layout (PORTRAIT `Column` / LANDSCAPE `Row`) |
| `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` | **Modify** — strip private components + both layout functions; wire strategy + `QiblaLayout` |
| `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayoutStrategyTest.kt` | **New** — unit tests for `qiblaLayoutStrategy` |

Extraction order matters: `QiblaStatusBlock` needs `TurnPill`+`AccuracyBadge` extracted first (Tasks 4–5); `QiblaLayout` needs `LocationChip`+`BearingBadge` extracted first (Tasks 2–3). Task 1 (strategy) is independent and TDD-driven.

---

### Task 1: Add `QiblaLayoutStrategy` + unit tests (TDD)

**Files:**
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayoutStrategy.kt`
- Test: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayoutStrategyTest.kt`

- [ ] **Step 1: Write the failing test**

Create `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayoutStrategyTest.kt`:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class QiblaLayoutStrategyTest {

    @Test
    fun `landscape when width exceeds height`() {
        assertThat(qiblaLayoutStrategy(maxWidth = 320.dp, maxHeight = 240.dp))
            .isEqualTo(QiblaLayoutStrategy.LANDSCAPE)
    }

    @Test
    fun `portrait when height exceeds width`() {
        assertThat(qiblaLayoutStrategy(maxWidth = 240.dp, maxHeight = 320.dp))
            .isEqualTo(QiblaLayoutStrategy.PORTRAIT)
    }

    @Test
    fun `square defaults to portrait`() {
        assertThat(qiblaLayoutStrategy(maxWidth = 240.dp, maxHeight = 240.dp))
            .isEqualTo(QiblaLayoutStrategy.PORTRAIT)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.qibla.components.QiblaLayoutStrategyTest"`
Expected: FAIL — `"qiblaLayoutStrategy" is not defined` (unresolved reference).

- [ ] **Step 3: Write minimal implementation**

Create `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayoutStrategy.kt`:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.ui.unit.Dp

enum class QiblaLayoutStrategy { PORTRAIT, LANDSCAPE }

fun qiblaLayoutStrategy(maxWidth: Dp, maxHeight: Dp): QiblaLayoutStrategy =
    if (maxWidth > maxHeight) QiblaLayoutStrategy.LANDSCAPE else QiblaLayoutStrategy.PORTRAIT
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.qibla.components.QiblaLayoutStrategyTest"`
Expected: PASS (process finished with exit code 0, 3 tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayoutStrategy.kt \
        prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayoutStrategyTest.kt
git commit -m "feat(qibla): add adaptive layout strategy seam"
```

---

### Task 2: Extract `LocationChip` to `components/`

**Files:**
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/LocationChip.kt`
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` (remove private `LocationChip` at lines 169–184, add import)

- [ ] **Step 1: Create the public component**

Create `LocationChip.kt`:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LocationChip(locationName: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Text(
            text = locationName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 2: Remove the private copy from `QiblaScreen.kt` and import**

Delete the private `LocationChip` composable block (current `QiblaScreen.kt:169-184`) so the file no longer declares it. Add the import:

```kotlin
import com.kutluoglu.prayer_feature.qibla.components.LocationChip
```

(Place it with the other `components` imports already present in the file, e.g. after `components.QiblaCompass`.)

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (the two call sites `LocationChip(locationName = it)` at what were lines 91 and 134 now resolve to the public component).

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/LocationChip.kt \
        prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "refactor(qibla): extract LocationChip to components"
```

---

### Task 3: Extract `BearingBadge` to `components/`

**Files:**
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/BearingBadge.kt`
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` (remove private `BearingBadge` at lines 186–202, add import)

- [ ] **Step 1: Create the public component**

Create `BearingBadge.kt`:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.R
import kotlin.math.roundToInt

@Composable
fun BearingBadge(bearing: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFB8860B).copy(alpha = 0.35f))
    ) {
        Text(
            text = stringResource(R.string.qibla_degrees_north, bearing.roundToInt()),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB8860B)
        )
    }
}
```

- [ ] **Step 2: Remove the private copy from `QiblaScreen.kt` and import**

Delete the private `BearingBadge` composable block (current `QiblaScreen.kt:186-202`). Add the import:

```kotlin
import com.kutluoglu.prayer_feature.qibla.components.BearingBadge
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (`BearingBadge(bearing = uiState.qiblaBearing)` at both call sites resolves to the public component).

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/BearingBadge.kt \
        prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "refactor(qibla): extract BearingBadge to components"
```

---

### Task 4: Extract `TurnPill` to `components/`

**Files:**
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/TurnPill.kt`
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` (remove private `TurnPill` at lines 204–252, add import)

- [ ] **Step 1: Create the public component**

Create `TurnPill.kt`:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.R

@Composable
fun TurnPill(qiblaAngle: Float, modifier: Modifier = Modifier) {
    val label = qiblaDistanceLabel(qiblaAngle, QIBLA_ALIGNMENT_THRESHOLD)
    val isAligned = label is QiblaDistanceLabel.Aligned
    val container = if (isAligned) Color(0xFF1E7E34) else Color(0xFFB8860B)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (label) {
                is QiblaDistanceLabel.Aligned -> {
                    Text(
                        text = stringResource(R.string.qibla_aligned_pill),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                is QiblaDistanceLabel.Turn -> {
                    Text(
                        text = "${label.degrees}°",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(
                            if (label.direction == TurnDirection.RIGHT) {
                                R.string.qibla_turn_right_pill
                            } else {
                                R.string.qibla_turn_left_pill
                            }
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Remove the private copy from `QiblaScreen.kt` and import**

Delete the private `TurnPill` composable block (current `QiblaScreen.kt:204-252`). Add the import:

```kotlin
import com.kutluoglu.prayer_feature.qibla.components.TurnPill
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (both call sites `TurnPill(qiblaAngle = uiState.qiblaAngle)` resolve to the public component).

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/TurnPill.kt \
        prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "refactor(qibla): extract TurnPill to components"
```

---

### Task 5: Extract `AccuracyBadge` to `components/`

**Files:**
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/AccuracyBadge.kt`
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` (remove private `AccuracyBadge` at lines 254–287, add import)

- [ ] **Step 1: Create the public component**

Create `AccuracyBadge.kt`:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.R

@Composable
fun AccuracyBadge(sensorAccuracy: Int, modifier: Modifier = Modifier) {
    val level = accuracyLevel(sensorAccuracy)
    val (text, container, content) = when (level) {
        AccuracyLevel.HIGH -> Triple(
            stringResource(R.string.qibla_accuracy_high_badge),
            Color(0xFFE6F4EA),
            Color(0xFF1E7E34)
        )
        AccuracyLevel.MEDIUM -> Triple(
            stringResource(R.string.qibla_accuracy_medium_badge),
            Color(0xFFFFF4E0),
            Color(0xFFB26A00)
        )
        AccuracyLevel.LOW -> Triple(
            stringResource(R.string.qibla_calibration_required),
            Color(0xFFFDE8E8),
            Color(0xFFB3261E)
        )
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}
```

- [ ] **Step 2: Remove the private copy from `QiblaScreen.kt` and import**

Delete the private `AccuracyBadge` composable block (current `QiblaScreen.kt:254-287`). Add the import:

```kotlin
import com.kutluoglu.prayer_feature.qibla.components.AccuracyBadge
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (both call sites `AccuracyBadge(sensorAccuracy = uiState.sensorAccuracy)` resolve to the public component).

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/AccuracyBadge.kt \
        prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "refactor(qibla): extract AccuracyBadge to components"
```

---

### Task 6: Add `QiblaStatusBlock` and replace the duplicated block

**Files:**
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaStatusBlock.kt`
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` (replace duplicated block in `PortraitLayout` and `LandscapeLayout`)

- [ ] **Step 1: Create `QiblaStatusBlock`**

Create `QiblaStatusBlock.kt`:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.R

@Composable
fun QiblaStatusBlock(
    qiblaAngle: Float,
    sensorAccuracy: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TurnPill(qiblaAngle = qiblaAngle)
        Spacer(modifier = Modifier.height(8.dp))
        AccuracyBadge(sensorAccuracy = sensorAccuracy)
        if (accuracyLevel(sensorAccuracy) == AccuracyLevel.LOW) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.qibla_calibrate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

- [ ] **Step 2: Replace the duplicated block in `PortraitLayout`**

In `QiblaScreen.kt`, replace the sequence that is currently inside `PortraitLayout`:

```kotlin
        TurnPill(qiblaAngle = uiState.qiblaAngle)
        Spacer(modifier = Modifier.height(8.dp))
        AccuracyBadge(sensorAccuracy = uiState.sensorAccuracy)
        if (accuracyLevel(uiState.sensorAccuracy) == AccuracyLevel.LOW) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.qibla_calibrate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
```

with:

```kotlin
        QiblaStatusBlock(
            qiblaAngle = uiState.qiblaAngle,
            sensorAccuracy = uiState.sensorAccuracy
        )
```

- [ ] **Step 3: Replace the duplicated block in `LandscapeLayout`**

Repeat Step 2 for the identical block currently inside `LandscapeLayout` (the same 10-line sequence). Replace it with the same `QiblaStatusBlock(...)` call. Add the import:

```kotlin
import com.kutluoglu.prayer_feature.qibla.components.QiblaStatusBlock
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Note: `accuracyLevel` and `R.string.qibla_calibrate` are no longer referenced in `QiblaScreen.kt`; remove the now-unused imports (`components.accuracyLevel`) if the compiler flags them, keeping the file warning-free.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaStatusBlock.kt \
        prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "refactor(qibla): extract shared status block"
```

---

### Task 7: Add `QiblaLayout` and unify the layouts in `QiblaScreen`

**Files:**
- Create: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayout.kt`
- Rewrite: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt`

- [ ] **Step 1: Create `QiblaLayout`**

Create `QiblaLayout.kt`:

```kotlin
package com.kutluoglu.prayer_feature.qibla.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QiblaLayout(
    strategy: QiblaLayoutStrategy,
    qiblaBearing: Double,
    deviceAzimuth: Float,
    qiblaAngle: Float,
    sensorAccuracy: Int,
    locationName: String?,
    modifier: Modifier = Modifier
) {
    when (strategy) {
        QiblaLayoutStrategy.PORTRAIT -> PortraitColumn(
            qiblaBearing = qiblaBearing,
            deviceAzimuth = deviceAzimuth,
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy,
            locationName = locationName,
            modifier = modifier
        )
        QiblaLayoutStrategy.LANDSCAPE -> LandscapeRow(
            qiblaBearing = qiblaBearing,
            deviceAzimuth = deviceAzimuth,
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy,
            locationName = locationName,
            modifier = modifier
        )
    }
}

@Composable
private fun PortraitColumn(
    qiblaBearing: Double,
    deviceAzimuth: Float,
    qiblaAngle: Float,
    sensorAccuracy: Int,
    locationName: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        locationName?.let {
            LocationChip(locationName = it)
            Spacer(modifier = Modifier.height(8.dp))
        }
        BearingBadge(bearing = qiblaBearing)
        Spacer(modifier = Modifier.height(16.dp))
        QiblaCompass(
            deviceAzimuth = deviceAzimuth,
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy
        )
        Spacer(modifier = Modifier.height(16.dp))
        QiblaStatusBlock(
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy
        )
    }
}

@Composable
private fun LandscapeRow(
    qiblaBearing: Double,
    deviceAzimuth: Float,
    qiblaAngle: Float,
    sensorAccuracy: Int,
    locationName: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            locationName?.let {
                LocationChip(locationName = it)
                Spacer(modifier = Modifier.height(8.dp))
            }
            BearingBadge(bearing = qiblaBearing)
        }

        QiblaCompass(
            deviceAzimuth = deviceAzimuth,
            qiblaAngle = qiblaAngle,
            sensorAccuracy = sensorAccuracy
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            QiblaStatusBlock(
                qiblaAngle = qiblaAngle,
                sensorAccuracy = sensorAccuracy
            )
        }
    }
}
```

- [ ] **Step 2: Rewrite `QiblaScreen.kt`**

Replace the **entire contents** of `QiblaScreen.kt` with:

```kotlin
package com.kutluoglu.prayer_feature.qibla

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kutluoglu.prayer_feature.qibla.components.QiblaLayout
import com.kutluoglu.prayer_feature.qibla.components.qiblaLayoutStrategy

@Composable
fun QiblaScreen(
    uiState: QiblaUiState,
    locationName: String? = "Istanbul, TR",
    onEvent: (QiblaEvent) -> Unit
) {
    LaunchedEffect(Unit) {
        onEvent(QiblaEvent.OnStart)
    }

    DisposableEffect(Unit) {
        onDispose {
            onEvent(QiblaEvent.OnStop)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.error != null -> {
                Text(stringResource(R.string.qibla_location_error))
            }
            !uiState.isLocationAvailable -> {
                Text(stringResource(R.string.qibla_waiting_location))
            }
            else -> {
                QiblaLayout(
                    strategy = qiblaLayoutStrategy(maxWidth = maxWidth, maxHeight = maxHeight),
                    qiblaBearing = uiState.qiblaBearing,
                    deviceAzimuth = uiState.deviceAzimuth,
                    qiblaAngle = uiState.qiblaAngle,
                    sensorAccuracy = uiState.sensorAccuracy,
                    locationName = locationName
                )
            }
        }
    }
}
```

`R` resolves automatically (same package `com.kutluoglu.prayer_feature.qibla`). `maxWidth`/`maxHeight` are available inside `BoxWithConstraints` scope and are of type `Dp`, matching `qiblaLayoutStrategy`.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Any remaining unused imports in the old file (e.g. `Surface`, `BorderStroke`, `FontWeight`, `Color`, `roundToInt`) are gone with the rewrite.

- [ ] **Step 4: Run the module unit tests**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest`
Expected: PASS — `QiblaLayoutStrategyTest` (3 tests), `QiblaViewModelTest`, `QiblaInfoFormatterTest` all green.

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaLayout.kt \
        prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "refactor(qibla): unify portrait and landscape layouts"
```

---

### Task 8: Final verification

**Files:**
- No code changes.

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (Task 1's strategy tests + existing ViewModel/InfoFormatter/OrientationProvider/QiblaDataStoreImp tests).

- [ ] **Step 2: Full debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — confirms the app module compiles against the refactored library module.

- [ ] **Step 3: Impact check**

Run GITNEXUS `gitnexus_detect_changes()` (scope: unstaged). Confirm the only affected symbols are the Qibla UI composables and that no execution flows outside `prayer_feature:qibla` changed. Review any unexpected hits before final commit.

- [ ] **Step 4: Commit any leftovers**

If Step 3 surfaces unrelated staging or the detect run flags leftover changes, commit them with an appropriate `chore:`/`refactor:` message. Otherwise no action.

---

## Spec Coverage Check

- **Section 1 (extract components)** → Tasks 2, 3, 4, 5 (`LocationChip`, `BearingBadge`, `TurnPill`, `AccuracyBadge`) and Task 6 (`QiblaStatusBlock`).
- **Section 2 (adaptive layout + strategy)** → Task 1 (`QiblaLayoutStrategy` + tests) and Task 7 (`QiblaLayout`, screen rewrite).
- **Section 3 (testing)** → Task 1 new unit tests; Tasks 7–8 run existing suites; no new UI tests added.
- **Public API unchanged** → `QiblaScreen(uiState, locationName, onEvent)` signature preserved in Task 7; `qiblaGraph` untouched.