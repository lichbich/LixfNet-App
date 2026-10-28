package com.lixfnet.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Domain Movie Model used across all UI screens.
 */
data class Movie(
    val id: String = "",
    val title: String = "",
    val subTitle: String = "",
    val slug: String = "",
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val logoUrl: String? = null,
    val year: Int = 2026,
    val rating: String = "T16",
    val quality: String = "HD",
    val type: String = "series", // "movie", "series", "anime"
    val durationText: String = "",
    val currentEpisode: String = "",
    val isTop10: Boolean = false,
    val top10Rank: Int = 0,
    val platform: String? = null, // "netflix", "apple", "hbo", "disney"
    val platformName: String? = null, // "Netflix", "Apple TV+", "HBO"
    val ribbonBadge: String? = null, // "TẬP MỚI", "MỚI THÊM", "#1"
    val countrySlug: String = "",
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val director: List<String> = emptyList(),
    val trailerYoutubeKey: String? = null,
    val episodes: List<Episode> = emptyList(),
    val progress: Float? = null, // 0.0f to 1.0f
    val isNewEpisode: Boolean = false
) {
    fun sanitize(): Movie = copy(
        id = id ?: "",
        title = title ?: "",
        subTitle = subTitle ?: "",
        slug = slug ?: "",
        description = description ?: "",
        posterUrl = posterUrl ?: "",
        backdropUrl = backdropUrl ?: "",
        durationText = durationText ?: "",
        currentEpisode = currentEpisode ?: "",
        countrySlug = countrySlug ?: "",
        genres = (genres ?: emptyList()).filterNotNull(),
        countries = (countries ?: emptyList()).filterNotNull(),
        cast = (cast ?: emptyList()).filterNotNull(),
        director = (director ?: emptyList()).filterNotNull(),
        episodes = (episodes ?: emptyList()).filterNotNull()
    )
}

data class LixfNetTop10Response(
    @SerializedName("films") val films: List<String>?,
    @SerializedName("series") val series: List<String>?,
    @SerializedName("week") val week: String?,
    @SerializedName("source") val source: String?
)

data class Episode(
    val id: String,
    val name: String,
    val slug: String,
    val filename: String = "",
    val linkM3u8: String = "",
    val linkEmbed: String = "",
    val durationMinutes: Int = 45,
    val stillPath: String? = null,
    val synopsis: String? = null
)

data class EpisodeProgress(
    val currentTime: Long = 0L,
    val duration: Long = 0L,
    val progressPercent: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val m3u8Url: String = ""
)

data class WatchProgress(
    val movieId: String,
    val movieSlug: String,
    val episodeTitle: String,
    val currentSeconds: Long,
    val totalSeconds: Long,
    val progressPercent: Float,
    val episodeM3u8: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val episodesProgress: Map<String, EpisodeProgress> = emptyMap()
)

// --- Raw KKPhim API DTOs ---

data class KKHomeResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("items") val items: List<KKMovieItem>?
)

data class KKListResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("data") val data: KKListData?
)

data class KKListData(
    @SerializedName("items") val items: List<KKMovieItem>?,
    @SerializedName("params") val params: KKPaginationParams?
)

data class KKPaginationParams(
    @SerializedName("pagination") val pagination: KKPagination?
)

data class KKPagination(
    @SerializedName("totalItems") val totalItems: Int?,
    @SerializedName("totalItemsPerPage") val totalItemsPerPage: Int?,
    @SerializedName("currentPage") val currentPage: Int?,
    @SerializedName("pageRanges") val pageRanges: Int?
)

data class KKDetailResponse(
    @SerializedName("status") val status: Any?, // boolean or string
    @SerializedName("movie") val movie: KKMovieDetail?,
    @SerializedName("episodes") val episodes: List<KKServerEpisode>?
)

data class KKMovieItem(
    @SerializedName("_id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("origin_name") val originName: String?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    @SerializedName("poster_url") val posterUrl: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("type") val type: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("episode_current") val episodeCurrent: String?,
    @SerializedName("quality") val quality: String?,
    @SerializedName("lang") val lang: String?,
    @SerializedName("category") val category: List<KKNameItem>?,
    @SerializedName("country") val country: List<KKNameItem>?
)

data class KKMovieDetail(
    @SerializedName("_id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("origin_name") val originName: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("thumb_url") val thumbUrl: String?,
    @SerializedName("poster_url") val posterUrl: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("episode_current") val episodeCurrent: String?,
    @SerializedName("episode_total") val episodeTotal: String?,
    @SerializedName("quality") val quality: String?,
    @SerializedName("lang") val lang: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("actor") val actor: List<String>?,
    @SerializedName("director") val director: List<String>?,
    @SerializedName("category") val category: List<KKNameItem>?,
    @SerializedName("country") val country: List<KKNameItem>?
)

data class KKNameItem(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("slug") val slug: String?
)

data class KKServerEpisode(
    @SerializedName("server_name") val serverName: String?,
    @SerializedName("server_data") val serverData: List<KKEpisodeData>?
)

data class KKEpisodeData(
    @SerializedName("name") val name: String?,
    @SerializedName("slug") val slug: String?,
    @SerializedName("filename") val filename: String?,
    @SerializedName("link_embed") val linkEmbed: String?,
    @SerializedName("link_m3u8") val linkM3u8: String?
)
