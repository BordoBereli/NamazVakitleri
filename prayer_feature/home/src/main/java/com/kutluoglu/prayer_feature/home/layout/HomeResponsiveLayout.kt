package com.kutluoglu.prayer_feature.home.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.kutluoglu.prayer_feature.common.LocalIsLandscape

@Composable
fun HomeResponsiveLayout(
    innerPadding: PaddingValues,
    topContainer: @Composable (Modifier) -> Unit,
    dailyPrayers: @Composable (Modifier) -> Unit,
    bottomContainer: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val layoutDirection = LocalLayoutDirection.current
        CompositionLocalProvider(LocalIsLandscape provides isLandscape) {
            if (isLandscape) {
                LandscapeMode(innerPadding, topContainer, dailyPrayers, bottomContainer)
            } else {
                PortraitMode(innerPadding, layoutDirection, topContainer, dailyPrayers, bottomContainer)
            }
        }
    }
}

@Composable
private fun LandscapeMode(
    innerPadding: PaddingValues,
    topContainer: @Composable (Modifier) -> Unit,
    dailyPrayers: @Composable (Modifier) -> Unit,
    bottomContainer: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        topContainer(Modifier.weight(0.4f))
        Column(
            modifier = Modifier.weight(0.6f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dailyPrayers(Modifier.weight(0.8f))
            bottomContainer(Modifier.weight(0.2f))
        }
    }
}

@Composable
private fun PortraitMode(
    innerPadding: PaddingValues,
    layoutDirection: LayoutDirection,
    topContainer: @Composable (Modifier) -> Unit,
    dailyPrayers: @Composable (Modifier) -> Unit,
    bottomContainer: @Composable (Modifier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding()
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topContainer(Modifier.weight(0.37f))
        dailyPrayers(Modifier.weight(0.50f))
        bottomContainer(Modifier.weight(0.13f))
    }
}
