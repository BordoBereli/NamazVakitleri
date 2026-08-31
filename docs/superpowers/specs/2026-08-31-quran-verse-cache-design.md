# Quran Verse Cache-First + Bookmark Design

**Status:** Approved 2026-08-31
**Branch:** none yet (new feature off `main`)

## Problem

The home screen loads a random Quran verse with **one remote call per verse** and **zero
caching**. `QuranDataSource.getRandomVerse` picks a random ayah (1–6236) and hits
`https://api.alquran.cloud/v1/ayah/{n}/{edition}` on every invocation. Every app open and every
on-demand tap is a fresh network call — wasteful and slow.

## Goal

Reduce remote calls while preserving verse diversity:

1. **Cache-first + batch-fill.** Pick a random surah; serve from a local cache when present; on a
   miss, fetch the whole surah in a single call and cache it. Verified: the alquran.cloud
   `/v1/surah/{n}/{edition}` endpoint returns all ayahs of a surah in one response.
2. **Bookmark toggle.** Add a save/unsave button in the verse detail sheet, persisted locally.
   Viewing saved verses is out of scope for now.

## Scope

- **In scope:**
  - `:prayer:model` — `QuranApiSurahResponse` + `toQuranVerses()` mapper
  - `:prayer_remote` — `QuranDataSource.getSurah(surahNumber, langCode)` (replaces `getRandomVerse`)
  - `:prayer:data` — new `QuranSurahCache` (DataStore); `QuranRepository` cache-first logic; new
    `SavedVersesStore` (DataStore)
  - `:prayer:domain` — `IQuranRepository` gains `isVerseSaved` / `toggleSavedVerse`; new
    `IsVerseSavedUseCase` / `ToggleSavedVerseUseCase`
  - `:prayer_feature:home` — `VerseDetailSheetContent` bookmark button; `QuranVerseLoader` /
    `HomeViewModel` / `QuranUiState` wiring for saved state; new `HomeEvent.OnToggleVerseSaved`
  - Tests for all of the above
- **Out of scope (untouched):** saved-verses list screen, bundled offline verses, API provider
  change, `QuranVerseLoader` polling behavior, `BottomContainer` / home layout.

## Decisions

1. **Random surah selection.** Pick a random surah (1–114), then a random ayah within it. The
   surah is the natural cache unit (the API returns whole surahs) and needs no static
   surah-boundary table. Distribution is uniform over surahs, giving good cross-surah diversity.
2. **Cache-first lives in the repository.** `QuranRepository` owns the logic: hit → serve from
   cache (no network); miss → `QuranDataSource.getSurah` (1 call) → cache → serve. The public
   `IQuranRepository.getRandomVerse(language)` signature is unchanged, so the use case, loader,
   and UI flow are untouched.
3. **DataStore-backed cache.** `QuranSurahCache` mirrors the existing `PrayerTimesCache` pattern:
   one Preferences key per surah (`quran_surah_{n}`) holding a JSON array of ayahs. Quran text is
   immutable, so no TTL/eviction is needed; a full cache is all 114 surahs (~1–2 MB, acceptable).
4. **Single-shot surah fetch.** `QuranDataSource.getSurah` calls
   `https://api.alquran.cloud/v1/surah/{n}/{edition}`. `getRandomVerse` is removed from the data
   source; randomness moves to the repository.
5. **Bookmark toggle only.** A bookmark `IconButton` in `VerseDetailSheetContent` toggles
   save/unsave. Saved verses persist in a DataStore-backed `SavedVersesStore` (JSON list of
   `AyahData`, key `saved_verses`). No list screen yet.
6. **Saved state via the loader.** `QuranUiState` gains `isSaved`; `QuranVerseLoader` reflects the
   store state after a verse loads and on toggle. The detail sheet renders the bookmark from
   `QuranUiState.isSaved`.

## Target Architecture

### `QuranDataSource` (`:prayer_remote`)

