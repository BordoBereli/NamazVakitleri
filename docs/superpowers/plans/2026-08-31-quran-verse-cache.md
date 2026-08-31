# Quran Verse Cache-First + Bookmark Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce Quran verse remote calls by fetching a whole surah in one shot, caching it locally, and serving random verses from cache — plus a bookmark toggle in the verse detail sheet.

**Architecture:** The public flow (UI → `QuranVerseLoader` → `GetRandomVerseUseCase` → `IQuranRepository`) is unchanged. `QuranRepository` becomes cache-first: pick a random surah (1–114), serve from a DataStore-backed `QuranSurahCache` on hit, or fetch the whole surah in one call on miss and cache it. A `SavedVersesStore` (DataStore) backs a bookmark toggle surfaced through `QuranUiState.isSaved`.

**Tech Stack:** Kotlin 2.2.20, Jetpack Compose, Koin (annotations), DataStore Preferences, kotlinx.serialization, OkHttp, JUnit 5 + MockK + Truth + MockWebServer + Robolectric.

**Spec:** `docs/superpowers/specs/2026-08-31-quran-verse-cache-design.md`

---

## File Map

| Action | File |
|--------|------|
| Create | `prayer/model/src/main/java/com/kutluoglu/prayer/model/quran/QuranApiSurahResponse.kt` |
| Modify | `prayer_remote/src/main/java/com/kutluoglu/prayer_remote/quran/QuranDataSource.kt` |
| Create | `prayer_remote/src/test/java/com/kutluoglu/prayer_remote/quran/QuranDataSourceTest.kt` |
| Create | `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/QuranSurahCache.kt` |
| Modify | `prayer/data/src/main/java/com/kutluoglu/prayer/data/di/PrayerDataModule.kt` |
| Create | `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/QuranSurahCacheTest.kt` |
| Modify | `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt` |
| Create | `prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt` |
| Modify | `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt` |
| Create | `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/IsVerseSavedUseCase.kt` |
| Create | `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ToggleSavedVerseUseCase.kt` |
| Create | `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt` |
| Create | `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt` |
| Modify | `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/QuranUiState.kt` |
| Modify | `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/domain/QuranVerseLoader.kt` |
| Modify | `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt` |
| Modify | `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt` |
| Modify | `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt` |
| Modify | `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStateMerger.kt` |
| Modify | `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/feature/VerseDetailSheetContent.kt` |
| Modify | `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt` |
| Modify | `prayer_feature/home/src/main/res/values/strings.xml` |
| Modify | `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/domain/QuranVerseLoaderTest.kt` |
| Modify | `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt` |

---

## Task 1: Model — `QuranApiSurahResponse` + mapper

**Files:**
- Create: `prayer/model/src/main/java/com/kutluoglu/prayer/model/quran/QuranApiSurahResponse.kt`

The `:prayer:model` module has no test infrastructure, so this is verified through the
`QuranDataSourceTest` in Task 2. `AyahData` and `SurahInfo` already exist in
`QuranApiAyahResponse.kt` and are reused.

- [ ] **Step 1: Create the response model + mapper**

```kotlin
package com.kutluoglu.prayer.model.quran

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuranApiSurahResponse(
    @SerialName("code") val code: Int,
    @SerialName("status") val status: String,
    @SerialName("data") val data: SurahData
)

@Serializable
data class SurahData(
    @SerialName("number") val number: Int,
    @SerialName("name") val name: String,
    @SerialName("englishName") val englishName: String,
    @SerialName("numberOfAyahs") val numberOfAyahs: Int,
    @SerialName("ayahs") val ayahs: List<Ayah>
)

@Serializable
data class Ayah(
    @SerialName("number") val number: Int,
    @SerialName("numberInSurah") val numberInSurah: Int,
    @SerialName("text") val text: String
)

fun QuranApiSurahResponse.toQuranVerses(): List<AyahData> = data.ayahs.map { ayah ->
    AyahData(
        text = ayah.text,
        surah = SurahInfo(
            englishName = data.englishName,
            name = data.name,
            number = data.number,
            numberOfAyahs = data.numberOfAyahs
        ),
        numberInSurah = ayah.numberInSurah
    )
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :prayer:model:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add prayer/model/src/main/java/com/kutluoglu/prayer/model/quran/QuranApiSurahResponse.kt
git commit -m "feat(model): add QuranApiSurahResponse and toQuranVerses mapper"
```

---

