package com.nexient.jazzy.core.model

/** Builds TMDB image URLs (spec §6.1). Sizes differ per form factor. */
object TmdbImages {
    var baseUrl: String = "https://image.tmdb.org/t/p/"

    fun poster(path: String?, tv: Boolean = false) = url(path, if (tv) "w500" else "w342")
    fun backdrop(path: String?, tv: Boolean = false) = url(path, if (tv) "w1280" else "w780")
    fun still(path: String?) = url(path, "w300")
    fun logo(path: String?) = url(path, "w500")
    fun profile(path: String?) = url(path, "w185")
    fun original(path: String?) = url(path, "original")

    private fun url(path: String?, size: String): String? = path?.let { "$baseUrl$size$it" }
}
