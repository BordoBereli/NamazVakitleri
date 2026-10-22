package com.kutluoglu.prayer_feature.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun TopContainer(
    modifier: Modifier = Modifier,
    painter: Painter,
    stringResourceId: Int,
    backgroundPath: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val backgroundFile = backgroundPath?.let { File(it) }
        if (backgroundFile != null && backgroundFile.exists()) {
            AsyncImage(
                model = backgroundFile,
                contentDescription = stringResource(id = stringResourceId),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.9F
            )
        } else {
            Image(
                painter = painter,
                contentDescription = stringResource(id = stringResourceId),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.9F
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            content = content
        )
    }
}