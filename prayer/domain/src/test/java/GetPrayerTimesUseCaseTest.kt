import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.CalculationMethod
import com.kutluoglu.prayer.model.Prayer
import com.kutluoglu.prayer.repository.IPrayerRepository
import com.kutluoglu.prayer.usecases.GetPrayerTimesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class GetPrayerTimesUseCaseTest {
    private lateinit var prayerRepository: IPrayerRepository
    private lateinit var useCase: GetPrayerTimesUseCase

    @Before
    fun setUp() {
        prayerRepository = mockk()
        useCase = GetPrayerTimesUseCase(prayerRepository)
    }

    @Test
    fun `invoke should return Success Result when repository succeeds`() = runTest {
        // Arrange
        val testDate = LocalDate.of(2024, 1, 1)
        val mockPrayerList = listOf(
                Prayer(
                    name = "Fajr",
                    arabicName = "الفجر",
                    time = LocalTime.of(5, 0),
                    date = testDate,
                    isCurrent = false,
                    notificationEnabled = false,
                    calculationMethod = CalculationMethod.TURKEY_DIYANET,
                    adjustmentMinutes = 0
                )
            )
        coEvery { prayerRepository.getPrayerTimes(any(), any(), any()) } returns mockPrayerList

        // Act
        val result = useCase(LocalDate.now(), 41.0, 29.0)

        // Assert
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(mockPrayerList)
    }

    @Test
    fun `invoke should return Failure Result when repository throws exception`() = runTest {
        // Arrange
        val exception = RuntimeException("Database error")
        coEvery { prayerRepository.getPrayerTimes(any(), any(), any()) } throws exception

        // Act
        val result = useCase(LocalDate.now(), 41.0, 29.0)

        // Assert
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }
}