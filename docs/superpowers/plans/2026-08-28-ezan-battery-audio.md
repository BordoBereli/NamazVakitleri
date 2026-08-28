# Ezan Battery Optimization & Audio Focus Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Ezan play reliably when the phone is locked (battery optimization guidance) and let the hardware volume buttons adjust the Ezan while it plays (audio focus).

**Architecture:** Two independent fixes. (1) `AdhanPlayer` requests audio focus (`AUDIOFOCUS_GAIN_TRANSIENT` + `USAGE_ALARM`) so the system routes volume keys to the alarm stream; `AdhanService` stops on permanent focus loss. (2) `NotificationsScreen` detects `PowerManager.isIgnoringBatteryOptimizations` and shows a banner + dialog (same pattern as the exact-alarm permission) that opens `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`.

**Tech Stack:** Kotlin, Jetpack Compose, Robolectric 4.14, MockK, Truth, Koin.

**Spec:** `docs/superpowers/specs/2026-08-28-ezan-battery-audio-design.md`

---

### Task 1: AdhanPlayer — audio focus (TDD)

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt`

- [ ] **Step 1: Write the failing tests**

Append to `AdhanPlayerTest.kt` (before the closing brace):

```kotlin
    @Test
    fun `play requests audio focus on alarm stream`() {
        val player = AdhanPlayer(context)
        player.play("Fajr", 30)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = shadowOf(audioManager).getLastAudioFocusRequest()
        assertThat(request).isNotNull()
        assertThat(request.durationHint).isEqualTo(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        assertThat(request.audioFocusRequest.audioAttributes.usage)
            .isEqualTo(AudioAttributes.USAGE_ALARM)
        player.stop()
    }

    @Test
    fun `stop abandons audio focus`() {
        val player = AdhanPlayer(context)
        player.play("Fajr", 30)
        player.stop()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        assertThat(shadowOf(audioManager).getLastAbandonedAudioFocusRequest()).isNotNull()
    }

    @Test
    fun `permanent focus loss invokes focus loss listener`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
        val player = AdhanPlayer(context)
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = shadowOf(audioManager).getLastAudioFocusRequest() ?: error("no focus request")
        request.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        assertThat(focusLost).isTrue()
    }

    @Test
    fun `transient focus loss does not stop playback`() {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_fajr}")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, uri), ShadowMediaPlayer.MediaInfo())
        var createdPlayer: MediaPlayer? = null
        ShadowMediaPlayer.setCreateListener { player, _ -> createdPlayer = player }
        val player = AdhanPlayer(context)
        var focusLost = false
        player.setOnFocusLossListener { focusLost = true }
        player.play("Fajr", 30)
        val mediaPlayer = createdPlayer ?: error("no player created")
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = shadowOf(audioManager).getLastAudioFocusRequest() ?: error("no focus request")
        request.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertThat(focusLost).isFalse()
        assertThat(shadowOf(mediaPlayer).isPlaying).isTrue()
    }
```

Add imports to `AdhanPlayerTest.kt`:

```kotlin
import android.media.AudioAttributes
import android.media.AudioManager
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AdhanPlayerTest*"`
Expected: FAIL — `getLastAudioFocusRequest()` returns null (no focus requested yet).

- [ ] **Step 3: Implement audio focus in AdhanPlayer**

Replace the body of `AdhanPlayer.kt` with:

