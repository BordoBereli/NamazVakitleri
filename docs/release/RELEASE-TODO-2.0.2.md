# Release ToDo List — NamazVakitleri → Play Store v2.0.2

Status: `[ ]` = pending, `[x]` = done, `[~]` = in progress.
Last updated: 2026-10-14

Current release target: **v2.0.2 (versionCode 202)** — update to a live listing (follow-up to 2.0.1).
Primary driver: **media-style adhan notification fix** for the Play foreground-service declaration.

---

## 🔴 Critical — media notification fix (blocks declaration video review)

- [ ] **1. Media-style adhan notification**
  - [ ] GitNexus impact check on `PrayerNotificationManager.buildAdhanNotification` + `AdhanVolumeController`
  - [ ] Add `androidx.media:media` dependency (for `MediaStyle`) if not already present via media3
  - [ ] Expose `MediaSession` token from `AdhanVolumeController`
  - [ ] `buildAdhanNotification`: apply `MediaStyle` with session token, `setOngoing(true)`, `CATEGORY_TRANSPORT`, keep Stop action
  - [ ] Unit test updates (notification builder is interface-seamed — mock/verify)
  - [ ] `./gradlew testDebugUnitTest` green
- [ ] **2. Rebuild release AAB**
  - [ ] Bump version → `2.0.2` / versionCode `202` in `gradle/libs.versions.toml`
  - [ ] `./gradlew bundleRelease` → signed AAB (keystore.properties ready, unicode-escaped path)
  - [ ] Verify signature + versionCode 202
  - [ ] Archive new `mapping.txt` → `docs/release/mapping-2.0.2/`
- [ ] **3. Commit** (GitNexus `detect_changes` first, as usual)

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
- [ ] **7. Update listing** if not yet done — copy/assets from `docs/release/PLAY-LISTING-2.0.1.md` + `2026Tasarım/prayer-icons/store/`

## 🚀 Upload

- [ ] **8. Upload AAB (2.0.2) → Internal testing track** → smoke test on device (adhan fires, media notification shows controls) → promote to production
- [ ] **9. Post-release** — monitor Crashlytics + Android vitals, verify FSP declaration accepted

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
