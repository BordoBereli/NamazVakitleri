# Namaz Vakitleri Android Uygulaması

Namaz Vakitleri, kullanıcıların bulundukları konuma göre günlük namaz vakitlerini takip etmelerini sağlayan modern ve sezgisel bir Android uygulamasıdır. Temiz bir mimari ve Jetpack Compose kullanılarak geliştirilen bu uygulama, güvenilir ve zengin bir kullanıcı deneyimi sunar.

## ✨ Özellikler

-   **Konum Tabanlı Vakitler**: Bulunduğunuz konuma göre en doğru namaz vakitlerini otomatik olarak hesaplar.
-   **Çoklu Konum Desteği**: Sınırsız konum ekleyebilir; her konum için ayrı bir ana ekran sayfası (kaydırılabilir `HorizontalPager` + konum çipleri) kullanabilirsiniz.
-   **Otomatik GPS Konumu**: İsteğe bağlı otomatik GPS konumu, manuel konumlardan görsel olarak ayırt edilir (GPS rozeti).
-   **Dinamik Geri Sayım**: Bir sonraki namaz vaktine ne kadar kaldığını gösteren canlı bir geri sayım sayacı içerir.
-   **Aylık Vakitler**: Ay navigasyonu ile geçmiş ve gelecek ayların namaz vakitlerini görüntüleyebilirsiniz.
-   **Kıble Pusulası**: Aktif konuma göre kıble yönünü gösteren pusula ekranı.
-   **Kuran Ayetleri**: Her gün rastgele bir Kuran ayeti göstererek manevi bir dokunuş sağlar. Ayetler, detaylarını görmek ve paylaşmak için interaktif bir modal ekranda açılabilir.
-   **Modern Arayüz**: Tamamen Jetpack Compose ile oluşturulmuş, açık ve koyu tema desteği sunan şık ve kullanıcı dostu bir arayüz.
-   **Çok Dilli Destek**: Namaz isimleri gibi metinler, cihazın diline göre yerelleştirilmiştir.
-   **Pull-to-Refresh**: Vakitleri manuel olarak yenilemek için aşağı çekme özelliği.

## 🛠️ Teknik Yapı ve Mimari

Bu proje, ölçeklenebilir, test edilebilir ve bakımı kolay bir uygulama oluşturmak için modern Android geliştirme prensipleri üzerine kurulmuştur.

-   **%100 Kotlin & Jetpack Compose**: Tüm kullanıcı arayüzü, reaktif ve deklaratif bir yaklaşımla Jetpack Compose kullanılarak oluşturulmuştur.
-   **Temiz Mimari (Clean Architecture)**: Proje, sorumlulukları ayıran katmanlı bir yapıya sahiptir:
    -   `:app`: Ana uygulama modülü, bağımlılıkların (Koin) başlatılması ve Firebase kurulumu.
    -   `:core:designsystem`, `:core:common`: Tema, renkler, paylaşılan bileşenler ve yardımcı fonksiyonlar.
    -   `:prayer_navigation:core`: Navigasyon hedefleri ve iç içe graflar.
    -   `:prayer:domain`, `:prayer:model`, `:prayer:data`: İş mantığı, veri modelleri ve veri kaynakları (DataStore tabanlı önbellek).
    -   `:prayer_remote`: Uzak veri kaynakları (Kuran API, şehir arama).
    -   `:prayer_location`: Konum servisleri ile ilgili mantığı soyutlayan modül.
    -   `:prayer_settings`: Ayarlar veri modelleri, depo ve use case'ler.
    -   `:prayer_qibla`: Sensör/yönelim mantığı ve kıble veri deposu.
    -   `:prayer_notifications`: Bildirimler (AlarmManager, WorkManager, FCM).
    -   `:prayer_widget`: Glance ana ekran widget'ı.
    -   `:app_update`: Uzaktan yapılandırma (Remote Config) ile yönlendirilen uygulama içi güncelleme akışı.
    -   `:prayer_feature:*`: Her bir özelliğe (`home`, `prayertimes`, `qibla`, `settings`, `common`) adanmış modüller.
-   **MVVM Mimarisi**: Her özellik ekranı, durumu yöneten ve iş mantığını yürüten bir `ViewModel` tarafından desteklenmektedir.
-   **Coroutines & Flow**: Asenkron işlemler ve reaktif durum yönetimi için kullanılır. `StateFlow`, UI durumunu `ViewModel`'den `Composable`'lara güvenli bir şekilde iletmek için kullanılır.
-   **Koin**: Bağımlılıkların yönetimi (Dependency Injection) için kullanılır.
-   **Turbine & MockK**: `ViewModel` ve `Flow` tabanlı mantığın test edilmesi için kullanılır.

## 🚀 Projeyi Kurma ve Çalıştırma

Bu projeyi yerel makinenizde kurmak ve çalıştırmak için aşağıdaki adımları izleyin:

1.  **Projeyi Klonlayın**:

2.  **Android Studio'da Açın**:
    -   Android Studio'yu açın.
    -   "Open an existing Project" (Mevcut bir projeyi aç) seçeneğini seçin ve klonladığınız proje dizinine gidin.

3.  **Gradle Senkronizasyonu**:
    -   Android Studio, projeyi açtıktan sonra bağımlılıkları indirmek ve projeyi senkronize etmek için otomatik olarak Gradle'ı çalıştıracaktır. Bu işlem birkaç dakika sürebilir.

4.  **Uygulamayı Çalıştırın**:
    -   Bir emülatör seçin veya fiziksel bir Android cihaz bağlayın.
    -   Android Studio'daki "Run 'app'" (▶️) düğmesine tıklayın.

## ✅ Testler

Proje, iş mantığının doğruluğunu sağlamak için birim testleri (unit tests) içerir. Testleri çalıştırmak için:

