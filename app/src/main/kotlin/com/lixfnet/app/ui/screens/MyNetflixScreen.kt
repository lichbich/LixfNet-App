package com.lixfnet.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.data.model.WatchProgress
import com.lixfnet.app.data.repository.AuthRepository
import com.lixfnet.app.data.repository.WatchProgressRepository
import com.lixfnet.app.data.update.AppUpdateManager
import com.lixfnet.app.data.update.UpdateUiState
import com.lixfnet.app.ui.theme.NetflixBackground
import com.lixfnet.app.ui.theme.NetflixRed

@Composable
fun MyNetflixScreen(
    watchRepo: WatchProgressRepository,
    authRepository: AuthRepository,
    onMovieClick: (Movie) -> Unit,
    onPlayMovie: (Movie) -> Unit = {}
) {
    val context = LocalContext.current
    val continueWatching by watchRepo.continueWatching.collectAsState()
    val distinctContinueWatching = remember(continueWatching) {
        WatchProgressRepository.deduplicateMovies(continueWatching)
    }
    val myList by watchRepo.myList.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()
    val updateState by AppUpdateManager.updateState.collectAsState()
    var showProfileMenu by remember { mutableStateOf(false) }

    val currentVersion = remember {
        AppUpdateManager.getCurrentAppVersion(context).second
    }

    LaunchedEffect(Unit) {
        watchRepo.syncWithFirebase()
    }

    LaunchedEffect(updateState) {
        if (updateState is UpdateUiState.UpToDate) {
            Toast.makeText(
                context,
                "Bạn đang sử dụng phiên bản mới nhất (v$currentVersion)",
                Toast.LENGTH_SHORT
            ).show()
            AppUpdateManager.dismiss()
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 110.dp)
        ) {
            // 1. Profile Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Avatar + Name + Dropdown Arrow
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showProfileMenu = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NetflixProfileAvatar(size = 38.dp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentUser?.displayName ?: "Trọng Lịch",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Tùy chọn tài khoản",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Profile Dropdown Menu
                    DropdownMenu(
                        expanded = showProfileMenu,
                        onDismissRequest = { showProfileMenu = false },
                        modifier = Modifier.background(Color(0xFF222222))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = currentUser?.displayName ?: "Trọng Lịch",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (currentUser?.role == "admin") "Quản trị viên (Admin)" else "Thành viên (Member)",
                                        color = NetflixRed,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            onClick = { showProfileMenu = false }
                        )
                        HorizontalDivider(color = Color(0x33FFFFFF))
                        DropdownMenuItem(
                            text = { Text("Kiểm tra cập nhật", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White)
                            },
                            onClick = {
                                showProfileMenu = false
                                AppUpdateManager.checkForUpdates(context, isManual = true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Đăng xuất", color = NetflixRed) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = NetflixRed)
                            },
                            onClick = {
                                showProfileMenu = false
                                authRepository.logout()
                            }
                        )
                    }
                }

                // Right Action Icons: Download tray & Notification bell with badge "1"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Tính năng tải phim ngoại tuyến đang phát triển", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Tải xuống",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier.size(32.dp)
                    ) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Bạn đang có phiên bản mới nhất v$currentVersion", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Thông báo",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        // Notification red badge
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(NetflixRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Download Banner ("Tải xuống")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141414))
                    .clickable {
                        Toast.makeText(context, "Các bộ phim và series bạn tải xuống xuất hiện tại đây.", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tải xuống",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Các bộ phim và series bạn tải xuống xuất hiện tại đây.",
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3. Section 1: "Tiếp tục xem" (Placed First!)
            Text(
                text = "Tiếp tục xem",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)
            )

            if (distinctContinueWatching.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141414))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bạn chưa xem dở bộ phim nào gần đây.\nKhi xem phim, tiến trình sẽ hiển thị tại đây để bạn tiếp tục bất kỳ lúc nào.",
                            color = Color(0xFF888888),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = distinctContinueWatching,
                        key = { it.slug.ifBlank { it.id } }
                    ) { movie ->
                        ContinueWatchingNetflixCard(
                            movie = movie,
                            progress = watchRepo.getProgressForMovie(movie.slug.ifBlank { movie.id }),
                            onPlayClick = { onPlayMovie(movie) },
                            onInfoClick = { onMovieClick(movie) },
                            onRemoveClick = { watchRepo.removeContinueWatching(movie) }
                        )
                    }
                }
            }

            // 4. Section 2: "Tác phẩm bạn đã thích" (Liked titles with Share button)
            Text(
                text = "Tác phẩm bạn đã thích",
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
            )

            if (myList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141414))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có tác phẩm nào trong danh sách bạn đã thích.\nBấm '+' trên phim bất kỳ để lưu lại vào đây!",
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = myList,
                        key = { "liked_${it.slug.ifBlank { it.id }}" }
                    ) { movie ->
                        LikedMovieCard(
                            movie = movie,
                            onMovieClick = { onMovieClick(movie) }
                        )
                    }
                }
            }

            // 5. Section 3: "Danh sách của tôi" (My List with Chevron Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Danh sách của tôi",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (myList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141414))
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Danh sách của bạn đang trống.",
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = myList,
                        key = { "mylist_${it.slug.ifBlank { it.id }}" }
                    ) { movie ->
                        StandardPosterCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 6. App Version & Update Check Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1AFFFFFF))
                    .clickable {
                        AppUpdateManager.checkForUpdates(context, isManual = true)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = NetflixRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Phiên bản v$currentVersion",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (updateState is UpdateUiState.Checking) "Đang kiểm tra cập nhật..." else "Chạm để kiểm tra bản cập nhật mới",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 7. Logout Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0DFFFFFF))
                    .clickable { authRepository.logout() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = NetflixRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đăng xuất tài khoản",
                    color = NetflixRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Netflix Snorlax Sleeping Avatar
 */
@Composable
fun NetflixProfileAvatar(modifier: Modifier = Modifier, size: Dp = 38.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE5A00D)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.75f)) {
            val w = this.size.width
            val h = this.size.height

            // Left sleeping eye
            drawLine(
                color = Color(0xFF1E2D2F),
                start = Offset(w * 0.15f, h * 0.42f),
                end = Offset(w * 0.40f, h * 0.45f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Right sleeping eye
            drawLine(
                color = Color(0xFF1E2D2F),
                start = Offset(w * 0.85f, h * 0.42f),
                end = Offset(w * 0.60f, h * 0.45f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Smiling mouth
            drawArc(
                color = Color(0xFF1E2D2F),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.32f, h * 0.48f),
                size = Size(w * 0.36f, h * 0.30f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Netflix Continue Watching Card
 * Poster with Play Button overlay, Progress Bar, and bottom Info & More actions
 */
@Composable
fun ContinueWatchingNetflixCard(
    movie: Movie,
    progress: WatchProgress?,
    onPlayClick: () -> Unit,
    onInfoClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(118.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141414))
    ) {
        // Poster Box
        Box(
            modifier = Modifier
                .width(118.dp)
                .height(165.dp)
                .clickable { onPlayClick() }
        ) {
            AsyncImage(
                model = movie.posterUrl.orEmpty().ifBlank { movie.backdropUrl.orEmpty() },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Centered Play Button (Translucent Black Circle with White Border)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0x77000000))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Xem tiếp",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Series Badge (Tập mới / Xem ngay)
            val isSeries = movie.type.equals("series", ignoreCase = true) ||
                    movie.type.equals("hoathinh", ignoreCase = true) ||
                    movie.currentEpisode.isNotBlank()
            if (isSeries) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE50914))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Tập mới",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Xem ngay",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Red Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0x66333333))
            ) {
                val progFraction = progress?.let {
                    if (it.totalSeconds > 0) it.currentSeconds.toFloat() / it.totalSeconds else (it.progressPercent / 100f)
                } ?: movie.progress ?: 0.35f

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progFraction.coerceIn(0.05f, 1f))
                        .background(NetflixRed)
                )
            }
        }

        // Bottom Action Bar: Info (i) & More options (⋮)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF141414)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Thông tin chi tiết",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF222222))
                ) {
                    DropdownMenuItem(
                        text = { Text("Phát phim", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        },
                        onClick = {
                            showMenu = false
                            onPlayClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xem chi tiết", color = Color.White) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color.White)
                        },
                        onClick = {
                            showMenu = false
                            onInfoClick()
                        }
                    )
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    DropdownMenuItem(
                        text = { Text("Xóa khỏi Tiếp tục xem", color = NetflixRed) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = NetflixRed)
                        },
                        onClick = {
                            showMenu = false
                            onRemoveClick()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Liked Movie Card with Poster and Share Button
 */
@Composable
fun LikedMovieCard(
    movie: Movie,
    onMovieClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(118.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141414))
    ) {
        // Poster
        Box(
            modifier = Modifier
                .width(118.dp)
                .height(165.dp)
                .clickable { onMovieClick() }
        ) {
            AsyncImage(
                model = movie.posterUrl.orEmpty().ifBlank { movie.backdropUrl.orEmpty() },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Share Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF141414))
                .clickable {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Xem ngay phim '${movie.title}' trên ứng dụng LixfNet!"
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ phim"))
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Chia sẻ",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Chia sẻ",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Standard Poster Card for "Danh sách của tôi"
 */
@Composable
fun StandardPosterCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(118.dp)
            .height(165.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = movie.posterUrl.orEmpty().ifBlank { movie.backdropUrl.orEmpty() },
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
