package com.kutluoglu.prayer_feature.home

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.data.di.PrayerDataModule
import com.kutluoglu.prayer.di.PrayerDomainModule
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.repository.IQuranRepository
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_remote.di.PrayerRemoteModule
import com.kutluoglu.prayer_remote.quran.QuranDataSource
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.di.PrayerFeatureHomeModule
import io.mockk.coEvery
import io.mockk.mockk
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
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SavedVersesScreenWipeTest {

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
                PrayerFeatureHomeModule.module,
                module {
                    single<QuranDataSource> {
                        mockk<QuranDataSource>(relaxed = true).apply {
                            coEvery { getSurah(any(), any()) } returns
                                Result.failure(RuntimeException("no network"))
                        }
                    }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `composing the screen does not wipe saved verses from the store`() {
        val verse = AyahData(
            text = "Bismillah",
            surah = SurahInfo("Al-Fatihah", "الفاتحة", 1, 7),
            numberInSurah = 1
        )
        runBlocking {
            val repository: IQuranRepository = GlobalContext.get().get()
            repository.toggleSavedVerse(verse)
        }

        val getSavedVersesUseCase: GetSavedVersesUseCase = GlobalContext.get().get()
        val reorderSavedVersesUseCase: ReorderSavedVersesUseCase = GlobalContext.get().get()
        val toggleSavedVerseUseCase: ToggleSavedVerseUseCase = GlobalContext.get().get()
        val languageProvider = LanguageProvider()
        val vm = SavedVersesViewModel(
            getSavedVersesUseCase,
            reorderSavedVersesUseCase,
            toggleSavedVerseUseCase,
            languageProvider
        )

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

        runBlocking {
            val repository: IQuranRepository = GlobalContext.get().get()
            val after = repository.getSavedVerses("tr").getOrThrow()
            assertThat(after).contains(verse)
        }
    }
}
