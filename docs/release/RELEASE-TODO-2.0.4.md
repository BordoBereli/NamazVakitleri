# Release ToDo List — NamazVakitleri → Play Store v2.0.4

Status: `[ ]` = pending, `[x]` = done, `[~]` = in progress.
Last updated: 2026-10-14

Planned follow-up release after 2.0.2. No content yet — created as a backlog for
the media3 architecture migration decided during the 2.0.2 media-notification fix.

---

## 🔵 Media3 architecture migration (Option C from 2.0.2 fix discussion)

Context: the 2.0.2 adhan notification fix applied `MediaStyle` manually in
`PrayerNotificationManager.buildAdhanNotification` (minimal fix to unblock the Play
foreground-service declaration). The textbook media3 pattern — letting the library
own the notification lifecycle — was deferred to avoid a large refactor in a
release-blocking fix.

- [ ] **1. Restructure `AdhanService` into a media3 `MediaSessionService`**
  - [ ] Extend `MediaSessionService` instead of `android.app.Service`
  - [ ] Move session ownership from `AdhanVolumeController` into the service lifecycle
  - [ ] Wire callbacks (completion, focus loss, volume-floor stop) into session/player state
  - [ ] Koin wiring changes (`AdhanPlayer`, service no longer `KoinComponent`-injects adhanPlayer?)
- [ ] **2. Replace manual notification with `DefaultMediaNotificationProvider`**
  - [ ] Remove `buildAdhanNotification` / `showAdhanNotification` media-style path from `PrayerNotificationManager`
  - [ ] Keep the custom adhan channel (`CHANNEL_ADHAN`) and Stop action via provider customization
  - [ ] Verify media notification appears with transport controls on API 26+ (minSdk 26)
- [ ] **3. Tests**
  - [ ] Rework `AdhanServiceTest` for `MediaSessionService` (Robolectric session service controller)
  - [ ] Remove/replace media-style builder tests in `PrayerNotificationManagerTest`
  - [ ] Full `testDebugUnitTest` green
- [ ] **4. Release engineering**
  - [ ] Bump version → `2.0.4` / versionCode `204` (2.0.3 reserved for hotfixes)
  - [ ] `./gradlew bundleRelease`, verify signature + versionCode, archive `mapping.txt`
  - [ ] GitNexus impact + `detect_changes` before commit
  - [ ] Smoke test on device: adhan fires in background, media notification with controls, stop works
