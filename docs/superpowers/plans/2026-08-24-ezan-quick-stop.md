# Ezan Quick-Stop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user stop the Ezan sound instantly via a notification Stop button, swiping the notification away, or pressing volume-down.

**Architecture:** Move Ezan playback from `AlarmReceiver` into a new foreground service `AdhanService`. The service plays the Ezan via `AdhanPlayer`, shows a foreground notification (prayer name + "Ezan çalıyor" + Stop action + delete intent), detects volume-down via a `ContentObserver` on `Settings.System.VOLUME_ALARM` (since `Service` has no `onKeyDown`), and auto-stops when the audio completes. `AlarmReceiver` starts the service when adhan is enabled and stops it on `ACTION_STOP_ADHAN`.

**Tech Stack:** Kotlin, Android Services, MediaPlayer, NotificationCompat, Robolectric, MockK, Truth, Koin.

**Spec:** `docs/superpowers/specs/2026-08-24-ezan-quick-stop-design.md`

---

### Task 1: Add `notification_adhan_playing` string to all 15 locales

**Files:**
- Modify: `prayer_notifications/src/main/res/values/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-ar/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-ta/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-fa/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-es/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-ur/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-th/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-tr/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-fr/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-de/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-ru/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-bn/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-hi/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-id/strings.xml`
- Modify: `prayer_notifications/src/main/res/values-ms/strings.xml`

- [ ] **Step 1: Add the string to each locale file**

In each `strings.xml`, add the line directly after the `notification_stop` line (which is line 11 in every file):

- `values/strings.xml` (en): `<string name="notification_adhan_playing">Adhan playing</string>`
- `values-ar/strings.xml`: `<string name="notification_adhan_playing">الأذان يعزف</string>`
- `values-ta/strings.xml`: `<string name="notification_adhan_playing">அதான் ஒலிக்கிறது</string>`
- `values-fa/strings.xml`: `<string name="notification_adhan_playing">اذان در حال پخش است</string>`
- `values-es/strings.xml`: `<string name="notification_adhan_playing">Reproduciendo adhan</string>`
- `values-ur/strings.xml`: `<string name="notification_adhan_playing">اذان چل رہی ہے</string>`
- `values-th/strings.xml`: `<string name="notification_adhan_playing">กำลังเล่นอาซาน</string>`
- `values-tr/strings.xml`: `<string name="notification_adhan_playing">Ezan çalıyor</string>`
- `values-fr/strings.xml`: `<string name="notification_adhan_playing">Adhan en cours</string>`
- `values-de/strings.xml`: `<string name="notification_adhan_playing">Adhan wird abgespielt</string>`
- `values-ru/strings.xml`: `<string name="notification_adhan_playing">Азан играет</string>`
- `values-bn/strings.xml`: `<string name="notification_adhan_playing">আজান বাজছে</string>`
- `values-hi/strings.xml`: `<string name="notification_adhan_playing">अज़ान बज रही है</string>`
- `values-id/strings.xml`: `<string name="notification_adhan_playing">Adzan diputar</string>`
- `values-ms/strings.xml`: `<string name="notification_adhan_playing">Azan dimainkan</string>`

- [ ] **Step 2: Verify the resources compile**

Run: `./gradlew :prayer_notifications:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no resource errors).

- [ ] **Step 3: Commit**

```bash
git add prayer_notifications/src/main/res
git commit -m "feat(notifications): add adhan playing string in all locales"
```

---

### Task 2: Add completion callback to `AdhanPlayer`

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt`

- [ ] **Step 1: Write the failing test**

Add this test to `AdhanPlayerTest.kt` (after the existing `repeated play calls do not throw` test), and add the imports at the top:

```kotlin
import android.media.MediaPlayer
import com.google.common.truth.Truth.assertThat
import org.robolectric.Shadows.shadowOf
```

