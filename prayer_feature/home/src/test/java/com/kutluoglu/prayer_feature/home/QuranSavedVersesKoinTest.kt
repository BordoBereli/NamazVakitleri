package com.kutluoglu.prayer_feature.home

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.data.di.PrayerDataModule
import com.kutluoglu.prayer.di.PrayerDomainModule
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer.repository.IQuranRepository
import com.kutluoglu.prayer_remote.di.PrayerRemoteModule
import com.kutluoglu.prayer_remote.quran.QuranDataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
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
class QuranSavedVersesKoinTest {

    @Before
    fun setUp() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                PrayerDataModule.module,
                PrayerDomainModule.module,
                PrayerRemoteModule.module,
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
    fun `saved verse is readable through koin repository`() = runBlocking {
        val repository: IQuranRepository = GlobalContext.get().get()
        val verse = AyahData(
            text = "Bismillah",
            surah = SurahInfo("Al-Fatihah", "الفاتحة", 1, 7),
            numberInSurah = 1
        )

        repository.toggleSavedVerse(verse)

        val saved = repository.getSavedVerses("tr").getOrThrow()
        assertThat(saved.flatMap { it.verses }).contains(verse)
    }
}
