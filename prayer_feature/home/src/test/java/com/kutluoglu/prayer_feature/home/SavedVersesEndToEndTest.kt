package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.data.di.PrayerDataModule
import com.kutluoglu.prayer.di.PrayerDomainModule
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.repository.IQuranRepository
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_remote.di.PrayerRemoteModule
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.di.PrayerFeatureHomeModule
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.ksp.generated.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SavedVersesEndToEndTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                PrayerDataModule.module,
                PrayerDomainModule.module,
                PrayerRemoteModule.module,
                PrayerFeatureHomeModule.module
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `saved verse appears on the saved verses screen`() {
        val verse = AyahData(
            text = "Bismillah",
            surah = SurahInfo("Al-Fatihah", "الفاتحة", 1, 7),
            numberInSurah = 1
        )
        runBlocking {
            val repository: IQuranRepository = GlobalContext.get().get()
            repository.toggleSavedVerse(verse)
            val saved = repository.getSavedVerses().getOrThrow()
            assertThat(saved).contains(verse)
        }

        val getSavedVersesUseCase: GetSavedVersesUseCase = GlobalContext.get().get()
        val reorderSavedVersesUseCase: ReorderSavedVersesUseCase = GlobalContext.get().get()
        val toggleSavedVerseUseCase: ToggleSavedVerseUseCase = GlobalContext.get().get()
        val repo1: IQuranRepository = GlobalContext.get().get()
        val repo2: IQuranRepository = GlobalContext.get().get()
        val store1: com.kutluoglu.prayer.data.cache.SavedVersesStore = GlobalContext.get().get()
        val store2: com.kutluoglu.prayer.data.cache.SavedVersesStore = GlobalContext.get().get()
        assertThat(repo1 === repo2).isTrue()
        assertThat(store1 === store2).isTrue()
        runBlocking {
            val direct = repo1.getSavedVerses().getOrThrow()
            val viaUseCase = getSavedVersesUseCase().getOrThrow()
            assertThat(direct).contains(verse)
            assertThat(viaUseCase).contains(verse)
        }
        val vm = SavedVersesViewModel(getSavedVersesUseCase, reorderSavedVersesUseCase, toggleSavedVerseUseCase)

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            SavedVersesScreen(
                state = state,
                verseFormatter = GlobalContext.get().get<QuranVerseFormatter>(),
                onNavigateBack = {},
                onEvent = vm::onEvent
            )
        }
        composeTestRule.waitForIdle()
        assertThat(vm.uiState.value).isEqualTo(SavedVersesUiState.Success(listOf(verse)))
        composeTestRule.onNodeWithText("Bismillah").assertIsDisplayed()
    }
}
