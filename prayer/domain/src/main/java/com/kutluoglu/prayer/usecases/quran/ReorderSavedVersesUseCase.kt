package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class ReorderSavedVersesUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(groups: List<SavedVerseGroup>): Result<Unit> =
        repository.reorderSavedVerses(groups)
}
