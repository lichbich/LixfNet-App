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

    private fun loadTop10FromPrefs() {
        if (prefs == null) return
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

            if (dictEntry != null) {
                Movie(
                    id = dictEntry.slug.ifBlank { "netflix-${if (isSeries) "series" else "film"}-${index + 1}" },
                    title = dictEntry.vietnameseTitle,
                    subTitle = dictEntry.originalTitle,
                    slug = dictEntry.slug,
                    description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng ${if (isSeries) "Series" else "Phim lẻ"} thịnh hành tại Việt Nam tuần này.",
                    posterUrl = dictEntry.posterUrl,
                    backdropUrl = dictEntry.posterUrl,
                    year = dictEntry.year,
                    quality = "4K Ultra HD",
                    type = if (isSeries) "series" else "movie",
                    isTop10 = true,
                    top10Rank = index + 1,
                    platform = "netflix",
                    platformName = "Netflix"
                )
            } else {
                // Dynamic KKPhim search fallback
                val searchRes = try {
                    api.searchMovies(keyword = cleanTitle, page = 1).data?.items?.firstOrNull()
                } catch (e: Exception) {
                    null
                }

                if (searchRes != null) {
                    Movie(
                        id = searchRes.id ?: searchRes.slug ?: "netflix-${if (isSeries) "series" else "film"}-${index + 1}",
                        title = searchRes.name ?: cleanTitle,
                        subTitle = searchRes.originName ?: rawTitle,
                        slug = searchRes.slug ?: cleanTitle.lowercase().replace(" ", "-"),
                        description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng ${if (isSeries) "Series" else "Phim lẻ"} thịnh hành tại Việt Nam tuần này.",
                        posterUrl = ApiClient.formatImageUrl(searchRes.posterUrl ?: searchRes.thumbUrl),
                        backdropUrl = ApiClient.formatImageUrl(searchRes.thumbUrl ?: searchRes.posterUrl),
                        year = searchRes.year ?: 2026,
                        quality = "4K Ultra HD",
                        type = if (isSeries) "series" else "movie",
                        isTop10 = true,
                        top10Rank = index + 1,
                        platform = "netflix",
                        platformName = "Netflix"
                    )
                } else {
                    Movie(
                        id = "netflix-${if (isSeries) "series" else "film"}-${index + 1}",
                        title = cleanTitle,
                        subTitle = rawTitle,
                        slug = cleanTitle.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
                        description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng ${if (isSeries) "Series" else "Phim lẻ"} thịnh hành tại Việt Nam tuần này.",
                        posterUrl = "https://image.tmdb.org/t/p/w500/jzqPdwpiDTPzvnbV0pZzTtOIgWz.jpg",
                        backdropUrl = "https://image.tmdb.org/t/p/w500/jzqPdwpiDTPzvnbV0pZzTtOIgWz.jpg",
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

            val tudumList = StreamingPlatformData.NETFLIX_VN_SERIES_TUDUM
            return@withContext tudumList.mapIndexed { index, entry ->
                Movie(
                    id = entry.slug.ifBlank { "netflix-series-${index + 1}" },
                    title = entry.vietnameseTitle,
                    subTitle = entry.originalTitle,
                    slug = entry.slug,
                    description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng Series thịnh hành tại Việt Nam tuần này.",
                    posterUrl = entry.posterUrl,
                    backdropUrl = entry.posterUrl,
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

            val tudumList = StreamingPlatformData.NETFLIX_VN_FILMS_TUDUM
            return@withContext tudumList.mapIndexed { index, entry ->
                Movie(
                    id = entry.slug.ifBlank { "netflix-film-${index + 1}" },
                    title = entry.vietnameseTitle,
                    subTitle = entry.originalTitle,
                    slug = entry.slug,
                    description = "Đang đứng vị trí #${index + 1} trên bảng xếp hạng Phim lẻ thịnh hành tại Việt Nam tuần này.",
                    posterUrl = entry.posterUrl,
                    backdropUrl = entry.posterUrl,
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

    /**
     * Top 10 Apple TV+ Originals
     */
    suspend fun getAppleTvTop10(): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val originals = StreamingPlatformData.APPLE_TV_ORIGINALS
            return@withContext originals.mapIndexed { index, entry ->
                Movie(
                    id = entry.slug.ifBlank { "apple-${index + 1}" },
                    title = entry.vietnameseTitle,
                    subTitle = entry.originalTitle,
                    slug = entry.slug,
                    description = "Phim bộ nguyên bản Apple TV+ Originals đặc sắc.",
                    posterUrl = entry.posterUrl,
                    backdropUrl = entry.posterUrl,
                    year = entry.year,
                    quality = "4K Ultra HD",
                    type = "series",
                    isTop10 = true,
                    top10Rank = index + 1,
                    platform = "apple",
                    platformName = "Apple TV+"
                )
            }
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

    suspend fun getMovieDetail(slug: String): Movie = withContext(Dispatchers.IO) {
        try {
            val res = api.getMovieDetail(slug)
            val rawMovie = res.movie ?: throw IllegalStateException("Movie detail null")
            val episodesList = mutableListOf<Episode>()

            // Flatten and clean episode data
            res.episodes?.forEach { server ->
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
                            durationMinutes = parseDurationMinutes(rawMovie.time),
                            synopsis = rawMovie.content
                        )
                    )
                }
            }

            return@withContext mapDetailToMovie(rawMovie, episodesList)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackHeroMovie()
        }
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

        return Movie(
            id = item.id ?: item.slug ?: "",
            title = item.name ?: "Không có tiêu đề",
            subTitle = item.originName ?: "",
            slug = item.slug ?: "",
            description = detail?.content?.replace(Regex("<[^>]*>"), "") ?: "Xem ngay trên LixfNet với chất lượng cao nhất.",
            posterUrl = poster.ifBlank { thumb },
            backdropUrl = thumb.ifBlank { poster },
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

        return Movie(
            id = detail.id ?: detail.slug ?: "",
            title = detail.name ?: "Không có tiêu đề",
            subTitle = detail.originName ?: "",
            slug = detail.slug ?: "",
            description = detail.content?.replace(Regex("<[^>]*>"), "") ?: "Xem ngay trên LixfNet với chất lượng cao nhất.",
            posterUrl = poster.ifBlank { thumb },
            backdropUrl = thumb.ifBlank { poster },
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
