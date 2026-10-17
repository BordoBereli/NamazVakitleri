# Play Store Listing Copy — v2.0.2 (bilingual TR/EN)

Structure mirrors `PLAY-LISTING-2.0.1.md` (still the live listing) so the two can
be diffed block by block. Copy is pasted at release time; the console clicks that
make the new form factors appear are in `RELEASE-TODO-2.0.2.md`, and the
user-facing "What's new" text is in `RELEASE-NOTES-2.0.2.md`.

## Copy fixes applied against the live listing

Three machine-translation artefacts are corrected below; everything else is
carried over verbatim from the live listing.

| Live listing (broken) | 2.0.2 copy | Why |
|---|---|---|
| `⌚ CLOCK SUPPORT` | `⌚ WEAR OS SUPPORT` | Wear OS is the platform name; "clock support" is a literal translation that hides the feature |
| `Save the reference to save the verses` | `Save verses to your favourites` | The live string is a broken blend of two bullets (save a verse / tap a reference); the corrected line says what the button does |
| TR: `canlı kadran/tile` | TR: `canlı tile` | The watch feature is a **tile**, not a watch face (kadran); the app ships no watch face |

## App title (max 30 chars)

- **EN:** Namaz Vakitleri: Prayer Times
- **TR:** Namaz Vakitleri: Ezan Vakti

## Short description (max 80 chars)

- **EN:** Prayer times, Qibla compass & Quran verses — works offline, on your watch & car
- **TR:** Namaz vakitleri, kıble pusulası ve Kuran ayetleri — çevrimdışı, saatte ve araba

## Full description (max 4000 chars)

### EN

Namaz Vakitleri is a beautiful, ad-free prayer times app for Muslims. Get accurate prayer times for your location, find the Qibla direction with a live compass, and read daily Quran verses — all in one elegant app.

🕌 ACCURATE PRAYER TIMES
• Precise prayer times for any location worldwide
• Multiple calculation methods (Diyanet, MWL, Egyptian, Karachi, ISNA and more)
• Monthly prayer schedule with today highlighted
• Live countdown to the next prayer
• Works OFFLINE — times are calculated on your device, no internet needed
• Home-screen widget with next-prayer countdown

🧭 QIBLA COMPASS
• Live compass with Kaaba indicator
• Degree readout and alignment guidance
• High-accuracy mode using device sensors

📖 DAILY QURAN VERSES
• A new verse every day with Turkish translation
• Save verses to your favourites
• Tap any reference to read the full surah

⌚ WEAR OS SUPPORT
• Prayer times right on your wrist
• Live tile with next-prayer countdown ring
• Works even without your phone nearby

🚗 ANDROID AUTO SUPPORT
• Daily prayer times on your car display
• Nearby mosques with one-tap navigation to Google Maps

🌙 MORE
• Full app redesign — clean, elegant Material Design
• Dark theme
• Turkish and English languages
• Hijri calendar with manual adjustment
• Prayer-time reminders and notifications
• No ads, no account required, no unnecessary permissions

Namaz Vakitleri is free and open source. Download now and never miss a prayer.

### TR

Namaz Vakitleri, Müslümanlar için güzel ve reklamsız bir namaz vakitleri uygulamasıdır. Bulunduğunuz yer için hassas namaz vakitleri, canlı pusula ile kıble yönü ve günlük Kuran ayetleri — hepsi tek bir şık uygulamada.

🕌 HASSAS NAMAZ VAKİTLERİ
• Dünyanın her yerinden hassas namaz vakitleri
• Çok sayıda hesaplama yöntemi (Diyanet, MWS, Mısır, Karaçi, ISNA ve daha fazlası)
• Bugünün vurgulandığı aylık namaz programı
• Bir sonraki vakite canlı geri sayım
• ÇEVRİMDIŞI çalışır — vakitler cihazınızda hesaplanır, internet gerekmez
• Bir sonraki vakit geri sayımı ile ana ekran widget'ı

🧭 KIBLE PUSULASI
• Kaaba göstergeli canlı pusula
• Derece göstergesi ve hizalama rehberi
• Cihaz sensörlerini kullanan yüksek doğruluk modu

📖 GÜNLÜK KURAN AYETLERİ
• Türkçe çevirisiyle her gün yeni bir ayet
• Sevdiğiniz ayetleri kaydedin
• Referansa dokunun, surenin tamamını okuyun

