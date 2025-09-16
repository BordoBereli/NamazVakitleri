import com.kutluoglu.core.model.CalculationMethod
import java.time.LocalDate
import java.time.LocalTime

data class Prayer (
    val name: String,
    val arabicName: String,
    val time: LocalTime,
    val date: LocalDate,
    val isCurrent: Boolean = false,
    val notificationEnabled: Boolean = false,
    val calculationMethod: CalculationMethod,
    val adjustmentMinutes: Int = 0
)