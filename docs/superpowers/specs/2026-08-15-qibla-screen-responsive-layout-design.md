# Qibla Screen Responsive Layout — Design

**Date:** 2026-08-15
**Status:** Approved
**Scope:** Qibla screen (`prayer_feature/qibla`)
**Goal:** Make the Qibla screen layout responsive so it looks good in both portrait and landscape. Portrait keeps the current info-above/below stack; landscape places the compass in the center with the "above" info (location, bearing) on the left and the "below" info (turn pill, accuracy) on the right.

## Background

The Qibla screen was recently restructured into a single centered column: location chip → bearing badge → compass → turn pill → accuracy badge → (calibration hint). This looks great in portrait but the tall column clips/overflows in landscape. The user wants a landscape-specific layout: compass centered, left info hugging the top, right info hugging the bottom (mirroring the portrait above/below relationship).

## Approach

Use `BoxWithConstraints` to detect landscape (`maxWidth > maxHeight`) and branch between two layouts. This is more robust than `LocalConfiguration.current.orientation` because it adapts to available space (split-screen, foldables, resizable windows), not just device rotation.

## Design

### Orientation detection

```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isLandscape = maxWidth > maxHeight
    if (isLandscape) {
        LandscapeLayout(...)
    } else {
        PortraitLayout(...)
    }
}
```

### Portrait layout (unchanged)

The current centered `Column` — location chip → bearing badge → compass → turn pill → accuracy badge → (calibration hint if low accuracy). Extracted into a `PortraitLayout` private composable.

### Landscape layout (new)

A `Row` with three parts, all vertically centered:

- **Left `Column`** (`weight(1f)`, `fillMaxHeight`, `Arrangement.Top`): location chip + bearing badge.
- **Center:** `QiblaCompass` (unchanged).
- **Right `Column`** (`weight(1f)`, `fillMaxHeight`, `Arrangement.Bottom`): turn pill + accuracy badge + (calibration hint if low accuracy).

### Shared states

The error / waiting-location branches stay in the main `QiblaScreen` composable (single `when`), and only the success branch branches to `PortraitLayout` or `LandscapeLayout`. This avoids duplicating the error/waiting UI.

### Component changes

- **`QiblaScreen.kt`** — wrap the success branch in `BoxWithConstraints`; extract the current column into `PortraitLayout`; add `LandscapeLayout`. The private `LocationChip` / `BearingBadge` / `TurnPill` / `AccuracyBadge` composables are reused as-is.
- **`QiblaCompass.kt`** — unchanged.
- **`QiblaInfoFormatter.kt`** — unchanged.
- **`strings.xml`** — no new strings expected.

## Files Touched

| File | Change |
|------|--------|
| `prayer_feature/qibla/.../QiblaScreen.kt` | Add `BoxWithConstraints`; extract `PortraitLayout`; add `LandscapeLayout` |

## Out of Scope

- Changes to `QiblaCompass.kt`, the sensor pipeline, or the ViewModel.
- Changes to the portrait layout's visual design.
- Other features or shared components.
