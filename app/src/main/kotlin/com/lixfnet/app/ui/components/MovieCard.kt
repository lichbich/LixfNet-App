package com.lixfnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.style.TextAlign

@Composable
fun MovieCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(6.dp)

    Column(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(95.dp) // 16:9 ratio
                .clip(shape)
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

            // Bottom subtle gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000))
                        )
                    )
            )

            // Quality badge
            if (movie.quality.orEmpty().isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color(0x99000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = movie.quality.orEmpty(),
                        color = Color(0xFFE0E0E0),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Smart Badges (TẬP MỚI / MỚI THÊM)
            val ribbon = movie.ribbonBadge ?: if (movie.isNewEpisode) "TẬP MỚI" else null
            if (!ribbon.isNullOrBlank()) {
                val badgeColor = when {
                    ribbon.contains("TẬP MỚI", ignoreCase = true) -> NetflixRed
                    ribbon.contains("MỚI THÊM", ignoreCase = true) -> Color(0xFFE55302)
                    else -> NetflixRed
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(badgeColor, RoundedCornerShape(bottomEnd = 4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = ribbon.uppercase(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = movie.title.orEmpty(),
            color = Color(0xFFE5E5E5),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
