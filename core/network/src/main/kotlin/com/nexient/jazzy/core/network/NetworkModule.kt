package com.nexient.jazzy.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbHttpClient

/** Shared lenient JSON for TMDB and for cached payloads. */
val JazzyJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = JazzyJson

    @Provides
    @Singleton
    @TmdbHttpClient
    fun tmdbClient(): OkHttpClient = OkHttpClient.Builder()
        // At most 8 concurrent TMDB requests (spec §6.3).
        .dispatcher(Dispatcher().apply { maxRequests = 8; maxRequestsPerHost = 8 })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(TmdbAuthInterceptor(BuildConfig.TMDB_TOKEN))
        .addInterceptor(RetryAfterInterceptor())
        .build()

    @Provides
    @Singleton
    fun tmdbApi(@TmdbHttpClient client: OkHttpClient, json: Json): TmdbApi = Retrofit.Builder()
        .baseUrl(TmdbApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TmdbApi::class.java)
}

class TmdbAuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build(),
    )
}

/** Retries HTTP 429 with exponential backoff, honouring `Retry-After` (spec §6.3). */
class RetryAfterInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response = chain.proceed(chain.request())
        var attempt = 0
        while (response.code == 429 && attempt < maxRetries) {
            val waitSeconds = response.header("Retry-After")?.toLongOrNull() ?: (1L shl attempt)
            response.close()
            Thread.sleep(waitSeconds.coerceIn(1, 10) * 1000)
            attempt++
            response = chain.proceed(chain.request())
        }
        return response
    }
}
