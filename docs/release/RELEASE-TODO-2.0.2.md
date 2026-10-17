# Release ToDo List — NamazVakitleri → Play Store v2.0.2

Status: `[ ]` = pending, `[x]` = done, `[~]` = in progress.
Last updated: 2026-10-17

Current release target: **v2.0.2 (versionCode 202)** — update to a live listing (follow-up to 2.0.1).
Primary driver: **media-style adhan notification fix** for the Play foreground-service declaration.
Second driver: **Wear OS + Android Auto discoverability** — form factors added in the console (see below); plan `docs/superpowers/plans/2026-10-17-wear-auto-discoverability.md`, design `docs/superpowers/specs/2026-10-17-wear-auto-discoverability-design.md`.

---

## 🔴 Critical — media notification fix (blocks declaration video review)

- [ ] **1. Media-style adhan notification**
  - [x] GitNexus impact check on `PrayerNotificationManager.buildAdhanNotification` + `AdhanVolumeController` (both LOW risk; only implementer is PrayerNotificationManager)
  - [~] ~~Add `androidx.media:media` dependency (for `MediaStyle`) if not already present via media3~~ — **not needed**: `media3-session` already provides `MediaStyleNotificationHelper.MediaStyle`
  - [x] Expose `MediaSession` token from `AdhanVolumeController` (via `AdhanPlayer.activeMediaSession`)
  - [x] `buildAdhanNotification`: apply `MediaStyle` with session token, `setOngoing(true)`, `CATEGORY_TRANSPORT`, keep Stop action (now with `ic_notification_stop` icon for MediaStyle transport buttons)
  - [x] Unit test updates (Robolectric tests: media style token extraction, ongoing+transport flags, service re-post with session)
  - [x] `./gradlew testDebugUnitTest` green (module + Koin graph tests green)
  - [x] **Redesign follow-up (Task 1, commit `6f85224b`)**: Mute action added to media notification, tap-to-open content intent (opens app), and elapsed-time chronometer — see `PrayerNotificationManager`
  - [ ] **Wear OS surfacing**: no code needed — paired watch mirrors phone media sessions automatically; verify-on-device smoke test item (check adhan notification + controls appear on the watch when fired)
- [x] **2. Rebuild release AAB**
  - [x] Bump version → `2.0.2` / versionCode `202` in `gradle/libs.versions.toml`
  - [x] `./gradlew bundleRelease` → signed AAB (keystore.properties ready, unicode-escaped path)
  - [x] Verify signature + versionCode 202 (jarsigner "jar verified", CN=Fahri Kutluoğlu; versionCode 202 in manifest)
  - [x] Archive new `mapping.txt` → `docs/release/mapping-2.0.2/`
- [x] **3. Commit** (GitNexus `detect_changes` first, as usual) — committed `682ba906` (risk HIGH flagged: central seam, mitigated by single implementer + green tests)

## 🟠 Play Console — foreground service declaration

