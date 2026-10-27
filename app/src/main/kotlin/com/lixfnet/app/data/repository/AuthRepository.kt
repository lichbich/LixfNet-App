package com.lixfnet.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lixfnet.app.data.firebase.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.mindrot.jbcrypt.BCrypt

data class FirebaseUser(
    val username: String,
    val displayName: String,
    val role: String,
    val passwordHash: String = ""
)

class AuthRepository(
    private val context: Context,
    private val firebaseManager: FirebaseManager
) {
    private val prefs = context.getSharedPreferences("lixfnet_auth_prefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        loadSavedUser()
    }

    private fun loadSavedUser() {
        val json = prefs.getString("current_user", null)
        if (!json.isNullOrBlank()) {
            try {
                _currentUser.value = gson.fromJson(json, FirebaseUser::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isLoggedIn(): Boolean = _currentUser.value != null

    suspend fun login(usernameInput: String, passwordInput: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val cleanUsername = usernameInput.trim().lowercase()
        val password = passwordInput.trim()

        if (cleanUsername.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Vui lòng nhập tên đăng nhập và mật khẩu"))
        }

        try {
            val token = firebaseManager.getAccessToken()
            val dbUrl = firebaseManager.getDatabaseUrl()

            // 1. Fetch user from Firebase
            val request = Request.Builder()
                .url("$dbUrl/lixfnet/users/$cleanUsername.json?access_token=$token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()

            var userJson = if (!body.isNullOrBlank() && body != "null") {
                gson.fromJson(body, JsonObject::class.java)
            } else null

            // 2. Auto-seed default admin if database is fresh
            if (userJson == null && cleanUsername == "tronglich") {
                val defaultHash = BCrypt.hashpw("389363", BCrypt.gensalt(10))
                val newAdmin = JsonObject().apply {
                    addProperty("username", "tronglich")
                    addProperty("passwordHash", defaultHash)
                    addProperty("role", "admin")
                    addProperty("displayName", "Trọng Lịch")
                    addProperty("createdAt", System.currentTimeMillis().toString())
                    addProperty("createdBy", "system")
                }

                val putRequest = Request.Builder()
                    .url("$dbUrl/lixfnet/users/tronglich.json?access_token=$token")
                    .put(gson.toJson(newAdmin).toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(putRequest).execute()
                userJson = newAdmin
            }

            if (userJson == null) {
                return@withContext Result.failure(Exception("Tài khoản không tồn tại. Vui lòng liên hệ quản trị viên."))
            }

            val storedHash = userJson.get("passwordHash")?.asString ?: ""

            // 3. Verify password with BCrypt (normalize $2b$ and $2y$ from Node.js bcrypt to $2a$ for jbcrypt)
            val normalizedHash = if (storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
                "$2a$" + storedHash.substring(4)
            } else {
                storedHash
            }

            val isMatch = try {
                BCrypt.checkpw(password, normalizedHash)
            } catch (e: Exception) {
                // Fallback for plain text password
                storedHash == password
            }

            if (!isMatch) {
                return@withContext Result.failure(Exception("Mật khẩu không chính xác"))
            }

            val user = FirebaseUser(
                username = userJson.get("username")?.asString ?: cleanUsername,
                displayName = userJson.get("displayName")?.asString ?: cleanUsername,
                role = userJson.get("role")?.asString ?: "member"
            )

            // 4. Save Session
            _currentUser.value = user
            prefs.edit().putString("current_user", gson.toJson(user)).apply()

            return@withContext Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("current_user").apply()
    }
}
