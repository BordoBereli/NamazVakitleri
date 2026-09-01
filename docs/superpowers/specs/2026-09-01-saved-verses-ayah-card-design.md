# Saved Verses — Ayah Card Distinction Design

**Status:** Approved 2026-09-01
**Branch:** none yet (off `main`)

## Problem

On the Saved Verses screen the surah header card and the ayah card look almost identical — same
full-width card shape, same 12dp rounded corners, same row layout with icons on the right — and
differ only by container color (`surfaceContainerHigh` vs `surfaceVariant`). They read as siblings
rather than header/content, so the ayah text feels like an afterthought instead of the focal point.
Neither card shows its own index: the surah header shows only the localized name, and the ayah row
shows the reference as a small `(Surah - n:m)` caption under the text.

## Goal

Make the ayah card visually distinct from the surah header and give each element its own index:

1. Surah header shows its surah number as a filled circle badge before the name.
2. Ayah card becomes an elevated white card with a primary accent bar and a soft circle medallion
   showing the ayah number.
3. Ayah text is centered and larger — the hero of the card.
4. Remove the redundant `(Surah - n:m)` caption (surah context is in the header, ayah number in the
   medallion).

## Scope

- **In scope:** `SavedVersesScreen.kt` — `SurahHeader` and `VerseRow` composables only.
- **Out of scope:** `VerseDetailSheetContent` (detail bottom sheet), search behavior, reorder
  behavior, swipe-to-delete, `SavedVersesViewModel` / state / events, data layer, other screens.

## Decisions

1. **Surah header keeps its structure; add a filled circle index badge.** The header stays a filled
   `surfaceContainerHigh` card (chevron, localized name, verse count, drag handle) and gains a
   filled circle badge (primary bg, onPrimary text) with the surah number before the name.
2. **Ayah card becomes an elevated white card with a primary accent bar.** Container switches from
   `surfaceVariant` to `surface`, gains ~2dp elevation and a 4dp primary accent bar on the start
   edge. This is the primary visual break from the header.
3. **Ayah number as a soft circle medallion, top-left.** `primaryContainer` background with primary
   text, placed in a top row alongside share + drag handle on the right.
4. **Ayah text centered and larger.** `titleMedium`, `TextAlign.Center`, no 2-line clamp (RTL
   Arabic reads naturally centered). Removes the `(Surah - n:m)` caption entirely.
5. **No behavior change.** Tap-to-select, share, drag, swipe-to-delete, and reorder all keep their
   existing handlers and event wiring.

## Target UI

### `SurahHeader` (modified)

```
[▼] (33) Al-Ahzab                    2  ≡
```

- `(33)` = filled circle badge (primary bg, onPrimary text), surah number.
- Everything else unchanged: chevron, localized name (`titleMedium`, primary), verse count, drag
  handle.

### `VerseRow` (redesigned)

```
┌────────────────────────────────────────┐
│ (56)                        ⤴  ≡       │   ← medallion top-left, actions top-right
│                                        │
│   إِنَّ اللَّهَ وَمَلَائِكَتَهُ         │   ← centered, titleMedium, no clamp
│   يُصَلُّونَ عَلَى النَّبِيِّ          │
└────────────────────────────────────────┘
```

- Card: `surface` container, 12dp rounded corners, ~2dp elevation, 4dp primary accent bar on the
  start edge.
- Top row: soft circle medallion (`primaryContainer` bg, primary text) with the ayah number;
  share + drag handle on the right.
- Body: ayah text centered (`TextAlign.Center`), `titleMedium`, no `maxLines` clamp.
- The `(Surah - n:m)` caption is removed.

## Error Handling

None new — this is a pure presentation change. No data, network, or persistence paths are touched.

## Testing

1. Update `SavedVersesScreenTest` if it asserts on the removed `(Surah - n:m)` caption or the old
   `surfaceVariant` container.
2. Existing tests (reorder, swipe-to-delete, search, wipe, end-to-end) must continue to pass —
   they assert on behavior, not container styling.
3. Full regression: `./gradlew assembleDebug testDebugUnitTest`;
   `gitnexus_detect_changes()` shows only `SurahHeader` / `VerseRow` in `SavedVersesScreen.kt`.

## Acceptance Criteria

- Surah header shows its surah number in a filled circle badge before the name.
- Ayah card is visually distinct from the header: elevated white card, primary accent bar, soft
  circle medallion with the ayah number, centered larger text.
- The `(Surah - n:m)` caption is gone; surah context comes from the header, ayah number from the
  medallion.
- All existing Saved Verses behavior (select, share, drag reorder, swipe-to-delete, search,
  collapse) is unchanged and all tests pass.
