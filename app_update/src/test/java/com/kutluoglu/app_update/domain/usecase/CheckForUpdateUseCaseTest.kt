package com.kutluoglu.app_update.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.model.UpdateInfo
import com.kutluoglu.app_update.domain.repository.UpdateRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CheckForUpdateUseCaseTest {

    private val repository = mockk<UpdateRepository>()

    private fun info(
        latest: Int = 200,
        min: Int = 150,
        force: List<Int> = emptyList(),
        optional: List<Int> = emptyList(),
    ) = UpdateInfo(
        latestVersionCode = latest,
        minVersionCode = min,
        latestVersionName = "2.0",
        releaseNotes = "notes",
        directDownloadUrl = "https://example.com/app.apk",
        forceVersionCodes = force,
        optionalVersionCodes = optional,
    )

    @Test
    fun `returns ForceUpdate when installed below minimum`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 100)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.ForceUpdate(info()))
    }

    @Test
    fun `returns OptionalUpdate when installed at minimum but below latest`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 150)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.OptionalUpdate(info()))
    }

    @Test
    fun `returns OptionalUpdate when installed between min and latest`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 175)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.OptionalUpdate(info()))
    }

    @Test
    fun `returns NoUpdate when installed at latest`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 200)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
    }

    @Test
    fun `returns NoUpdate when installed above latest`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info()
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 300)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
    }

    @Test
    fun `returns NoUpdate when repository returns null`() = runTest {
        coEvery { repository.getUpdateInfo() } returns null
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 100)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
    }

    @Test
    fun `returns ForceUpdate when installed version is in force list`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info(force = listOf(175))
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 175)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.ForceUpdate(info(force = listOf(175))))
    }

    @Test
    fun `returns OptionalUpdate when installed version is in optional list`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info(optional = listOf(175))
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 175)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.OptionalUpdate(info(optional = listOf(175))))
    }

    @Test
    fun `returns NoUpdate when installed version is not targeted`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info(
            latest = 190,
            min = 190,
            force = listOf(175),
            optional = listOf(180),
        )
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 190)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
    }

    @Test
    fun `force list takes precedence over min version`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info(min = 150, force = listOf(175))
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 175)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.ForceUpdate(info(min = 150, force = listOf(175))))
    }

    @Test
    fun `force list takes precedence over optional list`() = runTest {
        coEvery { repository.getUpdateInfo() } returns info(force = listOf(175), optional = listOf(175))
        val useCase = CheckForUpdateUseCase(repository, currentVersionCode = 175)

        val decision = useCase()

        assertThat(decision).isEqualTo(UpdateDecision.ForceUpdate(info(force = listOf(175), optional = listOf(175))))
    }
}
