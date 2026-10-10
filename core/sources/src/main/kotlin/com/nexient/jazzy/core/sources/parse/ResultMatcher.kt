package com.nexient.jazzy.core.sources.parse

import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.SourceQuery
import kotlin.math.abs

/**
 * Drops results that clearly belong to another title, year or episode (spec §5.8). It errs on the
 * side of keeping a row: only a clear mismatch is dropped.
 *
 * - **Title:** the name's title part (see [ReleaseParser]) is compared token by token with the
 *   query title and original title after [TitleNormalizer] folding. Tolerated: punctuation and
 *   spacing differences ("Spider-Man: No Way Home" vs "Spider.Man.No.Way.Home"), `&`/`and`,
 *   accents, leading articles, edition/country suffixes ("Extended", "US"), a trailing year that
 *   is really part of the title ("Blade.Runner.2049"), acronym endings ("Law.and.Order.SVU"),
 *   "AKA" alternatives, and a longer or shorter title when the release year matches exactly ±1
 *   (movies only). An IMDb ID equal to the query's (YTS, TPB) skips the title check.
 * - **Year (movies):** within ±1 of the query year when the name has one. For shows, only a
 *   year *earlier* than the show's first year is a mismatch (later seasons can carry later years).
 * - **Episode:** S/E must match; season packs and multi-season packs must contain the season;
 *   complete-series packs are kept. Rows without any season/episode info are dropped for an
 *   episode query. Movie queries drop rows that carry a season.
 */
object ResultMatcher {

    private val ALLOWED_SUFFIX = setOf(
        "us", "uk", "au", "nz", "ca", "extended", "directors", "director", "cut", "unrated", "uncut", "remastered",
        "theatrical", "imax", "edition", "special", "final", "ultimate", "collectors", "criterion", "version",
        "anniversary", "restored", "redux", "dc", "proper", "repack", "limited", "internal", "rerip",
    )
    private val AKA = Regex("""\s+(?:aka|a\s+k\s+a)\s+""", RegexOption.IGNORE_CASE)

    fun matches(query: SourceQuery, name: String, imdbId: String? = null): Boolean =
        matches(query, ReleaseParser.parseDetailed(name), imdbId)

    fun matches(query: SourceQuery, release: ReleaseParser.ParsedRelease, imdbId: String? = null): Boolean {
        val imdbExact = imdbId != null && query.imdbId != null && imdbId.equals(query.imdbId, ignoreCase = true)
        val titleMatch = if (imdbExact) TitleMatch(yearIsTitle = false) else matchTitle(query, release) ?: return false
        if (!titleMatch.yearIsTitle && !yearMatches(query, release.info.year)) return false
        return episodeMatches(query, release)
    }

    private class TitleMatch(val yearIsTitle: Boolean)

    private fun matchTitle(query: SourceQuery, release: ReleaseParser.ParsedRelease): TitleMatch? {
        val queryTitles = listOfNotNull(query.title, query.originalTitle)
            .map(TitleNormalizer::tokens)
            .filter { it.isNotEmpty() }
            .distinct()
        val nameTitles = release.title.split(AKA).map(TitleNormalizer::tokens).filter { it.isNotEmpty() }
        val year = release.info.year
        for (n in nameTitles) {
            for (q in queryTitles) {
                if (year != null && n + year.toString() == q) return TitleMatch(yearIsTitle = true)
                if (titleMatches(n, q, year, query)) return TitleMatch(yearIsTitle = false)
            }
        }
        return null
    }

    private fun titleMatches(n: List<String>, q: List<String>, nameYear: Int?, query: SourceQuery): Boolean {
        if (n == q) return true
        if (n.joinToString("") == q.joinToString("")) return true
        if (n.size > q.size && n.subList(0, q.size) == q && n.drop(q.size).all { it in ALLOWED_SUFFIX }) return true
        if (isAcronymEnding(n, q)) return true
        val queryYear = query.year
        val sameYear = query.mediaType == MediaType.MOVIE && nameYear != null && queryYear != null &&
            abs(nameYear - queryYear) <= 1
        if (sameYear) {
            // "Star.Wars.Episode.IV.A.New.Hope.1977" for "Star Wars" (1977), and
            // "Mission.Impossible.Dead.Reckoning.2023" for "Mission: Impossible - Dead Reckoning Part One".
            if (q.size >= 2 && n.size > q.size && n.subList(0, q.size) == q) return true
            if (n.size >= 2 && q.size > n.size && q.subList(0, n.size) == n && n.size * 2 >= q.size) return true
        }
        return false
    }

    /** "law order svu" for "law order special victims unit". */
    private fun isAcronymEnding(n: List<String>, q: List<String>): Boolean {
        if (n.size < 2) return false
        val k = n.size - 1
        if (q.size - k < 2 || q.subList(0, k) != n.subList(0, k)) return false
        return n.last() == q.drop(k).joinToString("") { it.first().toString() }
    }

    private fun yearMatches(query: SourceQuery, nameYear: Int?): Boolean {
        val queryYear = query.year ?: return true
        nameYear ?: return true
        return when (query.mediaType) {
            MediaType.MOVIE -> abs(nameYear - queryYear) <= 1
            MediaType.TV -> nameYear >= queryYear - 1
        }
    }

    private fun episodeMatches(query: SourceQuery, release: ReleaseParser.ParsedRelease): Boolean {
        if (query.mediaType == MediaType.MOVIE) return release.seasons == null
        val season = query.season ?: return true
        val episode = query.episode
        val seasons = release.seasons
        val episodes = release.episodes
        return when {
            seasons != null && episodes != null -> season in seasons && (episode == null || episode in episodes)
            seasons != null -> season in seasons
            // "Episode 5" without a season: only meaningful for a first season.
            episodes != null -> season == 1 && (episode == null || episode in episodes)
            else -> release.info.isCompleteSeries
        }
    }
}
