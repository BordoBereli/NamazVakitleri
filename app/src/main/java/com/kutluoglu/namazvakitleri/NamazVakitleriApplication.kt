package com.kutluoglu.namazvakitleri

import android.app.Application
import com.kutluoglu.core.ui.theme.di.CoreCommonModule
import com.kutluoglu.prayer.data.di.PrayerDataModule
import com.kutluoglu.prayer.di.PrayerDomainModule
import com.kutluoglu.prayer_feature.home.di.PrayerFeatureHomeModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module

class NamazVakitleriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@NamazVakitleriApplication)
            modules(
                PrayerDomainModule.module,
                PrayerDataModule.module,
                PrayerFeatureHomeModule.module,
                CoreCommonModule.module
            )
        }
    }
}