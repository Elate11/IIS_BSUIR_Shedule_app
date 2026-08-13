package com.example.schedule

import android.content.Context
import okhttp3.*
import okhttp3.Protocol
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Singleton HTTP client shared across the app.
 * - Uses CookieJar for automatic cookie persistence (like iOS URLSession)
 * - 60-second timeouts
 * - Retry on connection failure
 * - Proper headers (User-Agent, Accept, Content-Type)
 * - Auto re-login on 401 Unauthorized
 */
object NetworkClient {

    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            cookieStore.getOrPut(host) { mutableListOf() }.apply {
                // Remove old cookies with same name before adding new ones
                val newNames = cookies.map { it.name }.toSet()
                removeAll { it.name in newNames }
                addAll(cookies)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .cookieJar(cookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val BASE_URL = "https://iis.bsuir.by/api/v1"
    private const val USER_AGENT = "MyIIS/1.0 CFNetwork/1408.0.4 Darwin/22.5.0"

    /**
     * Build a GET request with proper headers and optional cookie token.
     */
    fun buildGetRequest(url: String, token: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Accept", "application/json")
            .get()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Cookie", token)
        }
        return builder.build()
    }

    /**
     * Execute a GET request with automatic retry on 401.
     * If a 401 is received, tries to re-authenticate using saved credentials.
     */
    fun executeWithAuth(request: Request, context: Context): Response {
        val response = client.newCall(request).execute()
        if (response.code == 401) {
            response.close()
            // Try to re-authenticate
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val username = prefs.getString("gradebook_number", null)
                ?: prefs.getString("gradebook", null)
            val password = prefs.getString("saved_password", null)
            if (username != null && password != null) {
                val loginSuccess = login(username, password, prefs)
                if (loginSuccess) {
                    // Rebuild request with new token
                    val newToken = prefs.getString("auth_token", "") ?: ""
                    val newRequest = request.newBuilder()
                        .removeHeader("Cookie")
                        .addHeader("Cookie", newToken)
                        .build()
                    return client.newCall(newRequest).execute()
                }
            }
            // If re-auth failed, return original 401-like response
            return client.newCall(request).execute()
        }
        return response
    }

    /**
     * Perform login and save token + credentials.
     */
    fun login(username: String, password: String, prefs: android.content.SharedPreferences): Boolean {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$BASE_URL/auth/login")
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    var extractedToken = ""

                    // Extract session cookies
                    val cookies = response.headers("Set-Cookie")
                    for (cookie in cookies) {
                        if (cookie.contains("SESSION") || cookie.contains("JSESSIONID") ||
                            cookie.contains("jwt") || cookie.contains("token")) {
                            extractedToken = cookie.substringBefore(";")
                            break
                        }
                    }
                    if (extractedToken.isEmpty()) extractedToken = responseBody ?: ""

                    // Save profile data from response
                    if (responseBody != null) {
                        try {
                            val respJson = JSONObject(responseBody)
                            prefs.edit()
                                .putString("auth_token", extractedToken)
                                .putString("login_fio", respJson.optString("fio"))
                                .putString("login_group", respJson.optString("group"))
                                .putString("login_photo", respJson.optString("photoUrl"))
                                .putString("saved_password", password)
                                .putString("gradebook_number", username)
                                .apply()
                        } catch (_: Exception) {
                            prefs.edit()
                                .putString("auth_token", extractedToken)
                                .putString("saved_password", password)
                                .putString("gradebook_number", username)
                                .apply()
                        }
                    }
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Clear cookies (used on logout) */
    fun clearCookies() {
        cookieStore.clear()
    }
}
