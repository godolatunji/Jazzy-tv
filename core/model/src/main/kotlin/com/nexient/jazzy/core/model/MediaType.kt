package com.nexient.jazzy.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType(val apiValue: String) {
    MOVIE("movie"),
    TV("tv");

    companion object {
        fun fromApi(value: String?): MediaType? = entries.firstOrNull { it.apiValue == value }
    }
}
