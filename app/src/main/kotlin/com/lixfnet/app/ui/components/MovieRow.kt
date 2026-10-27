package com.lixfnet.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lixfnet.app.data.model.Movie

enum class RowType {
    NORMAL,
    TOP10,
    CONTINUE_WATCHING
}

@Composable
fun MovieRow(
    title: String,
    movies: List<Movie>,
    rowType: RowType = RowType.NORMAL,
    onMovieClick: (Movie) -> Unit = {}
) {
    if (movies.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Row Section Title
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (rowType) {
                RowType.TOP10 -> {
                    itemsIndexed(movies) { index, movie ->
                        Top10Card(
                            movie = movie,
                            rank = index + 1,
                            onClick = { onMovieClick(movie) }
                        )
                    }
                }
                RowType.CONTINUE_WATCHING -> {
                    items(movies) { movie ->
                        ContinueWatchingCard(
                            movie = movie,
                            onPlayClick = { onMovieClick(movie) }
                        )
                    }
                }
                RowType.NORMAL -> {
                    items(movies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) }
                        )
                    }
                }
            }
        }
    }
}
