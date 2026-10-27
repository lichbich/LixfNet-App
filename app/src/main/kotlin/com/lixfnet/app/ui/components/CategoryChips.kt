package com.lixfnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lixfnet.app.ui.theme.ChipBackground
import com.lixfnet.app.ui.theme.ChipSelectedBackground
import com.lixfnet.app.ui.theme.ChipSelectedText

@Composable
fun CategoryChips(
    categories: List<String> = listOf("Tất cả", "Halloween", "Series", "Phim", "Mới & Hot"),
    selectedCategory: String = "Tất cả",
    onCategorySelected: (String) -> Unit = {}
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            val shape = RoundedCornerShape(20.dp)

            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) ChipSelectedBackground else ChipBackground)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.White else Color(0x33FFFFFF),
                        shape = shape
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = category,
                    color = if (isSelected) ChipSelectedText else Color(0xFFE5E5E5),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
