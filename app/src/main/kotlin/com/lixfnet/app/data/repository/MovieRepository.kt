package com.lixfnet.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lixfnet.app.data.api.ApiClient
import com.lixfnet.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MovieRepository(private val context: Context? = null) {
    private val api = ApiClient.kkPhimApi
    private val webApi = ApiClient.lixfNetWebApi
    private val gson = Gson()
    private val prefs = context?.getSharedPreferences("lixfnet_top10_cache", Context.MODE_PRIVATE)

    // In-memory cache for fast instant UI loading
    private var cachedHero: Movie? = null
    private var cachedChinaSlugs: Set<String> = emptySet()
    private var lastChinaFetchTime: Long = 0

    // Top 10 Weekly Cache
    private var cachedSeriesTop10: List<Movie>? = null
    private var cachedFilmsTop10: List<Movie>? = null
    private var cachedWeekId: String = ""
    private var nextTuesdayExpiry: Long = 0L

    /**
     * Fetches Chinese movie slugs to filter out Chinese content from home pools
     */
    suspend fun getChineseMovieSlugs(): Set<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedChinaSlugs.isNotEmpty() && now - lastChinaFetchTime < 15 * 60 * 1000) {
            return@withContext cachedChinaSlugs
        }
        try {
            val pages = (1..3).map { page ->
                async {
                    try {
                        api.getMoviesByCountry(slug = "trung-quoc", page = page).data?.items ?: emptyList()
                    } catch (e: Exception) {
                        emptyList<KKMovieItem>()
                    }
                }
            }.awaitAll().flatten()

            val slugSet = pages.mapNotNull { it.slug }.toSet()
            if (slugSet.isNotEmpty()) {
                cachedChinaSlugs = slugSet
                lastChinaFetchTime = now
            }
            return@withContext cachedChinaSlugs
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext cachedChinaSlugs
        }
    }

    fun isChineseMovie(movie: Movie, chinaSlugs: Set<String>): Boolean {
        val key = movie.slug.ifBlank { movie.id }
        if (chinaSlugs.contains(key)) return true
        if (movie.countrySlug == "trung-quoc") return true
        if (movie.countries.any { it.contains("trung", ignoreCase = true) }) return true
        if (movie.subTitle.contains("quan thoại", ignoreCase = true) ||
            movie.subTitle.contains("mandarin", ignoreCase = true) ||
            movie.subTitle.contains("quảng đông", ignoreCase = true)
        ) return true
        return false
    }

    fun filterNoChina(list: List<Movie>, chinaSlugs: Set<String>): List<Movie> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Movie>()
        for (m in list) {
            val key = m.slug.ifBlank { m.id }
            if (seen.contains(key)) continue
            seen.add(key)
            if (!isChineseMovie(m, chinaSlugs)) {
                result.add(m)
            }
        }
        return result
    }

    suspend fun getHeroMovie(): Movie = withContext(Dispatchers.IO) {
        cachedHero?.let { return@withContext it }
        try {
            val response = api.getSeriesMovies(page = 1)
            val firstItem = response.data?.items?.firstOrNull()
            if (firstItem != null) {
                val detail = try {
                    firstItem.slug?.let { api.getMovieDetail(it).movie }
                } catch (e: Exception) {
                    null
                }
                val hero = mapToMovie(firstItem, detail).copy(
                    isTop10 = true,
                    top10Rank = 1
                )
                cachedHero = hero
                return@withContext hero
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        fallbackHeroMovie()
    }

    suspend fun getNewUpdatedMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getNewUpdatedMovies(page = page)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Mới cập nhật")
        }
    }

    suspend fun getKoreanMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMoviesByCountry(slug = "han-quoc", page = page)
            res.data?.items?.map { mapToMovie(it).copy(countrySlug = "han-quoc") } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Hàn Quốc")
        }
    }

    suspend fun getUsMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMoviesByCountry(slug = "my", page = page)
            res.data?.items?.map { mapToMovie(it).copy(countrySlug = "my") } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Âu Mỹ")
        }
    }

    suspend fun getVietnameseMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMoviesByCountry(slug = "viet-nam", page = page)
            res.data?.items?.map { mapToMovie(it).copy(countrySlug = "viet-nam") } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Việt Nam")
        }
    }

    suspend fun getThaiMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMoviesByCountry(slug = "thai-lan", page = page)
            res.data?.items?.map { mapToMovie(it).copy(countrySlug = "thai-lan") } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Thái Lan")
        }
    }

    suspend fun getJapanMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMoviesByCountry(slug = "nhat-ban", page = page)
            res.data?.items?.map { mapToMovie(it).copy(countrySlug = "nhat-ban") } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Nhật Bản")
        }
    }

    suspend fun getActionMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMoviesByGenre(slug = "hanh-dong", page = page)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Hành động")
        }
    }

    suspend fun getHorrorMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMoviesByGenre(slug = "kinh-di", page = page)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Kinh dị")
        }
    }

    suspend fun getRomanceMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getMoviesByGenre(slug = "tinh-cam", page = page)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Tình cảm")
        }
    }

    suspend fun getSeriesMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getSeriesMovies(page = page)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Series")
        }
    }

    suspend fun getSingleMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getSingleMovies(page = page)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Phim lẻ")
        }
    }

    suspend fun getAnimeMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getAnimeMovies(page = page)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("Hoạt hình")
        }
    }

    suspend fun getTvShows(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val res = api.getTvShows(page = page)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMovies("TV Shows")
        }
    }

    /**
     * Calculates timestamp of the next official Netflix Tudum Tuesday release (20:00 UTC / 03:00 AM Wed VN).
     */
    fun getNextTuesdayUpdateTimestamp(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Tuesday = 3
        var daysUntilTuesday = (Calendar.TUESDAY - dayOfWeek + 7) % 7
        if (daysUntilTuesday == 0 && cal.get(Calendar.HOUR_OF_DAY) >= 20) {
            daysUntilTuesday = 7
        }
        cal.add(Calendar.DAY_OF_MONTH, daysUntilTuesday)
        cal.set(Calendar.HOUR_OF_DAY, 20)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val CACHE_VERSION = 3
    }

    private fun loadTop10FromPrefs() {
        if (prefs == null) return
        val cachedVersion = prefs.getInt("top10_cache_version", 0)
        if (cachedVersion < CACHE_VERSION) {
            // Invalidate corrupted/old cache with wrong images
            prefs.edit().clear().apply()
            cachedSeriesTop10 = null
            cachedFilmsTop10 = null
            return
        }
        val expiry = prefs.getLong("top10_next_tuesday_expiry", 0L)
        val now = System.currentTimeMillis()
        if (expiry > now) {
            nextTuesdayExpiry = expiry
            cachedWeekId = prefs.getString("top10_week_id", "") ?: ""
            val seriesJson = prefs.getString("top10_series_cache", null)
            val filmsJson = prefs.getString("top10_films_cache", null)
            val type = object : TypeToken<List<Movie>>() {}.type
            if (!seriesJson.isNullOrBlank()) {
                try {
                    cachedSeriesTop10 = gson.fromJson(seriesJson, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (!filmsJson.isNullOrBlank()) {
                try {
                    cachedFilmsTop10 = gson.fromJson(filmsJson, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveTop10ToPrefs(series: List<Movie>, films: List<Movie>, weekId: String, expiry: Long) {
        cachedSeriesTop10 = series
        cachedFilmsTop10 = films
        cachedWeekId = weekId
        nextTuesdayExpiry = expiry
        prefs?.edit()
            ?.putString("top10_series_cache", gson.toJson(series))
            ?.putString("top10_films_cache", gson.toJson(films))
            ?.putString("top10_week_id", weekId)
            ?.putLong("top10_next_tuesday_expiry", expiry)
            ?.putInt("top10_cache_version", CACHE_VERSION)
            ?.apply()
    }

    private suspend fun fetchTudumDirect(): Pair<List<String>, List<String>>? = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
        try {
            val tvReq = Request.Builder()
                .url("https://www.netflix.com/tudum/top10/vietnam/tv")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()
            val filmsReq = Request.Builder()
                .url("https://www.netflix.com/tudum/top10/vietnam")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()
            val tvRes = client.newCall(tvReq).execute()
            val filmsRes = client.newCall(filmsReq).execute()
            if (!tvRes.isSuccessful || !filmsRes.isSuccessful) return@withContext null
            val tvHtml = tvRes.body?.string() ?: ""
            val filmsHtml = filmsRes.body?.string() ?: ""
            val regex = Regex("alt=\"([^\"]+)\"\\s+class=\"[^\"]*cache-[^\"]*\"")
            val series = regex.findAll(tvHtml).map { it.groupValues[1].trim() }.take(10).toList()
            val films = regex.findAll(filmsHtml).map { it.groupValues[1].trim() }.take(10).toList()
            if (series.size >= 5 && films.size >= 5) {
                Pair(series, films)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun enrichTop10Titles(titles: List<String>, isSeries: Boolean): List<Movie> = withContext(Dispatchers.IO) {
        titles.mapIndexed { index, rawTitle ->
            val cleanTitle = StreamingPlatformData.cleanTudumTitle(rawTitle)
            val dictEntry = StreamingPlatformData.findTudumEntry(rawTitle)
                ?: StreamingPlatformData.findTudumEntry(cleanTitle)

            // Step 1: Exact TMDB ID from dictionary
            if (dictEntry?.tmdbId != null) {
                try {
                    val mediaType = dictEntry.type.ifBlank { if (isSeries) "series" else "movie" }
                    val isTv = mediaType == "series"
                    val tmdbData = if (isTv) {
                        ApiClient.tmdbApi.getTvDetails(dictEntry.tmdbId)
                    } else {
                        ApiClient.tmdbApi.getMovieDetails(dictEntry.tmdbId)
                    }

                    val poster = ApiClient.formatTmdbPoster(tmdbData.posterPath)
                    val backdrop = ApiClient.formatTmdbBackdrop(tmdbData.backdropPath)
                    val titleVi = dictEntry.vietnameseTitle.ifBlank { tmdbData.title ?: tmdbData.name ?: cleanTitle }
                    val subTitle = dictEntry.originalTitle.ifBlank { tmdbData.originalTitle ?: tmdbData.originalName ?: rawTitle }
                    val overview = if (!tmdbData.overview.isNullOrBlank()) tmdbData.overview else "Đang đứng vị trí #${index + 1} trên bảng xếp hạng ${if (isSeries) "Series" else "Phim lẻ"} thịnh hành tại Việt Nam tuần này."

                    var resolvedSlug = dictEntry.slug
                    if (resolvedSlug.isBlank()) {
                        try {
                            val kkMatch = ApiClient.kkPhimApi.getMovieByTmdb(if (isTv) "tv" else "movie", dictEntry.tmdbId)
                            kkMatch.movie?.slug?.let { resolvedSlug = it }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }

                    return@mapIndexed Movie(
                        id = resolvedSlug.ifBlank { "tmdb-${dictEntry.tmdbId}" },
                        title = titleVi,
                        subTitle = subTitle,
                        slug = resolvedSlug,
                        description = overview,
                        posterUrl = poster.ifBlank { backdrop }.ifBlank { "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop" },
                        backdropUrl = backdrop.ifBlank { poster }.ifBlank { "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?q=80&w=1920&auto=format&fit=crop" },
                        year = dictEntry.year,
                        quality = "4K Ultra HD",
                        type = mediaType,
                        isTop10 = true,
                        top10Rank = index + 1,
                        platform = "netflix",
                        platformName = "Netflix"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Step 2: Search TMDB with multi-field cross-validation & scoring
            val searchTitle = dictEntry?.vietnameseTitle ?: cleanTitle
            try {
                val isTv = isSeries || (dictEntry?.type == "series")
                val searchRes = if (isTv) {
                    ApiClient.tmdbApi.searchTv(searchTitle)
                } else {
                    ApiClient.tmdbApi.searchMovie(searchTitle)
                }

                val candidates = searchRes.results ?: emptyList()
                val targetYear = dictEntry?.year
                val scored = candidates.map { cand ->
                    var s = 0
                    val dateStr = cand.releaseDate ?: cand.firstAirDate ?: ""
                    val cYear = if (dateStr.length >= 4) dateStr.substring(0, 4).toIntOrNull() ?: 0 else 0
                    if (targetYear != null && targetYear > 0 && cYear > 0) {
                        val diff = kotlin.math.abs(targetYear - cYear)
                        if (diff >= 3) return@map Pair(cand, -100) // DISQUALIFIED
                        if (diff == 0) s += 50
                        else if (diff == 1) s += 30
                        else if (diff == 2) s += 10
                    }
                    val candOrig = (cand.originalTitle ?: cand.originalName ?: "").trim().lowercase()
                    val candTitle = (cand.title ?: cand.name ?: "").trim().lowercase()
                    val targetOrig = (dictEntry?.originalTitle ?: rawTitle).trim().lowercase()
                    val cleanNorm = cleanTitle.trim().lowercase()

                    if (targetOrig.isNotBlank() && candOrig.isNotBlank()) {
                        if (candOrig == targetOrig || candOrig.contains(targetOrig) || targetOrig.contains(candOrig)) s += 45
                    }
                    if (candTitle.isNotBlank() && cleanNorm.isNotBlank()) {
                        if (candTitle == cleanNorm || candTitle.contains(cleanNorm) || cleanNorm.contains(candTitle)) s += 40
                    }
                    s += ((cand.popularity ?: 0.0) / 10.0).toInt().coerceAtMost(20)
                    Pair(cand, s)
                }.sortedByDescending { it.second }

                val best = scored.firstOrNull()?.takeIf { it.second >= 10 }?.first
                if (best != null && !best.posterPath.isNullOrBlank()) {
                    val poster = ApiClient.formatTmdbPoster(best.posterPath)
                    val backdrop = ApiClient.formatTmdbBackdrop(best.backdropPath)
                    val titleVi = dictEntry?.vietnameseTitle ?: best.title ?: best.name ?: cleanTitle
                    val subTitle = dictEntry?.originalTitle ?: best.originalTitle ?: best.originalName ?: rawTitle
                    val candId = best.id ?: 0

                    var resolvedSlug = dictEntry?.slug ?: ""
                    if (resolvedSlug.isBlank() && candId > 0) {
                        try {
                            val kkMatch = ApiClient.kkPhimApi.getMovieByTmdb(if (isTv) "tv" else "movie", candId)
                            kkMatch.movie?.slug?.let { resolvedSlug = it }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }

                    return@mapIndexed Movie(
                        id = resolvedSlug.ifBlank { "tmdb-$candId" },
                        title = titleVi,
                        subTitle = subTitle,
                        slug = resolvedSlug,
                        description = best.overview ?: "Đang đứng vị trí #${index + 1} trên bảng xếp hạng ${if (isSeries) "Series" else "Phim lẻ"} thịnh hành tại Việt Nam tuần này.",
                        posterUrl = poster.ifBlank { backdrop }.ifBlank { "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop" },
                        backdropUrl = backdrop.ifBlank { poster }.ifBlank { "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?q=80&w=1920&auto=format&fit=crop" },
                        year = targetYear ?: 2026,
                        quality = "4K Ultra HD",
                        type = if (isSeries) "series" else "movie",
                        isTop10 = true,
                        top10Rank = index + 1,
                        platform = "netflix",
                        platformName = "Netflix"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Step 3: KKPhim search with title matching
            val kkMatch = try {
                val items = api.searchMovies(keyword = cleanTitle, page = 1, limit = 5).data?.items ?: emptyList()
                items.firstOrNull { item ->
                    val itemName = (item.name ?: "").trim().lowercase()
                    val itemOrig = (item.originName ?: "").trim().lowercase()
                    val target = cleanTitle.trim().lowercase()
                    itemName.contains(target) || target.contains(itemName) || itemOrig.contains(target) || target.contains(itemOrig)
                } ?: items.firstOrNull()
            } catch (e: Exception) {
                null
            }

            if (kkMatch != null) {
                val p = ApiClient.formatImageUrl(kkMatch.posterUrl ?: kkMatch.thumbUrl)
                val b = ApiClient.formatImageUrl(kkMatch.thumbUrl ?: kkMatch.posterUrl)
                return@mapIndexed Movie(
                    id = kkMatch.id ?: kkMatch.slug ?: "netflix-${if (isSeries) "series" else "film"}-${index + 1}",
                    title = dictEntry?.vietnameseTitle ?: kkMatch.name ?: cleanTitle,
                    subTitle = dictEntry?.originalTitle ?: kkMatch.originName ?: rawTitle,
                    slug = kkMatch.slug ?: cleanTitle.lowercase().replace(" ", "-"),
                    description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng ${if (isSeries) "Series" else "Phim lẻ"} thịnh hành tại Việt Nam tuần này.",
                    posterUrl = p.ifBlank { b }.ifBlank { "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop" },
                    backdropUrl = b.ifBlank { p }.ifBlank { "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?q=80&w=1920&auto=format&fit=crop" },
                    year = kkMatch.year ?: 2026,
                    quality = "4K Ultra HD",
                    type = if (isSeries) "series" else "movie",
                    isTop10 = true,
                    top10Rank = index + 1,
                    platform = "netflix",
                    platformName = "Netflix"
                )
            }

            // Step 4: Final safe aesthetic cinema placeholder - NEVER Pulp Fiction
            Movie(
                id = "netflix-${if (isSeries) "series" else "film"}-${index + 1}",
                title = dictEntry?.vietnameseTitle ?: cleanTitle,
                subTitle = dictEntry?.originalTitle ?: rawTitle,
                slug = cleanTitle.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
                description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng ${if (isSeries) "Series" else "Phim lẻ"} thịnh hành tại Việt Nam tuần này.",
                posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?q=80&w=1920&auto=format&fit=crop",
                year = 2026,
                quality = "4K Ultra HD",
                type = if (isSeries) "series" else "movie",
                isTop10 = true,
                top10Rank = index + 1,
                platform = "netflix",
                platformName = "Netflix"
            )
        }
    }

    private suspend fun syncLiveTop10IfNeeded() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedSeriesTop10 != null && cachedFilmsTop10 != null && now < nextTuesdayExpiry) {
            return@withContext
        }

        loadTop10FromPrefs()
        if (cachedSeriesTop10 != null && cachedFilmsTop10 != null && now < nextTuesdayExpiry) {
            return@withContext
        }

        // Fetch fresh weekly data from LixfNet Web API or direct Netflix Tudum
        var seriesRaw: List<String> = emptyList()
        var filmsRaw: List<String> = emptyList()
        var weekId = ""

        try {
            val liveData = webApi.getNetflixTop10()
            seriesRaw = liveData.series ?: emptyList()
            filmsRaw = liveData.films ?: emptyList()
            weekId = liveData.week ?: ""
        } catch (e: Exception) {
            // Direct Tudum fallback
            val direct = fetchTudumDirect()
            if (direct != null) {
                seriesRaw = direct.first
                filmsRaw = direct.second
            }
        }

        if (seriesRaw.isEmpty() || filmsRaw.isEmpty()) {
            seriesRaw = StreamingPlatformData.DEFAULT_SERIES_TITLES
            filmsRaw = StreamingPlatformData.DEFAULT_FILMS_TITLES
        }

        val expiry = getNextTuesdayUpdateTimestamp()

        if (seriesRaw.isNotEmpty() && filmsRaw.isNotEmpty()) {
            val enrichedSeries = enrichTop10Titles(seriesRaw, isSeries = true)
            val enrichedFilms = enrichTop10Titles(filmsRaw, isSeries = false)
            saveTop10ToPrefs(enrichedSeries, enrichedFilms, weekId, expiry)
        }
    }

    /**
     * Top 10 Series tại Việt Nam tuần này (Netflix Tudum)
     * Auto-refreshes every Tuesday when Netflix Tudum releases new weekly rankings.
     * Rule ERR-09: Do NOT filter China out of Top 10 Series to preserve official ranks 1..10!
     */
    suspend fun getNetflixSeriesTop10(): List<Movie> = withContext(Dispatchers.IO) {
        try {
            syncLiveTop10IfNeeded()
            if (!cachedSeriesTop10.isNullOrEmpty()) {
                return@withContext cachedSeriesTop10!!
            }

            val defaultSeries = enrichTop10Titles(StreamingPlatformData.DEFAULT_SERIES_TITLES, isSeries = true)
            if (defaultSeries.isNotEmpty()) {
                cachedSeriesTop10 = defaultSeries
                return@withContext defaultSeries
            }

            val tudumList = StreamingPlatformData.NETFLIX_VN_SERIES_TUDUM
            return@withContext tudumList.mapIndexed { index, entry ->
                val p = ApiClient.formatTmdbPoster(entry.posterUrl.ifBlank { entry.backdropUrl })
                val b = ApiClient.formatTmdbBackdrop(entry.backdropUrl.ifBlank { entry.posterUrl })
                Movie(
                    id = entry.slug.ifBlank { "netflix-series-${index + 1}" },
                    title = entry.vietnameseTitle,
                    subTitle = entry.originalTitle,
                    slug = entry.slug,
                    description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng Series thịnh hành tại Việt Nam tuần này.",
                    posterUrl = p.ifBlank { b }.ifBlank { "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop" },
                    backdropUrl = b.ifBlank { p }.ifBlank { "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?q=80&w=1920&auto=format&fit=crop" },
                    year = entry.year,
                    quality = "4K Ultra HD",
                    type = "series",
                    isTop10 = true,
                    top10Rank = index + 1,
                    platform = "netflix",
                    platformName = "Netflix"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackTop10Movies("Series")
        }
    }

    /**
     * Top 10 Phim lẻ tại Việt Nam tuần này (Netflix Tudum)
     * Auto-refreshes every Tuesday when Netflix Tudum releases new weekly rankings.
     */
    suspend fun getNetflixFilmsTop10(): List<Movie> = withContext(Dispatchers.IO) {
        try {
            syncLiveTop10IfNeeded()
            if (!cachedFilmsTop10.isNullOrEmpty()) {
                return@withContext cachedFilmsTop10!!
            }

            val defaultFilms = enrichTop10Titles(StreamingPlatformData.DEFAULT_FILMS_TITLES, isSeries = false)
            if (defaultFilms.isNotEmpty()) {
                cachedFilmsTop10 = defaultFilms
                return@withContext defaultFilms
            }

            val tudumList = StreamingPlatformData.NETFLIX_VN_FILMS_TUDUM
            return@withContext tudumList.mapIndexed { index, entry ->
                val p = ApiClient.formatTmdbPoster(entry.posterUrl.ifBlank { entry.backdropUrl })
                val b = ApiClient.formatTmdbBackdrop(entry.backdropUrl.ifBlank { entry.posterUrl })
                Movie(
                    id = entry.slug.ifBlank { "netflix-film-${index + 1}" },
                    title = entry.vietnameseTitle,
                    subTitle = entry.originalTitle,
                    slug = entry.slug,
                    description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng Phim lẻ thịnh hành tại Việt Nam tuần này.",
                    posterUrl = p.ifBlank { b }.ifBlank { "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop" },
                    backdropUrl = b.ifBlank { p }.ifBlank { "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?q=80&w=1920&auto=format&fit=crop" },
                    year = entry.year,
                    quality = "4K Ultra HD",
                    type = "movie",
                    isTop10 = true,
                    top10Rank = index + 1,
                    platform = "netflix",
                    platformName = "Netflix"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackTop10Movies("Phim lẻ")
        }
    }

    private var cachedAppleTvTop10: List<Movie>? = null

    /**
     * Top 10 Apple TV+ Originals
     */
    suspend fun getAppleTvTop10(): List<Movie> = withContext(Dispatchers.IO) {
        cachedAppleTvTop10?.let { return@withContext it }
        try {
            val originals = StreamingPlatformData.APPLE_TV_ORIGINALS
            val result = originals.mapIndexed { index, entry ->
                var poster = entry.posterUrl
                var backdrop = entry.backdropUrl
                var overview = "Phim bộ nguyên bản Apple TV+ Originals đặc sắc."
                if (entry.tmdbId != null) {
                    try {
                        val tmdb = ApiClient.tmdbApi.getTvDetails(entry.tmdbId)
                        if (poster.isBlank()) poster = ApiClient.formatTmdbPoster(tmdb.posterPath)
                        if (backdrop.isBlank()) backdrop = ApiClient.formatTmdbBackdrop(tmdb.backdropPath)
                        if (!tmdb.overview.isNullOrBlank()) overview = tmdb.overview
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Movie(
                    id = entry.slug.ifBlank { "apple-${index + 1}" },
                    title = entry.vietnameseTitle,
                    subTitle = entry.originalTitle,
                    slug = entry.slug,
                    description = overview,
                    posterUrl = poster.ifBlank { backdrop }.ifBlank { "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop" },
                    backdropUrl = backdrop.ifBlank { poster }.ifBlank { "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?q=80&w=1920&auto=format&fit=crop" },
                    year = entry.year,
                    quality = "4K Ultra HD",
                    type = "series",
                    isTop10 = true,
                    top10Rank = index + 1,
                    platform = "apple",
                    platformName = "Apple TV+"
                )
            }
            if (result.isNotEmpty()) {
                cachedAppleTvTop10 = result
            }
            return@withContext result
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackTop10Movies("Apple TV")
        }
    }

    suspend fun searchMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val res = api.searchMovies(keyword = query.trim(), page = 1, limit = 24)
            res.data?.items?.map { mapToMovie(it) } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun extractEpisodes(servers: List<KKServerEpisode>?, rawTime: String?, rawContent: String?): List<Episode> {
        val episodesList = mutableListOf<Episode>()
        servers?.forEach { server ->
            server.serverData?.forEachIndexed { index, ep ->
                val cleanEpName = cleanEpisodeName(ep.name ?: "Tập ${index + 1}")
                val epId = if (!ep.slug.isNullOrBlank()) ep.slug else "ep-${index + 1}"
                val epSlug = if (!ep.slug.isNullOrBlank()) ep.slug else "tap-${index + 1}"
                episodesList.add(
                    Episode(
                        id = epId,
                        name = cleanEpName,
                        slug = epSlug,
                        filename = ep.filename ?: cleanEpName,
                        linkM3u8 = ep.linkM3u8 ?: "",
                        linkEmbed = ep.linkEmbed ?: "",
                        durationMinutes = parseDurationMinutes(rawTime),
                        synopsis = rawContent
                    )
                )
            }
        }
        return episodesList
    }

    suspend fun getMovieDetail(slug: String): Movie = withContext(Dispatchers.IO) {
        if (slug.isBlank()) return@withContext fallbackHeroMovie()

        // Stage 1: TMDB ID direct lookup (e.g. tmdb-312359 or pure number)
        val tmdbId = if (slug.startsWith("tmdb-", ignoreCase = true)) {
            slug.substring(5).toIntOrNull()
        } else null

        if (tmdbId != null) {
            try {
                val kkTmdb = api.getMovieByTmdb("tv", tmdbId)
                val raw = kkTmdb.movie
                if (raw != null) {
                    val eps = extractEpisodes(kkTmdb.episodes, raw.time, raw.content)
                    return@withContext mapDetailToMovie(raw, eps)
                }
            } catch (e: Exception) {
                try {
                    val kkTmdb = api.getMovieByTmdb("movie", tmdbId)
                    val raw = kkTmdb.movie
                    if (raw != null) {
                        val eps = extractEpisodes(kkTmdb.episodes, raw.time, raw.content)
                        return@withContext mapDetailToMovie(raw, eps)
                    }
                } catch (e2: Exception) {
                    // Ignore
                }
            }
        }

        // Stage 2: Direct lookup via slug
        try {
            val res = api.getMovieDetail(slug)
            val rawMovie = res.movie
            if (rawMovie != null) {
                val eps = extractEpisodes(res.episodes, rawMovie.time, rawMovie.content)
                return@withContext mapDetailToMovie(rawMovie, eps)
            }
        } catch (e: Exception) {
            // Stage 3: Dynamic Fuzzy Match (ERR-13)
            try {
                val searchKeyword = slug.replace("-", " ")
                val searchRes = api.searchMovies(keyword = searchKeyword, page = 1, limit = 5)
                val best = searchRes.data?.items?.firstOrNull()
                if (best?.slug != null && best.slug != slug) {
                    val res = api.getMovieDetail(best.slug)
                    val rawMovie = res.movie
                    if (rawMovie != null) {
                        val eps = extractEpisodes(res.episodes, rawMovie.time, rawMovie.content)
                        return@withContext mapDetailToMovie(rawMovie, eps)
                    }
                }
            } catch (e2: Exception) {
                // Ignore
            }
        }

        // Safe fallback preserving the clicked movie's slug and readable title
        Movie(
            id = slug,
            title = slug.replace("-", " ").replaceFirstChar { it.uppercase() },
            slug = slug,
            description = "Thông tin phim đang được cập nhật trên LixfNet.",
            posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop",
            backdropUrl = "https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?q=80&w=1920&auto=format&fit=crop",
            year = 2026,
            quality = "FHD",
            type = "series"
        )
    }

    // --- Data transformation helpers ---

    private fun mapToMovie(item: KKMovieItem, detail: KKMovieDetail? = null): Movie {
        val thumb = ApiClient.formatImageUrl(detail?.thumbUrl ?: item.thumbUrl)
        val poster = ApiClient.formatImageUrl(detail?.posterUrl ?: item.posterUrl)
        val genres = detail?.category?.mapNotNull { it.name } ?: item.category?.mapNotNull { it.name } ?: emptyList()
        val countries = detail?.country?.mapNotNull { it.name } ?: item.country?.mapNotNull { it.name } ?: emptyList()

        val countrySlug = detail?.country?.firstOrNull()?.slug ?: item.country?.firstOrNull()?.slug ?: ""
        val epCurrent = detail?.episodeCurrent ?: item.episodeCurrent ?: ""
        val isNew = epCurrent.contains("tập", ignoreCase = true) || item.time?.contains("mới", ignoreCase = true) == true
        val ribbon = if (isNew) "TẬP MỚI" else null

        val safePoster = poster.ifBlank { thumb }.ifBlank { "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop" }
        val safeBackdrop = thumb.ifBlank { poster }.ifBlank { safePoster }

        return Movie(
            id = item.id ?: item.slug ?: "",
            title = item.name ?: "Không có tiêu đề",
            subTitle = item.originName ?: "",
            slug = item.slug ?: "",
            description = detail?.content?.replace(Regex("<[^>]*>"), "") ?: "Xem ngay trên LixfNet với chất lượng cao nhất.",
            posterUrl = safePoster,
            backdropUrl = safeBackdrop,
            year = detail?.year ?: item.year ?: 2026,
            rating = "T16",
            quality = item.quality ?: "FHD",
            type = item.type ?: "series",
            durationText = detail?.time ?: item.time ?: "45 phút",
            currentEpisode = epCurrent,
            countrySlug = countrySlug,
            genres = genres,
            countries = countries,
            cast = detail?.actor ?: emptyList(),
            director = detail?.director ?: emptyList(),
            ribbonBadge = ribbon,
            isNewEpisode = isNew
        )
    }

    private fun mapDetailToMovie(detail: KKMovieDetail, episodes: List<Episode>): Movie {
        val thumb = ApiClient.formatImageUrl(detail.thumbUrl)
        val poster = ApiClient.formatImageUrl(detail.posterUrl)
        val genres = detail.category?.mapNotNull { it.name } ?: emptyList()
        val countries = detail.country?.mapNotNull { it.name } ?: emptyList()
        val countrySlug = detail.country?.firstOrNull()?.slug ?: ""

        val epCurrent = detail.episodeCurrent ?: ""
        val isNew = epCurrent.contains("tập", ignoreCase = true) || detail.time?.contains("mới", ignoreCase = true) == true

        val safePoster = poster.ifBlank { thumb }.ifBlank { "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600&auto=format&fit=crop" }
        val safeBackdrop = thumb.ifBlank { poster }.ifBlank { safePoster }

        return Movie(
            id = detail.id ?: detail.slug ?: "",
            title = detail.name ?: "Không có tiêu đề",
            subTitle = detail.originName ?: "",
            slug = detail.slug ?: "",
            description = detail.content?.replace(Regex("<[^>]*>"), "") ?: "Xem ngay trên LixfNet với chất lượng cao nhất.",
            posterUrl = safePoster,
            backdropUrl = safeBackdrop,
            year = detail.year ?: 2026,
            rating = "T16",
            quality = detail.quality ?: "Full HD",
            type = detail.type ?: "series",
            durationText = detail.time ?: "45 phút",
            currentEpisode = epCurrent,
            countrySlug = countrySlug,
            genres = genres,
            countries = countries,
            cast = detail.actor ?: emptyList(),
            director = detail.director ?: emptyList(),
            episodes = episodes,
            ribbonBadge = if (isNew) "TẬP MỚI" else null,
            isNewEpisode = isNew
        )
    }

    private fun cleanEpisodeName(rawName: String): String {
        return rawName.replace(Regex("^Tập\\s*", RegexOption.IGNORE_CASE), "").trim().let {
            if (it.toIntOrNull() != null) "Tập $it" else it
        }
    }

    private fun parseDurationMinutes(rawTime: String?): Int {
        if (rawTime.isNullOrBlank()) return 45
        val match = Regex("(\\d+)\\s*(phút|min)", RegexOption.IGNORE_CASE).find(rawTime)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 45
    }

    // --- Fallbacks for initial offline render ---
    private fun fallbackHeroMovie(): Movie = Movie(
        id = "cuoc-doi-bi-mat-cua-nang-thu-ky",
        title = "Cuộc Đời Bí Mật Của Nàng Thư Ký",
        subTitle = "The Secret Life of My Secretary",
        slug = "cuoc-doi-bi-mat-cua-nang-thu-ky",
        description = "Một giám đốc điều hành mất khả năng nhận diện khuôn mặt sau một tai nạn bí ẩn, và chỉ người thư ký tận tụy mới có thể giúp anh che giấu điểm yếu này.",
        posterUrl = "https://phimimg.com/upload/vod/20240101-1/cuoc-doi-bi-mat-cua-nang-thu-ky.jpg",
        backdropUrl = "https://phimimg.com/upload/vod/20240101-1/cuoc-doi-bi-mat-cua-nang-thu-ky.jpg",
        year = 2026,
        rating = "T16",
        quality = "4K Ultra HD",
        type = "series",
        durationText = "16 tập",
        genres = listOf("Series", "Cảm động", "Lãng mạn", "Hài", "Công sở"),
        isTop10 = true,
        top10Rank = 1
    )

    private fun fallbackMovies(category: String): List<Movie> = (1..6).map { idx ->
        Movie(
            id = "demo-$category-$idx",
            title = "$category $idx",
            slug = "demo-$category-$idx",
            description = "Nội dung phim phong phú chất lượng cao được tuyển chọn trên LixfNet.",
            posterUrl = "",
            backdropUrl = "",
            year = 2026,
            quality = "HD",
            durationText = "45 phút",
            genres = listOf("Kịch tính", "Điện ảnh")
        )
    }

    private fun fallbackTop10Movies(platform: String): List<Movie> = (1..10).map { rank ->
        Movie(
            id = "top10-$platform-$rank",
            title = "Top $rank $platform",
            slug = "top10-$platform-$rank",
            description = "Bộ phim đạt lượt xem cao nhất trên bảng xếp hạng hôm nay.",
            posterUrl = "",
            backdropUrl = "",
            year = 2026,
            isTop10 = true,
            top10Rank = rank,
            platform = "netflix"
        )
    }
}
