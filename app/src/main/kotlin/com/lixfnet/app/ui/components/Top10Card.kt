package com.lixfnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.ui.theme.NetflixDarkCard
import com.lixfnet.app.ui.theme.NetflixRed

@Composable
fun Top10Card(
    movie: Movie,
    rank: Int,
    onClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(6.dp)

    Column(
        modifier = Modifier
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.height(175.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Giant 3D Rank Number (1 to 10)
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Shadow / outline layer
                Text(
                    text = rank.toString(),
                    color = Color(0xFF262626),
                    fontSize = 105.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-7).sp,
                    modifier = Modifier.offset(x = 10.dp, y = 16.dp)
                )
                // Front white 3D layer
                Text(
                    text = rank.toString(),
                    color = Color(0xFFE5E5E5),
                    fontSize = 98.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-7).sp,
                    modifier = Modifier.offset(x = 6.dp, y = 12.dp)
                )
            }

            // 2:3 Poster Card
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .height(170.dp)
                    .clip(cardShape)
                    .background(NetflixDarkCard)
            ) {
                AsyncImage(
                    model = movie.posterUrl.orEmpty().ifBlank { movie.backdropUrl.orEmpty() },
                    contentDescription = movie.title.orEmpty(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Platform Specific Top 10 Badge
                val isApple = movie.platform == "apple" || movie.platformName?.contains("Apple", ignoreCase = true) == true
                val isHbo = movie.platform == "hbo" || movie.platformName?.contains("HBO", ignoreCase = true) == true

                val badgeBg = when {
                    isApple -> Color(0xEE1A1A1A)
                    isHbo -> Color(0xFF5822B4)
                    else -> NetflixRed
                }

                val badgeText = when {
                    isApple -> "\uF8FFtv+ TOP 10"
                    isHbo -> "HBO TOP 10"
                    else -> "TOP 10"
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(badgeBg, RoundedCornerShape(bottomEnd = 4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title below poster
        Text(
            text = movie.title,
            color = Color(0xFFE5E5E5),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(140.dp)
                .padding(horizontal = 2.dp)
        )
    }
}
