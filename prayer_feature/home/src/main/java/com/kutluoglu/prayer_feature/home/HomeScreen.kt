package com.kutluoglu.prayer_feature.home

import android.R.attr.strokeWidth
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.kutluoglu.core.ui.theme.NamazVakitleriTheme
import com.kutluoglu.core.ui.theme.navigation.NestedGraph
import com.kutluoglu.prayer.model.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Created by F.K. on 22.10.2025.
 *
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        navController: NavController,
        uiState: HomeUiState
) {
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
            TopContainer(
                modifier = Modifier.statusBarsPadding(),
                painter = painterResource(id = R.drawable.home_page_fallback)
            )
        }

        // --- 2. Middle Container (Remaining Space) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.57f) // Takes the remaining height (1.0 - 0.35 - 0.08 = 0.57)
                .background(Color.Transparent) // Temporary background
        ) {
            DailyPrayers(uiState, navController)
        }

        // --- 3. Bottom Container (8%) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .weight(0.08f) // Takes 8% of the available height
                .background(Color.Blue.copy(alpha = 0.3f)), // Temporary background
            contentAlignment = Alignment.Center
        ) {
            BottomContainer()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopContainer(
        modifier: Modifier,
        painter: Painter
) {
    val borderColorFromTheme = MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(id = R.string.home_page_fallback),
            alpha = 0.9F,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                /*TopAppBar(
                    // This pushes the title down so it's below the status bar icons.
                    title = { LocationInfoSection() },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )*/
                Box(modifier = modifier
                    .weight(0.2F)
                    .padding(start = 16.dp, top = 16.dp)
                ){
                    LocationInfoSection()
                }
                Box(modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .weight(0.4F),
                    contentAlignment = Alignment.Center
                ){
                    TimeInfo()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4F)
                        .padding(start = 16.dp, end = 16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.3F),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .drawBehind {
                            drawPath(
                                path = defineCustomBorderShapeForCurrentPrayerContainer(),
                                color = borderColorFromTheme.copy(alpha = 0.7F),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center,
                ){
                    NextPrayerInfo()
                }
            }
        }

    }
}

private fun DrawScope.defineCustomBorderShapeForCurrentPrayerContainer(): Path = Path().apply {
    val cornerRadius = 16.dp.toPx()
    // Start from the bottom-right corner (where the border ends)
    moveTo(size.width, size.height)
    // Draw a line up to the top-right corner (just before the curve starts)
    lineTo(size.width, cornerRadius)
    // Draw the top-right rounded corner
    arcTo(
        rect = Rect(
            Offset(size.width - 2 * cornerRadius, 0f),
            Size(
                2 * cornerRadius,
                2 * cornerRadius
            )
        ),
        startAngleDegrees = 0f,
        sweepAngleDegrees = -90f,
        forceMoveTo = false
    )
    // Draw the top line
    lineTo(cornerRadius, 0f)
    // Draw the top-left rounded corner
    arcTo(
        rect = Rect(
            Offset(0f, 0f),
            Size(
                2 * cornerRadius,
                2 * cornerRadius
            )
        ),
        startAngleDegrees = 270f,
        sweepAngleDegrees = -90f,
        forceMoveTo = false
    )
    // Draw the left line down to the bottom
    lineTo(0f, size.height)
}

@Composable
fun LocationInfoSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.konum),
            contentDescription = stringResource(id = R.string.home_page_fallback),
            tint = Color.Unspecified
        )
        Text(
            text = "Konum Bilgisi",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TimeInfo() {
    // A Column to arrange the time info vertically
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Digital Clock Display
        Text(
            text = "10:45", // Placeholder for the actual time
            style = MaterialTheme.typography.displaySmall, // A larger, more prominent style
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        // Add some space between the icon and the text
        Spacer(modifier = Modifier.height(8.dp))

        // 2. Gregorian Date with Day Name
        Text(
            // Add the day of the week to the text
            text = "Monday, October 27, 2025", // Placeholder for the actual date
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        // 3. Hijri Date
        Text(
            text = "22 Rabi' al-awwal 1447", // Placeholder for the Hijri date
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) // Slightly less prominent
        )
    }
}

@Composable
fun NextPrayerInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Icon and time-sensitive message in a Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp) // Adds space between icon and text
        ) {
            Icon(
                painter = painterResource(id = R.drawable.isha_gold), // Using a standard clock icon
                contentDescription = stringResource(id = R.string.time_until_message), // For accessibility
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp) // Make the icon a bit smaller to match text
            )
            Text(
                text = "Time until Isha", // Example of a time-sensitive message
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) // Slightly faded
            )
        }

        // Add some space between the message and the countdown
        Spacer(modifier = Modifier.height(4.dp))

        // 2. Remaining time countdown (Hour and Minute only)
        Text(
            text = "01:23", // Placeholder updated to "HH:mm" format
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}


