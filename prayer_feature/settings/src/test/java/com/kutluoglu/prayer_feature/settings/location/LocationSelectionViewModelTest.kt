package com.kutluoglu.prayer_feature.settings.location

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.prayer_settings.data.location.LocationServiceHelper
import com.kutluoglu.prayer_remote.location.NetworkException
import com.kutluoglu.prayer.model.location.City
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_settings.domain.repository.LocationRepository
import com.kutluoglu.prayer_settings.domain.usecase.SearchLocationUseCase
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@OptIn(ExperimentalCoroutinesApi::class)
@Execution(value = ExecutionMode.SAME_THREAD)
class LocationSelectionViewModelTest {

    @JvmField
    @RegisterExtension
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var locationRepository: LocationRepository
    private lateinit var searchLocationUseCase: SearchLocationUseCase
    private lateinit var locationServiceHelper: LocationServiceHelper
    private lateinit var locationsCoordinator: LocationsCoordinator
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private lateinit var viewModel: LocationSelectionViewModel

    private val presetCities = listOf(
        City("Istanbul", "Turkey", 41.0082, 28.9784, "Europe/Istanbul", "Istanbul"),
        City("Ankara", "Turkey", 39.9334, 32.8597, "Europe/Istanbul", "Ankara"),
        City("London", "United Kingdom", 51.5074, -0.1278, "Europe/London", "London")
    )

    @BeforeEach
    fun setUp() {
        locationRepository = mockk()
        searchLocationUseCase = mockk()
        locationServiceHelper = mockk()
        locationsCoordinator = mockk(relaxed = true)
        coEvery { locationRepository.getPresetCities() } returns presetCities
        viewModel = LocationSelectionViewModel(
            locationRepository,
            searchLocationUseCase,
            locationServiceHelper,
            locationsCoordinator,
            analyticsTracker,
            defaultDispatcher = mainCoroutineRule.dispatcher
        )
    }

