package com.lixfnet.app.data.firebase

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

class FirebaseManager(private val context: Context) {

    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private val mutex = Mutex()

    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    private var clientEmail: String = ""
    private var privateKeyString: String = ""
    private var projectId: String = "unichat-acfc2"

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        try {
            val jsonString = context.assets.open("firebasekey.json").bufferedReader().use { it.readText() }
            val json = gson.fromJson(jsonString, JsonObject::class.java)
            clientEmail = json.get("client_email")?.asString ?: ""
            privateKeyString = json.get("private_key")?.asString ?: ""
            projectId = json.get("project_id")?.asString ?: "unichat-acfc2"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDatabaseUrl(): String {
        return "https://$projectId-default-rtdb.firebaseio.com"
    }

    suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis() / 1000
        mutex.withLock {
            if (cachedToken != null && now < tokenExpiresAt - 300) {
                return@withLock cachedToken!!
            }

            val token = requestNewAccessToken()
            cachedToken = token
            tokenExpiresAt = now + 3600
            return@withLock token
        }
    }

    private fun requestNewAccessToken(): String {
        val now = System.currentTimeMillis() / 1000
        val exp = now + 3600

        // 1. JWT Header
        val header = JsonObject().apply {
            addProperty("alg", "RS256")
            addProperty("typ", "JWT")
        }
        val headerBase64 = base64UrlEncode(gson.toJson(header).toByteArray(Charsets.UTF_8))

        // 2. JWT Claims
        val claims = JsonObject().apply {
            addProperty("iss", clientEmail)
            addProperty("scope", "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/firebase.database")
            addProperty("aud", "https://oauth2.googleapis.com/token")
            addProperty("exp", exp)
            addProperty("iat", now)
        }
        val claimsBase64 = base64UrlEncode(gson.toJson(claims).toByteArray(Charsets.UTF_8))

        // 3. Signature
        val signatureInput = "$headerBase64.$claimsBase64"
        val privateKey = parsePrivateKey(privateKeyString)
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signatureInput.toByteArray(Charsets.UTF_8))
        }.sign()
        val signatureBase64 = base64UrlEncode(signature)

        val jwtAssertion = "$signatureInput.$signatureBase64"

        // 4. Request OAuth2 token
        val formBody = FormBody.Builder()
            .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            .add("assertion", jwtAssertion)
            .build()

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(formBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty token response")

        val tokenJson = gson.fromJson(responseBody, JsonObject::class.java)
        return tokenJson.get("access_token")?.asString
            ?: throw IllegalStateException("No access_token in response: $responseBody")
    }

    private fun parsePrivateKey(pem: String): PrivateKey {
        val cleanKey = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")

        val keyBytes = Base64.decode(cleanKey, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
