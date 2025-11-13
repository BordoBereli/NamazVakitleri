package com.kutluoglu.prayer.usecases

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

/**
 * Created by F.K. on 11.11.2025.
 *
 */

@Factory
class GetRandomVerseUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(): Result<AyahData> =
        repository.getRandomVerse()
}