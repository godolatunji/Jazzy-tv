package com.nexient.jazzy.core.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/** TMDB v3, authenticated with the v4 read access token (spec §6.1). */
interface TmdbApi {
    @GET("configuration")
    suspend fun configuration(): ConfigurationDto

    @GET("genre/{type}/list")
    suspend fun genres(@Path("type") type: String, @Query("language") language: String): GenreListDto

    @GET("trending/{type}/{window}")
    suspend fun trending(
        @Path("type") type: String,
        @Path("window") window: String,
        @Query("language") language: String,
        @Query("page") page: Int,
    ): PageDto<MediaItemDto>

    /** `movie/popular`, `tv/top_rated`, `movie/upcoming`, … */
    @GET("{type}/{list}")
    suspend fun list(
        @Path("type") type: String,
        @Path("list") list: String,
        @Query("language") language: String,
        @Query("region") region: String?,
        @Query("page") page: Int,
    ): PageDto<MediaItemDto>

    @GET("discover/{type}")
    suspend fun discover(
        @Path("type") type: String,
        @Query("language") language: String,
        @Query("page") page: Int,
        @QueryMap filters: Map<String, String>,
    ): PageDto<MediaItemDto>

    @GET("{type}/{id}")
    suspend fun details(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("language") language: String,
        @Query("append_to_response") append: String,
        @Query("include_image_language") imageLanguages: String,
    ): DetailsDto

    @GET("tv/{id}/season/{season}")
    suspend fun season(@Path("id") id: Int, @Path("season") season: Int, @Query("language") language: String): SeasonDto

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("page") page: Int,
        @Query("include_adult") includeAdult: Boolean = false,
    ): PageDto<MediaItemDto>

    @GET("person/{id}/combined_credits")
    suspend fun personCredits(@Path("id") id: Int, @Query("language") language: String): CombinedCreditsDto

    @GET("{type}/{id}/release_dates")
    suspend fun releaseDates(@Path("type") type: String, @Path("id") id: Int): ReleaseDatesDto

    @GET("tv/{id}/content_ratings")
    suspend fun contentRatings(@Path("id") id: Int): ContentRatingsDto

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val MOVIE_APPEND = "videos,credits,images,release_dates,external_ids,recommendations,similar"
        const val TV_APPEND = "videos,credits,images,content_ratings,external_ids,recommendations,similar"
    }
}