-   **Android Studio'dan**:
    -   Test etmek istediğiniz dosyayı (örneğin, `HomeViewModelTest.kt`) açın.
    -   Sınıf adının yanındaki yeşil "play" ikonuna tıklayarak tüm testleri çalıştırın.

-   **Gradle ile Komut Satırından**:
    ```shell
    ./gradlew testDebugUnitTest
    ```

---

Bu komut, projenizdeki tüm modüllerde bulunan "debug" derleme varyantına ait birim testlerini (unit tests) çalıştıracaktır.


## ⌚ Wear OS Tile (Akıllı Saat Kartı)

Uygulama, telefonla eşleştirilmiş bir Wear OS akıllı saatte bir **namaz kartı (tile)** sunar. Kart; bir sonraki namazı, canlı geri sayımı ve altın geri sayım halkasını gösterir. Veriler, telefonun `WatchDataSyncer` bileşeni tarafından Wearable Data Layer üzerinden saate iletilir.

### Mimari

-   `:prayer_wear_shared` — `WatchTileData` modeli, JSON/DataMap codec'i ve geri sayım/ring hesaplamaları.
-   `:prayer_widget` — `WatchDataSyncer` ile mevcut widget yenileme hattı üzerinden veriyi saate gönderir; `WatchDataSyncListenerService` saatten gelen senkron isteklerini dinler ve yeniden gönderir.
-   `:wear` — `PrayerTileService` (Material3TileService) tek sayfalı kartı çizer (altın geri sayım halkası + sonraki namaz); `TileDataRepository` veriyi DataClient'tan okur ve yerel DataStore önbelleğine düşer. Kartta veri yoksa telefona `MessageClient` ile senkron isteği gönderir (pull).

### Önemli Notlar

-   **Wear OS 2'de kart çalışmaz** — kartlar Wear OS 3+ (API 30+) gerektirir.
-   **Veri katmanı aynı imza anahtarını gerektirir** — iki debug APK sorunsuzdur; debug telefon + release saat karıştırmayın.
-   **Widget hattı olmadan senkronizasyon olmaz** — widget'ı hiç yerleştirmezseniz veri yalnızca ayar/konum değişikliklerinde senkronize olur. Kart "Telefonunuzda uygulamayı açın" gösteriyorsa 3. adımdaki senkronizasyonu tetikleyin.
-   **Samsung Galaxy Watch'ta Google veri katmanı kararsız olabilir** — bazı Samsung Galaxy Watch cihazlarında (ör. Galaxy Watch 8) Google Play Services, Bluetooth bağlı olsa bile veri katmanı ağını `DISCONNECTED` olarak raporlar. Bu durumda `getDataItems()` boş döner ve `WatchDataListener` tetiklenmez; kart yerel DataStore önbelleğine düşer. Uygulama kodu doğrudur (emülatörde çalışır) ve bu, cihazın GMS veri katmanı bağlantısıyla ilgili Samsung'a özgü bir sorundur. Kartın senkron isteği (pull) bu bağlantı kullanılabilir olduğunda çalışır; bağlantı tamamen kopuksa cihaz düzeyinde çözüm gerekir (eşleştirmeyi sıfırlama, GMS güncellemesi, Wi-Fi üzerinden bağlanma).

### Gerçek Saatte Test Etme

1.  **ADB Bağlantısı**:
    -   Saatte: **Ayarlar → Sistem → Hakkında → "Derleme numarası"na 7 kez dokunun** (geliştirici seçeneklerini açar).
    -   **Ayarlar → Geliştirici seçenekleri → ADB hata ayıklamayı etkinleştirin** (ve kablosuz hata ayıklamayı).
    -   Mac'inizden eşleştirin ve bağlanın (Wear OS 3+ kablosuz eşleştirme kullanır):
        ```shell
        adb pair <saat-ip>:<eşleştirme-portu>   # saatte görünür
        adb connect <saat-ip>:<port>
        adb devices                              # "device" olarak göründüğünü doğrulayın
        ```

2.  **Her İki Uygulamayı Kurun** (aynı imza anahtarı gerekir):
    ```shell
    ./gradlew :wear:installDebug    # saate kurar (bağlı cihaz)
    ./gradlew :app:installDebug     # telefona kurar
    ```
    > Her iki cihaz da bağlıysa hedefi belirtin: `adb -s <saat-serial> install -r wear/build/outputs/apk/debug/wear-debug.apk`.

3.  **Veri Senkronizasyonunu Tetikleyin**:
    -   Telefonda uygulamayı açın, **bir konum seçin** ve **ana ekran widget'ını yerleştirin** (dakikalık tick + worker'ı programlar) **veya** bir ayarı/konumu değiştirin (`WidgetRefresher`'ı tetikler).
    -   Telefonda logcat ile doğrulayın: `adb logcat -s WatchDataSyncer`.

4.  **Kartı Saate Ekleyin**:
    -   Saat yüzünden **sola kaydırın** → kart karuselinde **"+"** simgesine gidin → **Namaz Vakitleri** kartını bulun → ekleyin.
    -   Kart: sonraki namaz + canlı geri sayım + altın halka (tek sayfa).

5.  **Doğrulama**:
    -   Geri sayım her dakika güncellenmeli (yenileme aralığı 60 sn).
    -   Telefon uygulamasını kapatın / menzil dışına çıkın → kart, "Son senkron: HH:mm" satırıyla (5 dk sonra bayat gösterge) önbelleğe alınmış veriyi göstermeli.

## 🤝 Katkıda Bulunma

Katkılarınız projeyi daha da geliştirmemize yardımcı olur! Katkıda bulunmak isterseniz, lütfen bir `pull request` açın veya bir `issue` oluşturun.
