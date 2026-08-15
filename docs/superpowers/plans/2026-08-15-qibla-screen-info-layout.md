# Qibla Screen Info Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the Qibla screen so all info (location, bearing, turn rotation, accuracy) is integrated above and below the compass — removing the disconnected bottom info card — while keeping the compass clean and dominant.

**Architecture:** A single centered `Column` in `QiblaScreen.kt` replaces the current `weight(0.78f)` compass area + `weight(0.22f)` info card. New small private composables (`LocationChip`, `BearingBadge`, `TurnPill`) plus the existing `AccuracyBadge` render the info around the compass. `QiblaInfoSection.kt` is deleted. All label logic reuses the existing, already-tested `qiblaDistanceLabel` / `accuracyLevel` from `QiblaInfoFormatter.kt`.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), JUnit 5 + MockK + Turbine + Truth for unit tests.

**Verification note:** This module has no Compose UI test infrastructure (no `createComposeRule` deps). This is a pure layout change with no new business logic, so verification is: (1) the module compiles, and (2) existing unit tests (`QiblaViewModelTest`, `QiblaInfoFormatterTest`) stay green.

---

### Task 1: Add new string resources for the turn pill + aligned pill

**Files:**
- Modify: `prayer_feature/qibla/src/main/res/values/strings.xml`
- Modify: `prayer_feature/qibla/src/main/res/values-tr/strings.xml`

The new layout needs three strings the existing set doesn't cover: direction-only turn labels (degrees are rendered as a separate big number) and the aligned pill text.

- [ ] **Step 1: Add strings to the default (English) resource file**

In `prayer_feature/qibla/src/main/res/values/strings.xml`, add these three entries inside `<resources>` (after the existing `qibla_degrees_north` line):

```xml
<string name="qibla_turn_right_pill">Turn right</string>
<string name="qibla_turn_left_pill">Turn left</string>
<string name="qibla_aligned_pill">Facing Qibla</string>
```

- [ ] **Step 2: Add strings to the Turkish resource file**

In `prayer_feature/qibla/src/main/res/values-tr/strings.xml`, add the Turkish equivalents (after the existing `qibla_degrees_north` line):

```xml
<string name="qibla_turn_right_pill">Sağa dön</string>
<string name="qibla_turn_left_pill">Sola dön</string>
<string name="qibla_aligned_pill">Kıbleye hizalı</string>
```

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/qibla/src/main/res/values/strings.xml prayer_feature/qibla/src/main/res/values-tr/strings.xml
git commit -m "feat(qibla): add turn pill and aligned pill strings"
```

---

### Task 2: Restructure `QiblaScreen.kt` to the info-above/below layout

**Files:**
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt`

Replace the entire file content with the new layout. The `when` branches for error / waiting-location are preserved. The success branch now renders: location chip → bearing badge → compass → turn pill → accuracy badge → (calibration hint if low accuracy), all centered in a single `Column`.

- [ ] **Step 1: Write the new `QiblaScreen.kt`**

Replace the full contents of `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` with:

```kotlin
package com.kutluoglu.prayer_feature.qibla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kutluoglu.prayer_feature.qibla.components.AccuracyLevel
import com.kutluoglu.prayer_feature.qibla.components.QIBLA_ALIGNMENT_THRESHOLD
import com.kutluoglu.prayer_feature.qibla.components.QiblaCompass
import com.kutluoglu.prayer_feature.qibla.components.QiblaDistanceLabel
import com.kutluoglu.prayer_feature.qibla.components.TurnDirection
import com.kutluoglu.prayer_feature.qibla.components.accuracyLevel
import com.kutluoglu.prayer_feature.qibla.components.qiblaDistanceLabel
import kotlin.math.roundToInt

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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            uiState.error != null -> {
                Text(stringResource(R.string.qibla_location_error))
            }
            !uiState.isLocationAvailable -> {
                Text(stringResource(R.string.qibla_waiting_location))
            }
            else -> {
                locationName?.let {
                    LocationChip(locationName = it)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                BearingBadge(bearing = uiState.qiblaBearing)
                Spacer(modifier = Modifier.height(16.dp))
                QiblaCompass(
                    deviceAzimuth = uiState.deviceAzimuth,
                    qiblaAngle = uiState.qiblaAngle,
                    sensorAccuracy = uiState.sensorAccuracy
                )
                Spacer(modifier = Modifier.height(16.dp))
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
            }
        }
    }
}

@Composable
private fun LocationChip(locationName: String, modifier: Modifier = Modifier) {
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

@Composable
private fun BearingBadge(bearing: Double, modifier: Modifier = Modifier) {
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

@Composable
private fun TurnPill(qiblaAngle: Float, modifier: Modifier = Modifier) {
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

@Composable
private fun AccuracyBadge(sensorAccuracy: Int, modifier: Modifier = Modifier) {
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

- [ ] **Step 2: Verify the module compiles**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no unresolved references — `QIBLA_ALIGNMENT_THRESHOLD`, `QiblaDistanceLabel`, `TurnDirection`, `qiblaDistanceLabel`, `accuracyLevel` all come from the `components` package).

- [ ] **Step 3: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "feat(qibla): integrate info above and below the compass"
```

---

### Task 3: Remove `QiblaInfoSection.kt`

**Files:**
- Delete: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoSection.kt`

`QiblaInfoSection` is now unused (the only reference was in the old `QiblaScreen.kt`, which Task 2 replaced). Its logic is folded into the new private composables.

- [ ] **Step 1: Delete the file**

```bash
git rm prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoSection.kt
```

- [ ] **Step 2: Verify no remaining references**

Run: `rg -n "QiblaInfoSection" prayer_feature/qibla/src`
Expected: no matches in `src/` (matches in `docs/` are historical and fine).

- [ ] **Step 3: Verify the module still compiles**

Run: `./gradlew :prayer_feature:qibla:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A prayer_feature/qibla
git commit -m "refactor(qibla): remove obsolete QiblaInfoSection"
```

---

### Task 4: Run the full verification

**Files:**
- Test: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/QiblaViewModelTest.kt` (unchanged, must stay green)
- Test: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoFormatterTest.kt` (unchanged, must stay green)

- [ ] **Step 1: Run the qibla module unit tests**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (QiblaViewModelTest + QiblaInfoFormatterTest).

- [ ] **Step 2: Run the full debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run GitNexus change detection before final commit**

Run: `gitnexus_detect_changes()` (scope: unstaged/staged)
Expected: only `QiblaScreen.kt`, `QiblaInfoSection.kt`, and the two `strings.xml` files changed; no unexpected execution flows affected.

- [ ] **Step 4: Final commit (if any stragglers)**

```bash
git status
git add -A
git commit -m "chore(qibla): finalize info layout restructure"
```

---

## Self-Review Notes

- **Spec coverage:** Layout (location chip → bearing badge → compass → turn pill → accuracy badge) ✓ (Task 2); states (turning / aligned / low accuracy) ✓ (TurnPill + AccuracyBadge + existing compass ring logic); removal of bottom card + `QiblaInfoSection` ✓ (Tasks 2 & 3); strings ✓ (Task 1); `QiblaCompass.kt` unchanged ✓ (not touched).
- **Type consistency:** `QiblaDistanceLabel.Turn(degrees: Int, direction: TurnDirection)` matches `QiblaInfoFormatter.kt`; `qibla_degrees_north` takes `%1$d` (Int) and `bearing.roundToInt()` supplies Int; `QIBLA_ALIGNMENT_THRESHOLD` is the public `const val` in `QiblaCompass.kt`.
- **No placeholders:** every step has exact file paths, full code, and exact commands.
