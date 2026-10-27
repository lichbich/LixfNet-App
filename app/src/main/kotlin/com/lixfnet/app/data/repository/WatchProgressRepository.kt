package com.lixfnet.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.lixfnet.app.data.firebase.FirebaseManager
import com.lixfnet.app.data.model.Episode
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

    init {
        loadLocalData()
        syncWithFirebase()
    }

    private fun loadLocalData() {
        val progressJson = prefs.getString("watch_progress_list", null)
        if (!progressJson.isNullOrBlank()) {
            val type = object : TypeToken<List<Movie>>() {}.type
            try {
                val list: List<Movie>? = gson.fromJson(progressJson, type)
                _continueWatching.value = list?.map { it.sanitize() } ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val mapJson = prefs.getString("watch_progress_map", null)
        if (!mapJson.isNullOrBlank()) {
            val type = object : TypeToken<Map<String, WatchProgress>>() {}.type
            try {
                _progressMap.value = gson.fromJson(mapJson, type)
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
        return _progressMap.value[slugOrId] ?: _progressMap.value[slugOrId.trim().lowercase()]
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
                    val progressMap = mutableMapOf<String, WatchProgress>()

                    rawMap.values.forEach { rawItem ->
                        val itemJson = gson.toJsonTree(rawItem).asJsonObject
                        val percent = itemJson.get("progressPercent")?.asFloat ?: 0f
                        val epTitle = itemJson.get("episodeTitle")?.asString ?: "Đang xem dở"
                        val currentSec = itemJson.get("currentTime")?.asLong ?: 0L
                        val durationSec = itemJson.get("duration")?.asLong ?: 0L
                        val epM3u8 = itemJson.get("episodeM3u8")?.asString ?: ""
                        val mId = itemJson.get("movieId")?.asString ?: ""
                        val mSlug = itemJson.get("movieSlug")?.asString ?: ""

                        val movieObj = itemJson.getAsJsonObject("movie")
                        if (movieObj != null) {
                            val movie = parseMovieFromJson(movieObj).copy(
                                progress = percent / 100f,
                                currentEpisode = epTitle
                            )
                            syncedList.add(movie)
                        }

                        val wp = WatchProgress(
                            movieId = mId,
                            movieSlug = mSlug,
                            episodeTitle = epTitle,
                            currentSeconds = currentSec,
                            totalSeconds = durationSec,
                            progressPercent = percent,
                            episodeM3u8 = epM3u8
                        )
                        if (mSlug.isNotBlank()) progressMap[mSlug] = wp
                        if (mId.isNotBlank()) progressMap[mId] = wp
                    }

                    if (syncedList.isNotEmpty()) {
                        _continueWatching.value = syncedList
                        prefs.edit().putString("watch_progress_list", gson.toJson(syncedList)).apply()
                    }
                    if (progressMap.isNotEmpty()) {
                        _progressMap.value = progressMap
                        prefs.edit().putString("watch_progress_map", gson.toJson(progressMap)).apply()
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
                        prefs.edit().putString("my_list_movies", gson.toJson(syncedMyList)).apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveProgress(movie: Movie, episodeTitle: String, currentSec: Long, totalSec: Long, episodeM3u8: String = "") {
        if (totalSec <= 0) return
        val percent = (currentSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

        val updatedMovie = movie.copy(
            progress = percent,
            currentEpisode = episodeTitle
        )

        val currentList = _continueWatching.value.toMutableList()
        currentList.removeAll { it.id == movie.id || it.slug == movie.slug }

        if (percent < 0.95f || movie.type == "series") {
            currentList.add(0, updatedMovie)
        }

        _continueWatching.value = currentList
        prefs.edit().putString("watch_progress_list", gson.toJson(currentList)).apply()

        // Update in-memory progressMap
        val wp = WatchProgress(
            movieId = movie.id,
            movieSlug = movie.slug,
            episodeTitle = episodeTitle,
            currentSeconds = currentSec,
            totalSeconds = totalSec,
            progressPercent = percent * 100f,
            episodeM3u8 = episodeM3u8
        )
        val currentMap = _progressMap.value.toMutableMap()
        if (movie.slug.isNotBlank()) currentMap[movie.slug] = wp
        if (movie.id.isNotBlank()) currentMap[movie.id] = wp
        _progressMap.value = currentMap
        prefs.edit().putString("watch_progress_map", gson.toJson(currentMap)).apply()

        // Sync with Firebase in background
        val user = authRepository.currentUser.value
        if (user != null) {
            scope.launch {
                try {
                    val token = firebaseManager.getAccessToken()
                    val dbUrl = firebaseManager.getDatabaseUrl()
                    val cleanKey = (movie.slug.ifBlank { movie.id }).replace(Regex("[/\\.#$\\[\\]]"), "_")

                    val payload = JsonObject().apply {
                        addProperty("movieId", movie.id)
                        addProperty("movieSlug", movie.slug)
                        addProperty("episodeTitle", episodeTitle)
                        addProperty("episodeM3u8", episodeM3u8)
                        addProperty("currentTime", currentSec)
                        addProperty("duration", totalSec)
                        addProperty("progressPercent", (percent * 100).toInt())
                        addProperty("updatedAt", System.currentTimeMillis())
                        add("movie", gson.toJsonTree(movie))
                    }

                    val putReq = Request.Builder()
                        .url("$dbUrl/lixfnet/users/${user.username}/watchProgress/$cleanKey.json?access_token=$token")
                        .put(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                        .build()

                    httpClient.newCall(putReq).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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
            posterUrl = obj.get("posterUrl")?.asString ?: obj.get("poster_url")?.asString ?: "",
            backdropUrl = obj.get("backdropUrl")?.asString ?: obj.get("thumb_url")?.asString ?: "",
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
