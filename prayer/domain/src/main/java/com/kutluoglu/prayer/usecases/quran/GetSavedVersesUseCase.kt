package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class GetSavedVersesUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(): Result<List<AyahData>> = repository.getSavedVerses()
}
