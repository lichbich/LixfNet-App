package com.lixfnet.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.data.repository.WatchProgressRepository
import com.lixfnet.app.ui.components.ContinueWatchingCard
import com.lixfnet.app.ui.components.MovieCard
import com.lixfnet.app.ui.theme.NetflixBackground
import com.lixfnet.app.ui.theme.NetflixDarkCard
import com.lixfnet.app.ui.theme.NetflixRed

enum class MyListTab {
    MY_LIST,
    CONTINUE_WATCHING
}

@Composable
fun MyNetflixScreen(
    watchRepo: WatchProgressRepository,
    authRepository: com.lixfnet.app.data.repository.AuthRepository,
    onMovieClick: (Movie) -> Unit
) {
    val continueWatching by watchRepo.continueWatching.collectAsState()
    val myList by watchRepo.myList.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()
    var selectedTab by remember { mutableStateOf(MyListTab.MY_LIST) }

    LaunchedEffect(Unit) {
        watchRepo.syncWithFirebase()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
        ) {
            // Profile Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Snorlax Avatar Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF7D070))
                        .border(2.dp, Color(0xFF2A7571), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💤", fontSize = 28.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.displayName ?: "Trọng Lịch",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentUser?.role == "admin") "Quản trị viên (Admin)" else "Thành viên (Member)",
                        color = NetflixRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = { authRepository.logout() }) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Đăng xuất",
                        tint = Color.White
                    )
                }
            }

            // Tabs Selector: Danh sách của tôi | Tiếp tục xem
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabPill(
                    title = "Danh sách của tôi (${myList.size})",
                    isSelected = selectedTab == MyListTab.MY_LIST,
                    onClick = { selectedTab = MyListTab.MY_LIST }
                )

                TabPill(
                    title = "Tiếp tục xem (${continueWatching.size})",
                    isSelected = selectedTab == MyListTab.CONTINUE_WATCHING,
                    onClick = { selectedTab = MyListTab.CONTINUE_WATCHING }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content List
            val displayList = if (selectedTab == MyListTab.MY_LIST) myList else continueWatching

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == MyListTab.MY_LIST)
                            "Chưa có phim nào trong Danh sách của bạn.\nHãy bấm '+' tại phim bất kỳ để lưu lại!"
                        else
                            "Bạn chưa xem dở bộ phim nào gần đây.",
                        color = Color(0xFFA3A3A3),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(displayList) { movie ->
                        MovieCard(
                            movie = movie,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onMovieClick(movie) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (isSelected) Color.White else Color(0x22FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
