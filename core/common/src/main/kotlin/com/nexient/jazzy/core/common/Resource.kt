package com.nexient.jazzy.core.common

/** Cache-first data: whatever is cached now, whether a refresh is running, and the last error. */
data class Resource<out T>(
    val data: T? = null,
    val refreshing: Boolean = false,
    val error: Throwable? = null,
    /** True when [data] came from cache and is older than its TTL. */
    val stale: Boolean = false,
) {
    val isLoadingWithoutData: Boolean get() = data == null && refreshing
    val isErrorWithoutData: Boolean get() = data == null && !refreshing && error != null

    companion object {
        fun <T> loading(data: T? = null) = Resource(data = data, refreshing = true)
    }
}
