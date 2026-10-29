package com.lixfnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.ui.theme.NetflixDarkCard
import com.lixfnet.app.ui.theme.NetflixRed

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ContinueWatchingCard(
    movie: Movie,
    onPlayClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(8.dp)
    val progress = movie.progress ?: 0.35f

    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onPlayClick() }
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(95.dp)
                .clip(cardShape)
                .background(NetflixDarkCard)
        ) {
            val imageUrl = movie.backdropUrl.orEmpty().ifBlank { movie.posterUrl.orEmpty() }
            var isImageError by remember(imageUrl) { mutableStateOf(imageUrl.isBlank()) }

            if (imageUrl.isNotBlank() && !isImageError) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = movie.title.orEmpty(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isImageError = true }
                )
            }

            if (isImageError || imageUrl.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF2B2B2B), Color(0xFF141414))
                            )
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = movie.title.orEmpty(),
                        color = Color(0xFFDDDDDD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Center semi-transparent Play Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Xem tiếp",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom Red Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0x66444444))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress.coerceIn(0.05f, 1f))
                        .background(NetflixRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = movie.title.orEmpty(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = movie.currentEpisode.orEmpty().ifBlank { "Đang xem dở" },
            color = Color(0xFFA3A3A3),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
