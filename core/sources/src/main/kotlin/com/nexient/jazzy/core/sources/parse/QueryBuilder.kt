package com.nexient.jazzy.core.sources.parse

import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.SourceQuery
import java.util.Locale

/**
 * Builds the free-text search strings for a [SourceQuery] (spec §5.8 "Query building"):
 * - Movie: `"{title} {year}"`, plus `"{original title} {year}"` when the original title differs
 *   (after normalisation) and is written in a script the sites can search.
 * - Episode: `"{show} S01E02"`, then the season pack `"{show} S01"`, then `"{show} complete"`.
 *
 * YTS's IMDb-ID search is decided per provider by the searcher, not here.
 */
object QueryBuilder {

    fun build(query: SourceQuery): List<String> = when (query.mediaType) {
        MediaType.MOVIE -> movieQueries(query)
        MediaType.TV -> tvQueries(query)
    }.filter { it.isNotBlank() }.distinct()

    private fun movieQueries(query: SourceQuery): List<String> {
        val titles = buildList {
            add(query.title)
            val original = query.originalTitle
            if (original != null &&
                TitleNormalizer.matchKey(original) != TitleNormalizer.matchKey(query.title) &&
                TitleNormalizer.isLatinSearchable(original)
            ) {
                add(original)
            }
        }
        return titles.map { title -> listOfNotNull(TitleNormalizer.searchText(title), query.year?.toString()).joinToString(" ") }
    }

    private fun tvQueries(query: SourceQuery): List<String> {
        val show = TitleNormalizer.searchText(query.title)
        val season = query.season ?: return listOf(show)
        val episode = query.episode
        return listOfNotNull(
            episode?.let { "$show S${pad(season)}E${pad(it)}" },
            "$show S${pad(season)}",
            "$show complete",
        )
    }

    private fun pad(n: Int) = String.format(Locale.ROOT, "%02d", n)
}
