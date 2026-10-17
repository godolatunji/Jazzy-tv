package com.nexient.jazzy.core.model

/** Builds TMDB image URLs (spec §6.1). Sizes differ per form factor. */
object TmdbImages {
    var baseUrl: String = "https://image.tmdb.org/t/p/"

    fun poster(path: String?, tv: Boolean = false) = url(path, if (tv) "w500" else "w342")

    /** Card-sized 16:9 art (Continue Watching, Watch Next, history, notifications). */
    fun backdrop(path: String?, tv: Boolean = false) = url(path, "w780")

    /** Full-screen 16:9 art for the Home/Player hero only; larger bucket, so keep it off list cards. */
    fun heroBackdrop(path: String?) = url(path, "w1280")
    fun still(path: String?) = url(path, "w300")
    fun logo(path: String?) = url(path, "w500")
    fun profile(path: String?) = url(path, "w185")
    fun original(path: String?) = url(path, "original")

    private fun url(path: String?, size: String): String? = path?.let { "$baseUrl$size$it" }
}
