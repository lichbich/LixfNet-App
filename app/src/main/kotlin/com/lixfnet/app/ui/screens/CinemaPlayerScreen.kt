package com.lixfnet.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lixfnet.app.data.model.Episode
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.data.repository.WatchProgressRepository
import com.lixfnet.app.ui.theme.NetflixRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun CinemaPlayerScreen(
    movie: Movie,
    episode: Episode,
    initialPositionSec: Long = 0L,
    watchRepo: WatchProgressRepository,
    onBack: () -> Unit,
    onSelectEpisode: (Episode) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current

    // Episode & Player State
    var currentEpisode by remember(episode) { mutableStateOf(episode) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var showControls by remember { mutableStateOf(true) }
    var seekNotice by remember { mutableStateOf<String?>(null) }
    var resumeNotice by remember { mutableStateOf<String?>(null) }
    var showNextEpisodeCard by remember { mutableStateOf(false) }

    // Screen Lock & Features State
    var isScreenLocked by remember { mutableStateOf(false) }
    var showEpisodesSheet by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Next & Previous Episode calculation based on currentEpisode
    val currentIndex = movie.episodes.indexOfFirst {
        (it.linkM3u8.isNotBlank() && it.linkM3u8 == currentEpisode.linkM3u8) ||
        (it.name.isNotBlank() && it.name.equals(currentEpisode.name, ignoreCase = true)) ||
        (it.slug.isNotBlank() && it.slug == currentEpisode.slug) ||
        (it.id.isNotBlank() && it.id == currentEpisode.id)
    }
    val previousEpisode = if (currentIndex > 0) movie.episodes[currentIndex - 1] else null
    val nextEpisode = if (currentIndex in 0 until movie.episodes.size - 1) movie.episodes[currentIndex + 1] else null

    // Single persistent ExoPlayer instance for the entire cinema session
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Attach ExoPlayer Listener & Release on leave
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val d = exoPlayer.duration
                    if (d > 0) duration = d
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Sync Playback Speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Load / Switch Episode Media in the single ExoPlayer instance
    LaunchedEffect(currentEpisode) {
        val videoUrl = if (currentEpisode.linkM3u8.isNotBlank()) {
            currentEpisode.linkM3u8
        } else {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        }

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))

        val targetSeekMs = if (currentEpisode == episode && initialPositionSec > 0L) {
            initialPositionSec * 1000L
        } else 0L

        if (targetSeekMs > 0L) {
            exoPlayer.seekTo(targetSeekMs)
            resumeNotice = "Đang xem tiếp: ${formatTime(targetSeekMs)} (${currentEpisode.name})"
        } else {
            exoPlayer.seekTo(0L)
            resumeNotice = "Đang phát: ${currentEpisode.name}"
        }

        exoPlayer.prepare()
        exoPlayer.play()

        delay(3000)
        resumeNotice = null
    }

    // Auto-hide controls timer (5s)
    LaunchedEffect(showControls, isPlaying, isScreenLocked, showEpisodesSheet) {
        if (showControls && isPlaying && !isScreenLocked && !showEpisodesSheet) {
            delay(5000)
            showControls = false
        }
    }

    // Periodic time & watch progress update loop (every 1 second)
    LaunchedEffect(exoPlayer, currentEpisode) {
        while (isActive) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            val d = exoPlayer.duration
            if (d > 0) duration = d

            // Trigger Next Episode Card in last 90 seconds
            val secondsRemaining = (duration - currentPosition) / 1000
            if (nextEpisode != null && secondsRemaining in 1..90 && duration > 120_000) {
                showNextEpisodeCard = true
            }

            // Save watch progress to local cache and Firebase RTDB
            if (currentPosition > 0) {
                watchRepo.saveProgress(
                    movie = movie,
                    episodeTitle = currentEpisode.name,
                    currentSec = currentPosition / 1000,
                    totalSec = duration / 1000,
                    episodeM3u8 = currentEpisode.linkM3u8
                )
            }

            delay(1000)
        }
    }

    // Lock to Landscape on enter, keep screen awake, and restore on leave
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive Fullscreen (hide system bars)
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Screen Layout & Gesture Area
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isScreenLocked, showEpisodesSheet) {
                if (!showEpisodesSheet) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            if (!isScreenLocked) {
                                val screenWidth = size.width
                                if (offset.x < screenWidth / 2) {
                                    // Left Double Tap: Tua lùi 10s
                                    val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                    exoPlayer.seekTo(newPos)
                                    seekNotice = "<< 10s"
                                } else {
                                    // Right Double Tap: Tua tới 10s
                                    val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                    exoPlayer.seekTo(newPos)
                                    seekNotice = "10s >>"
                                }
                            }
                        }
                    )
                }
            }
    ) {
        // 1. Native Media3 Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Double Tap Seek Feedback Indicator
        LaunchedEffect(seekNotice) {
            if (seekNotice != null) {
                delay(800)
                seekNotice = null
            }
        }

        seekNotice?.let { notice ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0xAA000000))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = notice,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Resume Playback Toast Banner (Top Center)
        AnimatedVisibility(
            visible = resumeNotice != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            resumeNotice?.let { text ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xDD1E1E1E))
                        .border(1.dp, NetflixRed, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NetflixRed)
                        )
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4. Next Episode Floating Card (Last 90 seconds)
        AnimatedVisibility(
            visible = showNextEpisodeCard && nextEpisode != null && !isScreenLocked,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = if (showControls) 110.dp else 24.dp)
        ) {
            nextEpisode?.let { next ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xE61E1E1E))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Tập tiếp theo",
                                color = Color(0xFFA3A3A3),
                                fontSize = 11.sp
                            )
                            Text(
                                text = next.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                currentEpisode = next
                                onSelectEpisode(next)
                                showNextEpisodeCard = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Phát ngay",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Screen Locked Overlay (Chỉ hiện nút mở khóa khi chạm)
        if (isScreenLocked) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Button(
                    onClick = {
                        isScreenLocked = false
                        showControls = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xCC000000),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Mở khóa",
                            tint = NetflixRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Màn hình đã khóa • Chạm để mở",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 6. Player Full Controls HUD Overlay (Khi không bị khóa)
        if (!isScreenLocked) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xDD000000),
                                    Color.Transparent,
                                    Color(0xEE000000)
                                )
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControls = false
                        }
                ) {
                    // --- TOP BAR ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Thoát",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentEpisode.name,
                                    color = Color(0xFFA3A3A3),
                                    fontSize = 12.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(2.dp))
                                        .background(Color(0x22FFFFFF), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = movie.quality.ifBlank { "HD" },
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Top Action: Playback Speed Toggle
                        TextButton(
                            onClick = {
                                val nextSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.75f
                                    else -> 1.0f
                                }
                                playbackSpeed = nextSpeed
                                exoPlayer.setPlaybackSpeed(nextSpeed)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x33FFFFFF))
                                .height(32.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Top Action: Lock Screen
                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                showControls = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LockOpen,
                                contentDescription = "Khóa màn hình",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // --- CENTER QUICK CONTROLS (Tua -10s | Play/Pause | Tua +10s) ---
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(40.dp)
                    ) {
                        // Nút Tua lùi 10 giây
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(newPos)
                                seekNotice = "<< 10s"
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Tua lại 10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Nút Play / Pause tròn lớn trung tâm
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .border(1.5.dp, Color(0x88FFFFFF), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        // Nút Tua tới 10 giây
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                exoPlayer.seekTo(newPos)
                                seekNotice = "10s >>"
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = "Tua tới 10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // --- BOTTOM BAR (Scrubber & Actions: Tập tiếp, Tập phim, Tua 30s) ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        // Scrubber Slider
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { fraction ->
                                val seekTo = (fraction * duration).toLong()
                                exoPlayer.seekTo(seekTo)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NetflixRed,
                                activeTrackColor = NetflixRed,
                                inactiveTrackColor = Color(0x55FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Timestamps
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color(0xFFA3A3A3),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bottom Action Icons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left group: Tập trước & Tập tiếp theo
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nút Tập trước
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = previousEpisode != null) {
                                            previousEpisode?.let { prev ->
                                                currentEpisode = prev
                                                onSelectEpisode(prev)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Tập trước",
                                        tint = if (previousEpisode != null) Color.White else Color(0x44FFFFFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Tập trước",
                                        color = if (previousEpisode != null) Color.White else Color(0x44FFFFFF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Nút Tập tiếp theo
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = nextEpisode != null) {
                                            nextEpisode?.let { next ->
                                                currentEpisode = next
                                                onSelectEpisode(next)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Tập tiếp",
                                        tint = if (nextEpisode != null) Color.White else Color(0x44FFFFFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Tập tiếp",
                                        color = if (nextEpisode != null) Color.White else Color(0x44FFFFFF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Right group: Danh sách các tập & Tua 30s
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nút Danh sách các tập phim (Episodes List)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x33FFFFFF))
                                        .clickable {
                                            showEpisodesSheet = true
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VideoLibrary,
                                        contentDescription = "Danh sách tập",
                                        tint = NetflixRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Tập phim (${movie.episodes.size.coerceAtLeast(1)})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Nút Tua tới 30s
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val newPos = (exoPlayer.currentPosition + 30000).coerceAtMost(duration)
                                            exoPlayer.seekTo(newPos)
                                            seekNotice = "+30s >>"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FastForward,
                                        contentDescription = "Tua 30s",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "30s",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Episodes Drawer / Sheet Overlay (Danh sách các tập phong cách Netflix)
        AnimatedVisibility(
            visible = showEpisodesSheet,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Dim Scrim - Tapping dismisses drawer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000))
                        .clickable { showEpisodesSheet = false }
                )

                // Glass Panel on Right side - Clean layout without intercepting child clickables
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(360.dp)
                        .align(Alignment.CenterEnd)
                        .background(Color(0xF0121212))
                        .border(1.dp, Color(0x33FFFFFF))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sheet Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Danh sách tập",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${movie.title} • ${movie.episodes.size} tập",
                                    color = Color(0xFFA3A3A3),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { showEpisodesSheet = false }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Đóng",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0x33FFFFFF), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Scrollable List of Episodes
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(movie.episodes) { index, ep ->
                                val isCurrent = (ep.linkM3u8.isNotBlank() && ep.linkM3u8 == currentEpisode.linkM3u8) ||
                                    (ep.name.isNotBlank() && ep.name.equals(currentEpisode.name, ignoreCase = true)) ||
                                    (ep.slug.isNotBlank() && ep.slug == currentEpisode.slug) ||
                                    (ep.id.isNotBlank() && ep.id == currentEpisode.id)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) Color(0x33E50914) else Color(0x22FFFFFF))
                                        .border(
                                            width = if (isCurrent) 1.5.dp else 0.5.dp,
                                            color = if (isCurrent) NetflixRed else Color(0x33FFFFFF),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            currentEpisode = ep
                                            onSelectEpisode(ep)
                                            showEpisodesSheet = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrent) Icons.Filled.PlayArrow else Icons.Filled.VideoLibrary,
                                                contentDescription = null,
                                                tint = if (isCurrent) NetflixRed else Color(0xFFA3A3A3),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = ep.name,
                                                    color = if (isCurrent) Color.White else Color(0xFFE5E5E5),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                )
                                                if (ep.durationMinutes > 0) {
                                                    Text(
                                                        text = "${ep.durationMinutes} phút",
                                                        color = Color(0xFF888888),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }

                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(NetflixRed)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "ĐANG PHÁT",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