```kotlin
@Test
fun `completion listener is invoked when playback completes`() {
    var createdPlayer: MediaPlayer? = null
    ShadowMediaPlayer.setCreateListener { player, _ -> createdPlayer = player }
    val player = AdhanPlayer(context)
    var completed = false
    player.setOnCompletionListener { completed = true }
    player.play("Fajr")
    val mediaPlayer = createdPlayer ?: error("no player created")
    shadowOf(mediaPlayer).invokeCompletionListener()
    assertThat(completed).isTrue()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.AdhanPlayerTest"`
Expected: COMPILATION FAILURE — `setOnCompletionListener` does not exist on `AdhanPlayer`.

- [ ] **Step 3: Implement the completion callback**

In `AdhanPlayer.kt`, add a listener field, a setter, apply it in `play()`, and keep `stop()` unchanged (do NOT clear the listener — the service registers it once):

```kotlin
@Single
class AdhanPlayer(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private var onCompletion: (() -> Unit)? = null

    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletion = listener
    }

    fun play(prayerKey: String) {
        stop()
        val resId = when (prayerKey) {
            "Fajr" -> R.raw.adhan_fajr
            "Dhuhr" -> R.raw.adhan_dhuhr
            "Asr" -> R.raw.adhan_asr
            "Maghrib" -> R.raw.adhan_maghrib
            "Isha" -> R.raw.adhan_isha
            else -> R.raw.adhan_fajr
        }
        val player = MediaPlayer()
        try {
            player.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/$resId"))
                setOnCompletionListener { onCompletion?.invoke() }
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            runCatching { player.release() }
            Log.e("AdhanPlayer", "Failed to play adhan -> ${e.message}")
        }
    }

    fun stop() {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.AdhanPlayerTest"`
