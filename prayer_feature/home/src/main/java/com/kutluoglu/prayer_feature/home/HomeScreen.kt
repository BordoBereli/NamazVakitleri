package com.kutluoglu.prayer_feature.home

import androidx.compose.animation.core.copy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kutluoglu.core.common.now
import com.kutluoglu.core.ui.theme.NamazVakitleriTheme
import com.kutluoglu.prayer.model.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.androidx.compose.koinViewModel

/**
 * Created by F.K. on 22.10.2025.
 *
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    NamazVakitleriTheme {
        Box {
            // The main Column will divide the screen vertically
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- 1. Top Container (35%) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f), // Takes 35% of the available height
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.home_page_fallback),
                        contentDescription = stringResource(id = R.string.home_page_fallback),
                        alpha = 0.9F,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(1.5F)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                        ) {
                            TopAppBar(
                                title = { LocationInfoSection() },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }

                }

                // --- 2. Middle Container (Remaining Space) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.57f) // Takes the remaining height (1.0 - 0.35 - 0.08 = 0.57)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f)) // Temporary background
                ) {
                    // The content for the middle section (Loading, Error, Success) goes here
                    when (val state = uiState) {
                        is HomeUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is HomeUiState.Error   -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Error: ${state.message}")
                            }
                        }

                        is HomeUiState.Success -> {
                            LazyVerticalGrid(
                                // 1. Specify the number of columns in the grid
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(4.dp),
                                // Optional: add spacing between grid cells
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // 2. Use the 'items' extension for LazyVerticalGrid
                                items(state.data.prayers, key = { it.name }) { prayer ->
                                    PrayerCard(
                                        // The PrayerCard itself doesn't need to change
                                        prayer = prayer
                                        // The modifier here can be simplified as the grid handles the width
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 3. Bottom Container (8%) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.08f) // Takes 8% of the available height
                        .background(Color.Blue.copy(alpha = 0.3f)), // Temporary background
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bottom Area (8%)\n(e.g., for an ad banner or info text)",
                        textAlign = TextAlign.Center
                    )
                    // TODO: Place your ad banner or other bottom content here
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenState(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun LocationInfoSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.home),
            contentDescription = stringResource(id = R.string.home_page_fallback),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Konum Bilgisi",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    // Wrap the preview in your theme to see correct colors and fonts
    NamazVakitleriTheme {
        // Create sample data for the Success state
        val samplePrayer = Prayer("Asr", "صلاة العصر",
            LocalTime(12, 20),
            LocalDate(2025, 10, 22))
        val sampleData = HomeDataUiState(prayers = listOf(samplePrayer, samplePrayer, samplePrayer))
        val successState = HomeUiState.Success(sampleData)

        // Pass the sample state directly to the HomeScreen
        HomeScreen()
    }
}

@Preview
@Composable
fun LocationInfoSectionPreview() {
    NamazVakitleriTheme { // Also wrap this preview in your theme
        LocationInfoSection()
    }
}

