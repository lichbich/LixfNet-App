package com.lixfnet.app.data.api

import com.lixfnet.app.data.model.KKDetailResponse
import com.lixfnet.app.data.model.KKListResponse
import com.lixfnet.app.data.model.LixfNetTop10Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KKPhimApi {

    @GET("v1/api/danh-sach/phim-moi-cap-nhat")
    suspend fun getNewUpdatedMovies(
        @Query("page") page: Int = 1
    ): KKListResponse

    @GET("v1/api/danh-sach/phim-bo")
    suspend fun getSeriesMovies(
        @Query("page") page: Int = 1
    ): KKListResponse

    @GET("v1/api/danh-sach/phim-le")
    suspend fun getSingleMovies(
        @Query("page") page: Int = 1
    ): KKListResponse

    @GET("v1/api/danh-sach/hoat-hinh")
    suspend fun getAnimeMovies(
        @Query("page") page: Int = 1
    ): KKListResponse

    @GET("v1/api/danh-sach/tv-shows")
    suspend fun getTvShows(
        @Query("page") page: Int = 1
    ): KKListResponse

    @GET("v1/api/quoc-gia/{slug}")
    suspend fun getMoviesByCountry(
        @Path("slug") slug: String,
        @Query("page") page: Int = 1
    ): KKListResponse

    @GET("v1/api/the-loai/{slug}")
    suspend fun getMoviesByGenre(
        @Path("slug") slug: String,
        @Query("page") page: Int = 1
    ): KKListResponse

    @GET("v1/api/the-loai/{slug}")
    suspend fun getMoviesByCategory(
        @Path("slug") slug: String,
        @Query("page") page: Int = 1
    ): KKListResponse

    @GET("v1/api/tim-kiem")
    suspend fun searchMovies(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24
    ): KKListResponse

    @GET("phim/{slug}")
    suspend fun getMovieDetail(
        @Path("slug") slug: String
    ): KKDetailResponse
}

interface LixfNetWebApi {
    @GET("api/netflix-top10")
    suspend fun getNetflixTop10(): LixfNetTop10Response
}
