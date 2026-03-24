# Prayer Times Android Application - Technical Architecture Analysis
**Part 2: Qibla, Infrastructure, Testing & Deployment**

*Continuation of ANDROID_TECHNICAL_ANALYSIS.md*

---

## 4. Qibla Direction Module

### 4.1 Domain Layer (`feature:qibla:domain`)

#### **4.1.1 Domain Models**

```kotlin
// feature/qibla/domain/src/main/kotlin/com/prayertimes/qibla/domain/model/

//package com.prayertimes.qibla.domain.model

//import kotlin.math.*

/**
 * Kaaba coordinates in Mecca, Saudi Arabia
 */
object KaabaCoordinates {
    const val LATITUDE = 21.4225
    const val LONGITUDE = 39.8262
}

/**
 * Qibla direction information
 */
data class QiblaDirection(
    val qiblaAngle: Double, // 0-360 degrees from North
    val cardinalDirection: CardinalDirection,
    val distanceToKaaba: Distance,
    val userLocation: Coordinates
) {
    val qiblaAngleFormatted: String
        get() = "${qiblaAngle.toInt()}°"
}

/**
 * Cardinal direction representation
 */
enum class CardinalDirection(val displayName: String) {
    NORTH("North"),
    NORTH_EAST("North-East"),
    EAST("East"),
    SOUTH_EAST("South-East"),
    SOUTH("South"),
    SOUTH_WEST("South-West"),
    WEST("West"),
    NORTH_WEST("North-West");
    
    companion object {
        fun fromAngle(angle: Double): CardinalDirection {
            val normalized = (angle + 360) % 360
            return when {
                normalized < 22.5 -> NORTH
                normalized < 67.5 -> NORTH_EAST
                normalized < 112.5 -> EAST
                normalized < 157.5 -> SOUTH_EAST
                normalized < 202.5 -> SOUTH
                normalized < 247.5 -> SOUTH_WEST
                normalized < 292.5 -> WEST
                normalized < 337.5 -> NORTH_WEST
                else -> NORTH
            }
        }
    }
}

/**
 * Distance to Kaaba
 */
data class Distance(
    val kilometers: Double
) {
    val miles: Double
        get() = kilometers * 0.621371
    
    val displayText: String
        get() = "${kilometers.toInt().formatWithCommas()} km"
    
    private fun Int.formatWithCommas(): String {
        return toString().reversed().chunked(3).joinToString(",").reversed()
    }
}

/**
 * Coordinates with validation
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}

/**
 * Compass heading from device
 */
data class CompassHeading(
    val azimuth: Double, // 0-360 degrees from magnetic north
    val accuracy: CompassAccuracy,
    val magneticDeclination: Double = 0.0
) {
    /**
     * True north heading (adjusted for magnetic declination)
     */
    val trueNorth: Double
        get() = (azimuth + magneticDeclination + 360) % 360
}

/**
 * Compass accuracy levels
 */
enum class CompassAccuracy(val displayName: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    UNRELIABLE("Unreliable"),
    UNAVAILABLE("Unavailable");
    
    val isReliable: Boolean
        get() = this in listOf(HIGH, MEDIUM)
}

/**
 * Compass calibration state
 */
sealed class CalibrationState {
    object NotCalibrated : CalibrationState()
    data class Calibrating(val progress: Int) : CalibrationState() // 0-100
    object Calibrated : CalibrationState()
    data class Failed(val reason: String) : CalibrationState()
}

/**
 * Calibration data collected during process
 */
data class CalibrationData(
    val readings: List<SensorReading>,
    val startTime: Long,
    val endTime: Long?,
    val variance: Double
) {
    val duration: Long?
        get() = endTime?.let { it - startTime }
    
    val isStable: Boolean
        get() = variance < VARIANCE_THRESHOLD
    
    companion object {
        private const val VARIANCE_THRESHOLD = 10.0
    }
}

/**
 * Individual sensor reading
 */
data class SensorReading(
    val azimuth: Double,
    val pitch: Double,
    val roll: Double,
    val timestamp: Long
)

/**
 * Qibla calculation result wrapper
 */
sealed class QiblaResult<out T> {
    data class Success<T>(val data: T) : QiblaResult<T>()
    data class Error(val exception: QiblaException) : QiblaResult<Nothing>()
    object Loading : QiblaResult<Nothing>()
}

/**
 * Qibla-specific exceptions
 */
sealed class QiblaException(message: String) : Exception(message) {
    object SensorNotAvailable : QiblaException("Device does not have required sensors")
    object LocationPermissionDenied : QiblaException("Location permission is required")
    object LocationUnavailable : QiblaException("Unable to determine location")
    data class CalibrationFailed(val reason: String) : QiblaException("Calibration failed: $reason")
    data class UnknownError(val throwable: Throwable) : QiblaException(throwable.message ?: "Unknown error")
}
```

#### **4.1.2 Qibla Calculation Mathematics**

