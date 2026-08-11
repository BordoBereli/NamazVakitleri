# NamazVakitleri — TODO List

Persistent task list for the NamazVakitleri Android project.
Status: `[ ]` = pending, `[x]` = done, `[~]` = in progress.

Last updated: 2026-08-11

---

## 🔴 Bugs (fix first)

- [x] **1. Fix `prayer_qibla` test failure**
  - File: `prayer_qibla/build.gradle.kts`
  - Issue: missing `testRuntimeOnly(libs.junit.platform.launcher)` → "OutputDirectoryProvider not available" (unaligned junit-platform-engine/launcher).
  - Same fix already applied to `prayer_settings` (see CHANGE_SUMMARY.md).
  - Status: DONE 2026-08-11 — added `testRuntimeOnly(libs.junit.platform.launcher)`; `:prayer_qibla:testDebugUnitTest` passes.

- [x] **2. Settings sub-screens don't persist changes**
  - File: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/SettingsGraph.kt`
  - Issue: hardcoded placeholders (`currentMethod = ""`, `currentAdjustment = 0`, `currentLanguage = ""`); `onMethodSelected`/`onLanguageSelected`/`onAdjustmentSelected` callbacks only `popBackStack()` without calling `UpdateCalculationMethodUseCase`/`UpdateLanguageUseCase`/`UpdateHijriAdjustmentUseCase`.
  - The ViewModels (`CalculationMethodViewModel`, `LanguageSelectionViewModel`, `HijriAdjustmentViewModel`) don't use the settings repository.
  - Status: DONE 2026-08-11 (TDD) — ViewModels now inject `GetSettingsUseCase` + update use case, load current value in `init` (pre-selection), persist on select/confirm. Routes drop hardcoded params; `HijriAdjustmentRoute` counter driven by ViewModel `currentAdjustment` StateFlow. `AppModule.kt` factory registrations updated. Tests rewritten (RED→GREEN): CalculationMethod 13, Language 6, Hijri 8 — all pass. Full suite green.

- [x] **3. `SettingsViewModel` registered as `single` but resolved via `koinViewModel()`**
  - File: `app/src/main/java/com/kutluoglu/namazvakitleri/AppModule.kt:43` + `prayer_feature/settings/.../SettingsScreen.kt:65`
  - Issue: should be `viewModel`/`@KoinViewModel`; verify no runtime crash.
  - Status: DONE 2026-08-11 (TDD) — verified via Koin source that `koinViewModel()` resolves any definition type (no crash), but `single` made it an app-wide singleton. Changed registration to `viewModel { }` (screen-scoped). Added `SettingsViewModelKoinTest` (app module) asserting two `koin.get()` calls return different instances — RED (failed with `single`) → GREEN (passes with `viewModel`). Full suite green.

- [x] **4. `PrayerLogicEngine.calculateTimeRemaining` uses `ZoneId.systemDefault()`**
  - File: `prayer/domain/src/main/java/com/kutluoglu/prayer/domain/PrayerLogicEngine.kt:50`
  - Issue: wrong countdown for non-local timezones; should use the prayer's zone.
  - Status: DONE 2026-08-11 (TDD) — `calculateTimeRemaining(nextPrayerTime, zoneId)` now takes the zone; `HomeViewModel.updateCountdown` passes the location-derived zone. Added `PrayerLogicEngineTest` (RED→GREEN: UTC vs Istanbul 3h offset). Full suite green.
  - Follow-up (impact check): `findCurrentPrayer` had the same `ZoneId.systemDefault()` bug — `findCurrentAndNextPrayer(prayers, zoneId)` now takes the zone too; `PrayerLogicEngine` injects a `Clock` (default `systemDefaultZone`, Koin skips it) for deterministic tests. Callers updated (`HomeViewModel.updatePrayerState`, `PrayerTimesViewModel`). Full suite green.

## 🟡 Incomplete implementations (TODO stubs)

- [x] **5. `PrayerDataStoreImp.getPrayerTimes()` throws `TODO("Not yet implemented")`**
  - File: `prayer/data/src/main/java/com/kutluoglu/prayer/data/source/prayer/PrayerDataStoreImp.kt:24`
  - Status: DONE 2026-08-11 (TDD) — `PrayerDataStoreImp` now injects `PrayerCalculationService` and computes prayer times (Turkey Diyanet / Standard). Wired `PrayerDataStore` into `PrayerRepository` (repository now delegates to the data store instead of calling the service directly). Added `PrayerDataStoreImpTest` (RED→GREEN); `PrayerRepositoryTest` updated to mock the data store. Full suite green.

- [ ] **6. `ClearPrayerTimesCacheUseCase` is a no-op placeholder**
  - File: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/prayer/ClearPrayerTimesCacheUseCase.kt`
  - Wired into `SettingsViewModel.clearCache()` but does nothing.