## Task 2: Remote — `QuranDataSource.getSurah`

**Files:**
- Modify: `prayer_remote/src/main/java/com/kutluoglu/prayer_remote/quran/QuranDataSource.kt`
- Create: `prayer_remote/src/test/java/com/kutluoglu/prayer_remote/quran/QuranDataSourceTest.kt`

Replaces the single-ayah `getRandomVerse` with a whole-surah `getSurah`. Adds an injectable
`baseUrl` (default `https://api.alquran.cloud`) so MockWebServer can be used, matching the
`CitySearchRemoteDataSource` pattern.

- [ ] **Step 1: Write the failing test**

Create `prayer_remote/src/test/java/com/kutluoglu/prayer_remote/quran/QuranDataSourceTest.kt`:

```kotlin
package com.kutluoglu.prayer_remote.quran

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QuranDataSourceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataSource: QuranDataSource

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        dataSource = QuranDataSource(
            httpClient = OkHttpClient(),
            baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        )
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getSurah parses all ayahs of the surah`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "code": 200,
                      "status": "OK",
                      "data": {
                        "number": 1,
                        "name": "سُورَةُ ٱلْفَاتِحَةِ",
                        "englishName": "Al-Faatiha",
                        "englishNameTranslation": "The Opening",
                        "revelationType": "Meccan",
                        "numberOfAyahs": 2,
                        "ayahs": [
                          {"number": 1, "text": "Bismillah", "numberInSurah": 1, "juz": 1, "page": 1},
                          {"number": 2, "text": "Alhamdulillah", "numberInSurah": 2, "juz": 1, "page": 1}
                        ],
                        "edition": {"identifier": "tr.diyanet", "language": "tr", "name": "Diyanet", "englishName": "Diyanet", "format": "text", "type": "translation", "direction": "ltr"}
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = dataSource.getSurah(surahNumber = 1, langCode = "tr")

        assertThat(result.isSuccess).isTrue()
        val verses = result.getOrThrow()
        assertThat(verses).hasSize(2)
        assertThat(verses[0].text).isEqualTo("Bismillah")
        assertThat(verses[0].surah.number).isEqualTo(1)
        assertThat(verses[0].surah.englishName).isEqualTo("Al-Faatiha")
        assertThat(verses[0].numberInSurah).isEqualTo(1)
        assertThat(verses[1].numberInSurah).isEqualTo(2)
    }

    @Test
    fun `getSurah returns failure on non-200 response`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

        val result = dataSource.getSurah(surahNumber = 1, langCode = "tr")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getSurah returns failure on malformed JSON`() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("not json"))

        val result = dataSource.getSurah(surahNumber = 1, langCode = "tr")

        assertThat(result.isFailure).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_remote:test --tests="com.kutluoglu.prayer_remote.quran.QuranDataSourceTest"`
Expected: FAIL — `getSurah` does not exist on `QuranDataSource`

- [ ] **Step 3: Replace `QuranDataSource`**

Replace the entire contents of `prayer_remote/src/main/java/com/kutluoglu/prayer_remote/quran/QuranDataSource.kt`:

```kotlin
package com.kutluoglu.prayer_remote.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.QuranApiSurahResponse
import com.kutluoglu.prayer.model.quran.toQuranVerses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.io.IOException

@Single
class QuranDataSource(
    private val httpClient: OkHttpClient,
    private val baseUrl: String = "https://api.alquran.cloud"
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val supportedTranslations = mapOf(
        "tr" to "tr.diyanet",
        "en" to "en.sahih"
    )

    suspend fun getSurah(surahNumber: Int, langCode: String): Result<List<AyahData>> =
        withContext(Dispatchers.IO) {
            val translationIdentifier = supportedTranslations[langCode] ?: supportedTranslations["tr"]!!
            val request = Request.Builder()
                .url("$baseUrl/v1/surah/$surahNumber/$translationIdentifier")
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val apiResponse = json.decodeFromString<QuranApiSurahResponse>(body)
                        Result.success(apiResponse.toQuranVerses())
                    } else {
                        Result.failure(IOException("API response body was null."))
                    }
                } else {
                    Result.failure(IOException("API request failed with code: ${response.code}"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer_remote:test --tests="com.kutluoglu.prayer_remote.quran.QuranDataSourceTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer_remote/src/main/java/com/kutluoglu/prayer_remote/quran/QuranDataSource.kt prayer_remote/src/test/java/com/kutluoglu/prayer_remote/quran/QuranDataSourceTest.kt
git commit -m "feat(remote): fetch whole surah in one call via getSurah"
```

