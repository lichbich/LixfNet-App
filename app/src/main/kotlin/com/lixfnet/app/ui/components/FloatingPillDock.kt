package com.lixfnet.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lixfnet.app.ui.theme.DockGlassBackground
import com.lixfnet.app.ui.theme.DockGlassBorder

enum class NavTab {
    HOME,
    CLIP,
    SEARCH,
    MY_NETFLIX
}

@Composable
fun FloatingPillDock(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val dockShape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(dockShape)
                .background(DockGlassBackground)
                .border(width = 1.dp, color = DockGlassBorder, shape = dockShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DockItem(
                label = "Trang chủ",
                icon = if (currentTab == NavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                isSelected = currentTab == NavTab.HOME,
                onClick = { onTabSelected(NavTab.HOME) }
            )

            DockItem(
                label = "Clip",
                icon = Icons.Outlined.VideoLibrary,
                isSelected = currentTab == NavTab.CLIP,
                onClick = { onTabSelected(NavTab.CLIP) }
            )

            DockItem(
                label = "Tìm kiếm",
                icon = if (currentTab == NavTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                isSelected = currentTab == NavTab.SEARCH,
                onClick = { onTabSelected(NavTab.SEARCH) }
            )

            DockItem(
                label = "Netflix của tôi",
                icon = if (currentTab == NavTab.MY_NETFLIX) Icons.Filled.Person else Icons.Outlined.Person,
                isSelected = currentTab == NavTab.MY_NETFLIX,
                isAvatar = true,
                onClick = { onTabSelected(NavTab.MY_NETFLIX) }
            )
        }
    }
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isAvatar: Boolean = false,
    onClick: () -> Unit
) {
    val iconPillShape = RoundedCornerShape(18.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = if (isSelected) 46.dp else 36.dp, height = 30.dp)
                .clip(iconPillShape)
                .background(if (isSelected && !isAvatar) Color(0x33FFFFFF) else Color.Transparent)
                .border(
                    width = if (isSelected && !isAvatar) 1.dp else 0.dp,
                    color = if (isSelected && !isAvatar) Color(0x66FFFFFF) else Color.Transparent,
                    shape = iconPillShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isAvatar) {
                // Snorlax avatar yellow/blue circle placeholder
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF7D070))
                        .border(1.dp, Color(0xFF2A7571), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💤", fontSize = 11.sp)
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color.White else Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF8E8E93),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