```kotlin
package com.kutluoglu.prayer_notifications.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.kutluoglu.prayer_notifications.R
import org.koin.core.annotation.Single

@Single
class AdhanPlayer(
    private val context: Context
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var onCompletion: (() -> Unit)? = null
    private var onFocusLoss: (() -> Unit)? = null
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS) {
            stop()
            onFocusLoss?.invoke()
        }
    }

    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletion = listener
    }

    fun setOnFocusLossListener(listener: () -> Unit) {
        onFocusLoss = listener
    }

    fun play(prayerKey: String, volumePercent: Int) {
        stop()
        val volume = volumePercent.coerceIn(0, 100) / 100f
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
                setOnCompletionListener {
                    abandonAudioFocus()
                    onCompletion?.invoke()
                }
                prepare()
                start()
                setVolume(volume, volume)
            }
            mediaPlayer = player
            requestAudioFocus()
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
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AdhanPlayerTest*"`
Expected: PASS (all AdhanPlayerTest tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayer.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/manager/AdhanPlayerTest.kt
git commit -m "feat(notifications): request audio focus in adhan player"
```

---

### Task 2: AdhanService — stop on focus loss (TDD)

**Files:**
- Modify: `prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt`
- Test: `prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt`

- [ ] **Step 1: Write the failing test**

Append to `AdhanServiceTest.kt` (before the closing brace):

```kotlin
    @Test
    fun `focus loss callback stops the service`() {
        val focusLossSlot = slot<() -> Unit>()
        every { adhanPlayer.setOnFocusLossListener(capture(focusLossSlot)) } answers { }
        val controller = startService("Fajr")
        focusLossSlot.captured.invoke()
        assertThat(shadowOf(controller.get()).isStoppedBySelf()).isTrue()
        controller.destroy()
        verify { adhanPlayer.stop() }
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AdhanServiceTest*"`
Expected: FAIL — `setOnFocusLossListener` is never called, so the slot is never captured.

- [ ] **Step 3: Implement the focus-loss listener in AdhanService**

In `AdhanService.kt`, change `onCreate` (lines 49-52) to:

```kotlin
    override fun onCreate() {
        super.onCreate()
        adhanPlayer.setOnCompletionListener { stopSelf() }
        adhanPlayer.setOnFocusLossListener { stopSelf() }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_notifications:testDebugUnitTest --tests="*AdhanServiceTest*"`
Expected: PASS (all AdhanServiceTest tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_notifications/src/main/java/com/kutluoglu/prayer_notifications/scheduler/AdhanService.kt prayer_notifications/src/test/java/com/kutluoglu/prayer_notifications/scheduler/AdhanServiceTest.kt
git commit -m "feat(notifications): stop adhan service on audio focus loss"
```

---

### Task 3: Battery optimization strings (15 locales)

**Files:**
- Modify: `prayer_feature/settings/src/main/res/values/strings.xml` and the 14 locale variants (`values-ar`, `values-ta`, `values-fa`, `values-es`, `values-ur`, `values-th`, `values-tr`, `values-fr`, `values-de`, `values-ru`, `values-bn`, `values-hi`, `values-id`, `values-ms`)

- [ ] **Step 1: Add strings to `values/strings.xml`**

After the `exact_alarm_not_now` line (line 98), add:

```xml
    <string name="battery_optimization_hint">Disable battery optimization so the Ezan plays on time even when the phone is locked.</string>
    <string name="battery_optimization_dialog_title">Battery optimization</string>
    <string name="battery_optimization_dialog_body">To play the Ezan exactly on time even when the phone is locked, disable battery optimization for this app. Tap "Open settings", find Namaz Vakitleri in the list, and choose "Don\'t optimize".</string>
    <string name="battery_optimization_open_settings">Open settings</string>
    <string name="battery_optimization_not_now">Not now</string>
```

- [ ] **Step 2: Add strings to `values-tr/strings.xml`**

```xml
    <string name="battery_optimization_hint">Ezanın telefon kilitliyken de zamanında çalması için pil optimizasyonunu kapatın.</string>
    <string name="battery_optimization_dialog_title">Pil optimizasyonu</string>
    <string name="battery_optimization_dialog_body">Ezanın telefon kilitliyken bile tam vaktinde çalması için bu uygulama için pil optimizasyonunu kapatın. "Ayarları aç"a dokunun, listeden Namaz Vakitleri\'ni bulun ve "Optimize etme"yi seçin.</string>
    <string name="battery_optimization_open_settings">Ayarları aç</string>
    <string name="battery_optimization_not_now">Şimdi değil</string>
```

- [ ] **Step 3: Add strings to the remaining 13 locale files**

`values-ar`:
```xml
    <string name="battery_optimization_hint">عطّل تحسين البطارية حتى يعمل الأذان في وقته حتى عند قفل الهاتف.</string>
    <string name="battery_optimization_dialog_title">تحسين البطارية</string>
    <string name="battery_optimization_dialog_body">لتشغيل الأذان في وقته بالضبط حتى عند قفل الهاتف، عطّل تحسين البطارية لهذا التطبيق. اضغط "فتح الإعدادات"، وابحث عن Namaz Vakitleri في القائمة واختر "لا تحسّن".</string>
    <string name="battery_optimization_open_settings">فتح الإعدادات</string>
    <string name="battery_optimization_not_now">ليس الآن</string>
```

`values-ta`:
```xml
    <string name="battery_optimization_hint">தொலைபேசி பூட்டப்பட்டிருந்தாலும் அதான் சரியான நேரத்தில் ஒலிக்க பேட்டரி மேம்படுத்தலை முடக்கவும்.</string>
    <string name="battery_optimization_dialog_title">பேட்டரி மேம்படுத்தல்</string>
    <string name="battery_optimization_dialog_body">தொலைபேசி பூட்டப்பட்டிருந்தாலும் அதானை சரியான நேரத்தில் இயக்க, இந்த பயன்பாட்டிற்கான பேட்டரி மேம்படுத்தலை முடக்கவும். "அமைப்புகளைத் திற" என்பதைத் தட்டவும், பட்டியலில் Namaz Vakitleri-ஐக் கண்டறிந்து "மேம்படுத்த வேண்டாம்" என்பதைத் தேர்ந்தெடுக்கவும்.</string>
    <string name="battery_optimization_open_settings">அமைப்புகளைத் திற</string>
    <string name="battery_optimization_not_now">இப்போது இல்லை</string>
```

`values-fa`:
```xml
    <string name="battery_optimization_hint">برای اینکه اذان حتی وقتی گوشی قفل است به‌موقع پخش شود، بهینه‌سازی باتری را غیرفعال کنید.</string>
    <string name="battery_optimization_dialog_title">بهینه‌سازی باتری</string>
    <string name="battery_optimization_dialog_body">برای پخش دقیق اذان حتی وقتی گوشی قفل است، بهینه‌سازی باتری را برای این برنامه غیرفعال کنید. روی «باز کردن تنظیمات» بزنید، در فهرست Namaz Vakitleri را پیدا کنید و «بهینه نکن» را انتخاب کنید.</string>
    <string name="battery_optimization_open_settings">باز کردن تنظیمات</string>
    <string name="battery_optimization_not_now">الان نه</string>
```

`values-es`:
```xml
    <string name="battery_optimization_hint">Desactive la optimización de batería para que el adhan suene a tiempo incluso con el teléfono bloqueado.</string>
    <string name="battery_optimization_dialog_title">Optimización de batería</string>
    <string name="battery_optimization_dialog_body">Para reproducir el adhan exactamente a tiempo incluso con el teléfono bloqueado, desactive la optimización de batería para esta aplicación. Toque "Abrir ajustes", busque Namaz Vakitleri en la lista y elija "No optimizar".</string>
    <string name="battery_optimization_open_settings">Abrir ajustes</string>
    <string name="battery_optimization_not_now">Ahora no</string>
```

`values-ur`:
```xml
    <string name="battery_optimization_hint">اذان کو وقت پر چلانے کے لیے بیٹری کی اصلاح بند کریں چاہے فون مقفل ہو۔</string>
    <string name="battery_optimization_dialog_title">بیٹری کی اصلاح</string>
    <string name="battery_optimization_dialog_body">اذان کو عین وقت پر چلانے کے لیے اس ایپ کے لیے بیٹری کی اصلاح بند کریں۔ "ترتیبات کھولیں" پر ٹیپ کریں، فہرست میں Namaz Vakitleri تلاش کریں اور "اصلاح نہ کریں" منتخب کریں۔</string>
    <string name="battery_optimization_open_settings">ترتیبات کھولیں</string>
    <string name="battery_optimization_not_now">ابھی نہیں</string>
```

`values-th`:
```xml
    <string name="battery_optimization_hint">ปิดการเพิ่มประสิทธิภาพแบตเตอรี่เพื่อให้อาซานเล่นตรงเวลาแม้โทรศัพท์ถูกล็อก</string>
    <string name="battery_optimization_dialog_title">การเพิ่มประสิทธิภาพแบตเตอรี่</string>
    <string name="battery_optimization_dialog_body">เพื่อเล่นอาซานตรงเวลาแม้โทรศัพท์ถูกล็อก ให้ปิดการเพิ่มประสิทธิภาพแบตเตอรี่สำหรับแอปนี้ แตะ "เปิดการตั้งค่า" ค้นหา Namaz Vakitleri ในรายการแล้วเลือก "ไม่เพิ่มประสิทธิภาพ"</string>
    <string name="battery_optimization_open_settings">เปิดการตั้งค่า</string>
    <string name="battery_optimization_not_now">ไม่ใช่ตอนนี้</string>
```

`values-fr`:
```xml
    <string name="battery_optimization_hint">Désactivez l\'optimisation de la batterie pour que l\'adhan sonne à l\'heure même lorsque le téléphone est verrouillé.</string>
    <string name="battery_optimization_dialog_title">Optimisation de la batterie</string>
    <string name="battery_optimization_dialog_body">Pour jouer l\'adhan à l\'heure exacte même lorsque le téléphone est verrouillé, désactivez l\'optimisation de la batterie pour cette application. Touchez « Ouvrir les paramètres », trouvez Namaz Vakitleri dans la liste et choisissez « Ne pas optimiser ».</string>
    <string name="battery_optimization_open_settings">Ouvrir les paramètres</string>
    <string name="battery_optimization_not_now">Pas maintenant</string>
```

`values-de`:
```xml
    <string name="battery_optimization_hint">Deaktivieren Sie die Akkuoptimierung, damit der Adhan auch bei gesperrtem Telefon pünktlich ertönt.</string>
    <string name="battery_optimization_dialog_title">Akkuoptimierung</string>
    <string name="battery_optimization_dialog_body">Damit der Adhan auch bei gesperrtem Telefon pünktlich ertönt, deaktivieren Sie die Akkuoptimierung für diese App. Tippen Sie auf „Einstellungen öffnen", finden Sie Namaz Vakitleri in der Liste und wählen Sie „Nicht optimieren".</string>
    <string name="battery_optimization_open_settings">Einstellungen öffnen</string>
    <string name="battery_optimization_not_now">Später</string>
```

`values-ru`:
```xml
    <string name="battery_optimization_hint">Отключите оптимизацию батареи, чтобы азан звучал вовремя даже при заблокированном телефоне.</string>
    <string name="battery_optimization_dialog_title">Оптимизация батареи</string>
    <string name="battery_optimization_dialog_body">Чтобы азан звучал точно вовремя даже при заблокированном телефоне, отключите оптимизацию батареи для этого приложения. Нажмите «Открыть настройки», найдите Namaz Vakitleri в списке и выберите «Не оптимизировать».</string>
    <string name="battery_optimization_open_settings">Открыть настройки</string>
    <string name="battery_optimization_not_now">Не сейчас</string>
```

`values-bn`:
```xml
    <string name="battery_optimization_hint">ফোন লক থাকলেও আজান সময়মতো বাজাতে ব্যাটারি অপ্টিমাইজেশন বন্ধ করুন।</string>
    <string name="battery_optimization_dialog_title">ব্যাটারি অপ্টিমাইজেশন</string>
    <string name="battery_optimization_dialog_body">ফোন লক থাকলেও ঠিক সময়ে আজান বাজাতে এই অ্যাপের জন্য ব্যাটারি অপ্টিমাইজেশন বন্ধ করুন। "সেটিংস খুলুন" ট্যাপ করুন, তালিকায় Namaz Vakitleri খুঁজুন এবং "অপ্টিমাইজ করবেন না" বেছে নিন।</string>
    <string name="battery_optimization_open_settings">সেটিংস খুলুন</string>
    <string name="battery_optimization_not_now">এখন নয়</string>
```

`values-hi`:
```xml
    <string name="battery_optimization_hint">फ़ोन लॉक होने पर भी अज़ान समय पर बजाने के लिए बैटरी ऑप्टिमाइज़ेशन बंद करें।</string>
    <string name="battery_optimization_dialog_title">बैटरी ऑप्टिमाइज़ेशन</string>
    <string name="battery_optimization_dialog_body">फ़ोन लॉक होने पर भी अज़ान ठीक समय पर बजाने के लिए इस ऐप के लिए बैटरी ऑप्टिमाइज़ेशन बंद करें। "सेटिंग्स खोलें" पर टैप करें, सूची में Namaz Vakitleri खोजें और "ऑप्टिमाइज़ न करें" चुनें।</string>
    <string name="battery_optimization_open_settings">सेटिंग्स खोलें</string>
    <string name="battery_optimization_not_now">अभी नहीं</string>
```

`values-id`:
```xml
    <string name="battery_optimization_hint">Nonaktifkan pengoptimalan baterai agar azan berbunyi tepat waktu meskipun ponsel terkunci.</string>
    <string name="battery_optimization_dialog_title">Pengoptimalan baterai</string>
    <string name="battery_optimization_dialog_body">Untuk memutar azan tepat waktu meskipun ponsel terkunci, nonaktifkan pengoptimalan baterai untuk aplikasi ini. Ketuk "Buka pengaturan", temukan Namaz Vakitleri dalam daftar, dan pilih "Jangan optimalkan".</string>
    <string name="battery_optimization_open_settings">Buka pengaturan</string>
    <string name="battery_optimization_not_now">Nanti saja</string>
```

`values-ms`:
```xml
    <string name="battery_optimization_hint">Lumpuhkan pengoptimuman bateri supaya azan berbunyi tepat pada masanya walaupun telefon dikunci.</string>
    <string name="battery_optimization_dialog_title">Pengoptimuman bateri</string>
    <string name="battery_optimization_dialog_body">Untuk memainkan azan tepat pada masanya walaupun telefon dikunci, lumpuhkan pengoptimuman bateri untuk aplikasi ini. Ketik "Buka tetapan", cari Namaz Vakitleri dalam senarai, dan pilih "Jangan optimumkan".</string>
    <string name="battery_optimization_open_settings">Buka tetapan</string>
    <string name="battery_optimization_not_now">Tidak sekarang</string>
```

- [ ] **Step 4: Commit**

```bash
git add prayer_feature/settings/src/main/res/
git commit -m "feat(settings): add battery optimization strings in all locales"
```

---

### Task 4: NotificationsScreen — battery optimization UI (TDD)

**Files:**
- Modify: `prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt`
- Test: `prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt`

- [ ] **Step 1: Write the failing tests**

In `NotificationsScreenTest.kt`:
1. Add imports:
```kotlin
import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
```
2. Change the `@Before` method to also ignore battery optimization:
```kotlin
    @Before
    fun grantPermissions() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, true)
    }
```
3. Append these tests (before the closing brace):
```kotlin
    @Test
    fun `shows battery optimization banner when not ignoring battery optimization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        launchScreen(NotificationSettings(enabled = true))

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_hint)
        ).assertIsDisplayed()
    }

    @Test
    fun `enabling notifications without battery optimization shows dialog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_dialog_title)
        ).assertIsDisplayed()
        coVerify(exactly = 0) { updateUseCase(any()) }
    }

    @Test
    fun `open battery settings launches ignore battery optimization settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_open_settings)
        ).performClick()
        composeTestRule.waitForIdle()

        val startedIntent = shadowOf(composeTestRule.activity).nextStartedActivity
        assertThat(startedIntent?.action).isEqualTo(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    @Test
    fun `not now dismisses battery optimization dialog`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = false))

        composeTestRule.onAllNodes(isToggleable())[0].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_not_now)
        ).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_dialog_title)
        ).assertDoesNotExist()
    }

    @Test
    fun `shows battery optimization dialog on entry when enabled without exemption`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(powerManager).setIgnoringBatteryOptimizations(context.packageName, false)
        shadowOf(composeTestRule.activity).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        launchScreen(NotificationSettings(enabled = true))

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.battery_optimization_dialog_title)
        ).assertIsDisplayed()
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*NotificationsScreenTest*"`
Expected: FAIL — banner/dialog not shown (feature not implemented).

