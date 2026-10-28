package com.lixfnet.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.lixfnet.app.data.api.ApiClient
import com.lixfnet.app.data.firebase.FirebaseManager
import com.lixfnet.app.data.model.Episode
import com.lixfnet.app.data.model.EpisodeProgress
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.data.model.WatchProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class WatchProgressRepository(
    context: Context,
    private val firebaseManager: FirebaseManager,
    private val authRepository: AuthRepository
) {
    private val prefs = context.getSharedPreferences("lixfnet_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val httpClient = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _continueWatching = MutableStateFlow<List<Movie>>(emptyList())
    val continueWatching: StateFlow<List<Movie>> = _continueWatching.asStateFlow()

    private val _myList = MutableStateFlow<List<Movie>>(emptyList())
    val myList: StateFlow<List<Movie>> = _myList.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<String, WatchProgress>>(emptyMap())
    val progressMap: StateFlow<Map<String, WatchProgress>> = _progressMap.asStateFlow()

    private val remoteKeyMap = mutableMapOf<String, String>()

    companion object {
        fun isSameMovie(m1: Movie, m2: Movie): Boolean {
            if (m1.id.isNotBlank() && m2.id.isNotBlank() && m1.id.equals(m2.id, ignoreCase = true)) return true
            if (m1.slug.isNotBlank() && m2.slug.isNotBlank() && m1.slug.equals(m2.slug, ignoreCase = true)) return true

            val id1 = m1.id.lowercase().removePrefix("tmdb_")
            val id2 = m2.id.lowercase().removePrefix("tmdb_")
            if (id1.isNotBlank() && id2.isNotBlank() && id1 == id2) return true

            val t1 = m1.title.trim().lowercase()
            val t2 = m2.title.trim().lowercase()
            if (t1.isNotBlank() && t1 == t2) return true

            val norm1 = t1.replace(Regex("\\s*\\((?:phần|season)\\s*\\d+\\)", RegexOption.IGNORE_CASE), "").trim()
            val norm2 = t2.replace(Regex("\\s*\\((?:phần|season)\\s*\\d+\\)", RegexOption.IGNORE_CASE), "").trim()
            if (norm1.isNotBlank() && norm1 == norm2) return true

            return false
        }

        fun deduplicateMovies(movies: List<Movie>, progressMap: Map<String, WatchProgress> = emptyMap()): List<Movie> {
            val result = mutableListOf<Movie>()
            for (movie in movies) {
                val existingIndex = result.indexOfFirst { isSameMovie(it, movie) }
                if (existingIndex < 0) {
                    result.add(movie)
                } else {
                    val existing = result[existingIndex]
                    val existingWp = progressMap[existing.slug] ?: progressMap[existing.id]
                    val newWp = progressMap[movie.slug] ?: progressMap[movie.id]
                    val existingTime = existingWp?.updatedAt ?: 0L
                    val newTime = newWp?.updatedAt ?: 0L

                    val winner = if (newTime >= existingTime) {
                        movie.copy(
                            posterUrl = movie.posterUrl.ifBlank { existing.posterUrl },
                            backdropUrl = movie.backdropUrl.ifBlank { existing.backdropUrl },
                            currentEpisode = movie.currentEpisode.ifBlank { existing.currentEpisode },
                            durationText = if (movie.durationText.isNotBlank() && movie.durationText != "45 phút") movie.durationText else existing.durationText,
                            progress = movie.progress ?: existing.progress,
                            episodes = if (movie.episodes.isNotEmpty()) movie.episodes else existing.episodes
                        )
                    } else {
                        existing.copy(
                            posterUrl = existing.posterUrl.ifBlank { movie.posterUrl },
                            backdropUrl = existing.backdropUrl.ifBlank { movie.backdropUrl },
                            currentEpisode = existing.currentEpisode.ifBlank { movie.currentEpisode },
                            durationText = if (existing.durationText.isNotBlank() && existing.durationText != "45 phút") existing.durationText else movie.durationText,
                            progress = existing.progress ?: movie.progress,
                            episodes = if (existing.episodes.isNotEmpty()) existing.episodes else movie.episodes
                        )
                    }
                    result[existingIndex] = winner
                }
            }
            return result
        }
    }

    init {
        loadLocalData()
        syncWithFirebase()
    }

    private fun loadLocalData() {
        val mapJson = prefs.getString("watch_progress_map", null)
        val loadedMap: Map<String, WatchProgress> = if (!mapJson.isNullOrBlank()) {
            val type = object : TypeToken<Map<String, WatchProgress>>() {}.type
            try {
                gson.fromJson<Map<String, WatchProgress>>(mapJson, type) ?: emptyMap()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        } else emptyMap()
        _progressMap.value = loadedMap

        val progressJson = prefs.getString("watch_progress_list", null)
        if (!progressJson.isNullOrBlank()) {
            val type = object : TypeToken<List<Movie>>() {}.type
            try {
                val list: List<Movie>? = gson.fromJson(progressJson, type)
                val sanitized = list?.map { it.sanitize() } ?: emptyList()
                val deduplicated = deduplicateMovies(sanitized, loadedMap)
                val sorted = deduplicated.sortedByDescending { movie ->
                    loadedMap[movie.slug]?.updatedAt
                        ?: loadedMap[movie.id]?.updatedAt
                        ?: loadedMap.values.find { it.movieSlug.equals(movie.slug, ignoreCase = true) || it.movieId.equals(movie.id, ignoreCase = true) }?.updatedAt
                        ?: 0L
                }
                _continueWatching.value = sorted
                prefs.edit().putString("watch_progress_list", gson.toJson(sorted)).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val myListJson = prefs.getString("my_list_movies", null)
        if (!myListJson.isNullOrBlank()) {
            val type = object : TypeToken<List<Movie>>() {}.type
            try {
                val list: List<Movie>? = gson.fromJson(myListJson, type)
                _myList.value = list?.map { it.sanitize() } ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getProgressForMovie(slugOrId: String): WatchProgress? {
        if (slugOrId.isBlank()) return null
        val clean = slugOrId.trim().lowercase()
        _progressMap.value[slugOrId]?.let { return it }
        _progressMap.value[clean]?.let { return it }
        return _progressMap.value.values.find {
            it.movieSlug.equals(clean, ignoreCase = true) ||
            it.movieId.equals(clean, ignoreCase = true) ||
            it.movieSlug.equals(slugOrId, ignoreCase = true) ||
            it.movieId.equals(slugOrId, ignoreCase = true) ||
            (clean.startsWith("tmdb_") && it.movieId.equals(clean.removePrefix("tmdb_"), ignoreCase = true))
        }
    }

    fun syncWithFirebase() {
        val user = authRepository.currentUser.value ?: return
        scope.launch {
            try {
                val token = firebaseManager.getAccessToken()
                val dbUrl = firebaseManager.getDatabaseUrl()
                val username = user.username.lowercase().trim()

                // 1. Fetch Watch Progress from Firebase
                val wpRequest = Request.Builder()
                    .url("$dbUrl/lixfnet/users/$username/watchProgress.json?access_token=$token")
                    .get()
                    .build()
                val wpResponse = httpClient.newCall(wpRequest).execute()
                val wpBody = wpResponse.body?.string()

                if (!wpBody.isNullOrBlank() && wpBody != "null") {
                    val rawMap: Map<String, Any> = gson.fromJson(wpBody, object : TypeToken<Map<String, Any>>() {}.type)
                    val syncedList = mutableListOf<Movie>()
                    val mergedMap = _progressMap.value.toMutableMap()
                    val localPendingUploads = mutableListOf<Pair<Movie, WatchProgress>>()

                    rawMap.forEach { (rawKey, rawItem) ->
                        val itemJson = gson.toJsonTree(rawItem).asJsonObject
                        val currentSec = itemJson.get("currentTime")?.asLong ?: 0L
                        val durationSec = itemJson.get("duration")?.asLong ?: 0L
                        val rawPercent = itemJson.get("progressPercent")?.asFloat ?: 0f
                        val percent = if (durationSec > 0L) {
                            (currentSec.toFloat() / durationSec.toFloat()) * 100f
                        } else rawPercent
                        val epTitle = itemJson.get("episodeTitle")?.asString ?: "Đang xem dở"
                        val epM3u8 = itemJson.get("episodeM3u8")?.asString ?: ""
                        val mId = itemJson.get("movieId")?.asString ?: ""
                        val mSlug = itemJson.get("movieSlug")?.asString ?: ""
                        val remoteUpdatedAt = itemJson.get("updatedAt")?.asLong ?: 0L

                        // Track remote Firebase key (e.g. tmdb_307106)
                        if (mSlug.isNotBlank()) remoteKeyMap[mSlug] = rawKey
                        if (mId.isNotBlank()) remoteKeyMap[mId] = rawKey
                        remoteKeyMap[rawKey] = rawKey

                        val epProgressMap = mutableMapOf<String, EpisodeProgress>()
                        val epProgressJson = itemJson.getAsJsonObject("episodesProgress")
                        if (epProgressJson != null) {
                            epProgressJson.entrySet().forEach { (k, v) ->
                                try {
                                    val epObj = v.asJsonObject
                                    epProgressMap[k] = EpisodeProgress(
                                        currentTime = epObj.get("currentTime")?.asLong ?: 0L,
                                        duration = epObj.get("duration")?.asLong ?: 0L,
                                        progressPercent = epObj.get("progressPercent")?.asFloat ?: 0f,
                                        updatedAt = epObj.get("updatedAt")?.asLong ?: 0L,
                                        isCompleted = epObj.get("isCompleted")?.asBoolean ?: false,
                                        m3u8Url = epObj.get("m3u8Url")?.asString ?: ""
                                    )
                                } catch (e: Exception) {
                                    // ignore individual malformed episode
                                }
                            }
                        }

                        val remoteWp = WatchProgress(
                            movieId = mId,
                            movieSlug = mSlug,
                            episodeTitle = epTitle,
                            currentSeconds = currentSec,
                            totalSeconds = durationSec,
                            progressPercent = percent,
                            episodeM3u8 = epM3u8,
                            updatedAt = remoteUpdatedAt,
                            episodesProgress = epProgressMap
                        )

                        val movieObj = itemJson.getAsJsonObject("movie")
                        val parsedMovie = movieObj?.let {
                            parseMovieFromJson(it).copy(
                                progress = (percent / 100f).coerceIn(0f, 1f),
                                currentEpisode = epTitle
                            )
                        }
                        parsedMovie?.title?.let { remoteKeyMap[it.trim().lowercase()] = rawKey }

                        // Conflict resolution: compare updatedAt with local
                        val localWp = mergedMap[mSlug] ?: mergedMap[mId] ?: mergedMap[rawKey]

                        if (localWp != null && localWp.updatedAt > remoteUpdatedAt) {
                            // Local progress is newer! Keep local and prepare upload
                            if (parsedMovie != null) {
                                val updatedMovie = parsedMovie.copy(
                                    progress = (localWp.progressPercent / 100f).coerceIn(0f, 1f),
                                    currentEpisode = localWp.episodeTitle
                                )
                                syncedList.add(updatedMovie)
                                localPendingUploads.add(Pair(updatedMovie, localWp))
                            }
                        } else {
                            // Remote is newer or equal: accept remote
                            if (mSlug.isNotBlank()) mergedMap[mSlug] = remoteWp
                            if (mId.isNotBlank()) mergedMap[mId] = remoteWp
                            mergedMap[rawKey] = remoteWp
                            if (parsedMovie != null) {
                                syncedList.add(parsedMovie)
                            }
                        }
                    }

                    // Preserve any local continue-watching items not on remote
                    _continueWatching.value.forEach { localMovie ->
                        if (syncedList.none { isSameMovie(it, localMovie) }) {
                            syncedList.add(localMovie)
                        }
                    }

                    // Strict deduplication across all sources
                    val finalSyncedList = deduplicateMovies(syncedList, mergedMap).toMutableList()

                    // Sort continue watching so the most recently watched title is always first
                    finalSyncedList.sortByDescending { movie ->
                        mergedMap[movie.slug]?.updatedAt
                            ?: mergedMap[movie.id]?.updatedAt
                            ?: mergedMap.values.find { it.movieSlug.equals(movie.slug, ignoreCase = true) || it.movieId.equals(movie.id, ignoreCase = true) }?.updatedAt
                            ?: 0L
                    }

                    _continueWatching.value = finalSyncedList
                    _progressMap.value = mergedMap
                    prefs.edit()
                        .putString("watch_progress_list", gson.toJson(finalSyncedList))
                        .putString("watch_progress_map", gson.toJson(mergedMap))
                        .commit()

                    // Upload any local progress that was newer than remote
                    localPendingUploads.forEach { (movie, wp) ->
                        pushProgressToFirebase(user.username, movie, wp)
                    }
                }

                // 2. Fetch My List from Firebase
                val mlRequest = Request.Builder()
                    .url("$dbUrl/lixfnet/users/$username/myList.json?access_token=$token")
                    .get()
                    .build()
                val mlResponse = httpClient.newCall(mlRequest).execute()
                val mlBody = mlResponse.body?.string()

                if (!mlBody.isNullOrBlank() && mlBody != "null") {
                    val rawMap: Map<String, Any> = gson.fromJson(mlBody, object : TypeToken<Map<String, Any>>() {}.type)
                    val syncedMyList = mutableListOf<Movie>()

                    rawMap.values.forEach { rawItem ->
                        val itemJson = gson.toJsonTree(rawItem).asJsonObject
                        syncedMyList.add(parseMovieFromJson(itemJson))
                    }

                    if (syncedMyList.isNotEmpty()) {
                        _myList.value = syncedMyList
                        prefs.edit().putString("my_list_movies", gson.toJson(syncedMyList)).commit()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pushProgressToFirebase(username: String, movie: Movie, wp: WatchProgress) {
        scope.launch {
            try {
                val token = firebaseManager.getAccessToken()
                val dbUrl = firebaseManager.getDatabaseUrl()

                // Check for existing remote key in Firebase (e.g. tmdb_307106)
                val knownKey = remoteKeyMap[movie.id]
                    ?: remoteKeyMap[movie.slug]
                    ?: remoteKeyMap[movie.title.trim().lowercase()]
                    ?: remoteKeyMap.entries.find { (k, _) ->
                        k.equals(movie.id, ignoreCase = true) || k.equals(movie.slug, ignoreCase = true)
                    }?.value

                val cleanKey = (knownKey ?: (if (movie.id.startsWith("tmdb_")) movie.id else movie.slug.ifBlank { movie.id }))
                    .replace(Regex("[/\\.#$\\[\\]]"), "_")

                val payload = JsonObject().apply {
                    addProperty("movieId", movie.id)
                    addProperty("movieSlug", movie.slug)
                    addProperty("episodeTitle", wp.episodeTitle)
                    addProperty("episodeM3u8", wp.episodeM3u8)
                    addProperty("currentTime", wp.currentSeconds)
                    addProperty("duration", wp.totalSeconds)
                    addProperty("progressPercent", wp.progressPercent)
                    addProperty("updatedAt", wp.updatedAt)
                    add("episodesProgress", gson.toJsonTree(wp.episodesProgress))
                    add("movie", gson.toJsonTree(movie))
                }

                val putReq = Request.Builder()
                    .url("$dbUrl/lixfnet/users/$username/watchProgress/$cleanKey.json?access_token=$token")
                    .put(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                    .build()

                val res = httpClient.newCall(putReq).execute()
                if (res.isSuccessful) {
                    android.util.Log.d("WatchProgressRepo", "Uploaded progress for $cleanKey (HTTP ${res.code})")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveProgress(
        movie: Movie,
        episodeTitle: String,
        currentSec: Long,
        totalSec: Long,
        episodeM3u8: String = "",
        syncToCloud: Boolean = false,
        immediate: Boolean = false
    ) {
        if (totalSec <= 0) return
        val percent = (currentSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

        // Safety: protect existing progress if currentSec <= 5 (initialization glitch)
        val existing = getProgressForMovie(movie.slug) ?: getProgressForMovie(movie.id)
        val existingEpisodes = existing?.episodesProgress ?: emptyMap()
        val digits = episodeTitle.filter { it.isDigit() }
        val prevEpData = existingEpisodes[episodeTitle] ?: (if (digits.isNotBlank()) existingEpisodes[digits] else null)

        var finalCurrentSec = currentSec
        var finalPercent = percent

        if (finalCurrentSec <= 5 && prevEpData != null && prevEpData.currentTime > 5 && !prevEpData.isCompleted) {
            finalCurrentSec = prevEpData.currentTime
            finalPercent = prevEpData.progressPercent / 100f
        }

        val updatedMovie = movie.copy(
            progress = finalPercent,
            currentEpisode = episodeTitle
        )

        val currentList = _continueWatching.value.toMutableList()
        currentList.removeAll { isSameMovie(it, movie) }

        if (finalPercent < 0.95f || movie.type == "series") {
            currentList.add(0, updatedMovie)
        }

        val now = System.currentTimeMillis()
        val epData = EpisodeProgress(
            currentTime = finalCurrentSec,
            duration = totalSec,
            progressPercent = finalPercent * 100f,
            updatedAt = now,
            isCompleted = finalPercent >= 0.90f,
            m3u8Url = episodeM3u8.ifBlank { existing?.episodeM3u8 ?: "" }
        )

        val updatedEpisodes = existingEpisodes.toMutableMap()
        updatedEpisodes[episodeTitle] = epData
        if (digits.isNotBlank()) {
            updatedEpisodes[digits] = epData
            val padded = digits.padStart(2, '0')
            updatedEpisodes[padded] = epData
            updatedEpisodes["Tập $digits"] = epData
            updatedEpisodes["Tập $padded"] = epData
        }

        // Update in-memory progressMap
        val wp = WatchProgress(
            movieId = movie.id,
            movieSlug = movie.slug,
            episodeTitle = episodeTitle,
            currentSeconds = finalCurrentSec,
            totalSeconds = totalSec,
            progressPercent = finalPercent * 100f,
            episodeM3u8 = episodeM3u8,
            updatedAt = now,
            episodesProgress = updatedEpisodes
        )

        val currentMap = _progressMap.value.toMutableMap()
        if (movie.slug.isNotBlank()) currentMap[movie.slug] = wp
        if (movie.id.isNotBlank()) currentMap[movie.id] = wp
        _progressMap.value = currentMap

        val deduplicatedList = deduplicateMovies(currentList, currentMap)
        _continueWatching.value = deduplicatedList

        // Local disk save
        val editor = prefs.edit()
            .putString("watch_progress_list", gson.toJson(deduplicatedList))
            .putString("watch_progress_map", gson.toJson(currentMap))
        if (immediate) {
            editor.commit()
        } else {
            editor.apply()
        }

        // Sync with Firebase RTDB
        if (syncToCloud) {
            val user = authRepository.currentUser.value
            if (user != null) {
                pushProgressToFirebase(user.username, updatedMovie, wp)
            }
        }
    }

    fun flushProgress(
        movie: Movie,
        episodeTitle: String,
        currentSec: Long,
        totalSec: Long,
        episodeM3u8: String = ""
    ) {
        saveProgress(
            movie = movie,
            episodeTitle = episodeTitle,
            currentSec = currentSec,
            totalSec = totalSec,
            episodeM3u8 = episodeM3u8,
            syncToCloud = true,
            immediate = true
        )
    }

    fun toggleMyList(movie: Movie): Boolean {
        val currentList = _myList.value.toMutableList()
        val exists = currentList.any { it.id == movie.id || it.slug == movie.slug }

        if (exists) {
            currentList.removeAll { it.id == movie.id || it.slug == movie.slug }
        } else {
            currentList.add(0, movie)
        }

        _myList.value = currentList
        prefs.edit().putString("my_list_movies", gson.toJson(currentList)).apply()

        // Sync with Firebase in background
        val user = authRepository.currentUser.value
        if (user != null) {
            scope.launch {
                try {
                    val token = firebaseManager.getAccessToken()
                    val dbUrl = firebaseManager.getDatabaseUrl()
                    val cleanKey = (movie.slug.ifBlank { movie.id }).replace(Regex("[/\\.#$\\[\\]]"), "_")

                    if (!exists) {
                        // ADD to Firebase
                        val payload = gson.toJsonTree(movie).asJsonObject.apply {
                            addProperty("addedAt", System.currentTimeMillis())
                        }
                        val putReq = Request.Builder()
                            .url("$dbUrl/lixfnet/users/${user.username}/myList/$cleanKey.json?access_token=$token")
                            .put(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                            .build()
                        httpClient.newCall(putReq).execute()
                    } else {
                        // DELETE from Firebase
                        val delReq = Request.Builder()
                            .url("$dbUrl/lixfnet/users/${user.username}/myList/$cleanKey.json?access_token=$token")
                            .delete()
                            .build()
                        httpClient.newCall(delReq).execute()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return !exists
    }

    fun removeContinueWatching(movie: Movie) {
        val currentList = _continueWatching.value.toMutableList()
        currentList.removeAll { isSameMovie(it, movie) }
        _continueWatching.value = currentList
        prefs.edit().putString("watch_progress_list", gson.toJson(currentList)).apply()

        val user = authRepository.currentUser.value
        if (user != null) {
            scope.launch {
                try {
                    val token = firebaseManager.getAccessToken()
                    val dbUrl = firebaseManager.getDatabaseUrl()
                    val targetKey = remoteKeyMap[movie.id] ?: remoteKeyMap[movie.slug] ?: (movie.slug.ifBlank { movie.id }).replace(Regex("[/\\.#$\\[\\]]"), "_")
                    val delReq = Request.Builder()
                        .url("$dbUrl/lixfnet/users/${user.username}/watchProgress/$targetKey.json?access_token=$token")
                        .delete()
                        .build()
                    httpClient.newCall(delReq).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun parseMovieFromJson(obj: JsonObject): Movie {
        val genres = obj.getAsJsonArray("genres")?.map { it.asString } ?: emptyList()
        val cast = obj.getAsJsonArray("cast")?.map { it.asString } ?: emptyList()
        val episodes = if (obj.has("episodes") && obj.get("episodes").isJsonArray) {
            val epType = object : TypeToken<List<Episode>>() {}.type
            try {
                gson.fromJson<List<Episode>>(obj.get("episodes"), epType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        return Movie(
            id = obj.get("id")?.asString ?: obj.get("_id")?.asString ?: obj.get("slug")?.asString ?: "",
            title = obj.get("title")?.asString ?: obj.get("name")?.asString ?: "Không có tiêu đề",
            subTitle = obj.get("subTitle")?.asString ?: obj.get("origin_name")?.asString ?: "",
            slug = obj.get("slug")?.asString ?: "",
            description = obj.get("description")?.asString ?: obj.get("content")?.asString ?: "",
            posterUrl = ApiClient.formatImageUrl(obj.get("posterUrl")?.asString ?: obj.get("poster_url")?.asString ?: ""),
            backdropUrl = ApiClient.formatImageUrl(obj.get("backdropUrl")?.asString ?: obj.get("thumb_url")?.asString ?: ""),
            year = obj.get("year")?.asInt ?: 2026,
            rating = obj.get("rating")?.asString ?: "T16",
            quality = obj.get("quality")?.asString ?: "HD",
            type = obj.get("type")?.asString ?: "series",
            durationText = obj.get("duration")?.asString ?: obj.get("time")?.asString ?: "45 phút",
            currentEpisode = obj.get("currentEpisode")?.asString ?: obj.get("episode_current")?.asString ?: "",
            genres = genres,
            cast = cast,
            episodes = episodes
        )
    }
}
