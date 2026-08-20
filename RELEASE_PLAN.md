# Release Plan ToDo List — NamazVakitleri → Play Store

Status: `[ ]` = pending, `[x]` = done, `[~]` = in progress.
Last updated: 2026-08-20

Execution order: **Analytics & Crashlytics first**, then release hardening, then upload.

---

## Phase 0 — Analytics & Crashlytics Foundation (FIRST)

- [x] **0.1 Add Firebase deps**
  - `google-services` plugin + `firebase-crashlytics` + `firebase-analytics` in `app/build.gradle.kts`
  - `google-services.json` added (project: namaz-vakitleri, package: com.kutluoglu.namazvakitleri)
- [x] **0.2 Create `AnalyticsTracker` abstraction**
  - Interface + event/param/user-property constants in `:core:common` (`com.kutluoglu.core.common.analytics`)
  - `FirebaseAnalyticsTracker` in `:app`, bound via Koin `@Single(binds = [AnalyticsTracker::class])`
- [x] **0.3 Screen view tracking**
  - `LaunchedEffect` on current route in `MainAppScreen.kt` → `screen_view` for all tabs + settings sub-screens
- [x] **0.4 Core event tracking**
  - Wired into HomeViewModel, PrayerTimesViewModel, QiblaViewModel, SettingsViewModel, CalculationMethodViewModel, LanguageSelectionViewModel, HijriAdjustmentViewModel, MyLocationsViewModel, LocationSelectionViewModel + permission tracking in LocationSelectionScreen
- [x] **0.5 User properties**
  - `AnalyticsUserPropertiesManager` observes settings + locations, sets language/calculation method/hijri adjustment/location count/gps enabled/active location type
- [x] **0.6 Crashlytics init**
  - Enabled in `NamazVakitleriApplication.onCreate` + proguard keep rules added
- [~] **0.7 Verify analytics**
  - Unit tests green (tracker mocked) ✅ — pending: run app on device, confirm events in Firebase DebugView

### Analytics Event List (full design)

**Screen Views (navigation)**
| Event | Params |
|---|---|
| `screen_view` | `screen_name`: home / prayer_times / qibla / settings / calculation_method / language / hijri_adjustment / my_locations / location_selection / map |

**Home (core engagement)**
| Event | Params |
|---|---|
| `home_loaded` | `location_count`, `active_location_type` (gps/manual), `calculation_method` |
| `location_switched` | `location_id`, `location_type` |
| `pull_to_refresh` | — |
| `quran_verse_loaded` | `success`, `language`, `translation` |
| `quran_verse_opened` | `surah`, `ayah` |
| `quran_verse_dismissed` | — |

**Prayer Times**
| Event | Params |
|---|---|
| `month_navigated` | `direction` (prev/next), `target_month` |
| `today_pressed` | — |
| `prayer_times_error` | `reason` |

**Qibla**
| Event | Params |
|---|---|
| `qibla_opened` | — |
| `qibla_compass_started` / `stopped` | — |
| `qibla_aligned` | `degrees_off` (key success metric) |

**Settings & Configuration**
| Event | Params |
|---|---|
| `calculation_method_changed` | `from`, `to` |
| `language_changed` | `from`, `to` |
| `hijri_adjustment_changed` | `value` |
| `cache_cleared` | — |

**Location Management (funnel — most important for growth)**
| Event | Params |
|---|---|
| `location_selection_opened` | `tab` (search/map) |
| `location_search` | `query`, `result_count` |
| `location_selected` | `country`, `city`, `province`, `district` |
| `location_added` | `source` (search/map/gps) |
| `location_removed` | — |
| `location_reordered` | — |
| `gps_toggled` | `enabled` |
| `map_location_confirmed` | — |
| `use_my_location` | — |

**Permission Funnel (critical for GPS features)**
| Event | Params |
|---|---|
| `permission_requested` | `permission` (fine/coarse) |
| `permission_granted` / `permission_denied` | `permission`, `is_permanent_denial` |

**Errors & Performance**
| Event | Params |
|---|---|
| `prayer_times_load_error` | `reason` |
| `quran_verse_load_error` | `reason` |
| `location_search_error` | `reason` |
| `network_error` | `endpoint` |
| Crashlytics: `app_crash`, `ANR` | stack trace, non-fatal logs |

**User Properties (set once / on change)**
`language`, `calculation_method`, `hijri_adjustment`, `location_count`, `gps_enabled`, `active_location_type`, `app_version`, `install_source`

**Session / Retention**
`session_start`, `session_end` (Firebase auto-tracks), `app_foreground`/`app_background` → DAU/WAU/MAU, retention cohorts.

### Key Funnels to build
1. **Location add funnel:** `location_selection_opened` → `location_search` → `location_selected` → `location_added`
2. **GPS permission funnel:** `permission_requested` → `permission_granted` (measure denial rate)
3. **Qibla success:** `qibla_opened` → `qibla_aligned`
4. **Retention:** D1/D7/D30 via session events

---

## Phase 1 — 🔴 Critical (blocking)

- [x] **1.1 Revert broken package change** — `QuranDataSource.kt` restored to `com.kutluoglu.prayer_remote.quran` (done by user)
- [ ] **1.2 Wire release signing** — `signingConfigs` in `app/build.gradle.kts` using existing keystore (`keystore.properties`, gitignored)
- [ ] **1.3 Set versioning** — `versionCode = 1`, `versionName = "1.0.0"`
- [ ] **1.4 Verify build** — `./gradlew bundleRelease` → signed `.aab`
- [ ] **1.5 Push + merge** — push 19 local commits, merge `dev/feature/*` branches into `main`

## Phase 2 — 🟠 High (before upload)

- [ ] **2.1 Enable R8** — `isMinifyEnabled = true` + `isShrinkResources = true`; proguard rules for kotlinx.serialization, Koin, osmdroid, coil, okhttp, Firebase
- [ ] **2.2 Custom app icon** — replace default Android placeholder with branded launcher icon
- [ ] **2.3 Resolve hardcoded `darkTheme = true`** — `MainActivity.kt:27` (fix or document)
- [ ] **2.4 Play Store assets** — feature graphic (1024×500), screenshots, 512×512 icon, privacy policy URL, data-safety declaration (location + network usage)

## Phase 3 — 🟡 Quality gates

- [ ] **3.1 Full test suite** — `./gradlew allTests`, fix failures (67 test files)
- [ ] **3.2 Housekeeping** — TODO.md items (dup `getCountryCode`, legacy DataStores, stale `NamazVakitleriTechnicalAnalysis`), README placeholder image/module list
- [ ] **3.3 Pre-commit check** — `gitnexus_detect_changes()` per AGENTS.md

## Phase 4 — 🚀 Release

- [ ] **4.1 Upload AAB** to Play Console → Internal testing track → add testers → validate → promote to production when ready

---

## Required from user
- `google-services.json` (Phase 0)
- Keystore path + credentials (Phase 1)
- Play Store listing copy/screenshots (Phase 2)