⌚ WEAR OS DESTEĞİ
• Namaz vakitleri bileğinizde
• Bir sonraki vakit geri sayım halkasıyla canlı tile
• Telefonunuz yanında olmasa bile çalışır

🚗 ANDROID AUTO DESTEĞİ
• Araç ekranınızda günlük namaz vakitleri
• Yakın camiler ve tek dokunuşla Google Maps navigasyonu

🌙 DAHA FAZLASI
• Uygulamanın tamamı yeniden tasarlandı — temiz, şık Material Design
• Koyu tema
• Türkçe ve İngilizce dil desteği
• Manuel ayarlamalı Hicri takvim
• Namaz vakti hatırlatıcıları ve bildirimleri
• Reklam yok, hesap gerekmez, gereksiz izin yok

Namaz Vakitleri ücretsiz ve açık kaynaklıdır. Hemen indirin ve hiçbir vakiti kaçırmayın.

---

## What the 2.0.2 artifacts actually do (Wear OS / Android Auto)

The Wear OS and Android Auto sections above describe capability, not console
state. What backs each one in the built 2.0.2 bundles:

| Claim | Shipped fact |
|---|---|
| Wear OS: prayer times on the wrist, live tile, standalone | the watch bundle is `com.kutluoglu.namazvakitleri` / versionCode `3620200` / versionName `2.0.2`, declares `com.google.android.wearable.standalone=true`, and computes prayer times on-device (data layer → cache → local computation); the app provides a tile with the next-prayer countdown |
| Wear OS: not filtered out on GPS-less watches | `android.hardware.location.gps` / `android.hardware.location.network` are declared `required="false"`; `android.hardware.type.watch` stays required |
| Android Auto: daily times + nearby mosques with Maps handoff | the phone release manifest carries `PrayerCarAppService` (`androidx.car.app.CarAppService` action), the `androidx.car.app.category.POI` category, the `androidx.car.app.MAP_TEMPLATES` permission and `@xml/automotive_app_desc`, all in `prayer_auto/src/main` (previously debug-only); `NearbyMosquesScreen` uses `PlaceListMapTemplate` for the Maps handoff |
| Both form factors in one listing | both bundles carry the same package id `com.kutluoglu.namazvakitleri` and are signed with the same upload key; version codes `202` (phone) and `3620200` (watch) are unique across form factors |

### Console prerequisites (not store copy)

These are console steps, not text — full checklist in `RELEASE-TODO-2.0.2.md`:

- The Wear OS and Android Auto form factors are added under **Advanced settings → Form factors → Add form factor**, *not* on the publishing page. Until they are added, neither form factor is discoverable on Play.
- The Wear OS form factor requires **Wear OS screenshots** in the listing — upload them with this form factor (the 454×454 tile screenshot captured for 2.0.1 is at `/Users/a195143/2026Tasarım/prayer-icons/store/`).
- The Android Auto form factor requires accepting the **Android Auto Addendum**; the car-app quality review (POI category) adds review time before open testing / production goes live.

## Data-safety declaration (console form) — answers mapped from the live privacy policy

Unchanged from 2.0.1.

| Question | Answer |
|---|---|
| Does your app collect or share user data? | **Yes** |
| Location | Collected (approximate + precise) — purpose: app functionality; **not shared** |
| Personal info | Not collected |
| Financial info | Not collected |
| Health & fitness | Not collected |
| Messages | Not collected |
| Photos & videos | Not collected |
| Audio | Not collected |
| Files & docs | Not collected |
| Calendar | Not collected |
| Contacts | Not collected |
| App activity | Collected (app interactions) via Firebase Analytics — purpose: analytics; **not shared** |
| App info & performance | Collected (crash logs, diagnostics) via Firebase Crashlytics — purpose: analytics/stability; **not shared** |
| Device & other IDs | Collected (device IDs, FCM token) — purpose: app functionality (push notifications); **not shared** |
| All data is transmitted over a secure connection? | **Yes** (HTTPS) |
| Does the app allow users to request data deletion? | **Yes** — clear app data / uninstall; no account exists |

## Content rating questionnaire notes

Unchanged from 2.0.1.

- No user-generated content, no ads, no gambling, no mature themes → expect **Everyone / PEGI 3**
- Category: **Lifestyle** (or Books & Reference secondary)

## New features / release notes (console "What's new")

Use `docs/release/RELEASE-NOTES-2.0.2.md` — EN and TR versions ready to paste.
