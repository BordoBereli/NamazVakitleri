# AGENTS.md - Developer Guide for NamazVakitleri

This document provides essential information for agentic coding agents working on this Android/Kotlin project.

## Project Overview

- **Type**: Android Application with Jetpack Compose
- **Language**: Kotlin 2.2.20
- **Architecture**: Clean Architecture with MVVM
- **DI**: Koin (with KSP annotations)
- **Build System**: Gradle (Kotlin DSL)

## Build Commands

### Run the App
```bash
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease     # Release build
```

### Testing
```bash
./gradlew testDebugUnitTest   # Run all unit tests
./gradlew unitTests           # Run unit tests (excludes suites)
./gradlew testSuites          # Run test suites only
./gradlew allTests            # Run all tests (units + suites)
./gradlew listTestClasses     # List all available test classes
```

### Build & Analysis
```bash
./gradlew build               # Full build
./gradlew clean               # Clean build artifacts
./gradlew dependencies        # Show dependencies
```

## Project Structure

```
:app                   # Main application module
:core:common           # Shared utilities, extensions, base classes
:core:designsystem     # Reusable composables, MaterialTheme, colors, typography
:prayer:domain         # Business logic, use cases
:prayer:model          # Data classes
:prayer:data           # Repository implementations
:prayer:cache          # Local data sources
:prayer:remote         # Remote data sources
:prayer_location       # Location services wrapper
:prayer_feature        # All UI related screens main module
:prayer_feature:home   # Home screen feature
:prayer_feature:prayertimes  # Prayer times feature
:prayer_feature:qibla  # Qibla direction feature
:prayer_feature:settings     # Settings feature
```

## Code Style Guidelines

### Naming Conventions

- **Packages**: Reverse domain (`com.kutluoglu.*`)
- **Classes**: PascalCase (`HomeViewModel`, `PrayerTimesRepository`)
- **Functions**: camelCase (`loadPrayerTimesForCurrentLocation`)
- **Constants**: SCREAMING_SNAKE_CASE
- **ViewModels**: `*ViewModel` suffix (`HomeViewModel`, `PrayerTimesViewModel`)
- **UiState**: `*UiState` suffix with sealed class hierarchy (`HomeUiState`)
- **Events**: `*Event` suffix sealed class (`HomeEvent`)
- **Use Cases**: `*UseCase` suffix (`GetPrayerTimesUseCase`)

### Kotlin Conventions

- **Explicit return types**: Always specify return types for public functions
- **Null safety**: Use Kotlin null safety (`?`, `?:`, `?.`) - avoid `!!` except in rare cases
- **Immutability**: Prefer `val` over `var`
- **Data classes**: Use for immutable data models with `copy()`
- **Sealed classes**: For UI states and events

### Imports

Organize imports in this order (Android Studio default):
1. Project imports (`com.kutluoglu.*`)
2. Android imports (`androidx.*`)
3. Kotlin imports (`kotlin.*`)
4. Third-party libraries (`io.*`, `com.*`)

### Formatting

- **Indentation**: 4 spaces (Kotlin default)
- **Line length**: ~120 characters max
- **Blank lines**: Max 2 consecutive blank lines
- **Trailing commas**: Use them for readability
- **Braces**: Same-line braces for control structures

### Types

- **Primitive types**: Use Kotlin types (`Int`, `String`, `Boolean`)
- **Collections**: Use Kotlin collections (`List<T>`, `Map<K,V>`)
- **Dates/Times**: Use `kotlinx.datetime` (`LocalDate`, `LocalTime`, `LocalDateTime`)
- **Flow**: Use `StateFlow` for UI state exposure

### Error Handling

- Use `Result<T>` for operations that can fail
- Chain with `.onSuccess { }` and `.onFailure { }`
- ViewModels expose UI state via sealed classes (Success/Error/Loading)
- Log errors with appropriate tag: `Log.e("Tag", "message -> ${exception.message}")`

## Architecture Patterns

### MVVM + Clean Architecture

```
UI Layer (Composables) -> ViewModel -> Use Cases -> Repository -> Data Sources
```

### ViewModel Pattern

```kotlin
@KoinViewModel
class HomeViewModel(
    private val useCase: GetPrayerTimesUseCase,
    // ... other dependencies
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.stateIn(
        scope = viewModelScope,
        initialValue = HomeUiState.Loading,
        started = SharingStarted.WhileSubscribed(5_000)
    )
    
    fun onEvent(event: HomeEvent) { /* handle events */ }
}
```