- [ ] **4. FSP declaration** — Policy → App content → Foreground service permissions
  - [ ] Select **Media playback** for `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - [ ] Justification text (ready to paste):
    > The app plays the adhan (call to prayer) audio at the user's scheduled prayer times. The adhan plays in the background via a foreground service with mediaPlayback type (ExoPlayer + MediaSession), with a media notification showing playback controls. The user schedules adhan alerts per prayer in the app's settings; playback must continue when the app is not in the foreground.
  - [ ] **Record demo video** — screen recording on SM-S948B: schedule adhan 1–2 min ahead → app to background → adhan fires with the (now media-style) notification visible → stop recording
  - [ ] Upload video + submit

## 🟡 Carry-over from 2.0.1 (unresolved console items)

- [ ] **5. Data-safety declaration** — answers pre-mapped in `docs/release/PLAY-LISTING-2.0.1.md`
- [ ] **6. Upload deobfuscation file** — release → Deobfuscation files → upload the 2.0.2 `mapping.txt` (replaces 2.0.1's; Crashlytics side is automatic)
- [ ] **7. Update listing** if not yet done — copy/assets from `docs/release/PLAY-LISTING-2.0.2.md` (**supersedes** `PLAY-LISTING-2.0.1.md`, which has no Wear OS / Android Auto sections) + `2026Tasarım/prayer-icons/store/`

## 🚀 Upload

- [ ] **8. Upload AAB (2.0.2) → Internal testing track** → smoke test on device (adhan fires, media notification shows controls) → promote to production
- [ ] **9. Post-release** — monitor Crashlytics + Android vitals, verify FSP declaration accepted

## ⌚🚗 Play Console — Wear OS + Android Auto form factors (discoverability)

The control that makes both features *discoverable* is **Advanced settings → Form
factors → Add form factor** — it is **not** on the publishing page. Neither form
factor shows up on Play until it is added, no matter what the bundles declare.

- [ ] **10. Add both form factors**
  - [ ] `Wear OS` — add the form factor, then upload the **Wear OS screenshots the form factor requires** (the 454×454 tile screenshot captured for 2.0.1 is at `/Users/a195143/2026Tasarım/prayer-icons/store/`)
  - [ ] `Android Auto` — add the form factor; the store-listing copy for both is `docs/release/PLAY-LISTING-2.0.2.md`
- [ ] **11. Android Auto Addendum + quality review**
  - [ ] Accept the **Android Auto Addendum** and opt in to the Auto form factor
  - [ ] Expect the **car-app quality review** (POI category) to add review time before open testing / production goes live
- [ ] **12. Upload both bundles to the same release**
  - [ ] Phone bundle `docs/release/app-release.aab` — versionCode `202`
  - [ ] Watch bundle `docs/release/wear-release.aab` — versionCode `3620200`, package `com.kutluoglu.namazvakitleri`, same upload key as the phone app
  - [ ] **Verify both AABs are signed before uploading** — `jarsigner -verify -verbose:summary docs/release/app-release.aab docs/release/wear-release.aab` must report `jar verified.` for each, with the same signer certificate; this is the exact defect that kept the watch app off Play, and the no-signing-properties experiment overwrites `wear-release.aab` with an **unsigned** bundle, so **re-copy both AABs from `app/build/outputs/bundle/release/` and `wear/build/outputs/bundle/release/` after any rebuild** and re-run the check
  - [ ] Upload both bundles on the **same release**, in the release's app-bundles section — the phone bundle first, then the watch bundle for the **Wear OS** form factor (the form factors must already be added, item 10); confirm the two version codes stay unique across form factors — Play rejects a duplicate
- [ ] **13. Two consequences of the watch upload**
  - [ ] A physical watch carrying a locally-built **debug** watch app must **uninstall** it before installing the Play-signed release (different signing certificate)
  - [ ] **Wear 64-bit requirement** (already in force, Sep 2026) is **satisfied** — the watch app does ship native libraries (`libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`), and the bundle contains both 32-bit (`armeabi-v7a`) and 64-bit (`arm64-v8a`, `x86_64`) variants, which is what the rule requires; re-check with APK Analyzer if a dependency bump ever changes the native set
- [ ] **14. Retire the stale Wear planning note** — `docs/wear-sync-fix-prompt.md` still documents the old `…namazvakitleri.wear` package; marked historical, pointing at the plan/design above (file is local-only and gitignored, so the edit is not committed)

---

## Shipped in 2.0.1 (versionCode 201) — context

- [x] Signed AAB built & verified (23 MB, jarsigner OK, versionCode 201 confirmed)
- [x] Version bump, `isShrinkResources = true`, bilingual release notes (TR/EN)
- [x] Release docs consolidated under `docs/release/` (RELEASE-TODO, RELEASE-NOTES, PLAY-LISTING)
- [x] Store assets located/produced: feature graphic 1024×500, 512 icon, 4 phone screenshots (SM-S948B), 1 Wear tile screenshot (emulator 454×454) — at `/Users/a195143/2026Tasarım/prayer-icons/store/`
- [x] Privacy policy verified live: https://bordobereli.github.io/NamazVakitleri/privacy-policy.html
- [x] R8 mapping.txt archived (`docs/release/mapping-2.0.1/`)
- [x] Wear OS + Android Auto + full redesign + offline mode included in this release line

## Release engineering notes (for future releases)

- Keystore: `/Users/a195143/Kutluoglu/BenimUygulamalarım/Imzalarım/fahri.kutluoglu.jks`, alias `kutluoglu` (credentials in `keystore.properties`, gitignored; Turkish chars in path need `\u0131` unicode escapes in the properties file)
- Universal debug-installable APK from AAB: `bundletool build-apks --mode=universal` (bundletool-all jar at /tmp/bundletool.jar — re-download if purged)
- Wear screenshots: Wear_OS_Large_Round emulator; tile must be added to carousel via tile picker first
- Phone screenshots: SM-S948B (720×1560), connected via USB (`adb devices`)