```kotlin
suspend fun getSurah(surahNumber: Int, langCode: String): Result<List<AyahData>> =
    withContext(Dispatchers.IO) {
        val translationIdentifier = supportedTranslations[langCode] ?: supportedTranslations["tr"]!!
        val request = Request.Builder()
            .url("https://api.alquran.cloud/v1/surah/$surahNumber/$translationIdentifier")
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
```

### `QuranApiSurahResponse` (`:prayer:model`)

```kotlin
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

### `QuranSurahCache` (`:prayer:data`)

```kotlin
@Single
class QuranSurahCache(private val dataStore: DataStore<Preferences>) {
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

### `QuranRepository` (`:prayer:data`)

```kotlin
@Single
class QuranRepository(
    private val quranDataSource: QuranDataSource,
    private val quranSurahCache: QuranSurahCache
) : IQuranRepository {
    override suspend fun getRandomVerse(langCode: String): Result<AyahData> {
        val surahNumber = Random.nextInt(1, 115)
        val cached = quranSurahCache.getSurah(surahNumber)
        if (cached != null && cached.isNotEmpty()) {
            return Result.success(cached.random())
        }
        return quranDataSource.getSurah(surahNumber, langCode)
            .onSuccess { ayahs ->
                if (ayahs.isNotEmpty()) quranSurahCache.putSurah(surahNumber, ayahs)
            }
            .map { it.random() }
    }
}
```

### Bookmark (`:prayer:data` + `:prayer:domain` + `:prayer_feature:home`)

- `SavedVersesStore` (DataStore, JSON list of `AyahData`, key `saved_verses`):
  `getSavedVerses()`, `isSaved(verse)`, `toggle(verse)`.
- `IQuranRepository` gains `suspend fun isVerseSaved(verse: AyahData): Boolean` and
  `suspend fun toggleSavedVerse(verse: AyahData)`.
- New `IsVerseSavedUseCase` / `ToggleSavedVerseUseCase` in `:prayer:domain` (same pattern as
  `GetRandomVerseUseCase`).
- `QuranUiState` gains `isSaved: Boolean = false`; `QuranVerseLoader` sets it after a verse loads
  (via `IsVerseSavedUseCase`) and exposes `toggleSaved(verse)` (via `ToggleSavedVerseUseCase`).
- `HomeViewModel` handles `HomeEvent.OnToggleVerseSaved` → `quranVerseLoader.toggleSaved(verse)`.
- `VerseDetailSheetContent` gains a bookmark `IconButton` beside Share; icon reflects
  `QuranUiState.isSaved`.

## Error Handling

- Cache hit → no network, no failure path.
- Cache miss + network failure → `Result.failure` (unchanged behavior; loader logs).
- Corrupt cache JSON → `runCatching` returns null → treated as a miss (same as `PrayerTimesCache`).
- Save toggle failure → logged; UI state unchanged.

## Testing

1. `QuranDataSourceTest` (MockWebServer): surah endpoint parses all ayahs; non-200 → failure;
   malformed JSON → failure.
2. `QuranSurahCacheTest`: put/get round-trip; corrupt JSON → null; missing key → null.
3. `QuranRepositoryTest`: hit serves from cache (no remote call); miss fetches + caches; remote
   failure propagates; empty surah handled.
4. `SavedVersesStoreTest`: toggle add/remove; persistence round-trip.
5. `QuranVerseLoaderTest`: saved state reflects store after load; toggle updates state.
6. Full regression: `./gradlew assembleDebug testDebugUnitTest`; `gitnexus_detect_changes()`
   shows only the expected symbols.

## Acceptance Criteria

- First verse load fetches one surah (1 call); subsequent random picks within cached surahs make
  zero calls.
- Over time the cache fills and the call count drops toward zero without losing verse diversity.
- The verse detail sheet has a bookmark toggle that persists across app restarts.
- All existing tests pass; `QuranVerseLoader` polling behavior and the home UI flow are unchanged.