```kotlin
// feature/qibla/domain/src/main/kotlin/com/prayertimes/qibla/domain/calculator/

//package com.prayertimes.qibla.domain.calculator

//import com.prayertimes.qibla.domain.model.*
//import kotlin.math.*

/**
 * Qibla direction calculator using spherical trigonometry
 * 
 * Formula: Qibla angle from north
 * θ = atan2(sin(Δλ), cos(φ₁) × tan(φ₂) - sin(φ₁) × cos(Δλ))
 * 
 * Where:
 * φ₁ = user latitude (in radians)
 * φ₂ = Kaaba latitude (in radians)
 * Δλ = longitude difference (in radians)
 */
class QiblaCalculator {
    
    /**
     * Calculate Qibla direction from user's location
     */
    fun calculateQiblaDirection(userLocation: Coordinates): QiblaDirection {
        val qiblaAngle = calculateQiblaAngle(userLocation)
        val cardinalDirection = CardinalDirection.fromAngle(qiblaAngle)
        val distance = calculateDistanceToKaaba(userLocation)
        
        return QiblaDirection(
            qiblaAngle = qiblaAngle,
            cardinalDirection = cardinalDirection,
            distanceToKaaba = distance,
            userLocation = userLocation
        )
    }
    
    /**
     * Calculate Qibla angle using spherical trigonometry
     */
    private fun calculateQiblaAngle(userLocation: Coordinates): Double {
        // Convert to radians
        val lat1 = Math.toRadians(userLocation.latitude)
        val lng1 = Math.toRadians(userLocation.longitude)
        val lat2 = Math.toRadians(KaabaCoordinates.LATITUDE)
        val lng2 = Math.toRadians(KaabaCoordinates.LONGITUDE)
        
        // Calculate longitude difference
        val deltaLng = lng2 - lng1
        
        // Calculate Qibla angle using spherical trigonometry
        val y = sin(deltaLng)
        val x = cos(lat1) * tan(lat2) - sin(lat1) * cos(deltaLng)
        
        var qiblaAngle = Math.toDegrees(atan2(y, x))
        
        // Normalize to 0-360 range
        qiblaAngle = (qiblaAngle + 360) % 360
        
        return qiblaAngle
    }
    
    /**
     * Calculate distance to Kaaba using Haversine formula
     * 
     * Formula:
     * a = sin²(Δφ/2) + cos(φ₁) × cos(φ₂) × sin²(Δλ/2)
     * c = 2 × atan2(√a, √(1-a))
     * d = R × c
     * 
     * Where R is Earth's radius (6371 km)
     */
    private fun calculateDistanceToKaaba(userLocation: Coordinates): Distance {
        val earthRadiusKm = 6371.0
        
        // Convert to radians
        val lat1 = Math.toRadians(userLocation.latitude)
        val lng1 = Math.toRadians(userLocation.longitude)
        val lat2 = Math.toRadians(KaabaCoordinates.LATITUDE)
        val lng2 = Math.toRadians(KaabaCoordinates.LONGITUDE)
        
        // Haversine formula
        val deltaLat = lat2 - lat1
        val deltaLng = lng2 - lng1
        
        val a = sin(deltaLat / 2).pow(2) + 
                cos(lat1) * cos(lat2) * sin(deltaLng / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        val distanceKm = earthRadiusKm * c
        
        return Distance(kilometers = distanceKm)
    }
    
    /**
     * Calculate compass rotation needed to point to Qibla
     */
    fun calculateCompassRotation(
        currentHeading: CompassHeading,
        qiblaAngle: Double
    ): Double {
        // Use true north (corrected for magnetic declination)
        val currentTrueNorth = currentHeading.trueNorth
        
        // Calculate how much to rotate from current heading to Qibla
        var rotation = qiblaAngle - currentTrueNorth
        
        // Normalize to -180 to 180 range for shortest rotation
        while (rotation > 180) rotation -= 360
        while (rotation < -180) rotation += 360
        
        return rotation
    }
}
```

#### **4.1.3 Use Cases**

```kotlin
// feature/qibla/domain/src/main/kotlin/com/prayertimes/qibla/domain/usecase/

//package com.prayertimes.qibla.domain.usecase

//import com.prayertimes.qibla.domain.calculator.QiblaCalculator
//import com.prayertimes.qibla.domain.model.*
//import com.prayertimes.qibla.domain.repository.QiblaRepository
//import kotlinx.coroutines.flow.Flow

class CalculateQiblaDirectionUseCase(
    private val repository: QiblaRepository,
    private val calculator: QiblaCalculator
) {
    suspend operator fun invoke(): QiblaResult<QiblaDirection> {
        return when (val result = repository.getUserLocation()) {
            is QiblaResult.Success -> {
                val qiblaDirection = calculator.calculateQiblaDirection(result.data)
                QiblaResult.Success(qiblaDirection)
            }
            is QiblaResult.Error -> QiblaResult.Error(result.exception)
            QiblaResult.Loading -> QiblaResult.Loading
        }
    }
}

class ObserveCompassHeadingUseCase(
    private val repository: QiblaRepository
) {
    operator fun invoke(): Flow<QiblaResult<CompassHeading>> {
        return repository.observeCompassHeading()
    }
}
```

*[Continuing in separate file due to length...]*

---

## 5. Dependency Injection, Navigation & Testing

See **ANDROID_TECHNICAL_ANALYSIS_PART3.md** for:
- Complete Koin DI modules
- Navigation Compose setup
- Testing strategy with JUnit5 + Truth + Turbine
- Gradle configuration
- Deployment checklist