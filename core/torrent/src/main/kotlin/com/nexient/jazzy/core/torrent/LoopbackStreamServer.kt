package com.nexient.jazzy.core.torrent

import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secondary bridge (spec §5.9.2, P1): serves open streams over HTTP on `127.0.0.1` so an external
 * player can play them ("Open in external player"). Random port, random path token (other apps on
 * the device can reach loopback, the token keeps them out) and `Range` support.
 *
 * URLs look like `http://127.0.0.1:<port>/<token>/<infoHash>/<fileIndex>/<fileName>`. The file name
 * is cosmetic (players use it to guess the container).
 */
@Singleton
class LoopbackStreamServer @Inject constructor(private val files: TorrentFileProvider) {
    private val token: String = ByteArray(16).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
    private var server: Server? = null

    val isRunning: Boolean @Synchronized get() = server?.isAlive == true

    /** Starts (once) and returns the URL for [stream]. Blocking socket bind: call off the main thread. */
    @Synchronized
    fun urlFor(stream: TorrentStream): String {
        val running = server?.takeIf { it.isAlive } ?: Server().also {
            it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, /* daemon = */ true)
            server = it
        }
        val name = android.net.Uri.encode(stream.fileName)
        return "http://$HOST:${running.listeningPort}/$token/${stream.infoHash}/${stream.fileIndex}/$name"
    }

    @Synchronized
    fun stop() {
        server?.stop()
        server = null
    }

    private inner class Server : NanoHTTPD(HOST, 0) {
        override fun serve(session: IHTTPSession): Response {
            if (session.method != Method.GET && session.method != Method.HEAD) {
                return text(Response.Status.METHOD_NOT_ALLOWED, "Method not allowed")
            }
            val parts = session.uri.trim('/').split('/')
            if (parts.size < 3 || parts[0] != token) return text(Response.Status.FORBIDDEN, "Forbidden")
            val fileIndex = parts[2].toIntOrNull() ?: return text(Response.Status.NOT_FOUND, "Not found")
            val access = files.openFile(parts[1], fileIndex) ?: return text(Response.Status.NOT_FOUND, "No open stream")
            val size = access.fileSize
            val mime = FileSelector.mimeTypeFor(parts.getOrNull(3).orEmpty()) ?: "application/octet-stream"

            val range = parseRange(session.headers["range"], size)
            if (range == INVALID) {
                access.close()
                return text(Response.Status.RANGE_NOT_SATISFIABLE, "Bad range").apply {
                    addHeader("Content-Range", "bytes */$size")
                }
            }
            val (start, end) = range ?: (0L to size - 1)
            val length = end - start + 1
            access.seek(start)
            val body = if (session.method == Method.HEAD) {
                access.close()
                ByteArrayInputStream(ByteArray(0)) // InputStream.nullInputStream() needs API 33
            } else {
                AccessInputStream(access, start, length)
            }
            val status = if (range != null) Response.Status.PARTIAL_CONTENT else Response.Status.OK
            return newFixedLengthResponse(status, mime, body, length).apply {
                addHeader("Accept-Ranges", "bytes")
                if (range != null) addHeader("Content-Range", "bytes $start-$end/$size")
            }
        }

        private fun text(status: Response.Status, message: String) = newFixedLengthResponse(status, MIME_PLAINTEXT, message)
    }

    /** Blocking stream over a [TorrentFileAccess]; NanoHTTPD closes it when the client goes away. */
    private class AccessInputStream(private val access: TorrentFileAccess, start: Long, length: Long) : InputStream() {
        private var position = start
        private val end = start + length

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) <= 0) -1 else one[0].toInt() and 0xff
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (position >= end) return -1
            val n = access.read(position, b, off, minOf(len.toLong(), end - position).toInt(), READ_TIMEOUT_MS)
            if (n < 0) return -1
            position += n
            return n
        }

        override fun close() = access.close()
    }

    companion object {
        const val HOST = "127.0.0.1"
        private const val READ_TIMEOUT_MS = 60_000L
        private val INVALID = -1L to -1L

        /**
         * Parses a single `bytes=a-b`, `bytes=a-` or `bytes=-n` range. Returns null when absent
         * (whole file) and [INVALID] when unsatisfiable.
         */
        internal fun parseRange(header: String?, size: Long): Pair<Long, Long>? {
            if (header.isNullOrBlank()) return null
            val spec = header.trim().removePrefix("bytes=").substringBefore(',').trim()
            val dash = spec.indexOf('-')
            if (!header.trim().startsWith("bytes=") || dash < 0) return INVALID
            val a = spec.substring(0, dash).trim()
            val b = spec.substring(dash + 1).trim()
            return try {
                when {
                    a.isEmpty() -> {
                        val suffix = b.toLong()
                        if (suffix <= 0) INVALID else maxOf(0, size - suffix) to size - 1
                    }
                    else -> {
                        val start = a.toLong()
                        val end = if (b.isEmpty()) size - 1 else minOf(b.toLong(), size - 1)
                        if (start >= size || start > end) INVALID else start to end
                    }
                }
            } catch (e: NumberFormatException) {
                INVALID
            }
        }
    }
}
