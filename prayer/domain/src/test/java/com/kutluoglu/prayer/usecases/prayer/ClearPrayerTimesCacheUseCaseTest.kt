package com.kutluoglu.prayer.usecases.prayer

import com.kutluoglu.prayer.repository.IPrayerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ClearPrayerTimesCacheUseCaseTest {

    @Test
    fun `invoke clears the prayer times cache via the repository`() = runTest {
        // GIVEN a repository
        val repository = mockk<IPrayerRepository>()
        coEvery { repository.clearCache() } returns Unit
        val useCase = ClearPrayerTimesCacheUseCase(repository)

        // WHEN clearing the cache
        useCase()

        // THEN the repository's clearCache is called exactly once
        coVerify(exactly = 1) { repository.clearCache() }
    }
}
