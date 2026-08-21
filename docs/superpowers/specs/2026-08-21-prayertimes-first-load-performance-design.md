# PrayerTimes First-Load Performance

## Problem

The cold first load of the PrayerTimes tab takes seconds. Traced root causes, in order of impact:

1. **Per-day cache write amplification** — On a cold month load each of ~30 daily
   computations calls `PrayerDataStoreImp.getPrayerTimes`, which on a miss does
   `prayerTimesCache.put(...)` **and** launches `preCacheTomorrow(...)` (another computation +
   another `put`) (`prayer/data/.../source/prayer/PrayerDataStoreImp.kt:59-60`). The cache is
   backed by Preferences DataStore where every `put` rewrites the **entire file**
   (`prayer/data/.../cache/PrayerTimesCache.kt:42`). DataStore serializes all edits through a
   single actor, so a cold month load performs ~60 sequential full-file rewrites whose cost
   grows as keys accumulate (O(n²)).
2. **Persistence blocks first paint** — After computing the month,
   `saveMonthlyPrayerTimesUseCase(...)` (one more full-file rewrite, the largest payload) is
   awaited **before** `emitSuccess(...)` (`prayer_feature/prayertimes/.../PrayerTimesViewModel.kt:249-257`).
   It also sits outside any `try/catch`: a failed write propagates out of `viewModelScope.launch`
   and can crash the app. The UI stays in `Loading` until every disk write finishes.
3. **All-or-nothing rendering** — Nothing is shown until all ~30 days are computed, even though
   the UI safely supports partial lists (LazyColumn keyed by `dayOfMonth`
   (`PrayerContainer.kt:275`); `PrayerListScrollController.onMonthChanged` guards
   `todayIndex !in 0 until itemCount` (`PrayerListScrollController.kt:21`)).
4. **Error flash before location resolves** — `ActiveLocationProvider.location` is a
   `StateFlow<LocationData?>` starting at `null`. The collector treats the initial `null`
   emission as fatal and immediately sets
   `PrayerTimesUiState.Error("Failed to get active location.")`
   (`PrayerTimesViewModel.kt:86-91`), which flashes before `LocationsCoordinator.resolveInitial()`
   populates the flow.

## Goals

1. Cold-load first meaningful paint happens **before any persistence work**.
2. A cold month load performs exactly **one** disk write (the month key): no per-day `put`s,
   no `preCacheTomorrow` churn during bulk loads.
3. Rows render progressively; **today is computed first** so the scroll-to-today effect can fire
   early.
4. While the active location is being resolved the screen stays in `Loading`; `Error` appears
   only after a timeout.

## Non-Goals

- Changing the Home screen single-day load path (it keeps per-day `put` + `preCacheTomorrow`;
  they are the right behavior there).
- Changing cache-first GPS resolution in `LocationsCoordinator` — already implemented
  (see `docs/superpowers/specs/2026-08-17-cache-first-gps-background-refresh-design.md`). The
  first-ever run with no cached GPS fix still blocks by design.
- Changing UI layouts, theming, or the `PrayerTimesUiState` shape visible to composables
  beyond what streaming requires (none — `Success.monthlyPrayers` just starts shorter).

## Approach

### 1. Emit before persisting (`PrayerTimesViewModel`)

In the cold path of `loadMonth`:

```
locationCache[month] = monthlyPrayers
emitSuccess(...)                                  // FIRST
backgroundSaveScope.launch {                      // THEN persist off the critical path
    runCatching { saveMonthlyPrayerTimesUseCase(...) }
        .onFailure { log analytics PRAYER_TIMES_ERROR reason="monthly_save_failed" }
}
```

- New constructor dependency: `backgroundSaveScope: CoroutineScope`, provided by Koin as an
  app-scoped `CoroutineScope(SupervisorJob() + Dispatchers.Default)` in
  `PrayerFeaturePrayerTimesModule` (mirrors `provideLocationRefreshScope()` in
  `PrayerLocationModule.kt:31-33`). App-scoped so the save survives navigating away from the
  tab mid-write.
- This also removes the latent crash: the save is now wrapped in `runCatching`.
- Cached and persisted-month paths are unchanged (they never wrote anything).

### 2. Single-write bulk mode (suppress per-day persistence)

Add an optional trailing parameter `persistDailyCache: Boolean = true` through the read chain:

- `GetPrayerTimesUseCase.invoke(...)` (`prayer/domain/.../GetPrayerTimesUseCase.kt`)
- `IPrayerRepository.getPrayerTimes(...)` (interface default keeps source compatibility)
- `PrayerRepository.getPrayerTimes(...)` (pass-through)
- `PrayerDataStore.getPrayerTimes(...)` (interface)
- `PrayerDataStoreImp.getPrayerTimes(...)`: when `false`, **skip** `prayerTimesCache.put(...)`
  **and** skip `preCacheTomorrow(...)`. Cache reads still happen (per-day entries written by
  Home are still honored inside a bulk load).

