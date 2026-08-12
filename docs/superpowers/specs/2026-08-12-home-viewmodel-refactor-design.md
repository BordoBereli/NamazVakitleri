# HomeViewModel Refactor Design

- **Date**: 2026-08-12
- **Status**: Approved for implementation
- **Scope**: `prayer_feature/home` — refactor of `HomeViewModel` for performance, maintainability, and testability

## Problem Statement

`HomeViewModel.kt` is 379 lines with 10 injected dependencies, mixing five distinct concerns:

1. Location lifecycle (initial load, observe location changes, observe settings changes, GPS fallback, update location, country-code mapping)
2. Prayer times loading and current/next calculation
3. Per-second countdown loop
4. Quran verse loading with retry backoff
5. Error message mapping

This creates three coupled performance issues:

- `timeRemaining` lives inside `PrayerUiState` (`HomeUiStates.kt:31`). Every second `updateCountdown` copies `PrayerUiState`, forcing the `DailyPrayers` grid to recompose every second.
- `HomeTopContainer` has `LaunchedEffect(prayerState) { onStartCount() }` (`HomeTopContainer.kt:55`). Since `prayerState` is a fresh object each tick, the countdown job is cancelled and restarted every second — a self-defeating loop.
- Each tick copies the whole `HomeUiState.Success`, causing broad recomposition.

The 10-mock `HomeViewModelTest` is also painful to maintain.

## Goals

- Keep the `HomeUiState` / `HomeScreen` / `HomeEvent` / navigation contract intact.
- Decompose the ViewModel internals into per-concern components, each independently unit-testable.
- Isolate ticking data so only the clock and countdown text recompose per second.
- Eliminate the countdown self-restart loop.

## Chosen Approach

**Option 2 contract + Option 1 logic decomposition:**

- HomeViewModel publishes per-concern `StateFlow`s.
- HomeRoute collects them and merges into a single `HomeUiState` via a pure function.
- Logic moves into four plain component classes (Koin `@Factory`, picked up by the existing `@ComponentScan("com.kutluoglu.prayer_feature.home**")`).

## Architecture

### Flows published by HomeViewModel

| Flow | Contents | Changes when |
|------|----------|--------------|
| `screenGate` | `Loading` / `Error(message)` / `Ready` | refresh starts & finishes |
| `locationState` | `LocationUiState` (data + info text) | location resolves |
| `timeState` | `TimeUiState` (hijri/gregorian strings) | location changes |
| `prayerState` | `PrayerUiState` minus `timeRemaining` (prayers, current, next) | prayer load |
| `countdownState` | `CountdownUiState` = `timeRemaining` + `currentTime` | every 1s |
| `quranState` | `QuranUiState` = verse + `isSheetVisible` | verse load / sheet toggle |
| `promptState` | `showLocationUpdatePrompt: Boolean` | GPS drift detected |

### Components

#### 1. `LocationCoordinator`

- Deps: `SettingsRepository`, `GetSavedLocationUseCase`, `SaveLocationUseCase`, `ObserveLocationUseCase`, `LocationService`
- Emits: `locationFlow: Flow<LocationData>`, `promptFlow: Flow<Boolean>`
- Suspend: `resolveInitial()`, `resolveSavedAndDetectDrift()`, `refreshFromGps()`
- Contains today's: `setLocationDataFrom`, `getCountryCode`, `fallbackToGps`, `updateLocationChange`, `isDifferentThen` logic, plus the GPS-drift prompt orchestration.

#### 2. `PrayerTimesLoader`

- Deps: `GetPrayerTimesUseCase`, `PrayerLogicEngine`, `PrayerFormatter`
- Suspend: `load(location): LoadedPayload` where `LoadedPayload = prayerState (no timeRemaining) + static timeState + locationUiState`
- Contains today's: `processLocationForPrayerTimes`, `updatePrayerState`. Pure data transformation — no loops, no lifecycle.

#### 3. `CountdownEngine`

- Deps: `PrayerLogicEngine`, `PrayerFormatter`
- Emits: `countdownState: StateFlow<CountdownUiState>`, `dayChangedSignal: Flow<Unit>`
- Functions: `start(prayerState, timeState, location)`, `stop()`
- Contains today's: `startPrayerCountdown`, `updateCountdown`, `isDayChanged`.

#### 4. `QuranVerseLoader`

- Deps: `GetRandomVerseUseCase`, `LanguageProvider`
- Emits: `quranState: StateFlow<QuranUiState>`
- Functions: `loadVerse()`, `setSheetVisible(Boolean)`
- Contains today's: `loadRandomVerse`, `setVerseSheetVisibility`.

### Refactored ViewModel

```kotlin
@KoinViewModel
class HomeViewModel(
    private val locationCoordinator: LocationCoordinator,
    private val prayerTimesLoader: PrayerTimesLoader,
    private val countdownEngine: CountdownEngine,
    private val quranVerseLoader: QuranVerseLoader
) : ViewModel()
```

