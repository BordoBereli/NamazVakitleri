package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class SetCollapsedSurahsUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(surahs: Set<Int>) {
        repository.setCollapsedSurahs(surahs)
    }
}
