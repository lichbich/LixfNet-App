package com.lixfnet.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val KKPHIM_BASE_URL = "https://phimapi.com/"
    private const val LIXFNET_WEB_BASE_URL = "https://lixfnet.vercel.app/"
    const val IMAGE_BASE_URL = "https://phimimg.com/"

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    val kkPhimApi: KKPhimApi by lazy {
        Retrofit.Builder()
            .baseUrl(KKPHIM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KKPhimApi::class.java)
    }

    val lixfNetWebApi: LixfNetWebApi by lazy {
        Retrofit.Builder()
            .baseUrl(LIXFNET_WEB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LixfNetWebApi::class.java)
    }

    /**
     * Resolves CDN image URLs from KKPhim API.
     * If relative path, prepends `https://phimimg.com/`.
     */
    fun formatImageUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            IMAGE_BASE_URL + url.trimStart('/')
        }
    }
}