- [x] **7. `PrayerRepository` Room caching TODOs**
  - File: `prayer/data/src/main/java/com/kutluoglu/prayer/data/prayer/PrayerRepository.kt:23,37`
  - No Room DB exists; always recalculates. Implement caching or remove TODOs.
  - Status: DONE 2026-08-11 (TDD) — implemented a DataStore-backed prayer-times cache instead of Room (corporate proxy blocks new dependency downloads; DataStore was already cached). `PrayerTimesCache` (Preferences DataStore, JSON-serialized `CachedPrayer` DTO) + `PrayerDataStoreImp` now checks cache first, calculates on miss, and stores. `PrayerRepository` TODOs were already removed in item 5. Added `PrayerTimesCacheTest` + rewrote `PrayerDataStoreImpTest` (RED→GREEN). Koin graph verified. Full suite green.

- [ ] **8. `prayer_remote` module is empty**
  - File: `prayer_remote/src/main/java/com/kutluoglu/prayer_remote/MyClass.kt`
  - Not included in `app` dependencies. Implement or remove module.

## 🟠 Functional gaps

- [ ] **9. Qibla compass locked to portrait mode**
  - File: `prayer_feature/qibla/src/main/java/com/kutluoglu/prayer_feature/qibla/QiblaScreen.kt:41`
  - Forces `SCREEN_ORIENTATION_PORTRAIT`; landscape unsupported.

- [ ] **10. Monthly prayer times only for current month**
  - File: `prayer_feature/prayertimes/src/main/java/com/kutluoglu/prayer_feature/prayertimes/PrayerTimesViewModel.kt`
  - No month navigation.

- [ ] **11. Duplicated `getCountryCode` mapping**
  - Files: `prayer_feature/home/.../HomeViewModel.kt:135`, `prayer_settings/.../SettingsRepositoryImpl.kt:39`
  - Extract to shared util.

- [ ] **12. Legacy duplicate DataStores**
  - `prayer_cache/SettingsDataStoreImp.kt` + `LocationDataStoreImp.kt` (JSON-string based) vs `prayer_settings/data/local/SettingsDataStore.kt` (typed).
  - Two separate stores with different names; consolidate.

## ⚪ Housekeeping

- [ ] **13. Uncommitted changes**
  - `AGENTS.md` (gitnexus block) modified; `.claude/` + `CLAUDE.md` untracked. Commit or clean up.

- [ ] **14. `NamazVakitleriTechnicalAnalysis` is aspirational**
  - Describes Room, WorkManager, notifications, widgets — none implemented. Update or remove.

## 🔴 Found while verifying item 1

- [x] **15. `HomeViewModelTest.kt` doesn't compile**
  - File: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeViewModelTest.kt`
  - Line 16 imported package `com.kutluoglu.prayer_settings.domain.repository` instead of class `SettingsRepository`; `Settings` and `LocationSettings` (used at lines 75–76) had no imports.
  - Pre-existing; was masked because the full suite previously stopped at `prayer_qibla`.
  - Status: DONE 2026-08-11 — fixed line 16 import + added `Settings`/`LocationSettings` imports. `HomeViewModelTest` runs 4 tests, all pass; full `testDebugUnitTest` green.
