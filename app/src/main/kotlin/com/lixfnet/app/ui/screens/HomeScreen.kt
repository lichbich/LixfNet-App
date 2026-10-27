package com.lixfnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.data.recommendation.CandidateRow
import com.lixfnet.app.data.recommendation.RecommendationEngine
import com.lixfnet.app.data.repository.MovieRepository
import com.lixfnet.app.data.repository.WatchProgressRepository
import com.lixfnet.app.ui.components.*
import com.lixfnet.app.ui.theme.NetflixBackground
import com.lixfnet.app.ui.theme.NetflixRed
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    movieRepository: MovieRepository,
    watchRepo: WatchProgressRepository,
    onMovieClick: (Movie) -> Unit,
    onPlayMovie: (Movie) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var heroMovie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Tất cả") }

    // Content Pools
    var newUpdates by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var koreanList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var actionList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var vietnamList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var usList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var thaiList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var japanList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var horrorList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var animeList by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var tvShowsList by remember { mutableStateOf<List<Movie>>(emptyList()) }

    // Platform Tops Pools
    var netflixSeriesTop10 by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var netflixFilmsTop10 by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var appleTvTop10 by remember { mutableStateOf<List<Movie>>(emptyList()) }

    // State collections
    val continueWatching by watchRepo.continueWatching.collectAsState()
    val myList by watchRepo.myList.collectAsState()

    // Stage 1: Instant Fast Render (~300ms)
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // 1. Fetch Chinese movie slugs set for filtering
                val chinaSlugs = movieRepository.getChineseMovieSlugs()

                // 2. Fetch native pools in parallel
                val dKorea = async { movieRepository.getKoreanMovies() }
                val dUpdates = async { movieRepository.getNewUpdatedMovies(1) }
                val dAction = async { movieRepository.getActionMovies() }
                val dVietnam = async { movieRepository.getVietnameseMovies() }
                val dUs = async { movieRepository.getUsMovies() }
                val dThai = async { movieRepository.getThaiMovies() }
                val dJapan = async { movieRepository.getJapanMovies() }
                val dHorror = async { movieRepository.getHorrorMovies() }
                val dAnime = async { movieRepository.getAnimeMovies() }
                val dShows = async { movieRepository.getTvShows() }

                // Apply China filtering to standard categories
                koreanList = movieRepository.filterNoChina(dKorea.await(), chinaSlugs)
                newUpdates = movieRepository.filterNoChina(dUpdates.await(), chinaSlugs)
                actionList = movieRepository.filterNoChina(dAction.await(), chinaSlugs)
                vietnamList = movieRepository.filterNoChina(dVietnam.await(), chinaSlugs)
                usList = movieRepository.filterNoChina(dUs.await(), chinaSlugs)
                thaiList = movieRepository.filterNoChina(dThai.await(), chinaSlugs)
                japanList = movieRepository.filterNoChina(dJapan.await(), chinaSlugs)
                horrorList = movieRepository.filterNoChina(dHorror.await(), chinaSlugs)
                animeList = movieRepository.filterNoChina(dAnime.await(), chinaSlugs)
                tvShowsList = movieRepository.filterNoChina(dShows.await(), chinaSlugs)

                // Pick an impressive Hero Movie from Korean, Action or Latest
                val heroCandidates = (koreanList.take(3) + actionList.take(3) + newUpdates.take(3)).filter { it.backdropUrl.isNotBlank() }
                heroMovie = heroCandidates.randomOrNull() ?: movieRepository.getHeroMovie()

                // Immediately release loading indicator
                isLoading = false

                // Stage 2: Background Enrichment for Streaming Tops
                launch {
                    try {
                        netflixSeriesTop10 = movieRepository.getNetflixSeriesTop10()
                    } catch (e: Exception) { e.printStackTrace() }
                }
                launch {
                    try {
                        netflixFilmsTop10 = movieRepository.getNetflixFilmsTop10()
                    } catch (e: Exception) { e.printStackTrace() }
                }
                launch {
                    try {
                        appleTvTop10 = movieRepository.getAppleTvTop10()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    val allMoviesPool = remember(koreanList, newUpdates, actionList, horrorList, vietnamList, usList, animeList, tvShowsList) {
        (koreanList + newUpdates + actionList + horrorList + vietnamList + usList + animeList + tvShowsList)
            .distinctBy { it.slug.orEmpty().ifBlank { it.id.orEmpty() } }
    }

    val tasteProfile = remember(continueWatching) {
        RecommendationEngine.buildTasteProfile(continueWatching)
    }

    val personalizedPicks = remember(tasteProfile, allMoviesPool, continueWatching) {
        RecommendationEngine.curatePersonalizedRecommendations(
            allPoolMovies = allMoviesPool,
            continueWatchingMovies = continueWatching,
            profile = tasteProfile,
            limit = 12
        )
    }

    // Dynamic Ranked Rows Assembly
    val displayRows = remember(
        selectedCategory,
        netflixSeriesTop10,
        netflixFilmsTop10,
        appleTvTop10,
        koreanList,
        newUpdates,
        actionList,
        horrorList,
        vietnamList,
        usList,
        thaiList,
        japanList,
        animeList,
        tvShowsList,
        personalizedPicks,
        tasteProfile
    ) {
        val candidates = mutableListOf<CandidateRow>()

        when (selectedCategory) {
            "Series" -> {
                if (netflixSeriesTop10.isNotEmpty()) {
                    candidates.add(CandidateRow("top10-series", "Top 10 series tại Việt Nam tuần này", type = RowType.TOP10, movies = netflixSeriesTop10, priorityBoost = 10000))
                }
                if (appleTvTop10.isNotEmpty()) {
                    candidates.add(CandidateRow("apple-tv", "Top 10 Apple TV+ Originals", type = RowType.TOP10, movies = appleTvTop10, priorityBoost = 9000))
                }
                if (koreanList.isNotEmpty()) {
                    candidates.add(CandidateRow("korea", "Điện ảnh & Series Hàn Quốc được yêu thích", countrySlug = "han-quoc", movies = koreanList, priorityBoost = 8000))
                }
                if (tvShowsList.isNotEmpty()) {
                    candidates.add(CandidateRow("tv-shows", "Chương trình truyền hình & TV Shows", movies = tvShowsList, priorityBoost = 7000))
                }
                if (animeList.isNotEmpty()) {
                    candidates.add(CandidateRow("anime", "Thế giới Hoạt hình & Anime đỉnh cao", genreSlug = "hoat-hinh", movies = animeList, priorityBoost = 6000))
                }
            }
            "Phim" -> {
                if (netflixFilmsTop10.isNotEmpty()) {
                    candidates.add(CandidateRow("top10-films", "Top 10 phim lẻ tại Việt Nam tuần này", type = RowType.TOP10, movies = netflixFilmsTop10, priorityBoost = 10000))
                }
                if (actionList.isNotEmpty()) {
                    candidates.add(CandidateRow("action", "Phim hành động & Phiêu lưu kịch tính", genreSlug = "hanh-dong", movies = actionList, priorityBoost = 8500))
                }
                if (vietnamList.isNotEmpty()) {
                    candidates.add(CandidateRow("vietnam", "Phim điện ảnh Việt Nam mới nhất", countrySlug = "viet-nam", movies = vietnamList, priorityBoost = 8000))
                }
                if (usList.isNotEmpty()) {
                    candidates.add(CandidateRow("us", "Phim điện ảnh Âu Mỹ bom tấn", countrySlug = "my", movies = usList, priorityBoost = 7500))
                }
                if (horrorList.isNotEmpty()) {
                    candidates.add(CandidateRow("horror", "Phim kinh dị & Giật gân rùng rợn", genreSlug = "kinh-di", movies = horrorList, priorityBoost = 7000))
                }
            }
            "Halloween" -> {
                if (horrorList.isNotEmpty()) {
                    candidates.add(CandidateRow("horror-top", "Phim kinh dị rùng rợn mùa Halloween", genreSlug = "kinh-di", movies = horrorList, priorityBoost = 10000))
                }
                if (actionList.isNotEmpty()) {
                    candidates.add(CandidateRow("thriller", "Phim giật gân & Hồi hộp kịch tính", genreSlug = "hanh-dong", movies = actionList, priorityBoost = 8000))
                }
            }
            "Mới & Hot" -> {
                if (newUpdates.isNotEmpty()) {
                    candidates.add(CandidateRow("new-hot", "Phim mới cập nhật hôm nay", movies = newUpdates, priorityBoost = 10000))
                }
                if (netflixSeriesTop10.isNotEmpty()) {
                    candidates.add(CandidateRow("top10-series", "Top 10 series tại Việt Nam tuần này", type = RowType.TOP10, movies = netflixSeriesTop10, priorityBoost = 9000))
                }
                if (netflixFilmsTop10.isNotEmpty()) {
                    candidates.add(CandidateRow("top10-films", "Top 10 phim lẻ tại Việt Nam tuần này", type = RowType.TOP10, movies = netflixFilmsTop10, priorityBoost = 8500))
                }
            }
            else -> {
                // "Tất cả" - Full Homepage layout matching Lixfnet Web

                // 1. Korean Cinema & Series
                if (koreanList.isNotEmpty()) {
                    candidates.add(CandidateRow("korea", "Điện ảnh & Series Hàn Quốc được yêu thích", countrySlug = "han-quoc", movies = koreanList, priorityBoost = 10500))
                }

                // 2. Top 10 Series Netflix VN (Thẻ đứng 2:3 với số 3D)
                if (netflixSeriesTop10.isNotEmpty()) {
                    candidates.add(CandidateRow("top10-series", "Top 10 series tại Việt Nam tuần này", type = RowType.TOP10, movies = netflixSeriesTop10, priorityBoost = 10000))
                }

                // 3. Top 10 Films Netflix VN (Thẻ đứng 2:3 với số 3D)
                if (netflixFilmsTop10.isNotEmpty()) {
                    candidates.add(CandidateRow("top10-films", "Top 10 phim lẻ tại Việt Nam tuần này", type = RowType.TOP10, movies = netflixFilmsTop10, priorityBoost = 9800))
                }

                // 4. Phim mới cập nhật hôm nay
                if (newUpdates.isNotEmpty()) {
                    candidates.add(CandidateRow("new-updated", "Phim mới cập nhật hôm nay", movies = newUpdates, priorityBoost = 9500))
                }

                // 5. Top 10 Apple TV+ Originals
                if (appleTvTop10.isNotEmpty()) {
                    candidates.add(CandidateRow("apple-tv", "Top 10 Apple TV+ Originals", type = RowType.TOP10, movies = appleTvTop10, priorityBoost = 9000))
                }

                // 6. Gợi ý theo gu của bạn (Personalized Picks)
                if (personalizedPicks.isNotEmpty()) {
                    val genreLabels = tasteProfile.favoriteGenreSlugs.mapNotNull { slug ->
                        when (slug) {
                            "kinh-di" -> "Kinh dị"
                            "hanh-dong" -> "Hành động"
                            "tinh-cam" -> "Tình cảm"
                            "phim-hai" -> "Hài hước"
                            "hoat-hinh" -> "Hoạt hình"
                            else -> null
                        }
                    }.joinToString(", ")

                    val title = if (genreLabels.isNotBlank()) "Gợi ý theo gu của bạn: Phim $genreLabels" else "Gợi ý dành riêng cho bạn"
                    candidates.add(CandidateRow("personalized", title, movies = personalizedPicks, priorityBoost = 8800))
                }

                // 7. Phim hành động & Phiêu lưu kịch tính
                if (actionList.isNotEmpty()) {
                    candidates.add(CandidateRow("action", "Phim hành động & Phiêu lưu kịch tính", genreSlug = "hanh-dong", movies = actionList, priorityBoost = 8500))
                }

                // 8. Phim điện ảnh Việt Nam mới nhất
                if (vietnamList.isNotEmpty()) {
                    candidates.add(CandidateRow("vietnam", "Phim điện ảnh Việt Nam mới nhất", countrySlug = "viet-nam", movies = vietnamList, priorityBoost = 5000))
                }

                // 9. Phim & Anime Nhật Bản đặc sắc
                if (japanList.isNotEmpty()) {
                    candidates.add(CandidateRow("japan", "Phim & Anime Nhật Bản đặc sắc", countrySlug = "nhat-ban", movies = japanList, priorityBoost = 4500))
                }

                // 10. Phim điện ảnh Âu Mỹ bom tấn
                if (usList.isNotEmpty()) {
                    candidates.add(CandidateRow("us", "Phim điện ảnh Âu Mỹ bom tấn", countrySlug = "my", movies = usList, priorityBoost = 4000))
                }

                // 11. Phim kinh dị & Giật gân rùng rợn
                if (horrorList.isNotEmpty()) {
                    candidates.add(CandidateRow("horror", "Phim kinh dị & Giật gân rùng rợn", genreSlug = "kinh-di", movies = horrorList, priorityBoost = 3500))
                }

                // 12. Điện ảnh & Phim truyền hình Thái Lan
                if (thaiList.isNotEmpty()) {
                    candidates.add(CandidateRow("thai", "Điện ảnh & Phim truyền hình Thái Lan", countrySlug = "thai-lan", movies = thaiList, priorityBoost = 3000))
                }

                // 13. Hoạt hình & Anime đỉnh cao
                if (animeList.isNotEmpty()) {
                    candidates.add(CandidateRow("anime", "Thế giới Hoạt hình & Anime đỉnh cao", genreSlug = "hoat-hinh", movies = animeList, priorityBoost = 2500))
                }

                // 14. TV Shows
                if (tvShowsList.isNotEmpty()) {
                    candidates.add(CandidateRow("shows", "Chương trình truyền hình & TV Shows", movies = tvShowsList, priorityBoost = 2000))
                }
            }
        }

        // Apply ranking algorithm
        RecommendationEngine.rankHomepageRows(candidates, tasteProfile)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBackground)
    ) {
        if (isLoading && heroMovie == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NetflixRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // 1. Top Header Bar (N Logo, Title, Download, Notifications)
                item {
                    TopHeaderBar(title = "Trang chủ")
                }

                // 2. Category Filter Chips (Tất cả, Halloween, Series, Phim, Mới & Hot)
                item {
                    CategoryChips(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            selectedCategory = category
                        }
                    )
                }

                // 3. Hero Featured Card
                item {
                    heroMovie?.let { hero ->
                        val inMyList = myList.any { it.id == hero.id || it.slug == hero.slug }
                        HeroFeaturedCard(
                            movie = hero,
                            isInMyList = inMyList,
                            onCardClick = { onMovieClick(hero) },
                            onPlayClick = { onPlayMovie(hero) },
                            onMyListClick = { watchRepo.toggleMyList(hero) }
                        )
                    }
                }

                // 4. Continue Watching Row (Tiếp tục xem - Personalized from Firebase)
                if (continueWatching.isNotEmpty()) {
                    item {
                        MovieRow(
                            title = "Tiếp tục xem",
                            movies = continueWatching,
                            rowType = RowType.CONTINUE_WATCHING,
                            onMovieClick = onPlayMovie
                        )
                    }
                }

                // 5. Dynamic Filtered & Ranked Rows
                items(displayRows, key = { it.id }) { row ->
                    MovieRow(
                        title = row.title,
                        movies = row.movies,
                        rowType = row.type,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }
    }
}
