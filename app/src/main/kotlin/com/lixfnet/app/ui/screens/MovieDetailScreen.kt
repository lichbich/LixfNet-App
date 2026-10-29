package com.lixfnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lixfnet.app.data.model.Episode
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.data.model.WatchProgress
import com.lixfnet.app.data.repository.MovieRepository
import com.lixfnet.app.data.repository.WatchProgressRepository
import com.lixfnet.app.ui.theme.NetflixBackground
import com.lixfnet.app.ui.theme.NetflixDarkCard
import com.lixfnet.app.ui.theme.NetflixRed
import kotlinx.coroutines.launch

@Composable
fun MovieDetailScreen(
    movie: Movie,
    movieRepository: MovieRepository,
    watchRepo: WatchProgressRepository,
    onBack: () -> Unit,
    onPlayEpisode: (Movie, Episode) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var fullMovie by remember { mutableStateOf(movie) }
    var isLoadingDetail by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(true) }
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
    var savedProgress by remember { mutableStateOf<WatchProgress?>(watchRepo.getProgressForMovie(movie.slug) ?: watchRepo.getProgressForMovie(movie.id)) }

    LaunchedEffect(movie.slug) {
        coroutineScope.launch {
            try {
                val detail = movieRepository.getMovieDetail(movie.slug)
                fullMovie = detail

                val progress = watchRepo.getProgressForMovie(detail.slug) ?: watchRepo.getProgressForMovie(detail.id)
                savedProgress = progress

                val matchedEp = if (progress != null) {
                    detail.episodes.find { ep ->
                        ep.name.equals(progress.episodeTitle, ignoreCase = true) ||
                        (progress.episodeM3u8.isNotBlank() && ep.linkM3u8 == progress.episodeM3u8) ||
                        (ep.name.filter { it.isDigit() }.isNotBlank() && ep.name.filter { it.isDigit() } == progress.episodeTitle.filter { it.isDigit() })
                    } ?: detail.episodes.firstOrNull()
                } else {
                    detail.episodes.firstOrNull()
                }
                selectedEpisode = matchedEp
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingDetail = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // 1. Top Video Preview Area (Matching Screenshot 1)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.Black)
                ) {
                    // Preview image / video still
                    val imageUrl = fullMovie.backdropUrl.ifBlank { fullMovie.posterUrl }
                    var isImageError by remember(imageUrl) { mutableStateOf(imageUrl.isBlank()) }

                    if (imageUrl.isNotBlank() && !isImageError) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = fullMovie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = { isImageError = true }
                        )
                    }

                    // Top Back Arrow
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 8.dp, top = 4.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Subtitle overlay simulation ("Xin phép.")
                    Text(
                        text = "Xin phép.",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    )

                    // Audio Mute/Unmute Toggle Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(14.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .clickable { isMuted = !isMuted },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Âm lượng",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Movie Metadata & Details Header (Matching Screenshot 1)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Netflix Brand Red Tag
                    Text(
                        text = "NETFLIX",
                        color = NetflixRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Movie Title
                    Text(
                        text = fullMovie.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metadata Badges Line: 2026 | T16 | loạt phim truyền hình ngắn | HD
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${fullMovie.year}",
                            color = Color(0xFFA3A3A3),
                            fontSize = 13.sp
                        )

                        // Age Rating Badge: T16
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(3.dp))
                                .background(Color(0x22FFFFFF), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = fullMovie.rating,
                                color = Color(0xFFE5E5E5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = fullMovie.durationText,
                            color = Color(0xFFA3A3A3),
                            fontSize = 13.sp
                        )

                        // Quality Badge: HD
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(3.dp))
                                .background(Color(0x22FFFFFF), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "HD",
                                color = Color(0xFFE5E5E5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Top 10 Ranking Line: [TOP 10] #2 trong Series hôm nay
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(NetflixRed)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TOP\n10",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 8.sp
                            )
                        }

                        Text(
                            text = "#2 trong Series hôm nay",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val hasResume = savedProgress != null && (savedProgress?.currentSeconds ?: 0L) > 5L
                    val epToPlay = selectedEpisode ?: fullMovie.episodes.firstOrNull() ?: Episode("ep-1", "Tập 1", "tap-1")

                    // Primary Play Button: White button with black text
                    Button(
                        onClick = {
                            onPlayEpisode(fullMovie, epToPlay)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Phát",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasResume) "Tiếp tục xem (${epToPlay.name})" else "Phát",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Download Button: Dark Gray button
                    Button(
                        onClick = { /* Offline download trigger */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2B2B2B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileDownload,
                                contentDescription = "Tải xuống",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tải xuống M1:T1",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Episode Title subtitle
                    Text(
                        text = "M1:T1 \"${selectedEpisode?.name ?: "Tập 1"}\"",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Synopsis / Description
                    Text(
                        text = fullMovie.description,
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cast info
                    if (fullMovie.cast.isNotEmpty()) {
                        Text(
                            text = "Diễn viên: ${fullMovie.cast.joinToString(", ")}... thêm",
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (fullMovie.director.isNotEmpty()) {
                        Text(
                            text = "Tác giả: ${fullMovie.director.joinToString(", ")}",
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 3. Episodes Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tập phim",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mùa 1",
                        color = Color(0xFFA3A3A3),
                        fontSize = 14.sp
                    )
                }
            }

            // Episode List Items
            val displayEpisodes = if (fullMovie.episodes.isNotEmpty()) {
                fullMovie.episodes
            } else {
                (1..8).map { idx ->
                    Episode(
                        id = "demo-ep-$idx",
                        name = "Tập $idx",
                        slug = "tap-$idx",
                        durationMinutes = 45,
                        synopsis = fullMovie.description
                    )
                }
            }

            itemsIndexed(displayEpisodes) { index, ep ->
                EpisodeItemRow(
                    episode = ep,
                    backdropUrl = fullMovie.backdropUrl,
                    isSelected = selectedEpisode?.slug == ep.slug,
                    onClick = {
                        selectedEpisode = ep
                        onPlayEpisode(fullMovie, ep)
                    }
                )
            }
        }
    }
}

@Composable
fun EpisodeItemRow(
    episode: Episode,
    backdropUrl: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Thumbnail (16:9)
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NetflixDarkCard)
            ) {
                AsyncImage(
                    model = episode.stillPath ?: backdropUrl,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Phát tập",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Episode Title and Duration
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.name,
                    color = if (isSelected) NetflixRed else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${episode.durationMinutes} phút",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp
                )
            }

            // Download Icon
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = "Tải tập",
                    tint = Color(0xFFA3A3A3),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Episode Synopsis
        Text(
            text = episode.synopsis ?: "Khám phá diễn biến kịch tính trong tập phim.",
            color = Color(0xFF9E9E9E),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
