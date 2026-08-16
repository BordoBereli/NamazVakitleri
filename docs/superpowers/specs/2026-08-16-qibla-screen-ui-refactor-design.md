# Qibla Screen UI Layer Refactor — Design

**Date:** 2026-08-16
**Status:** Approved
**Scope:** `prayer_feature/qibla` UI layer only (`QiblaScreen.kt`)
**Goal:** Improve maintainability of the Qibla screen by deduplicating duplicated layout code, extracting reusable components, and unifying the portrait/landscape layouts into a single adaptive layout with a testable strategy seam. **No visual or behavioral change** — the rendered screen stays identical.

## Background

`QiblaScreen.kt` (287 lines) is the entry composable for the Qibla feature. It handles state gating (error / no-location / active), orientation detection via `BoxWithConstraints`, and defines four private component composables inline. The two orientation layouts (`PortraitLayout` and `LandscapeLayout`) each repeat the same bottom status block: `TurnPill` + `AccuracyBadge` + optional calibrate hint — a literal copy-paste that must be maintained twice.

This spec is a UI-only follow-up to the broader `2026-08-15-qibla-feature-refactor-design.md`. It does not touch the sensor pipeline (`prayer_qibla`), the ViewModel, the domain layer, or any visual output.

## Pain Points

1. **Duplicated layout block** — `PortraitLayout` (`QiblaScreen.kt:102-112`) and `LandscapeLayout` (`QiblaScreen.kt:154-164`) repeat the exact same status block contents. Any change (text, spacing, new hint) must be made twice and can drift.
2. **Inline private components** — `LocationChip`, `BearingBadge`, `TurnPill`, `AccuracyBadge` are `private` to the screen file, while `QiblaCompass` and `QiblaInfoFormatter` live in `prayer_feature/qibla/components/`. Component organization is inconsistent, and the private helpers are unusable elsewhere.
3. **Bare orientation check** — the layout decision is an inline `maxWidth > maxHeight` (`QiblaScreen.kt:55`), with no seam to grow into tablet/multi-window support later.

## Approach

**Approach B + strategy (user-approved).** Merge the two layout functions into a single adaptive `QiblaLayout` composable that switches on a small, unit-testable `QiblaLayoutStrategy` enum, extracting all four inline components into the `components/` package as public composables and introducing one shared `QiblaStatusBlock` composable that kills the duplication. Layout output is preserved exactly by construction.

## Design

### 1. Extracted components — new files in `prayer_feature/qibla/.../components/`

All extracted as **public** `@Composable` functions with identical visuals (colors, shapes, paddings, strings) to today:

| New file | Component | Moved from |
|---|---|---|
| `components/LocationChip.kt` | `LocationChip(locationName, modifier)` | `QiblaScreen.kt:170` |
| `components/BearingBadge.kt` | `BearingBadge(bearing, modifier)` | `QiblaScreen.kt:187` |
| `components/TurnPill.kt` | `TurnPill(qiblaAngle, modifier)` | `QiblaScreen.kt:205` |
| `components/AccuracyBadge.kt` | `AccuracyBadge(sensorAccuracy, modifier)` | `QiblaScreen.kt:255` |
| `components/QiblaStatusBlock.kt` | `QiblaStatusBlock(qiblaAngle, sensorAccuracy, modifier)` | **new** — deduplicated block |

`QiblaStatusBlock` encapsulates the repeated block: `TurnPill` + `AccuracyBadge` + optional calibrate hint (shown only when `accuracyLevel(sensorAccuracy) == AccuracyLevel.LOW`).

No naming collision: `LocationChip` also exists in `prayer_feature/home/components/`, but different packages keep imports unambiguous.

### 2. Adaptive layout + strategy seam

**New file: `components/QiblaLayoutStrategy.kt`** — dependency-free, unit-testable:

```kotlin
enum class QiblaLayoutStrategy { PORTRAIT, LANDSCAPE }

fun qiblaLayoutStrategy(maxWidth: Dp, maxHeight: Dp): QiblaLayoutStrategy =
    if (maxWidth > maxHeight) QiblaLayoutStrategy.LANDSCAPE else QiblaLayoutStrategy.PORTRAIT
```

**Refactored `QiblaScreen.kt`** (~110 lines): state gating → `BoxWithConstraints` → strategy resolution → `QiblaLayout`.

**New file: `components/QiblaLayout.kt`** — `internal QiblaLayout(strategy, qiblaBearing, deviceAzimuth, qiblaAngle, sensorAccuracy, locationName, modifier)` composable (scalar params keep it decoupled from `QiblaUiState`). Switches on strategy:

- **PORTRAIT** → `Column`: `LocationChip` + `BearingBadge` + `QiblaCompass` + `QiblaStatusBlock`.
- **LANDSCAPE** → `Row`: left `Column` (`LocationChip` + `BearingBadge`), center `QiblaCompass`, right `Column` (`QiblaStatusBlock`).

Spacing and arrangement values are copied verbatim from today's layouts.

### 3. Public API

`QiblaScreen(uiState: QiblaUiState, locationName: String?, onEvent: (QiblaEvent) -> Unit)` signature is unchanged. `qiblaGraph` route and all callers are unaffected.

## Testing

- **New unit test**: `qiblaLayoutStrategy()` — portrait, landscape, and width==height edge case (→ PORTRAIT).
- **Existing tests must remain green**: `QiblaViewModelTest`, `QiblaInfoFormatterTest`, `OrientationProviderTest`, `QiblaDataStoreImpTest`.
- **No new UI/screenshot tests** — the extracted components are presentational, and their formatting logic already has unit coverage.

## Files Touched

| File | Change |
|------|--------|
| `prayer_feature/qibla/.../QiblaScreen.kt` | Rewrite to state gating + strategy + `QiblaLayout`; remove private components and duplicated block |
| `prayer_feature/qibla/.../components/LocationChip.kt` | **New** — extracted from screen |
| `prayer_feature/qibla/.../components/BearingBadge.kt` | **New** — extracted from screen |
| `prayer_feature/qibla/.../components/TurnPill.kt` | **New** — extracted from screen |
| `prayer_feature/qibla/.../components/AccuracyBadge.kt` | **New** — extracted from screen |
| `prayer_feature/qibla/.../components/QiblaStatusBlock.kt` | **New** — deduplicated status block |
| `prayer_feature/qibla/.../components/QiblaLayoutStrategy.kt` | **New** — strategy enum + function |
| `prayer_feature/qibla/.../components/QiblaLayout.kt` | **New** — unified adaptive layout (portrait/landscape arrangements) |
| `prayer_feature/qibla/.../test/QiblaLayoutStrategyTest.kt` | **New** — strategy unit tests |

## Out of Scope

- Sensor pipeline (`prayer_qibla`), ViewModel, domain/data layers.
- Any visual or text change to the rendered screen.
- WindowSizeClass / new dependencies — deliberately avoided; the strategy seam covers future needs.
- Other feature screens or shared components.