---

## Task 3: Data — `QuranSurahCache` + `quranStore` DataStore

**Files:**
- Create: `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/QuranSurahCache.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/di/PrayerDataModule.kt`
- Create: `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/QuranSurahCacheTest.kt`

A new `@Named("quranStore")` DataStore is added so the existing unqualified
`prayer_times_cache` DataStore (used by `PrayerTimesCache`) stays unambiguous.

- [ ] **Step 1: Write the failing test**

Create `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/QuranSurahCacheTest.kt`:

```kotlin
package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class QuranSurahCacheTest {

    private lateinit var cache: QuranSurahCache
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = createTempDir()
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        cache = QuranSurahCache(dataStore)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun verse(numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo(
            englishName = "Al-Faatiha",
            name = "الفاتحة",
            number = 1,
            numberOfAyahs = 7
        ),
        numberInSurah = numberInSurah
    )

    @Test
    fun `getSurah returns null for a missing surah`() = runBlocking {
        assertThat(cache.getSurah(1)).isNull()
    }

    @Test
    fun `putSurah then getSurah returns the cached ayahs`() = runBlocking {
        val ayahs = listOf(verse(1), verse(2))

        cache.putSurah(1, ayahs)

        assertThat(cache.getSurah(1)).isEqualTo(ayahs)
    }

    @Test
    fun `getSurah returns null for corrupt json`() = runBlocking {
        val key = androidx.datastore.preferences.core.stringPreferencesKey("quran_surah_1")
        dataStore.edit { it[key] = "not-json" }

        assertThat(cache.getSurah(1)).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="com.kutluoglu.prayer.data.cache.QuranSurahCacheTest"`
Expected: FAIL — `QuranSurahCache` does not exist

- [ ] **Step 3: Create `QuranSurahCache`**

Create `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/QuranSurahCache.kt`:

```kotlin
package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.quran.AyahData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Persists fetched surahs (as lists of ayahs) keyed by surah number so random
 * verses can be served without a network call. Backed by Preferences DataStore.
 */
@Single
class QuranSurahCache(
    @Named("quranStore") private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getSurah(surahNumber: Int): List<AyahData>? {
        val key = stringPreferencesKey("quran_surah_$surahNumber")
        val raw = dataStore.data.map { it[key] }.firstOrNull()
        if (raw.isNullOrBlank()) return null
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrNull()
        }
    }

    suspend fun putSurah(surahNumber: Int, ayahs: List<AyahData>) {
        val key = stringPreferencesKey("quran_surah_$surahNumber")
        val raw = withContext(Dispatchers.Default) { json.encodeToString(ayahs) }
        dataStore.edit { it[key] = raw }
    }
}
```

- [ ] **Step 4: Add the `quranStore` DataStore provider**

Modify `prayer/data/src/main/java/com/kutluoglu/prayer/data/di/PrayerDataModule.kt` — add this
function inside the `PrayerDataModule` object (after `providePrayerTimesDataStore`):

```kotlin
    @Single
    @Named("quranStore")
    fun provideQuranDataStore(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("quran_store") }
        )
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="com.kutluoglu.prayer.data.cache.QuranSurahCacheTest"`
Expected: PASS (3 tests)

- [ ] **Step 6: Commit**

```bash
git add prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/QuranSurahCache.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/di/PrayerDataModule.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/QuranSurahCacheTest.kt
git commit -m "feat(data): add QuranSurahCache backed by quranStore DataStore"
```

---

## Task 4: Data — `QuranRepository` cache-first

**Files:**
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt`
- Create: `prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

Create `prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt`:

```kotlin
package com.kutluoglu.prayer.data.quran

import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.data.cache.QuranSurahCache
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import com.kutluoglu.prayer_remote.quran.QuranDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class QuranRepositoryTest {

    private val quranDataSource: QuranDataSource = mockk()
    private val quranSurahCache: QuranSurahCache = mockk()

    private fun verse(surahNumber: Int, numberInSurah: Int) = AyahData(
        text = "Text",
        surah = SurahInfo(
            englishName = "Surah $surahNumber",
            name = "سورة",
            number = surahNumber,
            numberOfAyahs = 10
        ),
        numberInSurah = numberInSurah
    )

    @Test
    fun `serves from cache when the random surah is cached`() = runTest {
        val cached = listOf(verse(1, 1), verse(1, 2))
        coEvery { quranSurahCache.getSurah(any()) } returns cached

        val repository = QuranRepository(quranDataSource, quranSurahCache)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isIn(cached)
        coVerify(exactly = 0) { quranDataSource.getSurah(any(), any()) }
    }

    @Test
    fun `fetches and caches the surah on a cache miss`() = runTest {
        val fetched = listOf(verse(2, 1), verse(2, 2))
        coEvery { quranSurahCache.getSurah(any()) } returns null
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.success(fetched)

        val repository = QuranRepository(quranDataSource, quranSurahCache)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isIn(fetched)
        coVerify(exactly = 1) { quranDataSource.getSurah(any(), "tr") }
        coVerify(exactly = 1) { quranSurahCache.putSurah(any(), fetched) }
    }

    @Test
    fun `propagates failure when remote fetch fails`() = runTest {
        coEvery { quranSurahCache.getSurah(any()) } returns null
        coEvery { quranDataSource.getSurah(any(), "tr") } returns Result.failure(RuntimeException("network"))

        val repository = QuranRepository(quranDataSource, quranSurahCache)
        val result = repository.getRandomVerse("tr")

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { quranSurahCache.putSurah(any(), any()) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="com.kutluoglu.prayer.data.quran.QuranRepositoryTest"`
Expected: FAIL — `QuranRepository` constructor does not accept `QuranSurahCache`

- [ ] **Step 3: Replace `QuranRepository`**

Replace the entire contents of `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt`:

```kotlin
package com.kutluoglu.prayer.data.quran

import com.kutluoglu.prayer.data.cache.QuranSurahCache
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import com.kutluoglu.prayer_remote.quran.QuranDataSource
import org.koin.core.annotation.Single
import kotlin.random.Random

/**
 * Cache-first random verse provider. Picks a random surah (1-114); serves from
 * the local cache when present, otherwise fetches the whole surah in one call
 * and caches it for future use.
 */
@Single
class QuranRepository(
    private val quranDataSource: QuranDataSource,
    private val quranSurahCache: QuranSurahCache
) : IQuranRepository {
    override suspend fun getRandomVerse(langCode: String): Result<AyahData> {
        val surahNumber = Random.nextInt(1, 115)
        val cached = quranSurahCache.getSurah(surahNumber)
        if (!cached.isNullOrEmpty()) {
            return Result.success(cached.random())
        }
        return quranDataSource.getSurah(surahNumber, langCode)
            .onSuccess { ayahs ->
                if (ayahs.isNotEmpty()) quranSurahCache.putSurah(surahNumber, ayahs)
            }
            .mapCatching { ayahs ->
                require(ayahs.isNotEmpty()) { "Surah $surahNumber returned no ayahs" }
                ayahs.random()
            }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="com.kutluoglu.prayer.data.quran.QuranRepositoryTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/quran/QuranRepositoryTest.kt
git commit -m "feat(data): cache-first random verse in QuranRepository"
```

---

## Task 5: Domain — `IQuranRepository` + save use cases

**Files:**
- Modify: `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt`
- Create: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/IsVerseSavedUseCase.kt`
- Create: `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ToggleSavedVerseUseCase.kt`

These are thin wrappers (same pattern as `GetRandomVerseUseCase`); they are exercised through the
repository and loader tests, so no dedicated test file is created.

- [ ] **Step 1: Extend `IQuranRepository`**

Replace the contents of `prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt`:

```kotlin
package com.kutluoglu.prayer.repository

import com.kutluoglu.prayer.model.quran.AyahData

/**
 * Created by F.K. on 11.11.2025.
 *
 */
interface IQuranRepository {
    suspend fun getRandomVerse(language: String): Result<AyahData>
    suspend fun isVerseSaved(verse: AyahData): Boolean
    suspend fun toggleSavedVerse(verse: AyahData): Result<Unit>
}
```

- [ ] **Step 2: Create `IsVerseSavedUseCase`**

Create `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/IsVerseSavedUseCase.kt`:

```kotlin
package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class IsVerseSavedUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(verse: AyahData): Boolean = repository.isVerseSaved(verse)
}
```

- [ ] **Step 3: Create `ToggleSavedVerseUseCase`**

Create `prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ToggleSavedVerseUseCase.kt`:

```kotlin
package com.kutluoglu.prayer.usecases.quran

