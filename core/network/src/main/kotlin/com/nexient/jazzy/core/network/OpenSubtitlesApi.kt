package com.nexient.jazzy.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** OpenSubtitles.com REST API v1 (spec PL-8). Needs a consumer API key; login is optional. */
interface OpenSubtitlesApi {
    /** Movies: `imdb_id`. Episodes: `parent_imdb_id` + `season_number` + `episode_number`. IMDb IDs are numeric (no "tt"). */
    @GET("subtitles")
    suspend fun search(
        @Query("imdb_id") imdbId: Int? = null,
        @Query("parent_imdb_id") parentImdbId: Int? = null,
        @Query("season_number") season: Int? = null,
        @Query("episode_number") episode: Int? = null,
        @Query("languages") languages: String,
        @Query("order_by") orderBy: String = "download_count",
    ): OsSearchResponse

    @POST("login")
    suspend fun login(@Body body: OsLoginRequest): OsLoginResponse

    @POST("download")
    suspend fun download(@Body body: OsDownloadRequest, @Header("Authorization") bearer: String?): OsDownloadResponse

    companion object {
        const val BASE_URL = "https://api.opensubtitles.com/api/v1/"
        val apiKey: String get() = BuildConfig.OPENSUBTITLES_API_KEY
        val isConfigured: Boolean get() = apiKey.isNotBlank()
    }
}

@Serializable
data class OsSearchResponse(@SerialName("total_count") val totalCount: Int = 0, val data: List<OsSubtitle> = emptyList())

@Serializable
data class OsSubtitle(val id: String, val attributes: OsAttributes)

@Serializable
data class OsAttributes(
    val language: String? = null,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("hearing_impaired") val hearingImpaired: Boolean = false,
    val fps: Double? = null,
    val release: String? = null,
    @SerialName("from_trusted") val fromTrusted: Boolean? = null,
    @SerialName("ai_translated") val aiTranslated: Boolean? = null,
    @SerialName("machine_translated") val machineTranslated: Boolean? = null,
    val files: List<OsFile> = emptyList(),
)

@Serializable
data class OsFile(@SerialName("file_id") val fileId: Int, @SerialName("file_name") val fileName: String? = null)

@Serializable
data class OsLoginRequest(val username: String, val password: String)

@Serializable
data class OsLoginResponse(val token: String? = null, val status: Int? = null)

@Serializable
data class OsDownloadRequest(@SerialName("file_id") val fileId: Int, @SerialName("sub_format") val format: String = "srt")

@Serializable
data class OsDownloadResponse(
    val link: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    val remaining: Int? = null,
    val message: String? = null,
)

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenSubtitlesHttpClient

@Module
@InstallIn(SingletonComponent::class)
object OpenSubtitlesModule {
    @Provides
    @Singleton
    @OpenSubtitlesHttpClient
    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(
            Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Api-Key", OpenSubtitlesApi.apiKey)
                        .header("User-Agent", "Jazzy v0.1")
                        .header("Accept", "application/json")
                        .build(),
                )
            },
        )
        .build()

    @Provides
    @Singleton
    fun api(@OpenSubtitlesHttpClient client: OkHttpClient, json: Json): OpenSubtitlesApi = Retrofit.Builder()
        .baseUrl(OpenSubtitlesApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenSubtitlesApi::class.java)
}
