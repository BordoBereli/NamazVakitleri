package com.kutluoglu.prayer_feature.settings.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.RectRulers
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.Locale
import com.kutluoglu.core.designsystem.R
import com.kutluoglu.core.designsystem.components.SkeletonList
import com.kutluoglu.prayer.model.location.City
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.kutluoglu.core.common.analytics.AnalyticsEvents
import com.kutluoglu.core.common.analytics.AnalyticsParams
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionRoute(
    onNavigateBack: () -> Unit,
    onCitySelected: (City) -> Unit,
    viewModel: LocationSelectionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val analyticsTracker: AnalyticsTracker = koinInject()
    val languageCode = Locale.getDefault().language
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCountryFilter by remember { mutableStateOf(false) }
    var showCountySheet by remember { mutableStateOf(false) }
    var selectedCityForCounty by remember { mutableStateOf<City?>(null) }
    
    val tabs = listOf(
        stringResource(R.string.preset_cities),
        stringResource(R.string.search),
        stringResource(R.string.map)
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            analyticsTracker.logEvent(
                AnalyticsEvents.PERMISSION_GRANTED,
                mapOf(AnalyticsParams.PERMISSION to "location")
            )
            viewModel.onEvent(LocationSelectionEvent.UseMyLocation)
        } else {
            val permanentDenial = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            analyticsTracker.logEvent(
                AnalyticsEvents.PERMISSION_DENIED,
                mapOf(
                    AnalyticsParams.PERMISSION to "location",
                    AnalyticsParams.IS_PERMANENT_DENIAL to permanentDenial
                )
            )
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationAndUseMyLocation() {
        if (hasLocationPermission()) {
            viewModel.onEvent(LocationSelectionEvent.UseMyLocation)
        } else {
            analyticsTracker.logEvent(
                AnalyticsEvents.PERMISSION_REQUESTED,
                mapOf(AnalyticsParams.PERMISSION to "location")
            )
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.selectedCity.collectLatest { city ->
            onCitySelected(city)
        }
    }

    LaunchedEffect(selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> viewModel.onEvent(LocationSelectionEvent.SelectTab(LocationTab.PRESET))
            1 -> viewModel.onEvent(LocationSelectionEvent.SelectTab(LocationTab.SEARCH))
            2 -> viewModel.onEvent(LocationSelectionEvent.SelectTab(LocationTab.MAP))
        }
    }

    // County selection bottom sheet
    if (showCountySheet && selectedCityForCounty != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showCountySheet = false
                selectedCityForCounty = null
            },
            sheetState = sheetState
        ) {
            // Old county selection - removed
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_location)) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (uiState) {
                            is LocationSelectionUiState.CitySelection,
                            is LocationSelectionUiState.ProvinceSelection -> {
                                viewModel.onEvent(LocationSelectionEvent.GoBack)
                            }
                            else -> onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (selectedTabIndex == 0) {
                        val currentSortOrder = when (val state = uiState) {
                            is LocationSelectionUiState.CitySelection -> state.sortOrder
                            else -> SortOrder.ASCENDING
                        }

                        IconButton(
                            onClick = {
                                val newOrder = if (currentSortOrder == SortOrder.ASCENDING) {
                                    SortOrder.DESCENDING
                                } else {
                                    SortOrder.ASCENDING
                                }
                                viewModel.onEvent(LocationSelectionEvent.ChangeSortOrder(newOrder))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SortByAlpha,
                                contentDescription = stringResource(R.string.sort),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isLandscape = maxWidth > maxHeight
            Column(modifier = Modifier.fillMaxSize()) {
                // Use My Location Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = { requestLocationAndUseMyLocation() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.use_my_location))
                    }
                }

                // Tabs
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Tab Content
                if (isLandscape && selectedTabIndex == 0) {
                    PresetCitiesLandscapeContent(
                        uiState = uiState,
                        languageCode = languageCode,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSearch = { viewModel.onEvent(LocationSelectionEvent.SearchCountry(it)) },
                        onCountryClick = { viewModel.onEvent(LocationSelectionEvent.SelectCountry(it)) },
                        onCityClick = { city ->
                            viewModel.onEvent(LocationSelectionEvent.SelectCity(city))
                        },
                        onSelectProvince = { province, mainCity ->
                            viewModel.onEvent(LocationSelectionEvent.SelectProvince(province, mainCity))
                        },
                        onSelectDistrict = { district ->
                            viewModel.onEvent(LocationSelectionEvent.SelectDistrict(district))
                        }
                    )
                } else {
                    AnimatedVisibility(
                        visible = selectedTabIndex == 0,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it }
                    ) {
                        PresetCitiesContent(
                            uiState = uiState,
                            languageCode = languageCode,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearch = { viewModel.onEvent(LocationSelectionEvent.SearchCountry(it)) },
                            onCountryClick = { viewModel.onEvent(LocationSelectionEvent.SelectCountry(it)) },
                            onCityClick = { city ->
                                viewModel.onEvent(LocationSelectionEvent.SelectCity(city))
                            },
                            onSelectProvince = { province, mainCity ->
                                viewModel.onEvent(LocationSelectionEvent.SelectProvince(province, mainCity))
                            },
                            onSelectDistrict = { district ->
                                viewModel.onEvent(LocationSelectionEvent.SelectDistrict(district))
                            },
                            showCountryFilter = showCountryFilter,
                            onShowCountryFilter = { showCountryFilter = !showCountryFilter },
                            onCountrySelect = { viewModel.onEvent(LocationSelectionEvent.SelectCountry(it)) }
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedTabIndex == 1,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it }
                    ) {
                        SearchTab(
                            uiState = uiState,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearch = { viewModel.onEvent(LocationSelectionEvent.Search(it)) },
                            onClearSearch = {
                                searchQuery = ""
                                viewModel.onEvent(LocationSelectionEvent.ClearSearch)
                            },
                            onCityClick = { viewModel.onEvent(LocationSelectionEvent.SelectCity(it)) },
                            onClearHistory = { viewModel.onEvent(LocationSelectionEvent.ClearHistory) }
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedTabIndex == 2,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it }
                    ) {
                        MapTab(
                            uiState = uiState,
                            onLocationSelected = { lat, lon ->
                                viewModel.onEvent(LocationSelectionEvent.UpdateMapLocation(lat, lon))
                            },
                            onConfirmLocation = { location ->
                                viewModel.onEvent(LocationSelectionEvent.ConfirmMapLocation(location))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCitiesContent(
    uiState: LocationSelectionUiState,
    languageCode: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onCountryClick: (String) -> Unit,
    onCityClick: (City) -> Unit,
    onSelectProvince: (String, City) -> Unit,
    onSelectDistrict: (City) -> Unit,
    showCountryFilter: Boolean,
    onShowCountryFilter: () -> Unit,
    onCountrySelect: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is LocationSelectionUiState.Loading -> {
                SkeletonList(itemCount = 10)
            }
            
            is LocationSelectionUiState.CountrySelection -> {
                // Search field for countries
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        onSearchQueryChange(it)
                        onSearch(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text(stringResource(R.string.search_country)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                onSearchQueryChange("")
                                onSearch("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp)
                )

                CountryList(
                    countries = uiState.countries,
                    onCountryClick = onCountryClick
                )
            }

            is LocationSelectionUiState.CitySelection -> {
                // Header showing selected country
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.country,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Province filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box {
                        FilterChip(
                            selected = uiState.selectedProvince != null,
                            onClick = onShowCountryFilter,
                            label = { 
                                Text(uiState.selectedProvince ?: stringResource(R.string.all_provinces)) 
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        
                        DropdownMenu(
                            expanded = showCountryFilter,
                            onDismissRequest = onShowCountryFilter
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.all_provinces)) },
                                onClick = {
                                    onCountrySelect(uiState.country)
                                    onShowCountryFilter()
                                }
                            )
                            uiState.citiesByProvince.keys.forEach { province ->
                                DropdownMenuItem(
                                    text = { Text(province) },
                                    onClick = {
                                        onCountrySelect(province)
                                        onShowCountryFilter()
                                    }
                                )
                            }
                        }
                    }
                }

                ProvinceListByProvince(
                    citiesByProvince = uiState.citiesByProvince,
                    selectedProvince = uiState.selectedProvince,
                    languageCode = languageCode,
                    onProvinceClick = onSelectProvince
                )
            }

            is LocationSelectionUiState.ProvinceSelection -> {
                ProvinceDetailContent(
                    provinceSelection = uiState,
                    languageCode = languageCode,
                    onDistrictClick = onSelectDistrict,
                    onMainCityClick = { mainCity ->
                        onCityClick(mainCity)
                    }
                )
            }

            is LocationSelectionUiState.SearchResults -> {
                SearchResultsContent(
                    results = uiState.results,
                    searchHistory = uiState.searchHistory,
                    query = uiState.query,
                    onCityClick = onCityClick,
                    onHistoryClear = {}
                )
            }
            
            is LocationSelectionUiState.Searching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is LocationSelectionUiState.MapView -> {
                // Handled by MapTab
            }
            
            is LocationSelectionUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    onRetry = {}
                )
            }
            
            else -> {}
        }
    }
}

@Composable
private fun PresetCitiesLandscapeContent(
    uiState: LocationSelectionUiState,
    languageCode: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onCountryClick: (String) -> Unit,
    onCityClick: (City) -> Unit,
    onSelectProvince: (String, City) -> Unit,
    onSelectDistrict: (City) -> Unit
) {
    val focusManager = LocalFocusManager.current

    val countries = when (uiState) {
        is LocationSelectionUiState.CountrySelection -> uiState.countries
        is LocationSelectionUiState.CitySelection -> uiState.countries
        is LocationSelectionUiState.ProvinceSelection -> uiState.countries
        else -> emptyList()
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left pane: master (country search + country list)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    onSearchQueryChange(it)
                    onSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_country)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            onSearchQueryChange("")
                            onSearch("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp)
            )

            CountryList(
                countries = countries,
                onCountryClick = onCountryClick
            )
        }

        VerticalDivider()

        // Right pane: detail (provinces / districts)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (uiState) {
                is LocationSelectionUiState.Loading -> {
                    SkeletonList(itemCount = 10)
                }

                is LocationSelectionUiState.CountrySelection -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.select_country_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is LocationSelectionUiState.CitySelection -> {
                    ProvinceListByProvince(
                        citiesByProvince = uiState.citiesByProvince,
                        selectedProvince = uiState.selectedProvince,
                        languageCode = languageCode,
                        onProvinceClick = onSelectProvince
                    )
                }

                is LocationSelectionUiState.ProvinceSelection -> {
                    ProvinceDetailContent(
                        provinceSelection = uiState,
                        languageCode = languageCode,
                        onDistrictClick = onSelectDistrict,
                        onMainCityClick = onCityClick
                    )
                }

                is LocationSelectionUiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        onRetry = {}
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun CountryList(
    countries: List<CountryInfo>,
    onCountryClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Priority countries section
        val priorityCountries = countries.filter { it.isPriority }
        val otherCountries = countries.filter { !it.isPriority }

        if (priorityCountries.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.popular_countries),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(priorityCountries) { countryInfo ->
                CountryItem(
                    country = countryInfo.name,
                    cityCount = countryInfo.cityCount,
                    isPriority = true,
                    isTurkey = countryInfo.key == "Turkey",
                    onClick = { onCountryClick(countryInfo.key) }
                )
            }
        }

        if (otherCountries.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.other_countries),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(otherCountries) { countryInfo ->
                CountryItem(
                    country = countryInfo.name,
                    cityCount = countryInfo.cityCount,
                    isPriority = false,
                    isTurkey = false,
                    onClick = { onCountryClick(countryInfo.key) }
                )
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: String,
    cityCount: Int,
    isPriority: Boolean,
    isTurkey: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isTurkey -> MaterialTheme.colorScheme.primaryContainer
                isPriority -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isTurkey) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isTurkey) {
                    Text(
                        text = "🇹🇷",
                        style = MaterialTheme.typography.titleLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = country,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPriority) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = "$cityCount cities",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProvinceListByProvince(
    citiesByProvince: Map<String, List<City>>,
    selectedProvince: String?,
    languageCode: String,
    onProvinceClick: (String, City) -> Unit
) {
    val filteredData = if (selectedProvince != null) {
        mapOf(selectedProvince to (citiesByProvince[selectedProvince] ?: emptyList()))
    } else {
        citiesByProvince
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredData.forEach { (province, cities) ->
            val mainCity = cities.firstOrNull { it.city == province && it.name == province }
                ?: cities.firstOrNull()
            val displayProvince = CityLocalizer.localizedProvince(cities.first(), languageCode)

            item {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayProvince,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${cities.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(cities.distinctBy { it.name }) { city ->
                ProvinceItem(
                    city = city,
                    isMainCity = city.name == province,
                    languageCode = languageCode,
                    onClick = { onProvinceClick(province, mainCity ?: city) }
                )
            }
        }
    }
}

@Composable
private fun ProvinceItem(
    city: City,
    isMainCity: Boolean,
    languageCode: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isMainCity)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isMainCity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CityLocalizer.localizedName(city, languageCode),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isMainCity) FontWeight.SemiBold else FontWeight.Medium
                )
                Row {
                    Text(
                        text = "%.4f°N".format(city.latitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "  ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "%.4f°E".format(city.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isMainCity) {
                Text(
                    text = stringResource(R.string.province_capital),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProvinceDetailContent(
    provinceSelection: LocationSelectionUiState.ProvinceSelection,
    languageCode: String,
    onDistrictClick: (City) -> Unit,
    onMainCityClick: (City) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = CityLocalizer.localizedProvince(provinceSelection.mainCity, languageCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = CityLocalizer.localizedCountry(provinceSelection.mainCity, languageCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.province_capital),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            ProvinceItem(
                city = provinceSelection.mainCity,
                isMainCity = true,
                languageCode = languageCode,
                onClick = { onMainCityClick(provinceSelection.mainCity) }
            )
        }

        if (provinceSelection.districts.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.districts),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp)
                )
            }

            items(provinceSelection.districts) { district ->
                ProvinceItem(
                    city = district,
                    isMainCity = false,
                    languageCode = languageCode,
                    onClick = { onDistrictClick(district) }
                )
            }
        }
    }
}

