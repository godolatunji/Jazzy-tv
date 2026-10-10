package com.nexient.jazzy.core.sources.engine

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Base of every engine failure. Messages never contain full URLs or cookies (at most a host), so
 * they're safe to show and to store as a provider's "last error".
 */
internal open class ProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal class HttpStatusException(val code: Int, url: HttpUrl) : ProviderException("HTTP $code from ${url.host}")

/** The site answered with a Cloudflare (or similar) challenge. We never try to solve it. */
internal class BlockedException(provider: String) :
    ProviderException("$provider answered with a bot challenge")

internal class LoginRequiredException(provider: String) :
    ProviderException("Can't login: missing credentials for $provider")

/** The response wasn't in the shape the provider expects (JSON API changed, HTML page instead of JSON...). */
internal class ParseException(message: String, cause: Throwable? = null) : ProviderException(message, cause)

internal object EngineHttp {
    /** Desktop browser User-Agent: several sites serve a stripped or blocked page to unknown agents. */
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"

    /** Client used by `:core:sources` (exposed through Hilt as `@SourcesHttpClient`). */
    fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor(UserAgentInterceptor(USER_AGENT))
        .build()
}

/** Adds a User-Agent to requests that don't set one themselves. */
internal class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("User-Agent") != null) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("User-Agent", userAgent).build())
    }
}

/** Suspends until the call completes; cancelling the coroutine cancels the call. */
internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, _, _ -> response.close() }
        }
    })
}

/** Per-provider in-memory cookie store, the equivalent of `request.jar()` in the original. */
internal class MemoryCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { add(it) }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        this.cookies.removeAll { it.expiresAt < now }
        return this.cookies.filter { it.matches(url) }
    }

    @Synchronized
    fun add(cookie: Cookie) {
        cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
        cookies += cookie
    }

    @Synchronized
    fun clear() = cookies.clear()
}
