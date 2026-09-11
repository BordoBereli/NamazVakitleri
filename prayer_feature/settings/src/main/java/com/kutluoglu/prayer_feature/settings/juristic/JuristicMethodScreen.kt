package com.kutluoglu.prayer_feature.settings.juristic

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.R
import com.kutluoglu.core.designsystem.components.BackNavigationIcon
import com.kutluoglu.core.designsystem.components.RoundedPageTitleBar
import com.kutluoglu.core.designsystem.theme.IslamicGoldOlive
import com.kutluoglu.core.designsystem.theme.IslamicGoldSoft
import com.kutluoglu.prayer_feature.settings.R as SettingsR
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

internal fun juristicSelectedContainerColor(isDarkTheme: Boolean): Color =
    if (isDarkTheme) IslamicGoldOlive else IslamicGoldSoft

private data class JuristicMethodInfo(
    val id: String,
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
    val shadowFactor: Float,
    val angleLabel: String,
    @StringRes val captionRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuristicMethodRoute(
    onNavigateBack: () -> Unit,
    onMethodSelected: (String) -> Unit,
    viewModel: JuristicMethodViewModel = koinViewModel()
) {
    val currentMethod by viewModel.currentMethod.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.selectedMethod.collectLatest { method ->
            onMethodSelected(method)
        }
    }
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            RoundedPageTitleBar(
                title = stringResource(SettingsR.string.asr_calculation),
                navigationIcon = { BackNavigationIcon(onClick = onNavigateBack) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    JuristicMethodInfo(
                        id = "STANDARD",
                        labelRes = SettingsR.string.juristic_standard,
                        descRes = SettingsR.string.juristic_standard_description,
                        shadowFactor = 1f,
                        angleLabel = "45°",
                        captionRes = SettingsR.string.juristic_shadow_equals_height
                    ),
                    JuristicMethodInfo(
                        id = "HANAFI",
                        labelRes = SettingsR.string.juristic_hanafi,
                        descRes = SettingsR.string.juristic_hanafi_description,
                        shadowFactor = 2f,
                        angleLabel = "26.5°",
                        captionRes = SettingsR.string.juristic_shadow_twice_height
                    )
                )
            ) { info ->
                JuristicMethodCard(
                    info = info,
                    selected = info.id == currentMethod,
                    onSelect = { viewModel.onEvent(JuristicMethodEvent.SelectMethod(info.id)) }
                )
            }
        }
    }
}

@Composable
private fun JuristicMethodCard(
    info: JuristicMethodInfo,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                juristicSelectedContainerColor(isSystemInDarkTheme())
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected,
                    onClick = onSelect
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(info.labelRes),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(info.descRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            ShadowLengthDiagram(
                shadowFactor = info.shadowFactor,
                angleLabel = info.angleLabel,
                caption = stringResource(info.captionRes),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
