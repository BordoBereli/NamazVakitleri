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
    -   `:prayer_cache`: Eski (legacy) JSON-tabanlı DataStore uygulamaları.
    -   `:prayer_remote`: Uzak veri kaynakları (Kuran API, şehir arama).
    -   `:prayer_location`: Konum servisleri ile ilgili mantığı soyutlayan modül.
    -   `:prayer_settings`: Ayarlar veri modelleri, depo ve use case'ler.
    -   `:prayer_qibla`: Sensör/yönelim mantığı ve kıble veri deposu.
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


## 🤝 Katkıda Bulunma

Katkılarınız projeyi daha da geliştirmemize yardımcı olur! Katkıda bulunmak isterseniz, lütfen bir `pull request` açın veya bir `issue` oluşturun.
