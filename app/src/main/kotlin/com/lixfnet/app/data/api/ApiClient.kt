package com.lixfnet.app.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val KKPHIM_BASE_URL = "https://phimapi.com/"
    private const val LIXFNET_WEB_BASE_URL = "https://lixfnet.vercel.app/"
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    const val IMAGE_BASE_URL = "https://phimimg.com/"
    const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

    const val TMDB_API_KEY = "58dba4066f4f36e49e5e769304442088"
    const val TMDB_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI1OGRiYTQwNjZmNGYzNmU0OWU1ZTc2OTMwNDQ0MjA4OCIsIm5iZiI6MTc5MjU2MDgwMC41NzQwMDAxLCJzdWIiOiI2YWQ4NGVhMGExNDUxM2RiMTllYzMzODgiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.McbFdADhs0GuuwFrSNhR45TqDL1LbjWv_fH1ecHwziE"

    val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private val tmdbHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer $TMDB_ACCESS_TOKEN")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
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

    val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(tmdbHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    /**
     * Resolves CDN image URLs from KKPhim API or TMDB.
     * Distinguishes KKPhim upload paths from TMDB image paths.
     */
    fun formatImageUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val trimmed = url.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        val clean = trimmed.trimStart('/')
        if (clean.startsWith("upload/") || clean.startsWith("uploads/")) {
            return IMAGE_BASE_URL + clean
        }
        // TMDB image filename (e.g. /sGuCmIy8kEmx3FWZyMj654rF6g8.jpg)
        return formatTmdbPoster(trimmed)
    }

    /**
     * Formats official TMDB poster image with w500 width
     */
    fun formatTmdbPoster(posterPath: String?): String {
        if (posterPath.isNullOrBlank()) return ""
        val trimmed = posterPath.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val clean = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return "${TMDB_IMAGE_BASE_URL}w500$clean"
    }

    /**
     * Formats official TMDB backdrop image with original high quality
     */
    fun formatTmdbBackdrop(backdropPath: String?): String {
        if (backdropPath.isNullOrBlank()) return ""
        val trimmed = backdropPath.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val clean = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return "${TMDB_IMAGE_BASE_URL}original$clean"
    }
}
