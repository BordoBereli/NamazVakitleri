package com.kutluoglu.namazvakitleri

import android.content.Context
import android.hardware.SensorManager
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.data.UpdateConfigSource
import com.kutluoglu.app_update.di.AppUpdateModule
import com.kutluoglu.app_update.ui.UpdateViewModel
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.core.designsystem.di.CoreCommonModule
import com.kutluoglu.namazvakitleri.di.AppAnalyticsModule
import com.kutluoglu.prayer.data.di.PrayerDataModule
import com.kutluoglu.prayer.di.PrayerDomainModule
import com.kutluoglu.prayer_feature.common.di.PrayerFeatureCommonModule
import com.kutluoglu.prayer_feature.home.HomeViewModel
import com.kutluoglu.prayer_feature.home.SavedVersesViewModel
import com.kutluoglu.prayer_feature.home.di.PrayerFeatureHomeModule
import com.kutluoglu.prayer_feature.prayertimes.PrayerTimesViewModel
import com.kutluoglu.prayer_feature.prayertimes.di.PrayerFeaturePrayerTimesModule
import com.kutluoglu.prayer_feature.qibla.QiblaViewModel
import com.kutluoglu.prayer_feature.qibla.di.PayerFeatureQiblaModule
import com.kutluoglu.prayer_feature.settings.SettingsViewModel
import com.kutluoglu.prayer_feature.settings.calculation.CalculationMethodViewModel
import com.kutluoglu.prayer_feature.settings.di.PrayerFeatureSettingsModule
import com.kutluoglu.prayer_feature.settings.hijri.HijriAdjustmentViewModel
import com.kutluoglu.prayer_feature.settings.juristic.JuristicMethodViewModel
import com.kutluoglu.prayer_feature.settings.language.LanguageSelectionViewModel
import com.kutluoglu.prayer_feature.settings.location.LocationSelectionViewModel
import com.kutluoglu.prayer_feature.settings.location.MyLocationsViewModel
import com.kutluoglu.prayer_feature.settings.notifications.NotificationsViewModel
import com.kutluoglu.prayer_location.di.PrayerLocationModule
import com.kutluoglu.prayer_notifications.di.PrayerNotificationsModule
import com.kutluoglu.prayer_notifications.manager.NotificationDisplayer
import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
import com.kutluoglu.prayer_qibla.di.PrayerQiblaModule
import com.kutluoglu.prayer_remote.di.PrayerRemoteModule
import com.kutluoglu.prayer_settings.di.PrayerSettingsModule
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_widget.di.PrayerWidgetModule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.ksp.generated.module

/**
 * Verifies the full Koin graph (the same module set as [NamazVakitleriApplication.configurationModules])
 * boots without duplicate definitions and that every `@KoinViewModel` resolves and constructs.
 *
 * The `:app` module has no Robolectric, so Android/Firebase leaf dependencies that cannot be
 * constructed on the JVM are provided as relaxed mocks in [mockModule]. The mock module is loaded
 * last so its definitions override the real (unconstructable) providers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinGraphVerificationTest {

    private val allModules: List<Module> = listOf(
        AppAnalyticsModule.module,
        AppModule.module,
        AppUpdateModule.module,
        PrayerDataModule.module,
        PrayerWidgetModule.module,
        PayerFeatureQiblaModule.module,
        PrayerDomainModule.module,
        PrayerLocationModule.module,
        PrayerSettingsModule.module,
        PrayerNotificationsModule.module,
        PrayerFeatureHomeModule.module,
        CoreCommonModule.module,
        PrayerFeatureCommonModule.module,
        PrayerRemoteModule.module,
        PrayerQiblaModule.module,
        PrayerFeaturePrayerTimesModule.module,
        PrayerFeatureSettingsModule.module
    )

    private val mockModule: Module = module {
        single<Context> {
            val context = mockk<Context>(relaxed = true)
            every { context.applicationContext } returns context
            every { context.getSystemService(any<String>()) } returns mockk<SensorManager>(relaxed = true)
            context
        }
        single<AnalyticsTracker> { mockk(relaxed = true) }
        single<AlarmScheduler> { mockk(relaxed = true) }
        single<NotificationDisplayer> { mockk(relaxed = true) }
        single<UpdateConfigSource> { mockk(relaxed = true) }
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `all modules boot without duplicate definitions`() {
        // allowOverride(false) makes any duplicate definition throw DefinitionOverrideException
        // at module load time, catching a re-added DSL registration alongside an annotation.
        val koin = koinApplication {
            allowOverride(false)
            modules(allModules)
        }.koin

        assertThat(koin).isNotNull()
    }

    @Test
    fun `every ViewModel resolves from the full Koin graph`() {
        val koin = koinApplication {
            modules(allModules + mockModule)
        }.koin

        assertThat(koin.get<HomeViewModel>()).isNotNull()
        assertThat(koin.get<PrayerTimesViewModel>()).isNotNull()
        assertThat(koin.get<QiblaViewModel>()).isNotNull()
        assertThat(koin.get<SettingsViewModel>()).isNotNull()
        assertThat(koin.get<LocationSelectionViewModel>()).isNotNull()
        assertThat(koin.get<MyLocationsViewModel>()).isNotNull()
        assertThat(koin.get<CalculationMethodViewModel>()).isNotNull()
        assertThat(koin.get<HijriAdjustmentViewModel>()).isNotNull()
        assertThat(koin.get<JuristicMethodViewModel>()).isNotNull()
        assertThat(koin.get<LanguageSelectionViewModel>()).isNotNull()
        assertThat(koin.get<NotificationsViewModel>()).isNotNull()
        assertThat(koin.get<SavedVersesViewModel>()).isNotNull()
        assertThat(koin.get<UpdateViewModel>()).isNotNull()
    }

    @Test
    fun `representative types have exactly one definition`() {
        val koin = koinApplication {
            modules(allModules + mockModule)
        }.koin

        assertThat(koin.getAll<GetSettingsUseCase>().size).isEqualTo(1)
        assertThat(koin.getAll<SettingsRepository>().size).isEqualTo(1)
        assertThat(koin.getAll<HomeViewModel>().size).isEqualTo(1)
    }
}