- [ ] **Step 3: Implement battery optimization UI in NotificationsScreen**

In `NotificationsScreen.kt`:
1. Add import:
```kotlin
import android.os.PowerManager
```
2. In `NotificationsContent`, after the exact-alarm state declarations (after line 132), add:
```kotlin
    var ignoresBatteryOptimization by remember { mutableStateOf(checkBatteryOptimization()) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var batteryDialogDismissed by remember { mutableStateOf(false) }
    var pendingBatteryAction by remember { mutableStateOf<(() -> Unit)?>(null) }
```
3. After `checkExactAlarmPermission()` (line 145), add:
```kotlin
    fun checkBatteryOptimization(): Boolean =
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
```
4. In the `ON_RESUME` observer (lines 152-160), update to:
```kotlin
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = checkNotificationPermission()
                canScheduleExactAlarms = checkExactAlarmPermission()
                ignoresBatteryOptimization = checkBatteryOptimization()
                if (canScheduleExactAlarms) {
                    pendingExactAlarmAction?.invoke()
                }
                pendingExactAlarmAction = null
                if (ignoresBatteryOptimization) {
                    pendingBatteryAction?.invoke()
                }
                pendingBatteryAction = null
            }
```
5. After the exact-alarm `LaunchedEffect` (line 170), add:
```kotlin
    LaunchedEffect(
        settings.enabled,
        canScheduleExactAlarms,
        ignoresBatteryOptimization,
        batteryDialogDismissed
    ) {
        if (settings.enabled && canScheduleExactAlarms &&
            !ignoresBatteryOptimization && !batteryDialogDismissed
        ) {
            showBatteryDialog = true
        }
    }
```
6. In the Notifications `ToggleRow` `onCheckedChange` (lines 199-210), add a battery branch before the final `else`:
```kotlin
                } else if (enabled && !ignoresBatteryOptimization) {
                    pendingBatteryAction = { onEvent(NotificationsEvent.SetEnabled(true)) }
                    showBatteryDialog = true
                } else {
```
7. In the Ezan `ToggleRow` `onCheckedChange` (lines 258-269), add a battery branch before the final `else`:
```kotlin
                } else if (enabled && !ignoresBatteryOptimization) {
                    pendingBatteryAction = { onEvent(NotificationsEvent.SetAdhanEnabled(true)) }
                    showBatteryDialog = true
                } else {
```
8. After the exact-alarm `PermissionHintRow` block (line 243), add the battery banner:
```kotlin
        if (!ignoresBatteryOptimization) {
            PermissionHintRow(
                text = stringResource(R.string.battery_optimization_hint),
                actionText = stringResource(R.string.open_settings),
                onAction = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    }
                }
            )
        }
```
9. After the exact-alarm `AlertDialog` block (line 393), add the battery dialog:
```kotlin
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = {
                showBatteryDialog = false
                batteryDialogDismissed = true
                pendingBatteryAction = null
            },
            title = { Text(stringResource(R.string.battery_optimization_dialog_title)) },
            text = { Text(stringResource(R.string.battery_optimization_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    batteryDialogDismissed = true
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    }
                }) {
                    Text(stringResource(R.string.battery_optimization_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    batteryDialogDismissed = true
                    pendingBatteryAction = null
                }) {
                    Text(stringResource(R.string.battery_optimization_not_now))
                }
            }
        )
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:settings:testDebugUnitTest --tests="*NotificationsScreenTest*"`
Expected: PASS (all NotificationsScreenTest tests).

- [ ] **Step 5: Commit**

```bash
git add prayer_feature/settings/src/main/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreen.kt prayer_feature/settings/src/test/java/com/kutluoglu/prayer_feature/settings/notifications/NotificationsScreenTest.kt
git commit -m "feat(settings): prompt to disable battery optimization for reliable ezan"
```

---

### Task 5: Full verification

- [ ] **Step 1: Run the affected module test suites**

Run: `./gradlew :prayer_notifications:testDebugUnitTest :prayer_feature:settings:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 2: Run the full unit test suite**

Run: `./gradlew unitTests`
Expected: PASS (no regressions).

- [ ] **Step 3: Run gitnexus detect_changes**

Run: `gitnexus_detect_changes` and confirm only expected symbols (AdhanPlayer, AdhanService, NotificationsScreen) are affected.
