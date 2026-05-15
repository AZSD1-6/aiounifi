package com.anthropic.claudemonitor.data.auth

import android.content.Context
import com.anthropic.claudemonitor.data.api.ClaudeAiApi
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://claude.ai/api/"

object ClaudeAiClientFactory {

    fun create(context: Context): ClaudeAiApi {
        val session = SessionManager.getInstance(context)

        val cookieJar = object : CookieJar {
            private val store = mutableMapOf<String, List<Cookie>>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                store[url.host] = cookies
                // Persist the session cookie string for reuse across app restarts
                val cookieHeader = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                if (cookieHeader.isNotBlank()) session.sessionCookie = cookieHeader
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val stored = store[url.host]
                if (!stored.isNullOrEmpty()) return stored
                // Reconstruct from persisted string
                val raw = session.sessionCookie ?: return emptyList()
                return raw.split("; ").mapNotNull { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        Cookie.Builder()
                            .name(parts[0].trim())
                            .value(parts[1].trim())
                            .domain(url.host)
                            .build()
                    } else null
                }
            }
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "ClaudeMonitor/1.0 Android")
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .apply {
                        session.sessionToken?.let { header("Authorization", "Bearer $it") }
                    }
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClaudeAiApi::class.java)
    }
}