`PrayerTimesViewModel.computeDailyPrayer` passes `persistDailyCache = false`. The month is then
persisted exactly once via `SaveMonthlyPrayerTimesUseCase` (now backgrounded per §1).

### 3. Progressive streaming emission (`PrayerTimesViewModel`)

Cold path only (cached / persisted paths keep their atomic single emission):

- Launch today's computation first, then the remaining days in ascending order.
- Each completed day is stored into a shared result slot under a `Mutex`; after every
  completion, emit an intermediate `Success` containing the rows computed so far,
  **sorted ascending by `dayOfMonth`** (list order matters — LazyColumn renders in list order;
  keys dedupe during growth).
- Compute `timeState`/`locationState` payload once per `loadMonth` and reuse it for every
  partial emission (today, `formatter.getInitialTimeInfo` is not recomputed per emission).
- Every partial emission goes through the same staleness guard as `emitSuccess`
  (`month != selectedMonth() || locationId != activeLocationId → drop`), so mid-stream month /
  location switches behave exactly as today (`pendingMonth` machinery untouched).
- After all days complete, emit the final full `Success` (same content as today's behavior),
  then proceed with §1 (emit-before-persist ordering means the final emission precedes the
  background save).
- StateFlow conflation bounds the cost of up-to-N intermediate emissions; Compose already
  tolerates list growth (keyed items, guarded scroll).

### 4. Graceful null-location handling (`PrayerTimesViewModel`)

Replace the immediate `Error` on `null` in `loadMonthlyPrayerTimes()`'s collector:

```
if (location == null) {
    _uiState.value = PrayerTimesUiState.Loading          // stay loading, no flash
    locationTimeoutJob?.cancel()
    locationTimeoutJob = viewModelScope.launch {
        delay(LOCATION_RESOLUTION_TIMEOUT_MS)            // 15_000L constant
        _uiState.value = PrayerTimesUiState.Error("Failed to get active location.")
        analyticsTracker.logEvent(PRAYER_TIMES_ERROR, mapOf(REASON to "no_active_location"))
    }
} else {
    locationTimeoutJob?.cancel()
    loadForLocation(location)
}
```

- 15 s covers a slow first-ever cold GPS fix; normal resolutions (cache-first) arrive in ms and
  cancel the pending timeout.
- `locationTimeoutJob` is cancelled alongside `locationObservationJob` lifecycle (it is a child
  of `viewModelScope`; explicit cancel avoids a late Error overwriting a live Success after the
  collector was replaced).

## Data Flow (cold first load, after fix)

```
PrayerTimesRoute → loadMonthlyPrayerTimes()
  ├─ location == null → Loading (+ 15 s error timeout armed)
  ├─ location arrives (cache-first, usually instant) → cancel timeout → loadForLocation
  │     └─ loadMonth(currentMonth)
  │           ├─ settings read, in-memory miss, persisted-month miss
  │           ├─ compute today first, then rest in parallel (NO daily puts, NO precache)
  │           ├─ emit Success[today] … Success[growing sorted prefix] … Success[all days]
  │           └─ background: putMonth (single DataStore rewrite, failures logged not thrown)
  └─ still null after 15 s → Error (logged)
```

## Testing

- `PrayerTimesViewModelTest`:
  - cold load: `Success` is emitted **before** `saveMonthlyPrayerTimesUseCase` is invoked;
    save runs on the injected scope (advanceUntilIdle to flush).
  - save failure (throwing use case) does not change UI state and does not throw; error event
    logged.
  - `getPrayerTimesUseCase` receives `persistDailyCache = false` during a month load.
  - streaming: first `Success` emission contains today's row; final emission contains all days
    in ascending `dayOfMonth` order.
  - null location: state stays `Loading` (not `Error`) before timeout; becomes `Error` after
    advancing virtual time past 15 s; a later non-null cancels the pending error.
- `GetPrayerTimesUseCaseTest`: default `persistDailyCache = true` passes `true`; explicit
  `false` propagates to repository.
- `PrayerDataStoreImp` tests: with `persistDailyCache = false`, no `put` occurs and no
  precache job runs; cache hit path unaffected.
- Full suite must stay green: `./gradlew testDebugUnitTest`.

## Risks / Edge Cases

- **Up to N recompositions while streaming** — bounded by StateFlow conflation; LazyColumn is
  keyed by `dayOfMonth` and the scroll controller ignores missing indices, so growth is safe.
- **Header prayer-name row** renders from `monthlyPrayers.firstOrNull()?.prayers` — briefly
  empty until the first row lands; transient and consistent with the existing empty-list
  preview.
- **Background save lost on process death mid-write** — next cold load simply recomputes;
  benign by design.
- **Interface signature change** — default arguments keep all existing call sites and mocks
  source-compatible; relaxed MockK unaffected.
- **Stale stream emissions after month/location switch** — covered by the existing
  `selectedMonth()/activeLocationId` guard reused for partial emissions.
