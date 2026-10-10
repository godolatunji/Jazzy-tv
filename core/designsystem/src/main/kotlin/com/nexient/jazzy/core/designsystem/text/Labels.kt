package com.nexient.jazzy.core.designsystem.text

import androidx.annotation.StringRes
import com.nexient.jazzy.core.designsystem.R
import com.nexient.jazzy.core.model.MaturityLevel

/** Localised labels for model values that are defined in core modules without resources. */
@get:StringRes
val MaturityLevel.labelRes: Int
    get() = when (this) {
        MaturityLevel.LITTLE_KIDS -> R.string.maturity_little_kids
        MaturityLevel.OLDER_KIDS -> R.string.maturity_older_kids
        MaturityLevel.TEENS -> R.string.maturity_teens
        MaturityLevel.ALL -> R.string.maturity_all
    }

/** Unified genre IDs (`core/recommendations` `UnifiedGenre.id`) → label. */
@StringRes
fun genreLabelRes(unifiedGenreId: String): Int? = when (unifiedGenreId) {
    "action_adventure" -> R.string.genre_action_adventure
    "scifi_fantasy" -> R.string.genre_scifi_fantasy
    "war_politics" -> R.string.genre_war_politics
    "kids_family" -> R.string.genre_kids_family
    "animation" -> R.string.genre_animation
    "comedy" -> R.string.genre_comedy
    "crime" -> R.string.genre_crime
    "documentary" -> R.string.genre_documentary
    "drama" -> R.string.genre_drama
    "mystery" -> R.string.genre_mystery
    "western" -> R.string.genre_western
    "history" -> R.string.genre_history
    "horror" -> R.string.genre_horror
    "music" -> R.string.genre_music
    "romance" -> R.string.genre_romance
    "thriller" -> R.string.genre_thriller
    "tv_movie" -> R.string.genre_tv_movie
    "news" -> R.string.genre_news
    "reality" -> R.string.genre_reality
    "soap" -> R.string.genre_soap
    "talk" -> R.string.genre_talk
    else -> null
}

fun genreText(unifiedGenreId: String, fallback: String): UiText =
    genreLabelRes(unifiedGenreId)?.let { UiText.Res(it) } ?: UiText.Raw(fallback)

/**
 * Home rail title from its stable key (core/data `RailDefinition.key`, recommendation rail keys).
 * [fallback] is the English title the data layer carries; [networkName] fills "%s Originals".
 */
fun railTitle(key: String, fallback: String, networkName: String? = null): UiText = when {
    key == "trending" -> UiText.Res(R.string.rail_trending)
    key == "top10" -> UiText.Res(R.string.rail_top10)
    key == "popular" -> UiText.Res(R.string.rail_popular)
    key == "originals_more" -> UiText.Res(R.string.rail_more_originals)
    key.startsWith("originals_") && networkName != null -> UiText.Res(R.string.rail_originals, networkName)
    key == "top_rated" -> UiText.Res(R.string.rail_top_rated)
    key == "upcoming" -> UiText.Res(if (fallback == "New Episodes") R.string.rail_new_episodes else R.string.rail_upcoming)
    key == "now_playing" -> UiText.Res(if (fallback == "Airing Today") R.string.rail_airing_today else R.string.rail_now_playing)
    key == "genre_actionadventure" -> UiText.Res(R.string.rail_genre_action)
    key == "genre_comedy" -> UiText.Res(R.string.rail_genre_comedy)
    key == "genre_scififantasy" -> UiText.Res(R.string.rail_genre_scifi)
    key == "genre_crime" -> UiText.Res(R.string.rail_genre_crime)
    key == "genre_documentaries" -> UiText.Res(R.string.rail_genre_documentaries)
    key == "genre_animation" -> UiText.Res(R.string.rail_genre_animation)
    key == "recommended_for_you" -> UiText.Res(R.string.rail_recommended_for_you)
    key.startsWith("because_") -> genreLabelRes(key.removePrefix("because_"))
        ?.let { UiText.Res(R.string.rail_because_you_watch, UiText.Res(it)) } ?: UiText.Raw(fallback)
    else -> UiText.Raw(fallback)
}
