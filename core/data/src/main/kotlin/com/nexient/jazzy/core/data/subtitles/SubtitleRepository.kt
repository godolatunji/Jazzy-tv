package com.nexient.jazzy.core.data.subtitles

import android.content.Context
import com.nexient.jazzy.core.common.IoDispatcher
import com.nexient.jazzy.core.data.settings.SecretCipher
import com.nexient.jazzy.core.data.settings.SettingsRepository
import com.nexient.jazzy.core.network.OpenSubtitlesApi
import com.nexient.jazzy.core.network.OpenSubtitlesHttpClient
import com.nexient.jazzy.core.network.OsDownloadRequest
import com.nexient.jazzy.core.network.OsLoginRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** One subtitle file found online. */
data class OnlineSubtitle(
    val fileId: Int,
    val language: String,
    val release: String,
    val downloads: Int,
    val hearingImpaired: Boolean,
    val machineTranslated: Boolean,
)

class SubtitleException(message: String) : Exception(message)

/** OpenSubtitles search and download (spec PL-8, plan E10-13). Files are cached in `cacheDir/subtitles`. */
@Singleton
class SubtitleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: OpenSubtitlesApi,
    @OpenSubtitlesHttpClient private val http: OkHttpClient,
    private val settings: SettingsRepository,
    private val cipher: SecretCipher,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    val isConfigured: Boolean get() = OpenSubtitlesApi.isConfigured

    private val dir: File get() = File(context.cacheDir, "subtitles").apply { mkdirs() }

    /**
     * @param imdbId the movie's or the show's IMDb ID ("tt…"); for an episode pass the show ID plus season/episode.
     * @param languages ISO 639-1 codes, most wanted first.
     */
    suspend fun search(imdbId: String, season: Int?, episode: Int?, languages: List<String>): List<OnlineSubtitle> = withContext(io) {
        if (!isConfigured) throw SubtitleException("Online subtitles need an OpenSubtitles API key (OPENSUBTITLES_API_KEY in local.properties).")
        val numeric = imdbId.removePrefix("tt").toIntOrNull() ?: throw SubtitleException("This title has no IMDb ID, so subtitles can't be looked up.")
        val langs = languages.ifEmpty { listOf("en") }.joinToString(",")
        val response = try {
            if (season != null && episode != null) api.search(parentImdbId = numeric, season = season, episode = episode, languages = langs)
            else api.search(imdbId = numeric, languages = langs)
        } catch (e: HttpException) {
            throw SubtitleException(httpMessage(e))
        }
        response.data.mapNotNull { s ->
            val file = s.attributes.files.firstOrNull() ?: return@mapNotNull null
            OnlineSubtitle(
                fileId = file.fileId,
                language = s.attributes.language ?: "?",
                release = s.attributes.release ?: file.fileName ?: "Subtitle",
                downloads = s.attributes.downloadCount,
                hearingImpaired = s.attributes.hearingImpaired,
                machineTranslated = s.attributes.aiTranslated == true || s.attributes.machineTranslated == true,
            )
        }.sortedWith(compareBy<OnlineSubtitle> { languages.indexOf(it.language).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenByDescending { it.downloads })
    }

    /** Downloads (or reuses) the SRT for [fileId] and returns the local file. */
    suspend fun download(fileId: Int): File = withContext(io) {
        val target = File(dir, "$fileId.srt")
        if (target.exists() && target.length() > 0) return@withContext target
        val token = settings.get(TOKEN_KEY)?.let(cipher::decrypt)
        val link = try {
            api.download(OsDownloadRequest(fileId), token?.let { "Bearer $it" })
        } catch (e: HttpException) {
            throw SubtitleException(httpMessage(e))
        }.link ?: throw SubtitleException("OpenSubtitles didn't return a download link.")
        http.newCall(Request.Builder().url(link).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw SubtitleException("Subtitle download failed (HTTP ${resp.code}).")
            target.writeBytes(resp.body.bytes())
        }
        target
    }

    /** Writes a copy of [file] with every cue shifted by [offsetMs] (sync offset control). */
    suspend fun shifted(file: File, offsetMs: Long): File = withContext(io) {
        if (offsetMs == 0L) return@withContext file
        val out = File(dir, "${file.nameWithoutExtension.substringBefore("_shift")}_shift$offsetMs.srt")
        out.writeText(SrtShifter.shift(file.readText(), offsetMs))
        out
    }

    suspend fun login(username: String, password: String) = withContext(io) {
        if (!isConfigured) throw SubtitleException("Add an OpenSubtitles API key first.")
        val token = try {
            api.login(OsLoginRequest(username.trim(), password)).token
        } catch (e: HttpException) {
            throw SubtitleException(httpMessage(e))
        } ?: throw SubtitleException("Login failed.")
        settings.put(TOKEN_KEY, cipher.encrypt(token))
        settings.put(USER_KEY, username.trim())
    }

    suspend fun logout() {
        settings.put(TOKEN_KEY, null)
        settings.put(USER_KEY, null)
    }

    suspend fun loggedInUser(): String? = settings.get(USER_KEY)

    private fun httpMessage(e: HttpException) = when (e.code()) {
        401 -> "OpenSubtitles rejected the API key or login."
        406, 429 -> "OpenSubtitles download limit reached. Log in under Settings → Subtitles for a higher limit, or try later."
        else -> "OpenSubtitles error (HTTP ${e.code()})."
    }

    private companion object {
        const val TOKEN_KEY = "opensubtitles_token"
        const val USER_KEY = "opensubtitles_user"
    }
}

/** Shifts SRT timestamps (`00:01:02,345 --> 00:01:04,000`). Negative results clamp to zero. */
object SrtShifter {
    private val time = Regex("""(\d{1,2}):(\d{2}):(\d{2})[,.](\d{3})""")

    fun shift(srt: String, offsetMs: Long): String = srt.lineSequence().joinToString("\n") { line ->
        if ("-->" !in line) line
        else time.replace(line) { m ->
            val (h, mi, s, ms) = m.destructured
            val total = (h.toLong() * 3_600_000 + mi.toLong() * 60_000 + s.toLong() * 1_000 + ms.toLong() + offsetMs).coerceAtLeast(0)
            format(total)
        }
    }

    fun format(ms: Long): String = "%02d:%02d:%02d,%03d".format(ms / 3_600_000, (ms / 60_000) % 60, (ms / 1_000) % 60, ms % 1_000)
}