@Composable
private fun CityListByCounty(
    citiesByCounty: Map<String, List<City>>,
    selectedCounty: String?,
    onCityClick: (City) -> Unit
) {
    val filteredData = if (selectedCounty != null) {
        mapOf(selectedCounty to (citiesByCounty[selectedCounty] ?: emptyList()))
    } else {
        citiesByCounty
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredData.forEach { (county, cities) ->
            item {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = county,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            items(cities) { city ->
                CityItemWithCoords(
                    city = city,
                    onClick = { onCityClick(city) }
                )
            }
        }
    }
}

@Composable
private fun CityItemWithCoords(
    city: City,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                val cityDisplayName = if (city.county.isNullOrEmpty()) {
                    city.name
                } else {
                    "${city.county}, ${city.name}"
                }
                Text(
                    text = cityDisplayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    Text(
                        text = "%.4f°N".format(city.latitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "  ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "%.4f°E".format(city.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CountySelectionSheet(
    city: City,
    onCountySelected: (City) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Location",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Choose a location in ${city.name}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // City option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onCountySelected(city) },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "%.4f°N, %.4f°E".format(city.latitude, city.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // If county exists, show it too
        city.county?.let { county ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { 
                        val countyCity = city.copy(name = "$city.name, $county")
                        onCountySelected(countyCity)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$city.name, $county",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "%.4f°N, %.4f°E".format(city.latitude, city.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SearchTab(
    uiState: LocationSelectionUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCityClick: (City) -> Unit,
    onClearHistory: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
            onSearch(searchQuery)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text(stringResource(R.string.search_city)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(12.dp)
        )

        when (uiState) {
            is LocationSelectionUiState.SearchResults -> {
                SearchResultsContent(
                    results = uiState.results,
                    searchHistory = uiState.searchHistory,
                    query = uiState.query,
                    onCityClick = onCityClick,
                    onHistoryClear = onClearHistory
                )
            }
            is LocationSelectionUiState.Searching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LocationSelectionUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    onRetry = {}
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_city),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MapTab(
    uiState: LocationSelectionUiState,
    onLocationSelected: (Double, Double) -> Unit,
    onConfirmLocation: (MapLocationState) -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is LocationSelectionUiState.MapView -> {
                if (uiState.isLocating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.getting_location),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (uiState.error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = uiState.error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = { /* Request permission again */ }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .scale(0.9F)
                            .padding(bottom = 24.dp)
                    ) {
                        OSMMapView(
                            context = context,
                            initialLat = uiState.selectedLocation?.latitude ?: 41.0082,
                            initialLon = uiState.selectedLocation?.longitude ?: 28.9784,
                            onLocationSelected = onLocationSelected
                        )
                    }
                }

                uiState.selectedLocation?.let { location ->
                    LocationInfoCard(
                        location = location,
                        onConfirm = { onConfirmLocation(location) },
                        onClear = { onLocationSelected(0.0, 0.0) }
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tap_map_to_select),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationInfoCard(
    location: MapLocationState,
    onConfirm: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = location.locationInfoText.ifEmpty { stringResource(R.string.selected_location) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.clear))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Composable
private fun OSMMapView(
    context: Context,
    initialLat: Double,
    initialLon: Double,
    onLocationSelected: (Double, Double) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(10.0)
                
                val startPoint = GeoPoint(initialLat, initialLon)
                controller.setCenter(startPoint)
                
                val marker = Marker(this)
                marker.position = startPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                overlays.add(marker)

                setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        val projection = projection
                        val touchedPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                        
                        marker.position = touchedPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        overlays.add(marker)
                        invalidate()
                        
                        onLocationSelected(touchedPoint.latitude, touchedPoint.longitude)
                    }
                    false
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun SearchResultsContent(
    results: List<City>,
    searchHistory: List<City>,
    query: String,
    onCityClick: (City) -> Unit,
    onHistoryClear: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (query.isEmpty() && searchHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_searches),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    androidx.compose.material3.TextButton(onClick = onHistoryClear) {
                        Text(stringResource(R.string.clear_all))
                    }
                }
            }
            items(searchHistory) { city ->
                CityItemWithCoords(
                    city = city,
                    onClick = { onCityClick(city) }
                )
            }
        } else if (results.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(results) { city ->
                CityItemWithCoords(
                    city = city,
                    onClick = { onCityClick(city) }
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.retry))
            }
        }
    }
}
