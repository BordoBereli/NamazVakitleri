# Qibla Feature Refactor — Design

**Date:** 2026-08-15
**Status:** Approved
**Scope:** Qibla screen (`prayer_feature/qibla`) + sensor pipeline (`prayer_qibla`)
**Goal:** Balanced pass across visuals, performance, and accuracy. Redesign the Qibla screen (Classic Brass compass, hero layout, accuracy ring + badge, aligned state), fix the sensor pipeline (gyro fusion complementary filter, log cleanup, lifecycle fix), and make the angle display semantically correct.

## Background

The Qibla screen (`QiblaScreen.kt`, `QiblaCompass.kt`, `QiblaInfoSection.kt`) is a basic 60/40 vertical split: a Canvas-drawn compass on top, an info section in a `TopContainer` below. The sensor pipeline (`SensorService` → `OrientationProvider` → `QiblaDataStoreImp` → `QiblaViewModel`) reads accelerometer + magnetometer at `SENSOR_DELAY_UI` and emits a rotation matrix on every sensor change.

## Pain Points

1. **Visual** — the "Kaaba" icon is a white-square placeholder (`ic_kaaba.xml`); the compass dial is plain; no accuracy/calibration feedback beyond a text line; the info section shows a misleading "Kıble Açısı ... Kuzey" label; all strings are hardcoded Turkish.
2. **Performance** — `QiblaDataStoreImp` calls `Log.e` on **every** sensor emission (~60Hz error-level logging); the compass dial (36 tick lines + native text) is redrawn on every sensor frame; `Modifier.rotate` forces full redraw instead of GPU layer transform; the compass is started/stopped twice (inside `channelFlow` and via the use case's `stop()`).
3. **Accuracy** — no sensor smoothing, so the compass jitters; no gyroscope fusion, so orientation is noisy and less robust under tilt; `SENSOR_DELAY_UI` is slower than needed.

## Approach

**Approach 3 — Full pipeline rework.** Screen redesign + a quaternion-based complementary filter (gyroscope fused with accelerometer + magnetometer) extracted into a pure, unit-testable class. This addresses jitter at the source and gives robust orientation under tilt, at the cost of touching the `prayer_qibla` module and its tests.

## Design

### 1. Visual — Qibla Screen

**`QiblaCompass.kt`** — Classic Brass:
- Warm parchment background, brass/gold bezel, red North marker, gold Qibla arrow.
- Proper **Kaaba vector drawable** (black cube + gold band) replacing the white-square placeholder.
- **Accuracy ring** on the rim: dashed red (low, animating) / amber (medium) / glowing green (high).
- **Status badge** under the compass: "Kalibrasyon Gerekli" / "Orta Doğruluk" / "Yüksek Doğruluk".
- Alignment feedback: arrow turns green + scale pulse + vibration (kept) when facing Qibla.

**`QiblaScreen.kt`** — Hero layout:
- Compass dominates the screen (~78%), compact info card floats at the bottom.
- Location name shown under the compass.

**`QiblaInfoSection.kt`** — fixed semantics:
- **Konum** — location name.
- **Yön** — `qiblaBearing` from North (e.g. "150° Kuzey").
- **Kıbleye Uzaklık** — signed `qiblaAngle` with direction hint (e.g. "12° sağa dön" / "12° sola dön").
- **Ölçüm** — accuracy text.
- **Aligned state**: when `|qiblaAngle| < ALIGNMENT_THRESHOLD` (single shared constant, `10°`, reused by the compass arrow color/vibration and the info card), show a prominent green "Kıbleye Dönük!" banner with the Kaaba icon.
- All hardcoded Turkish strings moved to `strings.xml`.

### 2. Performance

- Draw the dial **once** and rotate via `Modifier.graphicsLayer { rotationZ = -deviceAzimuth }` — GPU-accelerated, no per-frame canvas redraw.
- Rotate the Qibla arrow via `graphicsLayer` instead of `Modifier.rotate`.
- Accuracy ring animates only on accuracy *state changes*, not per sensor frame.
- Remove the `Log.e` on every sensor emission in `QiblaDataStoreImp`.
- Fix the double start/stop of the compass — make start/stop balanced and idempotent.
- Use `SENSOR_DELAY_GAME` (~20ms) instead of `SENSOR_DELAY_UI`.
- Keep the `flatMapLatest` + `collectLatest` ViewModel structure (already correct).

### 3. Accuracy — gyro fusion

**New pure class `OrientationFusion`** (in `prayer_qibla`, no Android deps — unit-testable):
- Maintains a **quaternion** representing device orientation.
- `updateWithGyro(gx, gy, gz, dt)` — integrates gyroscope angular velocity (fast, smooth).
- `correctWithRotationMatrix(matrix)` — fuses with the accelerometer+magnetometer reference via slerp (slow, drift-free).
- `toRotationMatrix()` / `reset()` — converts back for azimuth extraction.
- Tunable gains: ~98% gyro / ~2% reference.

**`SensorService`** — register the **gyroscope** alongside accel+mag; extend `RawSensorState` to carry optional gyro values + timestamp (so `dt` can be computed).

**`OrientationProvider`** — feeds both inputs into `OrientationFusion`:
- Gyro sample → `updateWithGyro(...)`.
- Accel+mag sample → `correctWithRotationMatrix(...)`.
- Azimuth extracted from the fused quaternion (converted to matrix → existing remap for screen rotation → `getOrientation`) — preserves the tested remap logic.
- Bearing + `qiblaAngle` math unchanged.

### 4. Tests

- `OrientationFusionTest` — synthetic sensor data: gyro-only integration, drift correction toward reference, tilt handling, reset.
- Adapt `OrientationProviderTest` for the fusion-based flow.
- Keep `QiblaDataStoreImpTest` + `QiblaViewModelTest` green.

## Files Touched

| File | Change |
|------|--------|
| `prayer_feature/qibla/.../QiblaScreen.kt` | Hero layout, accuracy ring + badge wiring |
| `prayer_feature/qibla/.../components/QiblaCompass.kt` | Classic Brass visuals, graphicsLayer rotation, accuracy ring |
| `prayer_feature/qibla/.../components/QiblaInfoSection.kt` | Fixed semantics, aligned state, string resources |
| `prayer_feature/qibla/.../res/drawable/ic_kaaba.xml` | Proper Kaaba vector drawable |
| `prayer_feature/qibla/.../res/values*/strings.xml` | All UI strings |
| `prayer_qibla/.../OrientationFusion.kt` | **New** — pure quaternion complementary filter |
| `prayer_qibla/.../SensorService.kt` | Register gyroscope, emit gyro + timestamp |
| `prayer_qibla/.../RawSensorState.kt` | Carry optional gyro + timestamp |
| `prayer_qibla/.../OrientationProvider.kt` | Feed fusion, extract azimuth from fused quaternion |
| `prayer_qibla/.../QiblaDataStoreImp.kt` | Remove Log.e spam, fix lifecycle |
| `prayer_qibla/.../test/OrientationFusionTest.kt` | **New** — filter unit tests |
| `prayer_qibla/.../test/OrientationProviderTest.kt` | Adapt to fusion flow |

## Out of Scope

- Complementary filter beyond the simple gyro+accel+mag slerp fusion (no full Madgwick/Mahony AHRS).
- Changes to other features or the shared `TopContainer`.
- Dynamic color / theme changes outside the Qibla screen.
