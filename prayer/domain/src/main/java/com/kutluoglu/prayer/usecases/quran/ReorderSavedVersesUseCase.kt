package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class ReorderSavedVersesUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(verses: List<AyahData>): Result<Unit> =
        repository.reorderSavedVerses(verses)
}
