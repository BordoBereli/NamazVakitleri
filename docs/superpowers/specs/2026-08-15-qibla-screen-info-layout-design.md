# Qibla Screen Info Layout — Design

**Date:** 2026-08-15
**Status:** Approved
**Scope:** Qibla screen (`prayer_feature/qibla`)
**Goal:** Integrate the info (location, bearing, turn rotation, accuracy) with the compass so it feels like part of the compass experience — without overlaying anything on the compass or shrinking it. Turn direction + degrees are the hero info.

## Background

The Qibla screen currently uses a vertical split: the compass dominates the top ~78%, and a separate `Card` at the bottom (~22%) holds `QiblaInfoSection` (location, direction, distance, measurement). The user's feedback: the info card feels **disconnected** from the compass. The goal is to make the info feel part of the compass while keeping the compass fully dominant and clean.

## Pain Points

1. **Disconnected info** — the bottom card is a separate visual unit; the eye has to jump between compass and card.
2. **Turn direction not prominent enough** — the actionable "turn left/right X°" is buried in the card as a text row, not glanceable while holding the phone.
3. **Compass is compromised by layout** — the split reserves space for the card, and the card can feel cramped/cut off on small screens.

## Approach

**Chosen: Info above & below the compass (no overlay).** All info is arranged around the compass in a single centered column — location + bearing above, turn rotation + accuracy below. The compass itself stays completely clean (no HUD chips on it). This was validated against two alternatives (HUD overlay on the compass, center readout inside the compass) — the user rejected overlay on the compass.

## Design

### Layout (top → bottom, all centered)

1. **Location chip** — small pill, e.g. `📍 İstanbul, TR`.
2. **Bearing badge** — white pill, gold text, thin gold border, e.g. `158° Kuzey` (qibla bearing from North).
3. **Compass** — clean, full-size, dominant. Dial + Qibla arrow + Kaaba center. No overlay.
4. **Turn pill** — gold pill, white text: big `45°` + `⟳ Sağa dön` (or `⟲ Sola dön`). This is the hero element.
5. **Accuracy badge** — green/amber/red pill: `Yüksek doğruluk` / `Orta doğruluk` / `Kalibrasyon gerekli`.

The old bottom info `Card` + `QiblaInfoSection` is **removed** — all info (location, bearing, turn, accuracy) is now integrated around the compass.

### States

- **Turning:** gold arrow + gold turn pill showing `45° ⟳ Sağa dön`.
- **Aligned:** arrow turns green + scales (existing), turn pill becomes green `✓ Kıbleye hizalı`, vibration fires (existing).
- **Low accuracy:** accuracy ring pulses/dashes (existing), badge red `Kalibrasyon gerekli`, calibration hint text appears below the badge.

### Component changes

- **`QiblaScreen.kt`** — restructure: replace the `weight(0.78f)` compass area + `weight(0.22f)` info card with a single centered `Column` (info above / compass / info below). Keep the existing error / waiting-location branches.
- **`QiblaCompass.kt`** — unchanged (already clean; arrow + accuracy ring state logic stays).
- **`QiblaInfoSection.kt`** — removed. Its logic is folded into new small private composables in `QiblaScreen.kt` (location chip, bearing badge, turn pill, accuracy badge). The turn-pill and bearing-badge logic reuses `qiblaDistanceLabel` / `accuracyLevel` from `QiblaInfoFormatter.kt`.
- **`QiblaInfoFormatter.kt`** — keep `qiblaDistanceLabel` (turn pill) and `accuracyLevel` (accuracy badge). No changes expected.
- **`strings.xml`** — add any new strings (e.g. `Kıbleye hizalı`, `Kuzey` unit, turn direction labels) if not already present.

## Files Touched

| File | Change |
|------|--------|
| `prayer_feature/qibla/.../QiblaScreen.kt` | Restructure to info-above/below layout; remove bottom card |
| `prayer_feature/qibla/.../components/QiblaInfoSection.kt` | Removed; logic folded into new private composables in `QiblaScreen.kt` |
| `prayer_feature/qibla/.../res/values*/strings.xml` | New strings if needed |
| `prayer_feature/qibla/.../test/QiblaViewModelTest.kt` | Verify still green (no ViewModel change expected) |

## Out of Scope

- Changes to `QiblaCompass.kt` visuals or sensor pipeline (`prayer_qibla`).
- Changes to the ViewModel / sensor observation logic.
- Other features or shared components.