Expected: PASS (all 5 tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt
git commit -m "feat(notifications): add completion callback to AdhanPlayer"
```

---

### Task 3: Add `buildAdhanNotification` + `NOTIFICATION_ID_ADHAN` + `ACTION_STOP_ADHAN` constant

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt`
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt` (constant only)
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

Add these tests to `PrayerNotificationManagerTest.kt` (after the last test), and add the import:

```kotlin
import com.google.common.truth.Truth.assertThat
```

```kotlin
@Test
fun `buildAdhanNotification shows prayer name and playing text`() {
    val original = Locale.getDefault()
    try {
        Locale.setDefault(Locale("tr"))
        manager.createChannels()
        val notification = manager.buildAdhanNotification("Fajr")
        assertThat(notification.extras.getString("android.title")).isEqualTo("İmsak")
        assertThat(notification.extras.getString("android.text")).isEqualTo("Ezan çalıyor")
    } finally {
        Locale.setDefault(original)
    }
}

@Test
fun `buildAdhanNotification includes stop action and delete intent`() {
    manager.createChannels()
    val notification = manager.buildAdhanNotification("Fajr")
    assertThat(notification.actions).hasLength(1)
    assertThat(notification.actions!![0].title.toString()).isEqualTo("Stop")
    assertThat(notification.deleteIntent).isNotNull()
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.PrayerNotificationManagerTest"`
Expected: COMPILATION FAILURE — `buildAdhanNotification` does not exist, and `AlarmReceiver.ACTION_STOP_ADHAN` does not exist.

- [ ] **Step 3: Add the `ACTION_STOP_ADHAN` constant to `AlarmReceiver`**

In `AlarmReceiver.kt`, add the constant to the companion object (next to `ACTION_STOP_COUNTDOWN`):

```kotlin
const val ACTION_STOP_ADHAN = "STOP_ADHAN"
```

- [ ] **Step 4: Implement `buildAdhanNotification` in `PrayerNotificationManager`**

Add `NOTIFICATION_ID_ADHAN = 1009` to the companion object (after `NOTIFICATION_ID_PRE_SPECIAL_DAY = 1008`):

```kotlin
const val NOTIFICATION_ID_ADHAN = 1009
```

Add the import at the top of the file:

```kotlin
import android.app.Notification
```

Add this method to the class (after `showPrayerNotification`):

```kotlin
fun buildAdhanNotification(prayerName: String): Notification {
    val localizedName = localizedPrayerName(prayerName)
    val stopIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_STOP_ADHAN),
        PendingIntent.FLAG_IMMUTABLE
    )
    return NotificationCompat.Builder(context, CHANNEL_ADHAN)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(localizedName)
        .setContentText(localizedString(R.string.notification_adhan_playing))
        .setOngoing(false)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDeleteIntent(stopIntent)
        .addAction(0, localizedString(R.string.notification_stop), stopIntent)
        .build()
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.manager.PrayerNotificationManagerTest"`
Expected: PASS (all tests).

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManager.kt prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/PrayerNotificationManagerTest.kt
git commit -m "feat(notifications): build adhan notification with stop action"
```

---

### Task 4: Create `AdhanService` (foreground service) + manifest

**Files:**
- Create: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt`
- Modify: `prayer_notifications/src/main/AndroidManifest.xml`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt`

- [ ] **Step 1: Write the failing test**

Create `AdhanServiceTest.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdhanServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val adhanPlayer = mockk<AdhanPlayer>(relaxed = true)
    private val notificationManager = mockk<PrayerNotificationManager>(relaxed = true)

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(context)
                modules(module {
                    single { adhanPlayer }
                    single { notificationManager }
                })
            }
        }
        every { notificationManager.buildAdhanNotification(any()) } returns
            NotificationCompat.Builder(context, "adhan").build()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun startService(prayerKey: String = "Fajr"): AdhanService {
        val intent = Intent(context, AdhanService::class.java)
            .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, prayerKey)
        return Robolectric.buildService(AdhanService::class.java)
            .create()
            .get()
            .also { it.onStartCommand(intent, 0, 1) }
    }

    @Test
    fun `onStartCommand plays adhan and shows foreground notification`() {
        startService("Fajr")
        verify { adhanPlayer.play("Fajr") }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertThat(shadowOf(nm).allNotifications).isNotEmpty()
    }

    @Test
    fun `volume decrease stops the service`() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
        startService("Fajr")
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 4, 0)
        val uri = Settings.System.getUriFor(Settings.System.VOLUME_ALARM)
        shadowOf(context.contentResolver).getContentObservers(uri).forEach { it.onChange(false) }
        shadowOf(Looper.getMainLooper()).idle()
        verify { adhanPlayer.stop() }
    }

    @Test
    fun `completion callback stops the service`() {
        val completionSlot = slot<() -> Unit>()
        every { adhanPlayer.setOnCompletionListener(capture(completionSlot)) } answers { }
        startService("Fajr")
        completionSlot.captured.invoke()
        shadowOf(Looper.getMainLooper()).idle()
        verify { adhanPlayer.stop() }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.AdhanServiceTest"`
Expected: COMPILATION FAILURE — `AdhanService` does not exist.

- [ ] **Step 3: Implement `AdhanService`**

Create `AdhanService.kt`:

```kotlin
package com.kutluoglu.prayer_notifications.scheduler

import android.app.Service
import android.content.ContentObserver
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.kutluoglu.prayer_notifications.manager.AdhanPlayer
import com.kutluoglu.prayer_notifications.manager.PrayerNotificationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AdhanService : Service(), KoinComponent {

    private val adhanPlayer: AdhanPlayer by inject()
    private val notificationManager: PrayerNotificationManager by inject()

    private val audioManager: AudioManager by lazy {
        getSystemService(AUDIO_SERVICE) as AudioManager
    }

    private var lastAlarmVolume: Int = 0
    private var volumeObserverRegistered: Boolean = false

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current < lastAlarmVolume) {
                stopSelf()
            }
            lastAlarmVolume = current
        }
    }

    override fun onCreate() {
        super.onCreate()
        adhanPlayer.setOnCompletionListener { stopSelf() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerKey = intent?.getStringExtra(AlarmReceiver.EXTRA_PRAYER_KEY)
        if (prayerKey == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        adhanPlayer.play(prayerKey)
        startForeground(
            PrayerNotificationManager.NOTIFICATION_ID_ADHAN,
            notificationManager.buildAdhanNotification(prayerKey)
        )
        lastAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        registerVolumeObserver()
        return START_NOT_STICKY
    }

    private fun registerVolumeObserver() {
        if (volumeObserverRegistered) return
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.VOLUME_ALARM),
            false,
            volumeObserver
        )
        volumeObserverRegistered = true
    }

    private fun unregisterVolumeObserver() {
        if (!volumeObserverRegistered) return
        contentResolver.unregisterContentObserver(volumeObserver)
        volumeObserverRegistered = false
    }

    override fun onDestroy() {
        unregisterVolumeObserver()
        adhanPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

- [ ] **Step 4: Register the service and permissions in the manifest**

In `prayer_notifications/src/main/AndroidManifest.xml`, add the two permissions after the existing `<uses-permission ... RECEIVE_BOOT_COMPLETED />` line:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

And add the service declaration inside `<application>` (after the `AlarmReceiver` receiver):

```xml
<service
    android:name=".scheduler.AdhanService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.AdhanServiceTest"`
Expected: PASS (all 3 tests).

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt prayer_notifications/src/main/AndroidManifest.xml prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt
git commit -m "feat(notifications): add AdhanService foreground service with quick-stop"
```

---

### Task 5: Wire `AlarmReceiver` to start/stop `AdhanService`

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiverTest.kt`

- [ ] **Step 1: Write the failing tests**

Update `AlarmReceiverTest.kt`:

1. Replace the existing `prayer alarm posts prayer notification and plays adhan` test with these two tests, and add the imports:

```kotlin
import android.app.Application
import com.google.common.truth.Truth.assertThat
import org.robolectric.Shadows.shadowOf
```

```kotlin
@Test
fun `prayer alarm with adhan enabled starts adhan service`() = runTest {
    coEvery { dataStore.getSettings() } returns NotificationSettings(adhanEnabled = true)
    val receiver = AlarmReceiver()
    val intent = Intent(context, AlarmReceiver::class.java)
        .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
        .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Fajr")
    receiver.handleAlarm(context, intent)
    val started = shadowOf(context as Application).getNextStartedService()
    assertThat(started?.component?.className).isEqualTo(AdhanService::class.java.name)
    verify(exactly = 0) { notificationManager.showPrayerNotification(any(), any()) }
    verify(exactly = 0) { adhanPlayer.play(any()) }
}

@Test
fun `prayer alarm with adhan disabled posts prayer notification`() = runTest {
    coEvery { dataStore.getSettings() } returns NotificationSettings(adhanEnabled = false)
    val receiver = AlarmReceiver()
    val intent = Intent(context, AlarmReceiver::class.java)
        .putExtra(AlarmReceiver.EXTRA_ALARM_TYPE, AlarmType.PRAYER.name)
        .putExtra(AlarmReceiver.EXTRA_PRAYER_KEY, "Fajr")
    receiver.handleAlarm(context, intent)
    verify { notificationManager.showPrayerNotification("Fajr", any()) }
    verify(exactly = 0) { adhanPlayer.play(any()) }
}
```

2. Update every other `receiver.handleAlarm(intent)` call in the file to `receiver.handleAlarm(context, intent)` (there are 7 call sites: `jumuah prayer alarm posts jumuah notification`, `pre-prayer alarm posts pre-prayer notification`, `daily reminder alarm posts summary and re-arms`, `special day alarm posts special day notification`, `pre-special day alarm posts pre-special day notification`, `prayer alarm transitions countdown with firing prayer trigger as previous`, `prayer alarm without trigger time passes null previous`).

3. Add this test at the end of the class:

```kotlin
@Test
fun `STOP_ADHAN stops the adhan service`() {
    val receiver = AlarmReceiver()
    val intent = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_STOP_ADHAN)
    receiver.onReceive(context, intent)
    val stopped = shadowOf(context as Application).getNextStoppedService()
    assertThat(stopped?.component?.className).isEqualTo(AdhanService::class.java.name)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.AlarmReceiverTest"`
Expected: COMPILATION FAILURE — `handleAlarm` takes one argument, and `ACTION_STOP_ADHAN` handling does not exist.

- [ ] **Step 3: Implement the receiver changes**

In `AlarmReceiver.kt`:

1. In `onReceive`, add the `ACTION_STOP_ADHAN` branch and pass `context` to `handleAlarm`:

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
        ACTION_STOP_COUNTDOWN -> scheduler.cancelCountdown()
        ACTION_STOP_ADHAN -> context.stopService(Intent(context, AdhanService::class.java))
        ACTION_COUNTDOWN_TICK -> {
            val target = intent.getLongExtra(EXTRA_COUNTDOWN_TARGET, 0L)
            val name = intent.getStringExtra(EXTRA_COUNTDOWN_PRAYER_NAME) ?: return
            val previous = if (intent.hasExtra(EXTRA_COUNTDOWN_PREVIOUS_TIME)) {
                intent.getLongExtra(EXTRA_COUNTDOWN_PREVIOUS_TIME, 0L)
            } else {
                null
            }
            scheduler.updateCountdown(target, name, previous)
        }
        else -> {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    handleAlarm(context, intent)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
```

2. Change the `handleAlarm` signature and the PRAYER branch:

```kotlin
internal suspend fun handleAlarm(context: Context, intent: Intent) {
    val type = intent.getStringExtra(EXTRA_ALARM_TYPE)
        ?.let { runCatching { AlarmType.valueOf(it) }.getOrNull() }
        ?: return
    val settings = dataStore.getSettings()
    when (type) {
        AlarmType.PRAYER -> {
            val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: return
            if (settings.adhanEnabled) {
                context.startService(
                    Intent(context, AdhanService::class.java)
                        .putExtra(EXTRA_PRAYER_KEY, prayerKey)
                )
            } else if (intent.getBooleanExtra(EXTRA_IS_JUMUAH, false) && settings.jumuahEnabled) {
                notificationManager.showJumuahNotification()
            } else {
                notificationManager.showPrayerNotification(prayerKey, settings)
            }
            if (settings.countdownEnabled) {
                val nextTime = intent.getLongExtra(EXTRA_NEXT_PRAYER_TIME, 0L)
                val nextName = intent.getStringExtra(EXTRA_NEXT_PRAYER_NAME)
                val previous = if (intent.hasExtra(EXTRA_ALARM_TRIGGER_TIME)) {
                    intent.getLongExtra(EXTRA_ALARM_TRIGGER_TIME, 0L)
                } else {
                    null
                }
                if (nextTime > 0L && nextName != null) {
                    scheduler.updateCountdown(nextTime, nextName, previous)
                }
            }
        }
        // PRE_PRAYER, DAILY_REMINDER, SPECIAL_DAY, PRE_SPECIAL_DAY, COUNTDOWN_TICK branches unchanged
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="com.kutluoglu.prayer_notifications.scheduler.AlarmReceiverTest"`
Expected: PASS (all tests).

- [ ] **Step 5: Run the full module test suite**

Run: `./gradlew :prayer_notifications:testDebugUnitTest`
Expected: PASS (all tests across the module).

- [ ] **Step 6: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiver.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AlarmReceiverTest.kt
git commit -m "feat(notifications): start AdhanService on prayer alarm and stop on ACTION_STOP_ADHAN"
```

---

### Task 6: Update `TODO.md`

**Files:**
- Modify: `TODO.md`

- [ ] **Step 1: Add the completed feature entry**

Add a new entry under the `## ✅ Features completed` section (after the countdown notification improvement entry, which ends at line 31):

```markdown
- [x] **Ezan quick-stop** (2026-08-24, TDD)
  - Ezan playback moved into a foreground `AdhanService` that shows a notification with a Stop button, stops on swipe-to-dismiss, stops when the alarm volume is lowered (volume-down), and auto-stops when the audio completes.
  - Spec: `docs/superpowers/specs/2026-08-24-ezan-quick-stop-design.md`; Plan: `docs/superpowers/plans/2026-08-24-ezan-quick-stop.md`.
```

- [ ] **Step 2: Commit**

```bash
git add TODO.md
git commit -m "docs: mark Ezan quick-stop feature complete"
```

---

## Verification

After all tasks, run the full test suite to confirm nothing is broken:

```bash
./gradlew :prayer_notifications:testDebugUnitTest
./gradlew testDebugUnitTest
```

Expected: all tests pass.
