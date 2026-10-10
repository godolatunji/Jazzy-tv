package com.nexient.jazzy.navigation

import android.net.Uri
import com.nexient.jazzy.core.model.MediaType

/** `jazzy://title/{movie|tv}/{tmdbId}?s={season}&e={episode}` → Details with Sources focused. */
object DeepLinks {
    fun parse(uri: Uri?): DetailsRoute? {
        if (uri?.scheme != "jazzy" || uri.host != "title") return null
        val segments = uri.pathSegments
        if (segments.size < 2) return null
        val type = MediaType.fromApi(segments[0]) ?: return null
        val id = segments[1].toIntOrNull() ?: return null
        return DetailsRoute(id, type, uri.getQueryParameter("s")?.toIntOrNull(), uri.getQueryParameter("e")?.toIntOrNull(), focusSources = true)
    }
}