@Composable
fun DailyPrayers(uiState: HomeUiState, navController: NavController) {
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
                contentPadding = PaddingValues(8.dp),
                // Optional: add spacing between grid cells
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. ADD THE TITLE ITEM HERE
                item(
                    // This makes the item span all 3 columns
                    span = { GridItemSpan(3) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // This Text will take up all available space, pushing the next one to the end
                        Text(
                            text = stringResource(id = R.string.prayer_times_title), // "Vakitler"
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .weight(1f) // KEY CHANGE
                                .padding(vertical = 8.dp) // Simplified padding
                        )
                        // This Text will now be at the very end of the Row
                        Text(
                            text = stringResource(id = R.string.view_all_prayers), // "View All"
                            style = MaterialTheme.typography.bodyLarge, // Use a different style for the date
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clickable {
                                    // Navigate to the entire prayer times graph
                                    navController.navigate(NestedGraph.PRAYER_TIMES) {
                                        // This mimics the behavior of the bottom bar click for a consistent UX
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                        )
                    }
                }
                // 2. Use the 'items' extension for LazyVerticalGrid
                items(state.data.prayers, key = { it.name }) { prayer ->
                    PrayerCard(
                        // The PrayerCard itself doesn't need to change
                        prayer = prayer,
                        isCurrent = prayer.isCurrent
                        // The modifier here can be simplified as the grid handles the width
                    )
                }
            }
        }
    }
}

@Composable
fun BottomContainer() {

    Text(
        text = "Bottom Area (8%)\n(e.g., for an ad banner or info text)",
        textAlign = TextAlign.Center
    )
    // TODO: Place your ad banner or other bottom content here
}

// Separate, focused preview for BottomContainer
@Preview(showBackground = true)
@Composable
fun BottomContainerPreview() {
    NamazVakitleriTheme { // Apply the theme wrapper here too
        BottomContainer()
    }
}

// Optional: A focused preview for your Location section
@Preview(showBackground = true)
@Composable
fun LocationInfoSectionPreview() {
    NamazVakitleriTheme { // Apply the theme wrapper
        LocationInfoSection()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFCCCCCC) // Use a grey background for contrast
@Composable
fun TimeInfoPreview() {
    NamazVakitleriTheme {
        TimeInfo()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2C2C)
@Composable
fun NextPrayerInfoPreview() {
    NamazVakitleriTheme {
        NextPrayerInfo()
    }
}

@Preview(
    showBackground = true
)
@Composable
fun HomeScreenPreview() {
    // --- Create Sample Data (your existing code is fine) ---
    val basePrayer = Prayer(
        "Asr", "صلاة العصر",
        LocalTime(12, 20),
        LocalDate(2025, 10, 22)
    )

    // KEY CHANGE: Ensure each prayer has a unique name for the key
    val prayers = listOf(
        basePrayer.copy(name = "Fajr"),
        basePrayer.copy(name = "Dhuhr"),
        basePrayer.copy(name = "Asr"),
        basePrayer.copy(name = "Maghrib"),
        basePrayer.copy(name = "Isha"),
        basePrayer.copy(name = "Next") // Example for a 6th item
    )

    val sampleData = HomeDataUiState(prayers = prayers)
    val successState = HomeUiState.Success(sampleData)
    val navController = rememberNavController()

    // --- Create a Preview-Safe HomeScreen Composable ---
    NamazVakitleriTheme {
        HomeScreen(navController, successState)
    }
}