    @Test
    fun `initial state should load countries`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.CountrySelection::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LoadCountries should reload countries from repository`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.LoadCountries)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.CountrySelection::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { locationRepository.getPresetCities() }
    }

    @Test
    fun `SelectCountry should show cities for that country`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.SelectCountry("Turkey"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.CitySelection::class.java)
            val cityState = state as LocationSelectionUiState.CitySelection
            assertThat(cityState.country).isEqualTo("Turkey")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Search with valid query should return search results`() = runTest {
        val searchResults = listOf(
            City("Berlin", "Germany", 52.52, 13.405, "Europe/Berlin", "Berlin")
        )
        coEvery { searchLocationUseCase("Berlin") } returns searchResults

        viewModel.onEvent(LocationSelectionEvent.Search("Berlin"))

        viewModel.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.SearchResults::class.java)
            val resultsState = state as LocationSelectionUiState.SearchResults
            assertThat(resultsState.results).hasSize(1)
            assertThat(resultsState.query).isEqualTo("Berlin")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SelectCity should trigger city selection`() = runTest {
        val city = presetCities.first()
        
        viewModel.onEvent(LocationSelectionEvent.SelectCity(city))
        
        coVerify { locationsCoordinator.addLocation(any()) }
    }

    @Test
    fun `SelectDistrict should save location to settings`() = runTest {
        val district = City("Fatih", "Turkey", 41.0364, 28.9603, "Europe/Istanbul", "Istanbul")

        viewModel.onEvent(LocationSelectionEvent.SelectDistrict(district))

        coVerify { locationsCoordinator.addLocation(any()) }
    }

    @Test
    fun `SelectCity with preset district should save district from name`() = runTest {
        val district = City("Fatih", "Turkey", 41.0364, 28.9603, "Europe/Istanbul", "Istanbul")

        viewModel.onEvent(LocationSelectionEvent.SelectCity(district))

        val slot = slot<LocationEntry>()
        coVerify { locationsCoordinator.addLocation(capture(slot)) }
        assertThat(slot.captured.location.city).isEqualTo("Istanbul")
        assertThat(slot.captured.location.county).isEqualTo("Fatih")
    }

    @Test
    fun `SelectCity with main city should not set district`() = runTest {
        val mainCity = City("Ankara", "Turkey", 39.9334, 32.8597, "Europe/Istanbul", "Ankara")

        viewModel.onEvent(LocationSelectionEvent.SelectCity(mainCity))

        val slot = slot<LocationEntry>()
        coVerify { locationsCoordinator.addLocation(capture(slot)) }
        assertThat(slot.captured.location.city).isEqualTo("Ankara")
        assertThat(slot.captured.location.county).isNull()
    }

    @Test
    fun `SelectCity with reverse geocoded city should save county as district`() = runTest {
        val reverseCity = City("Bursa", "Turkey", 40.1826, 29.0676, "Europe/Istanbul", "Bursa", "Osmangazi")

        viewModel.onEvent(LocationSelectionEvent.SelectCity(reverseCity))

        val slot = slot<LocationEntry>()
        coVerify { locationsCoordinator.addLocation(capture(slot)) }
        assertThat(slot.captured.location.city).isEqualTo("Bursa")
        assertThat(slot.captured.location.county).isEqualTo("Osmangazi")
    }

    @Test
    fun `ChangeSortOrder should update state with new sort order`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.SelectCountry("Turkey"))
        
        viewModel.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        
        viewModel.onEvent(LocationSelectionEvent.ChangeSortOrder(SortOrder.DESCENDING))

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.CitySelection::class.java)
            val cityState = state as LocationSelectionUiState.CitySelection
            assertThat(cityState.sortOrder).isEqualTo(SortOrder.DESCENDING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Search failure should show error state with message`() = runTest {
        coEvery { searchLocationUseCase("Berlin") } throws NetworkException("No internet")

        viewModel.onEvent(LocationSelectionEvent.Search("Berlin"))

        viewModel.uiState.test {
            awaitItem()
            val errorState = awaitItem()
            assertThat(errorState).isInstanceOf(LocationSelectionUiState.Error::class.java)
            val error = errorState as LocationSelectionUiState.Error
            assertThat(error.message).contains("internet")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Search with query shorter than minimum length should not trigger search`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.Search("A"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.CountrySelection::class.java)
            coVerify(exactly = 0) { searchLocationUseCase(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GoBack from city selection should return to country selection`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.SelectCountry("Turkey"))
        
        viewModel.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        
        viewModel.onEvent(LocationSelectionEvent.GoBack)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.CountrySelection::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Cities should be grouped by province`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.SelectCountry("Turkey"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.CitySelection::class.java)
            val cityState = state as LocationSelectionUiState.CitySelection
            assertThat(cityState.citiesByProvince.keys).contains("Istanbul")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Country list should show priority countries first`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(LocationSelectionUiState.CountrySelection::class.java)
            val countryState = state as LocationSelectionUiState.CountrySelection
            val firstCountry = countryState.countries.first()
            assertThat(firstCountry.name).isEqualTo("Turkey")
            assertThat(firstCountry.isPriority).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SearchCountry should filter countries after debounce`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.SearchCountry("tur"))

        viewModel.uiState.test {
            var filtered: LocationSelectionUiState.CountrySelection? = null
            while (filtered == null) {
                val state = awaitItem()
                if (state is LocationSelectionUiState.CountrySelection && state.searchQuery == "tur") {
                    filtered = state
                }
            }
            assertThat(filtered.countries.map { it.name }).containsExactly("Turkey")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SearchCountry with blank query should restore full country list`() = runTest {
        viewModel.onEvent(LocationSelectionEvent.SearchCountry("tur"))
        viewModel.onEvent(LocationSelectionEvent.SearchCountry(""))

        viewModel.uiState.test {
            var restored: LocationSelectionUiState.CountrySelection? = null
            while (restored == null) {
                val state = awaitItem()
                if (state is LocationSelectionUiState.CountrySelection && state.searchQuery == "") {
                    restored = state
                }
            }
            assertThat(restored.countries.map { it.name })
                .containsExactly("Turkey", "United Kingdom")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
