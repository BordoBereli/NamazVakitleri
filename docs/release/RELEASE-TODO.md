# Release ToDo List — NamazVakitleri → Play Store v2.0.1

Status: `[ ]` = pending, `[x]` = done, `[~]` = in progress.
Last updated: 2026-10-14

Execution order: **signing & build first**, then store assets, then upload.

Current release target: **v2.0.1 (versionCode 201)** — update to a live listing.
This release adds: Wear OS support, Android Auto support, full app redesign,
offline prayer times, adaptive launcher icon, and numerous fixes.

---

## 🔴 Now (blocking)

- [ ] **1. Find & provide keystore path** + credentials → create `keystore.properties` locally (gitignored)

## 🟠 After keystore

- [ ] **2. Enable `isShrinkResources = true`** in the release build type
- [ ] **3. Build signed AAB** — `./gradlew bundleRelease` (clean, verify signature + versionCode 201)
- [ ] **4. Commit** version bump + release notes + this plan (GitNexus `detect_changes` first)

## 🟡 Play Store assets (before upload)

- [x] **5. Hosted privacy policy URL** — https://bordobereli.github.io/NamazVakitleri/privacy-policy.html (verified live 2026-10-14, bilingual EN/TR, last updated 13 Sep 2026)
- [x] **6. Feature graphic 1024×500** + **512×512 icon** — at `/Users/a195143/2026Tasarım/prayer-icons/store/` (verified 2026-10-14); emerald set recommended (matches app icon): `feature-graphic-emerald-en.png` (1024×500) + `icon-emerald-512-fullbleed.png` (512×512); TR variants + purple theme also available
- [x] **7. Screenshots** — 4 phone screenshots captured from SM-S948B (release build) at `/Users/a195143/2026Tasarım/prayer-icons/store/screenshots/`: home (live countdown + mosque photo), prayer times (monthly program), qibla (compass), settings; Wear screenshots: no watch device connected — skip or capture later if listing the watch app
- [ ] **8. Bilingual listing copy** — short + full descriptions (TR/EN)
- [ ] **9. Data-safety declaration** — console form: precise location, network, analytics/crashlytics collection

## 🟢 Optional quality gates

- [ ] **10. Run `./gradlew allTests`** (instrumented/suites) before upload
- [ ] **11. Firebase DebugView verification** — run app on device, confirm analytics events
- [ ] **12. Housekeeping** — TODO.md items, README cleanup

## 🚀 Upload

- [ ] **13. Upload AAB → Internal testing track** → smoke test on device → promote to production
- [ ] **14. Post-release** — monitor Crashlytics, verify real-user analytics

---

## Already completed this release ✅

- [x] Version bump → 2.0.1 (versionCode 201) in `gradle/libs.versions.toml`
- [x] Release notes (TR + EN) at `docs/release/RELEASE-NOTES-2.0.1.md`
- [x] Full unit test suite green across all modules incl. `:wear` (`testDebugUnitTest`, 2026-10-14)
- [x] Release signing config wired (`signingConfigs` + graceful `keystore.properties` gating)
- [x] Adaptive launcher icons + Wear tile artwork
- [x] Theme now derived from `settings.themeMode` (was hardcoded `darkTheme = true`)
- [x] Release plan gap analysis completed (2026-10-14)
- [x] Earlier release foundations: Firebase Analytics + Crashlytics, R8 minify + proguard rules, privacy policy content (`PRIVACY_POLICY.md`, `docs/privacy-policy.html`)

## Completed from the original release plan (Phase 0–1)

- [x] **Firebase deps** — `google-services` plugin + `firebase-crashlytics` + `firebase-analytics` in `app/build.gradle.kts`; `google-services.json` added (project: namaz-vakitleri)
- [x] **`AnalyticsTracker` abstraction** — interface + event/param/user-property constants in `:core:common`; `FirebaseAnalyticsTracker` in `:app`, bound via Koin
- [x] **Screen view tracking** — `LaunchedEffect` on current route in `MainAppScreen.kt` → `screen_view` for all tabs + settings sub-screens
- [x] **Core event tracking** — HomeViewModel, PrayerTimesViewModel, QiblaViewModel, SettingsViewModel, CalculationMethodViewModel, LanguageSelectionViewModel, HijriAdjustmentViewModel, MyLocationsViewModel, LocationSelectionViewModel + permission tracking
- [x] **User properties** — `AnalyticsUserPropertiesManager` observes settings + locations (language/calc method/hijri adjustment/location count/gps/active location type)
- [x] **Crashlytics init** — enabled in `NamazVakitleriApplication.onCreate` + proguard keep rules
- [x] **Revert broken package change** — `QuranDataSource.kt` restored to `com.kutluoglu.prayer_remote.quran`
- [x] **Push + merge** — only `main` branch remains, working tree nearly clean

## Completed since the v1.0 planning (covered by this release)

- [x] Wear OS module (`:wear`) — tiles, tests
- [x] Android Auto (`:prayer_auto`) — prayer times, nearby mosques + Maps handoff
- [x] Offline prayer times (cache-first data layer)
- [x] Full app redesign + adaptive icons
- [x] Bilingual release notes (TR/EN)

---

## Required from user

- **Keystore path + credentials** ← current blocker
- Play Console access for upload + data-safety form
- Decide who produces screenshots/feature graphic (device captures vs. supplied art)

---

## Appendix — Analytics Event List (full design)

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

### Key Funnels
1. **Location add funnel:** `location_selection_opened` → `location_search` → `location_selected` → `location_added`
2. **GPS permission funnel:** `permission_requested` → `permission_granted` (measure denial rate)
3. **Qibla success:** `qibla_opened` → `qibla_aligned`
4. **Retention:** D1/D7/D30 via session events
