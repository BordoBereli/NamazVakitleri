package com.kutluoglu.prayer_feature.settings.language

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.R
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

data class Language(
    val code: String,
    val name: String,
    val nativeName: String
)

val languages = listOf(
    Language("tr", "Turkish", "Türkçe"),
    Language("en", "English", "English"),
    Language("ar", "Arabic", "العربية"),
    Language("ur", "Urdu", "اردو"),
    Language("id", "Indonesian", "Bahasa Indonesia"),
    Language("ms", "Malay", "Bahasa Melayu"),
    Language("fa", "Persian", "فارسی"),
    Language("bn", "Bengali", "বাংলা"),
    Language("hi", "Hindi", "हिन्दी"),
    Language("ta", "Tamil", "தமிழ்"),
    Language("th", "Thai", "ไทย"),
    Language("ru", "Russian", "Русский"),
    Language("fr", "French", "Français"),
    Language("de", "German", "Deutsch"),
    Language("es", "Spanish", "Español")
)

sealed class LanguageUiState {
    data object Loading : LanguageUiState()
    data class LanguagesLoaded(val languages: List<Language>, val selectedLanguage: String) : LanguageUiState()
    data class Error(val message: String) : LanguageUiState()
}

sealed class LanguageEvent {
    data class SelectLanguage(val language: Language) : LanguageEvent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionRoute(
    currentLanguage: String,
    onNavigateBack: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    viewModel: LanguageSelectionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(currentLanguage) {
        viewModel.setCurrentLanguage(currentLanguage)
    }

    LaunchedEffect(Unit) {
        viewModel.selectedLanguage.collectLatest { language ->
            onLanguageSelected(language)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_language)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is LanguageUiState.Loading -> {
                    LoadingIndicator()
                }
                is LanguageUiState.LanguagesLoaded -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.languages) { language ->
                            LanguageItem(
                                language = language,
                                isSelected = language.code == state.selectedLanguage,
                                onClick = { viewModel.onEvent(LanguageEvent.SelectLanguage(language)) }
                            )
                        }
                    }
                }
                is LanguageUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
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
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = language.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
