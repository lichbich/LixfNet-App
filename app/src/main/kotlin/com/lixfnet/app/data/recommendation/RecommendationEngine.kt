package com.lixfnet.app.data.recommendation

import com.lixfnet.app.data.model.Movie
import com.lixfnet.app.ui.components.RowType

data class UserTasteProfile(
    val topCountries: Map<String, Float> = emptyMap(),
    val topGenres: Map<String, Float> = emptyMap(),
    val favoriteCountrySlug: String? = null,
    val favoriteGenreSlugs: List<String> = emptyList(),
    val totalWatched: Int = 0
)

data class CandidateRow(
    val id: String,
    val title: String,
    val countrySlug: String? = null,
    val genreSlug: String? = null,
    val type: RowType = RowType.NORMAL,
    val movies: List<Movie>,
    val priorityBoost: Int = 0
)

object RecommendationEngine {

    /**
     * Builds a personalized taste profile from continue-watching movies
     * using recency weighting:
     * - Top 3 recent watches: 1.5x
     * - Next 5 watches: 1.2x
     * - Others: 1.0x
     */
    fun buildTasteProfile(
        continueWatchingMovies: List<Movie>
    ): UserTasteProfile {
        if (continueWatchingMovies.isNullOrEmpty()) {
            return UserTasteProfile()
        }

        val topCountries = mutableMapOf<String, Float>()
        val topGenres = mutableMapOf<String, Float>()

        for ((index, movie) in continueWatchingMovies.withIndex()) {
            val recencyWeight = when {
                index < 3 -> 1.5f
                index < 8 -> 1.2f
                else -> 1.0f
            }

            // Track Country
            val rawCountry = movie.countrySlug.orEmpty()
            val countrySlug = if (rawCountry.isNotBlank()) rawCountry else {
                val cJoined = (movie.countries ?: emptyList()).filterNotNull().joinToString(" ").lowercase()
                when {
                    cJoined.contains("hàn") -> "han-quoc"
                    cJoined.contains("mỹ") || cJoined.contains("âu") -> "my"
                    cJoined.contains("việt") -> "viet-nam"
                    cJoined.contains("nhật") -> "nhat-ban"
                    cJoined.contains("thái") -> "thai-lan"
                    else -> ""
                }
            }
            if (countrySlug.isNotBlank()) {
                topCountries[countrySlug] = (topCountries[countrySlug] ?: 0f) + 3f * recencyWeight
            }

            // Track Genres
            val genresList = (movie.genres ?: emptyList()).filterNotNull()
            for (g in genresList) {
                if (g.isBlank()) continue
                val lower = g.lowercase().trim()
                val gSlug = when {
                    lower.contains("tình cảm") || lower.contains("lãng mạn") -> "tinh-cam"
                    lower.contains("kinh dị") || lower.contains("giật gân") || lower.contains("ma") -> "kinh-di"
                    lower.contains("hành động") || lower.contains("võ thuật") -> "hanh-dong"
                    lower.contains("hoạt hình") || lower.contains("anime") -> "hoat-hinh"
                    lower.contains("hài") -> "phim-hai"
                    lower.contains("tâm lý") || lower.contains("chính kịch") -> "tam-ly"
                    lower.contains("viễn tưởng") -> "vien-tuong"
                    else -> null
                }
                if (gSlug != null) {
                    topGenres[gSlug] = (topGenres[gSlug] ?: 0f) + 2.5f * recencyWeight
                }
            }
        }

        // Top favorite country
        val favoriteCountry = topCountries.maxByOrNull { it.value }?.key

        // Top favorite genres (up to 3)
        val favoriteGenres = topGenres.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        return UserTasteProfile(
            topCountries = topCountries,
            topGenres = topGenres,
            favoriteCountrySlug = favoriteCountry,
            favoriteGenreSlugs = favoriteGenres,
            totalWatched = continueWatchingMovies.size
        )
    }

    /**
     * Curates a personalized recommendation list matching the user's top genres & countries.
     */
    fun curatePersonalizedRecommendations(
        allPoolMovies: List<Movie>,
        continueWatchingMovies: List<Movie>,
        profile: UserTasteProfile,
        limit: Int = 12
    ): List<Movie> {
        if (profile.totalWatched == 0 || allPoolMovies.isNullOrEmpty()) return emptyList()

        val watchedIds = continueWatchingMovies.mapNotNull {
            val key = it.slug.orEmpty().ifBlank { it.id.orEmpty() }
            key.ifBlank { null }
        }.toSet()

        val candidateMovies = allPoolMovies.filter {
            val key = it.slug.orEmpty().ifBlank { it.id.orEmpty() }
            key.isNotBlank() && !watchedIds.contains(key)
        }.distinctBy { it.slug.orEmpty().ifBlank { it.id.orEmpty() } }

        return candidateMovies.map { movie ->
            var score = 0f
            val rawCountry = movie.countrySlug.orEmpty()
            val countries = (movie.countries ?: emptyList()).filterNotNull()
            if (profile.favoriteCountrySlug != null &&
                (rawCountry == profile.favoriteCountrySlug || countries.any { it.contains(profile.favoriteCountrySlug, ignoreCase = true) })
            ) {
                score += 30f
            }

            val genresList = (movie.genres ?: emptyList()).filterNotNull()
            for (g in genresList) {
                val lower = g.lowercase()
                profile.favoriteGenreSlugs.forEachIndexed { index, targetSlug ->
                    val match = when (targetSlug) {
                        "tinh-cam" -> lower.contains("tình cảm") || lower.contains("lãng mạn")
                        "kinh-di" -> lower.contains("kinh dị") || lower.contains("giật gân")
                        "hanh-dong" -> lower.contains("hành động") || lower.contains("võ thuật")
                        "hoat-hinh" -> lower.contains("hoạt hình") || lower.contains("anime")
                        "phim-hai" -> lower.contains("hài")
                        else -> false
                    }
                    if (match) {
                        score += (25f - index * 6f)
                    }
                }
            }
            Pair(movie, score)
        }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Ranks homepage rows dynamically:
     * - Favorite Country row gets +100 boost
     * - Top 1 genre gets +80, Top 2 gets +60, Top 3 gets +40
     */
    fun rankHomepageRows(
        rows: List<CandidateRow>,
        profile: UserTasteProfile
    ): List<CandidateRow> {
        if (rows.isEmpty()) return emptyList()

        val validRows = rows.filter { it.movies.isNotEmpty() }
        if (profile.totalWatched == 0) {
            return validRows.sortedByDescending { it.priorityBoost }
        }

        return validRows.sortedByDescending { row ->
            var score = row.priorityBoost

            // Country boost
            if (row.countrySlug != null && row.countrySlug == profile.favoriteCountrySlug) {
                score += 100
            }

            // Genre boost
            if (row.genreSlug != null) {
                when (row.genreSlug) {
                    profile.favoriteGenreSlugs.getOrNull(0) -> score += 80
                    profile.favoriteGenreSlugs.getOrNull(1) -> score += 60
                    profile.favoriteGenreSlugs.getOrNull(2) -> score += 40
                }
            }

            score
        }
    }
}
