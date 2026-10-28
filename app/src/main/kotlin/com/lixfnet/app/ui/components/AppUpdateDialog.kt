package com.lixfnet.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lixfnet.app.data.update.AppUpdateManager
import com.lixfnet.app.data.update.UpdateUiState
import com.lixfnet.app.ui.theme.NetflixDarkCard
import com.lixfnet.app.ui.theme.NetflixRed
import java.util.Locale

@Composable
fun AppUpdateDialog() {
    val context = LocalContext.current
    val updateState by AppUpdateManager.updateState.collectAsState()

    val showDialog = when (updateState) {
        is UpdateUiState.UpdateAvailable,
        is UpdateUiState.Downloading,
        is UpdateUiState.ReadyToInstall,
        is UpdateUiState.Error -> true
        else -> false
    }

    if (!showDialog) return

    val currentVersion = remember {
        AppUpdateManager.getCurrentAppVersion(context).second
    }

    Dialog(
        onDismissRequest = {
            if (updateState is UpdateUiState.UpdateAvailable) {
                val info = (updateState as UpdateUiState.UpdateAvailable).updateInfo
                if (!info.forceUpdate) AppUpdateManager.dismiss(context, info)
            } else if (updateState is UpdateUiState.Error) {
                AppUpdateManager.dismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = (updateState as? UpdateUiState.UpdateAvailable)?.updateInfo?.forceUpdate != true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .border(1.dp, Color(0x33E50914), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (updateState is UpdateUiState.Error) Color(0x33FF4444)
                            else Color(0x33E50914)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (updateState is UpdateUiState.Error) Icons.Default.Warning
                                      else if (updateState is UpdateUiState.Downloading) Icons.Default.Download
                                      else Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = if (updateState is UpdateUiState.Error) Color(0xFFFF4444) else NetflixRed,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = when (val state = updateState) {
                        is UpdateUiState.UpdateAvailable -> state.updateInfo.title.ifBlank { "Đã có bản cập nhật mới!" }
                        is UpdateUiState.Downloading -> "Đang tải bản cập nhật..."
                        is UpdateUiState.ReadyToInstall -> "Tải hoàn tất!"
                        is UpdateUiState.Error -> "Lỗi cập nhật"
                        else -> "Cập nhật ứng dụng"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version Tag
                when (val state = updateState) {
                    is UpdateUiState.UpdateAvailable -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0x22FFFFFF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Hiện tại: v$currentVersion",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = "➔", color = NetflixRed, fontSize = 12.sp)
                            Surface(
                                color = NetflixRed,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Mới: v${state.updateInfo.versionName}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Body
                when (val state = updateState) {
                    is UpdateUiState.UpdateAvailable -> {
                        if (state.updateInfo.changelog.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp),
                                color = Color(0xFF141414),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "Nội dung cập nhật:",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.updateInfo.changelog,
                                        color = Color(0xFFCCCCCC),
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                    is UpdateUiState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NetflixRed,
                                trackColor = Color(0x33FFFFFF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val percent = (state.progress * 100).toInt()
                            val curMb = state.currentBytes / (1024f * 1024f)
                            val totalMb = state.totalBytes / (1024f * 1024f)
                            Text(
                                text = if (state.totalBytes > 0)
                                    String.format(Locale.US, "%d%% (%.1f MB / %.1f MB)", percent, curMb, totalMb)
                                else "Đang chuẩn bị tải...",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                        }
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        Text(
                            text = "Bản cập nhật đã được tải về thành công. Vui lòng bấm Cài đặt để cập nhật ứng dụng.",
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    is UpdateUiState.Error -> {
                        Text(
                            text = state.message,
                            color = Color(0xFFFF6666),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                when (val state = updateState) {
                    is UpdateUiState.UpdateAvailable -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!state.updateInfo.forceUpdate) {
                                TextButton(
                                    onClick = { AppUpdateManager.dismiss(context, state.updateInfo) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Để sau", color = Color(0xFFAAAAAA))
                                }
                            }
                            Button(
                                onClick = {
                                    AppUpdateManager.startDownload(context, state.updateInfo)
                                },
                                modifier = Modifier.weight(if (state.updateInfo.forceUpdate) 2f else 1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Cập nhật ngay",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is UpdateUiState.Downloading -> {
                        Text(
                            text = "Vui lòng giữ ứng dụng mở trong lúc tải...",
                            color = Color(0xFF888888),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        Button(
                            onClick = {
                                AppUpdateManager.installApk(context, state.apkFile)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Cài đặt ngay",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is UpdateUiState.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { AppUpdateManager.dismiss() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Đóng", color = Color(0xFFAAAAAA))
                            }
                            Button(
                                onClick = {
                                    AppUpdateManager.checkForUpdates(context, isManual = true)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "Thử lại", color = Color.White)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
