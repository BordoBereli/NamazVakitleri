# Qibla Screen Responsive Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Qibla screen layout responsive so it looks good in both portrait and landscape. Portrait keeps the current info-above/below stack; landscape places the compass centered with the "above" info (location, bearing) on the left and the "below" info (turn pill, accuracy) on the right.

**Architecture:** Wrap the success branch of `QiblaScreen` in a `BoxWithConstraints` and branch on `maxWidth > maxHeight`. Extract the current centered column into a `PortraitLayout` composable and add a new `LandscapeLayout` composable (a `Row` with left column / compass / right column). The private `LocationChip`, `BearingBadge`, `TurnPill`, `AccuracyBadge` composables are reused unchanged.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), JUnit 5 + MockK + Turbine + Truth for unit tests.

**Verification note:** This module has no Compose UI test infrastructure (no `createComposeRule` deps). This is a pure layout change with no new business logic, so verification is: (1) the module compiles, and (2) existing unit tests (`QiblaViewModelTest`, `QiblaInfoFormatterTest`) stay green.

---

### Task 1: Make `QiblaScreen.kt` responsive (portrait + landscape)

**Files:**
- Modify: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt`

Replace the entire file content with the responsive version. The error / waiting-location branches stay in the main `QiblaScreen` composable (single `when`), and only the success branch branches to `PortraitLayout` or `LandscapeLayout`.

- [ ] **Step 1: Write the new `QiblaScreen.kt`**

Replace the full contents of `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt` with EXACTLY this:

```kotlin
package com.kutluoglu.prayer_feature.qibla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight
        when {
            uiState.error != null -> {
                Text(stringResource(R.string.qibla_location_error))
            }
            !uiState.isLocationAvailable -> {
                Text(stringResource(R.string.qibla_waiting_location))
            }
            else -> {
                if (isLandscape) {
                    LandscapeLayout(
                        uiState = uiState,
                        locationName = locationName
                    )
                } else {
                    PortraitLayout(
                        uiState = uiState,
                        locationName = locationName
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: QiblaUiState,
    locationName: String?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

@Composable
private fun LandscapeLayout(
    uiState: QiblaUiState,
    locationName: String?
) {
    Row(
        modifier = Modifier.fillMaxSize(),
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
            BearingBadge(bearing = uiState.qiblaBearing)
        }

        QiblaCompass(
            deviceAzimuth = uiState.deviceAzimuth,
            qiblaAngle = uiState.qiblaAngle,
            sensorAccuracy = uiState.sensorAccuracy
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
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
Expected: BUILD SUCCESSFUL (no unresolved references — `BoxWithConstraints`, `fillMaxHeight`, `maxWidth`, `maxHeight` all resolve from the Compose foundation layout APIs).

- [ ] **Step 3: Verify existing unit tests still pass**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass (QiblaViewModelTest + QiblaInfoFormatterTest).

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt
git commit -m "feat(qibla): add responsive landscape layout"
```

---

### Task 2: Run the full verification

**Files:**
- Test: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/QiblaViewModelTest.kt` (unchanged, must stay green)
- Test: `prayer_feature/qibla/src/test/java/com/kutluoglu/prayer_feature/qibla/components/QiblaInfoFormatterTest.kt` (unchanged, must stay green)

- [ ] **Step 1: Run the qibla module unit tests**

Run: `./gradlew :prayer_feature:qibla:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Run the full debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run GitNexus change detection**

Run: `gitnexus_detect_changes()` (scope: all) for repo NamazVakitleri, or if unavailable run `git status` and `git diff --stat`.
Expected: only `QiblaScreen.kt` changed; no unexpected execution flows affected.

- [ ] **Step 4: Confirm git state**

Run: `git status` and `git log --oneline -3`
Expected: working tree clean, HEAD is the responsive layout commit.

---

## Self-Review Notes

- **Spec coverage:** Orientation detection via `BoxWithConstraints` (`maxWidth > maxHeight`) ✓ (Task 1); portrait layout unchanged (extracted to `PortraitLayout`) ✓; landscape layout (left top-aligned info / center compass / right bottom-aligned info) ✓; error/waiting branches shared in main composable ✓; `QiblaCompass.kt` unchanged ✓.
- **Type consistency:** `LandscapeLayout`/`PortraitLayout` both take `(uiState: QiblaUiState, locationName: String?)`; all private composables keep their existing signatures.
- **No placeholders:** every step has exact file paths, full code, and exact commands.
- **Known edge case (out of scope):** the compass is a fixed 300dp; on very short landscape heights it could clip. The spec explicitly keeps `QiblaCompass.kt` unchanged.