- 4 deps (down from 10), ~7 flows exposed, ~60-80 lines of orchestration.
- `onEvent` maps directly to component methods; the three live observers (init) are wired to call `prayerTimesLoader.load(...)` via `locationCoordinator.locationFlow`.
- `onCleared`: stops countdown + cancels location, settings, day-change observers.

## Data Structure Changes

```kotlin
data class CountdownUiState(
    val timeRemaining: String = "--:--:--",
    val currentTime: String = ""
)

data class QuranUiState(
    val verse: AyahData? = null,
    val isSheetVisible: Boolean = false
)
```

- `HomeUiState.Success` gains a `countdownState: CountdownUiState = CountdownUiState()` slot. (Kotlin default value keeps callers compiling.)
- `PrayerUiState` **removes** `timeRemaining` (it moves to `CountdownUiState`).

## Merge Function

`mergeToHomeUiState` is a top-level pure function in the home package with its own test file:

```kotlin
fun mergeToHomeUiState(
    gate: HomeScreenGate,          // Loading / Error(message) / Ready
    location: LocationUiState,
    time: TimeUiState,
    prayer: PrayerUiState,
    countdown: CountdownUiState,
    quran: QuranUiState,
    prompt: Boolean
): HomeUiState
```

- `gate == Loading` → `HomeUiState.Loading`
- `gate == Error` → `HomeUiState.Error(message)`
- `gate == Ready` → `HomeUiState.Success(..., countdownState = countdown)` where the `PrayerUiState` instance is passed through un-copied on ticks (critical for the recomposition fix).

## HomeRoute Wiring

```kotlin
@Composable
fun HomeRoute(viewModel: HomeViewModel = koinViewModel(), verseFormatter: QuranVerseFormatter = koinInject(), navController: NavController) {
    val gate by viewModel.screenGate.collectAsState()
    val location by viewModel.locationState.collectAsState()
    val time by viewModel.timeState.collectAsState()
    val prayer by viewModel.prayerState.collectAsState()
    val countdown by viewModel.countdownState.collectAsState()
    val quran by viewModel.quranState.collectAsState()
    val prompt by viewModel.promptState.collectAsState()

    val uiState = remember(gate, location, time, prayer, countdown, quran, prompt) {
        mergeToHomeUiState(gate, location, time, prayer, countdown, quran, prompt)
    }
    HomeScreen(navController, uiState, verseFormatter) { viewModel.onEvent(it) }
}
```

## UI Changes

`HomeScreen`, `HomeEvent`, navigation: **unchanged**.

`HomeTopContainer` (contained change):
- `NextPrayerInfo` reads `countdownState.timeRemaining` instead of `prayerState.timeRemaining`.
- Time display reads `countdownState.currentTime` instead of `timeState.currentTime`.
- `LaunchedEffect(prayerState) { onStartCount() }` key changes to `countdownState` (or an explicit start event from the ViewModel) — eliminates the second-by-second restart.

## Error Handling

- `screenGate` (Loading/Error/Ready) replaces the `HomeUiState.Loading`/`Error` arms for refresh gating, so sub-flows stay populated during partial failures (today a failure nulls the whole Success).
- `getUserFriendlyErrorMessage` logic preserved (moves into the merger file or a small `HomeErrorMapper`). Same messages, same behavior.
- `dayChangedSignal` from `CountdownEngine` → ViewModel re-runs `prayerTimesLoader.load(...)`.
- `onCleared`: stops countdown + cancels observers.

## Test Plan

| Test file | Covers |
|-----------|--------|
| `LocationCoordinatorTest` | settings→saved→GPS precedence, drift prompt, error mapping |
| `PrayerTimesLoaderTest` | success/error, localized names, `isCurrent` flag, current/next calc |
| `CountdownEngineTest` | tick loop w/ `UnconfinedTestDispatcher`, day-change signal, stop/cancel |
| `QuranVerseLoaderTest` | verse success/failure, retry backoff, sheet visibility |
| `HomeUiStateMergerTest` | each gate → correct `HomeUiState` arm; prayerState instance reused on tick |
| `HomeViewModelTest` | rewritten, slimmer — 4 mocks, orchestration-only assertions |

The existing 10-mock `HomeViewModelTest` `setUp` (including the `spyk` on `Prayer.copy`) is retired.

## Impact Assessment

- Risk: **LOW** upstream — only `HomeRoute.kt` + `HomeViewModelTest.kt` reference `HomeViewModel` directly (`MainAppScreen` is a depth-2 import of `HomeRoute`).
- `HomeScreen`, `HomeEvent`, navigation, and other features (`PrayerTimesViewModel`, `SettingsViewModel`, etc.) are unaffected.

## Out of Scope

- No behavior changes to prayer calculation, localization, or error messages.
- No changes to `TimeUiState`/`LocationUiState` common states.
- No navigation or screen-layout changes.