import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.repository.IQuranRepository
import org.koin.core.annotation.Factory

@Factory
class ToggleSavedVerseUseCase(
    private val repository: IQuranRepository
) {
    suspend operator fun invoke(verse: AyahData): Result<Unit> = repository.toggleSavedVerse(verse)
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :prayer:domain:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add prayer/domain/src/main/java/com/kutluoglu/prayer/repository/IQuranRepository.kt prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/IsVerseSavedUseCase.kt prayer/domain/src/main/java/com/kutluoglu/prayer/usecases/quran/ToggleSavedVerseUseCase.kt
git commit -m "feat(domain): add isVerseSaved and toggleSavedVerse to quran repository"
```

---

## Task 6: Data — `SavedVersesStore` + repository save impl

**Files:**
- Create: `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt`
- Modify: `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt`
- Create: `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt`

- [ ] **Step 1: Write the failing test**

Create `prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt`:

```kotlin
package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SurahInfo
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class SavedVersesStoreTest {

    private lateinit var store: SavedVersesStore
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = createTempDir()
        dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        store = SavedVersesStore(dataStore)
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun verse(numberInSurah: Int) = AyahData(
        text = "Text $numberInSurah",
        surah = SurahInfo(
            englishName = "Al-Faatiha",
            name = "الفاتحة",
            number = 1,
            numberOfAyahs = 7
        ),
        numberInSurah = numberInSurah
    )

    @Test
    fun `isSaved is false for a verse never saved`() = runBlocking {
        assertThat(store.isSaved(verse(1))).isFalse()
    }

    @Test
    fun `toggle adds then removes a verse`() = runBlocking {
        val v = verse(1)

        store.toggle(v)
        assertThat(store.isSaved(v)).isTrue()

        store.toggle(v)
        assertThat(store.isSaved(v)).isFalse()
    }

    @Test
    fun `saved verses persist across store instances`() = runBlocking {
        val v = verse(2)
        store.toggle(v)

        val reloaded = SavedVersesStore(dataStore)
        assertThat(reloaded.isSaved(v)).isTrue()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="com.kutluoglu.prayer.data.cache.SavedVersesStoreTest"`
Expected: FAIL — `SavedVersesStore` does not exist

- [ ] **Step 3: Create `SavedVersesStore`**

Create `prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt`:

```kotlin
package com.kutluoglu.prayer.data.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kutluoglu.prayer.model.quran.AyahData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Persists user-bookmarked verses as a JSON list of [AyahData]. Backed by the
 * same quranStore Preferences DataStore as [QuranSurahCache].
 */
@Single
class SavedVersesStore(
    @Named("quranStore") private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("saved_verses")

    suspend fun getSavedVerses(): List<AyahData> {
        val raw = dataStore.data.map { it[key] }.firstOrNull()
        if (raw.isNullOrBlank()) return emptyList()
        return withContext(Dispatchers.Default) {
            runCatching { json.decodeFromString<List<AyahData>>(raw) }.getOrDefault(emptyList())
        }
    }

    suspend fun isSaved(verse: AyahData): Boolean = getSavedVerses().any { it == verse }

    suspend fun toggle(verse: AyahData) {
        val current = getSavedVerses()
        val updated = if (current.any { it == verse }) {
            current.filterNot { it == verse }
        } else {
            current + verse
        }
        val raw = withContext(Dispatchers.Default) { json.encodeToString(updated) }
        dataStore.edit { it[key] = raw }
    }
}
```

- [ ] **Step 4: Wire save methods into `QuranRepository`**

Modify `prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt`:

Add the import `import com.kutluoglu.prayer.data.cache.SavedVersesStore`, add
`private val savedVersesStore: SavedVersesStore` to the constructor, and add these two methods:

```kotlin
    override suspend fun isVerseSaved(verse: AyahData): Boolean = savedVersesStore.isSaved(verse)

    override suspend fun toggleSavedVerse(verse: AyahData): Result<Unit> =
        runCatching { savedVersesStore.toggle(verse) }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :prayer:data:testDebugUnitTest --tests="com.kutluoglu.prayer.data.cache.SavedVersesStoreTest" --tests="com.kutluoglu.prayer.data.quran.QuranRepositoryTest"`
Expected: PASS (3 + 3 tests)

- [ ] **Step 6: Commit**

```bash
git add prayer/data/src/main/java/com/kutluoglu/prayer/data/cache/SavedVersesStore.kt prayer/data/src/main/java/com/kutluoglu/prayer/data/quran/QuranRepository.kt prayer/data/src/test/java/com/kutluoglu/prayer/data/cache/SavedVersesStoreTest.kt
git commit -m "feat(data): add SavedVersesStore and repository save methods"
```

---

## Task 7: Feature — saved state through loader, view model, and state

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/QuranUiState.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/domain/QuranVerseLoader.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStateMerger.kt`
- Modify: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/domain/QuranVerseLoaderTest.kt`

- [ ] **Step 1: Write the failing loader tests**

Append these tests to `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/domain/QuranVerseLoaderTest.kt` and update the constructor call sites in the existing tests to pass the two new mocked use cases:

```kotlin
    private val isVerseSavedUseCase: IsVerseSavedUseCase = mockk()
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase = mockk()
```

Add the imports `import com.kutluoglu.prayer.usecases.quran.IsVerseSavedUseCase` and
`import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase` to the test file.

Existing `QuranVerseLoader(getRandomVerseUseCase, languageProvider)` calls become
`QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)`.

New tests:

```kotlin
    @Test
    fun `loadVerse sets isSaved from the store`() = runTest {
        val verse = AyahData(
            text = "Bismillah...",
            surah = SurahInfo(
                englishName = "Al-Fatihah",
                name = "الفاتحة",
                number = 1,
                numberOfAyahs = 7
            ),
            numberInSurah = 1
        )
        coEvery { languageProvider.getLanguageCode() } returns "tr"
        coEvery { getRandomVerseUseCase.invoke("tr") } returns Result.success(verse)
        coEvery { isVerseSavedUseCase.invoke(verse) } returns true

        val loader = QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)
        loader.loadVerse(scope = this, isScreenReady = { true })
        runCurrent()

        assertThat(loader.quranState.value.verse).isEqualTo(verse)
        assertThat(loader.quranState.value.isSaved).isTrue()
    }

    @Test
    fun `toggleSaved flips isSaved on success`() = runTest {
        val verse = AyahData(
            text = "Bismillah...",
            surah = SurahInfo(
                englishName = "Al-Fatihah",
                name = "الفاتحة",
                number = 1,
                numberOfAyahs = 7
            ),
            numberInSurah = 1
        )
        coEvery { toggleSavedVerseUseCase.invoke(verse) } returns Result.success(Unit)

        val loader = QuranVerseLoader(getRandomVerseUseCase, isVerseSavedUseCase, toggleSavedVerseUseCase, languageProvider)
        loader.toggleSaved(verse, scope = this)
        runCurrent()

        assertThat(loader.quranState.value.isSaved).isTrue()
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.domain.QuranVerseLoaderTest"`
Expected: FAIL — `QuranVerseLoader` constructor mismatch and missing `toggleSaved`

- [ ] **Step 3: Update `QuranUiState`**

Replace `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/QuranUiState.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home.state

import com.kutluoglu.prayer.model.quran.AyahData

data class QuranUiState(
    val verse: AyahData? = null,
    val isSheetVisible: Boolean = false,
    val isSaved: Boolean = false
)
```

- [ ] **Step 4: Update `QuranVerseLoader`**

Replace `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/domain/QuranVerseLoader.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home.domain

import android.util.Log
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.usecases.quran.GetRandomVerseUseCase
import com.kutluoglu.prayer.usecases.quran.IsVerseSavedUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_feature.home.state.QuranUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class QuranVerseLoader(
    private val getRandomVerseUseCase: GetRandomVerseUseCase,
    private val isVerseSavedUseCase: IsVerseSavedUseCase,
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase,
    private val languageProvider: LanguageProvider
) {
    private val _quranState = MutableStateFlow(QuranUiState())
    val quranState: StateFlow<QuranUiState> = _quranState

    /**
     * Polls until [isScreenReady] returns true (1s backoff doubling up to 30s),
     * then fetches the verse exactly once and reflects its saved state.
     */
    fun loadVerse(scope: CoroutineScope, isScreenReady: () -> Boolean) {
        scope.launch {
            var delayMillis = 1_000L
            while (true) {
                if (isScreenReady()) {
                    val language = languageProvider.getLanguageCode()
                    getRandomVerseUseCase(language)
                        .onSuccess { verse ->
                            val isSaved = isVerseSavedUseCase(verse)
                            _quranState.value = _quranState.value.copy(verse = verse, isSaved = isSaved)
                        }
                        .onFailure {
                            Log.e("QuranVerseLoader", "Failed to load random verse -> ${it.message}")
                        }
                    break
                }
                delay(delayMillis)
                delayMillis = (delayMillis * 2).coerceAtMost(30_000L)
            }
        }
    }

    fun toggleSaved(verse: AyahData, scope: CoroutineScope) {
        scope.launch {
            toggleSavedVerseUseCase(verse)
                .onSuccess {
                    _quranState.value = _quranState.value.copy(isSaved = !_quranState.value.isSaved)
                }
                .onFailure {
                    Log.e("QuranVerseLoader", "Failed to toggle saved verse -> ${it.message}")
                }
        }
    }

    fun setSheetVisible(isVisible: Boolean) {
        _quranState.value = _quranState.value.copy(isSheetVisible = isVisible)
    }
}
```

- [ ] **Step 5: Add the event**

Modify `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt` — add
`object OnToggleVerseSaved : HomeEvent` after `OnVerseDetailDismissed`:

```kotlin
    object OnVerseDetailDismissed : HomeEvent
    object OnToggleVerseSaved : HomeEvent
```

- [ ] **Step 6: Handle the event in `HomeViewModel`**

Modify `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt` — add
this branch to the `when` in `onEvent` (after the `OnVerseDetailDismissed` branch):

```kotlin
            HomeEvent.OnToggleVerseSaved -> {
                quranVerseLoader.quranState.value.verse?.let { verse ->
                    quranVerseLoader.toggleSaved(verse, viewModelScope)
                }
            }
```

- [ ] **Step 7: Add `isVerseSaved` to `HomeUiState.Success`**

Modify `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt` —
add the field to `Success`:

```kotlin
        val quranVerse: AyahData? = null,
        val isVerseDetailSheetVisible: Boolean = false,
        val isVerseSaved: Boolean = false
```

- [ ] **Step 8: Pass it through the merger**

Modify `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStateMerger.kt` —
in the `HomeUiState.Success(...)` call, add:

```kotlin
                    quranVerse = quran.verse,
                    isVerseDetailSheetVisible = quran.isSheetVisible,
                    isVerseSaved = quran.isSaved
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.domain.QuranVerseLoaderTest"`
Expected: PASS (5 tests)

- [ ] **Step 10: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/QuranUiState.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/domain/QuranVerseLoader.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeEvent.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/HomeViewModel.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStates.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/state/HomeUiStateMerger.kt prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/domain/QuranVerseLoaderTest.kt
git commit -m "feat(home): surface verse saved state through loader and view model"
```

---

## Task 8: Feature — bookmark button in the verse detail sheet

**Files:**
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/feature/VerseDetailSheetContent.kt`
- Modify: `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt`
- Modify: `prayer_feature/home/src/main/res/values/strings.xml`
- Modify: `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt`

- [ ] **Step 1: Write the failing Compose test**

Append this test to `prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt` (it already has a `composeTestRule` and imports for `onNodeWithContentDescription`-style assertions — add `import androidx.compose.ui.test.onNodeWithContentDescription` and `import androidx.compose.ui.test.performClick` if not already present):

```kotlin
    @Test
    fun `verse detail sheet bookmark toggles saved state`() {
        val verse = com.kutluoglu.prayer.model.quran.AyahData(
            text = "Bismillah",
            surah = com.kutluoglu.prayer.model.quran.SurahInfo(
                englishName = "Al-Fatihah",
                name = "الفاتحة",
                number = 1,
                numberOfAyahs = 7
            ),
            numberInSurah = 1
        )
        var toggled = false
        composeTestRule.setContent {
            HomeScreen(
                navController = mockk<NavController>(relaxed = true),
                uiState = HomeUiState.Success(
                    locationState = com.kutluoglu.prayer_feature.common.states.LocationUiState(
                        locationData = istanbul.location,
                        locationInfoText = "Istanbul, Turkey"
                    ),
                    quranVerse = verse,
                    isVerseDetailSheetVisible = true,
                    isVerseSaved = false
                ),
                locationsState = LocationsState(entries = listOf(istanbul), selectedId = "loc-1"),
                prayerDataByLocation = emptyMap(),
                activeLocationId = "loc-1",
                quranVerseFormatter = mockk<QuranVerseFormatter>(relaxed = true),
                onEvent = { if (it == HomeEvent.OnToggleVerseSaved) toggled = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Save Verse").performClick()
        assertThat(toggled).isTrue()
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.HomeScreenTest"`
Expected: FAIL — no node with content description "Save Verse"

- [ ] **Step 3: Add strings**

Add to `prayer_feature/home/src/main/res/values/strings.xml` (after `share_verse`):

```xml
    <string name="save_verse">Save Verse</string>
    <string name="unsave_verse">Unsave Verse</string>
```

- [ ] **Step 4: Update `VerseDetailSheetContent`**

Replace `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/feature/VerseDetailSheetContent.kt`:

```kotlin
package com.kutluoglu.prayer_feature.home.feature

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.home.R
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun VerseDetailSheetContent(
        verse: AyahData,
        verseFormatter: QuranVerseFormatter,
        isSaved: Boolean = false,
        onToggleSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val localizedSurahName = verseFormatter.getLocalizedNameOf(
        quranVerse = verse,
        context = context
    )
    val verseInfo = "($localizedSurahName - $verse)"
    val appName = context.getString(R.string.app_name)
    val sharedApp = "\n\n${context.getString(R.string.shared_from_app, appName)}"
    val fullTextToShare = "\"${verse.text}\" - $verseInfo $sharedApp"

    // Get screen height to calculate max height in Dp
    val screenHeight =
        LocalResources.current.displayMetrics.heightPixels.dp / LocalDensity.current.density

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Set a maximum height. The content will be scrollable if it exceeds this.
            // For short content, the Column will be smaller.
            .heightIn(min = 0.dp, max = screenHeight * 0.65f)
            // Make the entire column scrollable if content overflows
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = verse.text,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.height(16.dp))
        // for the verse info and the action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Pushes items to the ends
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = verseInfo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleSaved
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = context.getString(
                            if (isSaved) R.string.unsave_verse else R.string.save_verse
                        )
                    )
                }
                IconButton(
                    onClick = { shareVerse(fullTextToShare, context) }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = context.getString(R.string.share_verse)
                    )
                }
            }
        }
    }
}

private fun shareVerse(fullTextToShare: String, context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_TEXT, fullTextToShare)

        // Get the icon URI
        val iconUri = getIconUri(context)
        iconUri?.let {
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            context.getString(R.string.share_verse)
        )
    )
}

private fun getIconUri(context: Context): Uri? {
    try {
        // Get the launcher icon drawable
        val drawable = context.packageManager.getApplicationIcon(context.packageName)

        val originalBitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            // Create a bitmap from the drawable
            createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            ).also {
                val canvas = android.graphics.Canvas(it)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }

        // Save the bitmap to the cache directory
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val imageFile = File(imagesDir, "app_icon.png")

        FileOutputStream(imageFile).use {
            originalBitmap.compress(
                Bitmap.CompressFormat.PNG, 100, it
            )
        }

        // Get the content URI using FileProvider
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}
```

Note: the original file imported `androidx.compose.ui.platform.LocalResources` — keep that import
(used for `LocalResources.current.displayMetrics`).

- [ ] **Step 5: Wire the sheet call site in `LocationPager`**

Modify `prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt` — the
`VerseDetailSheetContent` call inside `PrayerContent` (around line 259) becomes:

```kotlin
                quranVerse?.let { verse ->
                    VerseDetailSheetContent(
                        verse = verse,
                        verseFormatter = quranVerseFormatter,
                        isSaved = successState?.isVerseSaved == true,
                        onToggleSaved = { onEvent(HomeEvent.OnToggleVerseSaved) }
                    )
                }
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :prayer_feature:home:testDebugUnitTest --tests="com.kutluoglu.prayer_feature.home.HomeScreenTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/feature/VerseDetailSheetContent.kt prayer_feature/home/src/main/java/com/kutluoglu/prayer_feature/home/LocationPager.kt prayer_feature/home/src/main/res/values/strings.xml prayer_feature/home/src/test/java/com/kutluoglu/prayer_feature/home/HomeScreenTest.kt
git commit -m "feat(home): add bookmark toggle to verse detail sheet"
```

---

## Task 9: Full regression + impact verification

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: Verify affected scope**

Run: `gitnexus_detect_changes()`
Expected: only the Quran-related symbols changed (data source, repository, cache, loader, view
model, detail sheet, state). No unrelated execution flows affected.

- [ ] **Step 3: Final commit (if any uncommitted changes remain)**

```bash
git status
git add -A
git commit -m "chore: finalize quran verse cache-first + bookmark"
```