### UI State Pattern

```kotlin
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(...) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
```

### Event Pattern

```kotlin
sealed class HomeEvent {
    data object OnRefresh : HomeEvent()
    data class OnLocationSelected(val location: LocationData) : HomeEvent()
}
```

### Compose Conventions

- Use `@OptIn(ExperimentalMaterial3Api::class)` for experimental Material3 APIs
- Use `@OptIn(ExperimentalPermissionsApi::class)` for Accompanist Permissions
- Mark composables with `@Composable`
- Prefer `remember { }` for local state
- Use `derivedStateOf` for computed state
- Use `LaunchedEffect` for side effects
- Preview functions should be top-level with `@Preview` annotation

### Use Cases

```kotlin
class GetPrayerTimesUseCase(
    private val repository: PrayerRepository
) {
    suspend operator fun invoke(
        date: LocalDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: String
    ): Result<List<Prayer>> = repository.getPrayerTimes(...)
}
```

## Dependency Injection (Koin)

- Use `@KoinViewModel` annotation for ViewModels
- Use `@KoinExperimentalAPI` if needed for experimental Koin features
- Entry Dependency Module and folder in `app/src/main/java/.../di/` and the other dependency modules Define modules in `*/*/*/*/.../di/` with the config as 
  @Module
  @Configuration
  @ComponentScan(
      "com.kutluoglu.prayer",      // Scans the domain module for use cases and engine
  )

```kotlin
@KoinViewModel
class MyViewModel(...) : ViewModel() { ... }
```

## Testing Patterns

### Unit Tests with JUnit 5

```kotlin
@ExperimentalCoroutinesApi
@Execution(value = ExecutionMode.SAME_THREAD)
@ExtendWith(MainCoroutineRule::class)
class HomeViewModelTest {
    
    @BeforeEach
    fun setUp() {
        // Initialize mocks
    }
    
    @Test
    fun `test description`() = runTest {
        // Arrange
        coEvery { useCase.invoke(any(), any(), any(), any()) } returns success(data)
        
        // Act
        viewModel.loadPrayerTimesForCurrentLocation()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(HomeUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Test Dependencies

- **MockK**: Mocking Kotlin classes
- **Turbine**: Testing `Flow` emissions
- **Truth**: Readable assertions (`assertThat(x).isEqualTo(y)`)
- **kotlinx-coroutines-test**: `runTest` for coroutine testing

### Test Configuration

- Tests run with JUnit 5 parallel execution enabled
- Use `@Execution(ExecutionMode.SAME_THREAD)` to force single-threaded execution when needed
- MockK relaxed mode: `mockk<T>(relaxed = true)` for auto-mocking
- MockK settings in `gradle.properties`:
  - `io.mockk.settings.relaxed = true`
  - `io.mockk.settings.varargCapture = true`
- Use the custom `MainCoroutineRule` for coroutine testing:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : AfterEachCallback, BeforeEachCallback {
    override fun afterEach(context: ExtensionContext) = Dispatchers.resetMain()
    override fun beforeEach(context: ExtensionContext) = Dispatchers.setMain(testDispatcher)
}
```

### Run Single Test

```bash
# From a specific module
./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.HomeViewModelTest"

# From any module (wildcard)
./gradlew testDebugUnitTest --tests="*HomeViewModelTest"
```

## Compose Previews

Preview composables must be:
- Top-level functions
- Annotated with `@Composable` and `@Preview`
- Use `@Preview` with appropriate parameters for device/name

```kotlin
@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(...)
}
```

## Known Configuration Details

- **Java Version**: 21
- **Compile SDK**: 36
- **Min SDK**: 26
- **Target SDK**: 36
- **Compose Compiler**: 2.0.3
- **Gradle JVM Args**: `-Xmx2048m -Dfile.encoding=UTF-8`

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **NamazVakitleri** (7045 symbols, 16270 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/NamazVakitleri/context` | Codebase overview, check index freshness |
| `gitnexus://repo/NamazVakitleri/clusters` | All functional areas |
| `gitnexus://repo/NamazVakitleri/processes` | All execution flows |
| `gitnexus://repo/NamazVakitleri/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
