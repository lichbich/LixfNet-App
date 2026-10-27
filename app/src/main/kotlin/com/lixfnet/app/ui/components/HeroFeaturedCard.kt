package com.lixfnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lixfnet.app.data.model.Movie

@Composable
fun HeroFeaturedCard(
    movie: Movie,
    isInMyList: Boolean = false,
    onCardClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onMyListClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(410.dp)
            .clip(cardShape)
            .border(width = 1.dp, color = Color(0x22FFFFFF), shape = cardShape)
            .clickable { onCardClick() }
    ) {
        // 1. Featured Backdrop / Poster Image
        AsyncImage(
            model = movie.posterUrl.orEmpty().ifBlank { movie.backdropUrl.orEmpty() },
            contentDescription = movie.title.orEmpty(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Cinematic Vignette Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x22000000),
                            Color(0x88000000),
                            Color(0xDD0A0A0A),
                            Color(0xFF0A0A0A)
                        ),
                        startY = 150f
                    )
                )
        )

        // 3. Content at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Movie Title
            Text(
                text = movie.title.orEmpty(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Genre Dots line (e.g. "Series • Cảm động • Lãng mạn • Hài • Công sở")
            val rawGenres = (movie.genres ?: emptyList()).filterNotNull().filter { it.isNotBlank() }
            val genreString = if (rawGenres.isNotEmpty()) {
                rawGenres.take(5).joinToString(separator = "  •  ")
            } else {
                "Series  •  Kịch tính  •  Thịnh hành"
            }

            Text(
                text = genreString,
                color = Color(0xFFD4D4D4),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Play Button: White button with black text
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
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
                        text = if (movie.type == "series") "Phát series" else "Phát",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary My List Button: Translucent dark button
            Button(
                onClick = onMyListClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x66333333),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isInMyList) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = "Danh sách",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInMyList) "Đã thêm vào danh sách" else "Danh sách của tôi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
