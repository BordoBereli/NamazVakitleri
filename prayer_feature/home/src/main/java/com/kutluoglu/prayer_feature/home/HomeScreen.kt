package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kutluoglu.prayer_feature.home.components.BottomContainer
import com.kutluoglu.prayer_feature.home.components.DailyPrayers
import com.kutluoglu.prayer_feature.home.components.TopContainer

@Composable
fun HomeScreen(
    navController: NavController,
    uiState: HomeUiState
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 1. Top Container (35%) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f),
            contentAlignment = Alignment.Center
        ) {
            TopContainer(
                modifier = Modifier.statusBarsPadding(),
                painter = painterResource(id = R.drawable.home_page_fallback),
                uiState = uiState
            )
        }

        // --- 2. Middle Container (57%) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.57f)
                .background(Color.Transparent)
        ) {
            DailyPrayers(uiState, navController)
        }

        // --- 3. Bottom Container (8%) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .weight(0.08f)
                .background(Color.Blue.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            BottomContainer()
        }
    }
}
