package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.SavedVersesSortOrder
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class GetSavedVersesSortOrderUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(): SavedVersesSortOrder = repository.getSavedVersesSortOrder()
}
