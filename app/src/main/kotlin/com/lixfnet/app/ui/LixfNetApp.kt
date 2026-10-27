package com.lixfnet.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lixfnet.app.data.firebase.FirebaseManager
import com.lixfnet.app.data.model.Episode
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.data.model.WatchProgress
import kotlinx.coroutines.launch
import com.lixfnet.app.data.repository.AuthRepository
import com.lixfnet.app.data.repository.MovieRepository
import com.lixfnet.app.data.repository.WatchProgressRepository
import com.lixfnet.app.ui.components.FloatingPillDock
import com.lixfnet.app.ui.components.NavTab
import com.lixfnet.app.ui.screens.*
import com.lixfnet.app.ui.theme.NetflixBackground

@Composable
fun LixfNetApp() {
    val context = LocalContext.current
    val firebaseManager = remember { FirebaseManager(context) }
    val authRepository = remember { AuthRepository(context, firebaseManager) }
    val movieRepository = remember { MovieRepository() }
    val watchProgressRepository = remember {
        WatchProgressRepository(context, firebaseManager, authRepository)
    }

    val currentUser by authRepository.currentUser.collectAsState()

    // Navigation State
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var activeMovie by remember { mutableStateOf<Movie?>(null) }
    var playingMovie by remember { mutableStateOf<Movie?>(null) }
    var playingEpisode by remember { mutableStateOf<Episode?>(null) }
    var initialResumePositionSec by remember { mutableLongStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    fun playMovie(movie: Movie, forcedEpisode: Episode? = null) {
        coroutineScope.launch {
            val fullMovie = if (movie.episodes.isEmpty()) {
                try {
                    movieRepository.getMovieDetail(movie.slug)
                } catch (e: Exception) {
                    movie
                }
            } else {
                movie
            }

            val progress = watchProgressRepository.getProgressForMovie(fullMovie.slug)
                ?: watchProgressRepository.getProgressForMovie(fullMovie.id)

            val targetEpisode = if (forcedEpisode != null) {
                forcedEpisode
            } else if (progress != null && fullMovie.episodes.isNotEmpty()) {
                val digitsProgress = progress.episodeTitle.filter { it.isDigit() }
                fullMovie.episodes.find { ep ->
                    ep.name.equals(progress.episodeTitle, ignoreCase = true) ||
                    (progress.episodeM3u8.isNotBlank() && ep.linkM3u8 == progress.episodeM3u8) ||
                    (digitsProgress.isNotBlank() && ep.name.filter { it.isDigit() } == digitsProgress)
                } ?: fullMovie.episodes.first()
            } else {
                fullMovie.episodes.firstOrNull() ?: Episode("ep-1", "Tập 1", "tap-1")
            }

            val resumePos = if (forcedEpisode != null) {
                val digitsProgress = progress?.episodeTitle?.filter { it.isDigit() } ?: ""
                val digitsForced = forcedEpisode.name.filter { it.isDigit() }
                if (progress != null && (
                    forcedEpisode.name.equals(progress.episodeTitle, ignoreCase = true) ||
                    forcedEpisode.linkM3u8 == progress.episodeM3u8 ||
                    (digitsForced.isNotBlank() && digitsForced == digitsProgress)
                )) {
                    progress.currentSeconds
                } else 0L
            } else {
                progress?.currentSeconds ?: 0L
            }

            initialResumePositionSec = resumePos
            playingMovie = fullMovie
            playingEpisode = targetEpisode
        }
    }

    // If user is not logged in, show LoginScreen
    if (currentUser == null) {
        LoginScreen(
            authRepository = authRepository,
            onLoginSuccess = {
                watchProgressRepository.syncWithFirebase()
            }
        )
        return
    }

    // Back button handling
    BackHandler(enabled = playingMovie != null) {
        playingMovie = null
        playingEpisode = null
        initialResumePositionSec = 0L
    }

    BackHandler(enabled = activeMovie != null && playingMovie == null) {
        activeMovie = null
    }

    BackHandler(enabled = currentTab != NavTab.HOME && activeMovie == null && playingMovie == null) {
        currentTab = NavTab.HOME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBackground)
    ) {
        // Main Screen Switcher based on currentTab
        AnimatedContent(
            targetState = currentTab,
            label = "TabSwitcher"
        ) { tab ->
            when (tab) {
                NavTab.HOME -> HomeScreen(
                    movieRepository = movieRepository,
                    watchRepo = watchProgressRepository,
                    onMovieClick = { movie -> activeMovie = movie },
                    onPlayMovie = { movie -> playMovie(movie) }
                )
                NavTab.CLIP -> ClipScreen(
                    movieRepository = movieRepository,
                    watchRepo = watchProgressRepository,
                    onPlayMovie = { movie -> playMovie(movie) }
                )
                NavTab.SEARCH -> SearchScreen(
                    movieRepository = movieRepository,
                    onMovieClick = { movie -> activeMovie = movie }
                )
                NavTab.MY_NETFLIX -> MyNetflixScreen(
                    watchRepo = watchProgressRepository,
                    authRepository = authRepository,
                    onMovieClick = { movie -> activeMovie = movie }
                )
            }
        }

        // Movie Detail Screen Layer
        activeMovie?.let { movie ->
            MovieDetailScreen(
                movie = movie,
                movieRepository = movieRepository,
                watchRepo = watchProgressRepository,
                onBack = { activeMovie = null },
                onPlayEpisode = { m, ep ->
                    playMovie(m, ep)
                }
            )
        }

        // Fullscreen Cinema Player Layer
        if (playingMovie != null && playingEpisode != null) {
            CinemaPlayerScreen(
                movie = playingMovie!!,
                episode = playingEpisode!!,
                initialPositionSec = initialResumePositionSec,
                watchRepo = watchProgressRepository,
                onBack = {
                    playingMovie = null
                    playingEpisode = null
                    initialResumePositionSec = 0L
                },
                onSelectEpisode = { nextEp ->
                    initialResumePositionSec = 0L
                    playingEpisode = nextEp
                }
            )
        }

        // Floating Pill Navigation Dock (only visible on main screens)
        if (playingMovie == null) {
            FloatingPillDock(
                currentTab = currentTab,
                onTabSelected = { newTab ->
                    activeMovie = null
                    currentTab = newTab
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
