package com.nexient.jazzy.core.data

import com.nexient.jazzy.core.common.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** A cached payload and when it was fetched. */
data class Cached<T>(val value: T, val fetchedAt: Long)

/**
 * Spec §6.3: emit the cached value right away (even if stale), refresh when missing or older than
 * [ttlMs] (or [force]), store, and emit again. On failure keep the stale data and attach the error.
 */
fun <T> cacheFirst(
    now: () -> Long,
    ttlMs: Long,
    force: Boolean = false,
    read: suspend () -> Cached<T>?,
    fetch: suspend () -> T,
    write: suspend (T) -> Unit,
): Flow<Resource<T>> = flow {
    val cached = read()
    val stale = cached == null || force || now() - cached.fetchedAt > ttlMs
    if (!stale) {
        emit(Resource(cached?.value))
        return@flow
    }
    emit(Resource(cached?.value, refreshing = true, stale = cached != null))
    try {
        val fresh = fetch()
        write(fresh)
        emit(Resource(fresh))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emit(Resource(cached?.value, refreshing = false, error = e, stale = cached != null))
    }
}

/** Single-shot variant for paging: cached if fresh, else fetch (falling back to stale on error). */
suspend fun <T> cacheFirstOnce(
    now: () -> Long,
    ttlMs: Long,
    read: suspend () -> Cached<T>?,
    fetch: suspend () -> T,
    write: suspend (T) -> Unit,
): T {
    val cached = read()
    if (cached != null && now() - cached.fetchedAt <= ttlMs) return cached.value
    return try {
        fetch().also { write(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        cached?.value ?: throw e
    }
}
