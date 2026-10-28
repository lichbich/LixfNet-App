package com.lixfnet.app.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.lixfnet.app.data.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class AppUpdateInfo(
    @SerializedName("versionCode") val versionCode: Int,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("title") val title: String,
    @SerializedName("changelog") val changelog: String,
    @SerializedName("apkUrl") val apkUrl: String,
    @SerializedName("forceUpdate") val forceUpdate: Boolean = false
)

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateUiState()
    data class Downloading(
        val progress: Float,
        val currentBytes: Long,
        val totalBytes: Long,
        val updateInfo: AppUpdateInfo
    ) : UpdateUiState()
    data class ReadyToInstall(val apkFile: File, val updateInfo: AppUpdateInfo) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
    data class UpToDate(val currentVersion: String) : UpdateUiState()
}

object AppUpdateManager {
    private const val GITHUB_OWNER = "lichbich"
    private const val GITHUB_REPO = "LixfNet-App"
    private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/version.json"
    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getCurrentAppVersion(context: Context): Pair<Int, String> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val code = PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
            val name = packageInfo.versionName ?: "1.0"
            Pair(code, name)
        } catch (e: Exception) {
            Pair(1, "1.0")
        }
    }

    fun checkForUpdates(context: Context, isManual: Boolean = false) {
        scope.launch {
            if (_updateState.value is UpdateUiState.Downloading) return@launch
            _updateState.value = UpdateUiState.Checking

            val (currentCode, currentName) = getCurrentAppVersion(context)

            try {
                // 1. Try fetching version.json directly from repository
                var updateInfo = fetchFromVersionJson()

                // 2. Fallback to GitHub Releases API if version.json is missing or invalid
                if (updateInfo == null) {
                    updateInfo = fetchFromGitHubReleases()
                }

                if (updateInfo != null) {
                    val isNewer = isNewerVersion(
                        remoteCode = updateInfo.versionCode,
                        remoteName = updateInfo.versionName,
                        localCode = currentCode,
                        localName = currentName
                    )

                    val isDismissed = if (!isManual && !updateInfo.forceUpdate) {
                        val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
                        val dismissedName = prefs.getString("dismissed_version_name", "") ?: ""
                        dismissedName.equals(updateInfo.versionName.trim().trimStart('v', 'V'), ignoreCase = true)
                    } else false

                    if (isNewer && !isDismissed && updateInfo.apkUrl.isNotBlank()) {
                        _updateState.value = UpdateUiState.UpdateAvailable(updateInfo)
                    } else {
                        _updateState.value = if (isManual) {
                            UpdateUiState.UpToDate(currentName)
                        } else {
                            UpdateUiState.Idle
                        }
                    }
                } else {
                    _updateState.value = if (isManual) {
                        UpdateUiState.UpToDate(currentName)
                    } else {
                        UpdateUiState.Idle
                    }
                }
            } catch (e: Exception) {
                if (isManual) {
                    _updateState.value = UpdateUiState.Error("Không thể kiểm tra bản cập nhật: ${e.localizedMessage ?: "Lỗi kết nối"}")
                } else {
                    _updateState.value = UpdateUiState.Idle
                }
            }
        }
    }

    private fun fetchFromVersionJson(): AppUpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(VERSION_JSON_URL)
                .header("Cache-Control", "no-cache")
                .build()

            ApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return null
                    gson.fromJson(bodyString, AppUpdateInfo::class.java)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromGitHubReleases(): AppUpdateInfo? {
        return try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API)
                .header("User-Agent", "LixfNet-Android-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            ApiClient.okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: return null
                    val json = gson.fromJson(bodyString, JsonObject::class.java)

                    val tagName = json.get("tag_name")?.asString?.trimStart('v', 'V') ?: ""
                    val title = json.get("name")?.asString ?: "Bản cập nhật mới ($tagName)"
                    val changelog = json.get("body")?.asString ?: "Cải tiến hiệu năng và sửa lỗi."

                    // Find APK download URL from assets
                    var apkUrl = ""
                    val assets = json.getAsJsonArray("assets")
                    if (assets != null) {
                        for (element in assets) {
                            val assetObj = element.asJsonObject
                            val name = assetObj.get("name")?.asString ?: ""
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = assetObj.get("browser_download_url")?.asString ?: ""
                                break
                            }
                        }
                    }

                    if (tagName.isNotBlank() && apkUrl.isNotBlank()) {
                        val derivedCode = parseVersionCode(tagName)
                        AppUpdateInfo(
                            versionCode = derivedCode,
                            versionName = tagName,
                            title = title,
                            changelog = changelog,
                            apkUrl = apkUrl,
                            forceUpdate = false
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersion(
        remoteCode: Int,
        remoteName: String,
        localCode: Int,
        localName: String
    ): Boolean {
        val cleanRemote = remoteName.trim().trimStart('v', 'V').trim()
        val cleanLocal = localName.trim().trimStart('v', 'V').trim()

        // If version names match exactly (e.g. 1.1 == 1.1), it is already installed!
        if (cleanRemote.equals(cleanLocal, ignoreCase = true)) {
            return false
        }

        // SemVer check
        val semVerDiff = compareSemVer(cleanRemote, cleanLocal)
        if (semVerDiff > 0) return true
        if (semVerDiff < 0) return false

        // Fallback to versionCode
        return remoteCode > localCode
    }

    private fun compareSemVer(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val length = maxOf(parts1.size, parts2.size)
        for (i in 0 until length) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    private fun parseVersionCode(versionName: String): Int {
        return try {
            val parts = versionName.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
            if (parts.size >= 2) {
                (parts.getOrElse(0) { 1 } * 100) + (parts.getOrElse(1) { 0 } * 10) + parts.getOrElse(2) { 0 }
            } else {
                versionName.filter { it.isDigit() }.toIntOrNull() ?: 1
            }
        } catch (e: Exception) {
            1
        }
    }

    fun startDownload(context: Context, updateInfo: AppUpdateInfo) {
        scope.launch {
            try {
                _updateState.value = UpdateUiState.Downloading(
                    progress = 0f,
                    currentBytes = 0L,
                    totalBytes = 0L,
                    updateInfo = updateInfo
                )

                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.cacheDir
                if (!downloadDir.exists()) downloadDir.mkdirs()

                val destinationFile = File(downloadDir, "LixfNet-${updateInfo.versionName}.apk")
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                val request = Request.Builder()
                    .url(updateInfo.apkUrl)
                    .build()

                val call = ApiClient.okHttpClient.newCall(request)
                val response = call.execute()

                if (!response.isSuccessful) {
                    _updateState.value = UpdateUiState.Error("Tải file thất bại (Mã lỗi: ${response.code})")
                    return@launch
                }

                val body = response.body ?: throw IllegalStateException("Response body is null")
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        var lastProgressReportTime = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastProgressReportTime > 100 || downloadedBytes == totalBytes) {
                                lastProgressReportTime = now
                                val progress = if (totalBytes > 0) {
                                    downloadedBytes.toFloat() / totalBytes.toFloat()
                                } else 0f

                                _updateState.value = UpdateUiState.Downloading(
                                    progress = progress,
                                    currentBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    updateInfo = updateInfo
                                )
                            }
                        }
                        output.flush()
                    }
                }

                if (destinationFile.exists() && destinationFile.length() > 0) {
                    _updateState.value = UpdateUiState.ReadyToInstall(destinationFile, updateInfo)
                    withContext(Dispatchers.Main) {
                        installApk(context, destinationFile)
                    }
                } else {
                    _updateState.value = UpdateUiState.Error("File APK tải về không hợp lệ.")
                }

            } catch (e: Exception) {
                _updateState.value = UpdateUiState.Error("Lỗi trong quá trình tải: ${e.localizedMessage ?: "Mất kết nối mạng"}")
            }
        }
    }

    fun installApk(context: Context, file: File) {
        try {
            if (!file.exists() || file.length() == 0L) {
                _updateState.value = UpdateUiState.Error("Không tìm thấy file cài đặt APK")
                return
            }

            // Android 8.0+ Check Unknown App Sources permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateUiState.Error("Không thể mở trình cài đặt: ${e.localizedMessage}")
        }
    }

    fun dismiss(context: Context? = null, updateInfo: AppUpdateInfo? = null) {
        _updateState.value = UpdateUiState.Idle
        if (context != null && updateInfo != null) {
            val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("dismissed_version_name", updateInfo.versionName.trim().trimStart('v', 'V'))
                .apply()
        }
    }
}
