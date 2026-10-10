package com.nexient.jazzy.core.torrent

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Media3 [DataSource] for `jazzytorrent://<infoHash>/<fileIndex>` (spec §5.9.2, primary bridge).
 *
 * Reads the selected file straight from disk at [DataSpec.position]. A read of bytes that aren't
 * downloaded yet blocks (TE-6) until the piece arrives, [readTimeoutMs] passes, the loader thread
 * is interrupted (ExoPlayer's `Loader` interrupts on cancel) or [close] is called. Every open and
 * read reports its position to the scheduler, which is how seeks re-prioritise pieces (TE-5).
 */
@OptIn(UnstableApi::class)
class TorrentDataSource(
    private val files: TorrentFileProvider,
    private val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
) : BaseDataSource(/* isNetwork = */ true) {

    private var uri: Uri? = null
    private var access: TorrentFileAccess? = null
    private var position = 0L
    private var bytesRemaining = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        val target = parse(dataSpec.uri)
            ?: throw DataSourceException("Not a torrent stream URI: ${dataSpec.uri}", PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        val file = files.openFile(target.first, target.second)
            ?: throw DataSourceException("No open torrent stream for ${dataSpec.uri}", PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        if (dataSpec.position > file.fileSize) {
            file.close()
            throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        }
        access = file
        position = dataSpec.position
        val available = file.fileSize - position
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) minOf(dataSpec.length, available) else available
        file.seek(position)
        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val file = access ?: throw IllegalStateException("read() before open()")
        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val read = try {
            file.read(position, buffer, offset, toRead, readTimeoutMs)
        } catch (e: InterruptedIOException) {
            throw e
        } catch (e: TorrentReadTimeoutException) {
            throw DataSourceException(e.message, e, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
        } catch (e: IOException) {
            throw DataSourceException(e, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        if (read < 0) return C.RESULT_END_OF_INPUT
        position += read
        bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        val file = access
        access = null
        file?.close()
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    class Factory(
        private val files: TorrentFileProvider,
        private val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = TorrentDataSource(files, readTimeoutMs)
    }

    companion object {
        /** TE-6 read timeout; ExoPlayer retries the load after it (DefaultLoadErrorHandlingPolicy). */
        const val DEFAULT_READ_TIMEOUT_MS = 60_000L

        /** `jazzytorrent://<infoHash>/<fileIndex>` → (infoHash, fileIndex). */
        fun parse(uri: Uri): Pair<String, Int>? {
            if (uri.scheme != TORRENT_URI_SCHEME) return null
            val hash = uri.authority?.takeIf { it.isNotBlank() } ?: return null
            val index = uri.pathSegments.firstOrNull()?.toIntOrNull() ?: return null
            return Magnets.normalizeInfoHash(hash) to index
        }
    }
}

/**
 * One factory for the whole player: `jazzytorrent://` goes to [TorrentDataSource], every other
 * URI (subtitles, trailers, artwork…) to [upstream].
 */
@OptIn(UnstableApi::class)
class TorrentAwareDataSourceFactory(
    private val files: TorrentFileProvider,
    private val upstream: DataSource.Factory,
    private val readTimeoutMs: Long = TorrentDataSource.DEFAULT_READ_TIMEOUT_MS,
) : DataSource.Factory {
    override fun createDataSource(): DataSource = RoutingDataSource(files, upstream, readTimeoutMs)
}

@OptIn(UnstableApi::class)
private class RoutingDataSource(
    private val files: TorrentFileProvider,
    private val upstreamFactory: DataSource.Factory,
    private val readTimeoutMs: Long,
) : DataSource {
    private val listeners = mutableListOf<TransferListener>()
    private var torrent: DataSource? = null
    private var upstream: DataSource? = null
    private var current: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        torrent?.addTransferListener(transferListener)
        upstream?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        check(current == null) { "open() called twice without close()" }
        val source = if (dataSpec.uri.scheme == TORRENT_URI_SCHEME) {
            torrent ?: TorrentDataSource(files, readTimeoutMs).also(::attach).also { torrent = it }
        } else {
            upstream ?: upstreamFactory.createDataSource().also(::attach).also { upstream = it }
        }
        current = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        (current ?: throw IllegalStateException("read() before open()")).read(buffer, offset, length)

    override fun getUri(): Uri? = current?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders ?: emptyMap()

    override fun close() {
        val source = current ?: return
        current = null
        source.close()
    }

    private fun attach(source: DataSource) = listeners.forEach(source::addTransferListener)
}
