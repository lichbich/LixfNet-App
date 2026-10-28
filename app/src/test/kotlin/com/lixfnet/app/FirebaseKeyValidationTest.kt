package com.lixfnet.app

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

class FirebaseKeyValidationTest {

    @Test
    fun testFirebaseKeyExchangeAndFetch() {
        // Read key from root firebasekey.json
        val keyFile = File("../firebasekey.json").takeIf { it.exists() } ?: File("firebasekey.json")
        assertTrue("firebasekey.json should exist", keyFile.exists())

        val jsonString = keyFile.readText()
        val gson = Gson()
        val json = gson.fromJson(jsonString, JsonObject::class.java)

        val clientEmail = json.get("client_email").asString
        val privateKeyString = json.get("private_key").asString
        val projectId = json.get("project_id").asString

        println("Testing with clientEmail: $clientEmail, projectId: $projectId")

        // 1. JWT Header
        val header = JsonObject().apply {
            addProperty("alg", "RS256")
            addProperty("typ", "JWT")
        }
        val headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(header.toString().toByteArray())

        // 2. JWT Claims
        val now = System.currentTimeMillis() / 1000
        val claims = JsonObject().apply {
            addProperty("iss", clientEmail)
            addProperty("scope", "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/firebase.database")
            addProperty("aud", "https://oauth2.googleapis.com/token")
            addProperty("exp", now + 3600)
            addProperty("iat", now)
        }
        val claimsB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toString().toByteArray())

        // 3. Signature
        val signatureInput = "$headerB64.$claimsB64"
        val cleanPem = privateKeyString
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val keyBytes = Base64.getDecoder().decode(cleanPem)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        val privateKey: PrivateKey = kf.generatePrivate(spec)

        val sig = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signatureInput.toByteArray())
        }.sign()
        val sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(sig)

        val assertion = "$signatureInput.$sigB64"

        // 4. Request Google OAuth2 Token
        val client = OkHttpClient()
        val form = FormBody.Builder()
            .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            .add("assertion", assertion)
            .build()

        val tokenRequest = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(form)
            .build()

        val tokenResponse = client.newCall(tokenRequest).execute()
        val tokenBody = tokenResponse.body?.string() ?: ""
        println("OAuth response code: ${tokenResponse.code}, body: $tokenBody")
        assertTrue("OAuth request must succeed", tokenResponse.isSuccessful)

        val tokenJson = gson.fromJson(tokenBody, JsonObject::class.java)
        val accessToken = tokenJson.get("access_token")?.asString
        assertNotNull("accessToken must not be null", accessToken)
        println("Successfully retrieved Google OAuth2 Access Token!")

        // 5. Test Firebase RTDB Read
        val rtdbUrl = "https://$projectId-default-rtdb.firebaseio.com/lixfnet/users/tronglich/watchProgress.json?access_token=$accessToken"
        val rtdbRequest = Request.Builder()
            .url(rtdbUrl)
            .get()
            .build()

        val rtdbResponse = client.newCall(rtdbRequest).execute()
        val rtdbBody = rtdbResponse.body?.string() ?: ""
        println("watchProgress response code: ${rtdbResponse.code}, body: $rtdbBody")

        // 6. Test PUT progress
        val testPayload = JsonObject().apply {
            addProperty("movieId", "test-movie-id")
            addProperty("movieSlug", "test-movie-slug")
            addProperty("currentTime", 120)
            addProperty("duration", 3600)
            addProperty("progressPercent", 3)
            addProperty("updatedAt", System.currentTimeMillis())
        }
        val putUrl = "https://$projectId-default-rtdb.firebaseio.com/lixfnet/users/tronglich/watchProgress/test-movie-slug.json?access_token=$accessToken"
        val putReq = Request.Builder()
            .url(putUrl)
            .put(testPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val putRes = client.newCall(putReq).execute()
        val putBody = putRes.body?.string() ?: ""
        println("Test PUT response code: ${putRes.code}, body: $putBody")
    }
}
