package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class GetCollapsedSurahsUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(): Set<Int> = repository.getCollapsedSurahs()
}
