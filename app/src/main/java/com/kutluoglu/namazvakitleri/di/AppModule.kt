package com.kutluoglu.namazvakitleri.di

import com.kutluoglu.namazvakitleri.ui.viewModel.HomeViewModel
import com.kutluoglu.prayer.data.PrayerRepository
import com.kutluoglu.prayer.domain.PrayerTimeEngine
import com.kutluoglu.prayer.repository.IPrayerRepository
import com.kutluoglu.prayer.services.PrayerCalculationService
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    val serviceModule = module {
        single<PrayerCalculationService> {
            PrayerTimeEngine()
        }
    }

    val repositoryModule = module {
        single<IPrayerRepository> {
            PrayerRepository(prayerCalculationService = get())
        }
    }

    val useCaseModule = module {
        factory { GetPrayerTimesUseCase(prayerRepository = get()) }
    }

    val viewModelModule = module {
        viewModel {
            HomeViewModel(getPrayerTimesUseCase = get())
        }
    }
}