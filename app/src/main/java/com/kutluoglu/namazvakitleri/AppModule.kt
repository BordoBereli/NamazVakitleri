package com.kutluoglu.namazvakitleri

import com.kutluoglu.prayer_feature.settings.SettingsViewModel
import com.kutluoglu.prayer_feature.settings.calculation.CalculationMethodViewModel
import com.kutluoglu.prayer_feature.settings.hijri.HijriAdjustmentViewModel
import com.kutluoglu.prayer_feature.settings.language.LanguageSelectionViewModel
import com.kutluoglu.prayer_feature.settings.location.LocationSelectionViewModel
import com.kutluoglu.prayer.data.repository.location.LocationDataStore
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import com.kutluoglu.prayer_settings.data.repository.SettingsRepositoryImpl
import com.kutluoglu.prayer_settings.domain.repository.LocationRepository
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.ClearLocationCacheUseCase
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_settings.domain.usecase.SearchLocationUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateCalculationMethodUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateHijriAdjustmentUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLanguageUseCase
import com.kutluoglu.prayer_settings.domain.usecase.UpdateLocationUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule: Module = module {
    // Settings DataStore (singleton to share data)
    single { SettingsDataStore(get()) }
    
    // Settings Repository (singleton to share flow between Settings and Home)
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get()) }
    
    // Location Repository (for LocationSelectionViewModel)
    factory<LocationRepository> { get<com.kutluoglu.prayer_settings.data.repository.LocationRepositoryImpl>() }
    
    // Settings UseCases
    factory { GetSettingsUseCase(get()) }
    factory { UpdateLocationUseCase(get()) }
    factory { UpdateCalculationMethodUseCase(get()) }
    factory { UpdateLanguageUseCase(get()) }
    factory { UpdateHijriAdjustmentUseCase(get()) }
    factory { ClearLocationCacheUseCase(get()) }
    factory { SearchLocationUseCase(get()) }
    
    // Settings ViewModels
    single { SettingsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { LocationSelectionViewModel(get(), get(), get(), get()) }
    factory { CalculationMethodViewModel() }
    factory { HijriAdjustmentViewModel() }
    factory { LanguageSelectionViewModel() }
}
