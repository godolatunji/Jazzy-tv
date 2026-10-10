package com.nexient.jazzy.tv

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import com.nexient.jazzy.R
import com.nexient.jazzy.core.model.SearchResult
import com.nexient.jazzy.core.model.TmdbImages
import com.nexient.jazzy.core.network.TmdbApi
import com.nexient.jazzy.core.network.toSearchResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Answers Google TV / system global search (spec SR-6, plan E7-05) with TMDB titles. Each result
 * deep-links to Details (`jazzy://title/...`). Adult titles are never returned.
 */
class SearchSuggestionsProvider : ContentProvider() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun tmdbApi(): TmdbApi
    }

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val query = (uri.lastPathSegment?.takeIf { it != SearchManager.SUGGEST_URI_PATH_QUERY } ?: selectionArgs?.firstOrNull()).orEmpty().trim()
        val cursor = MatrixCursor(COLUMNS)
        if (query.length < 2) return cursor
        val ctx = requireNotNull(context)
        val api = EntryPointAccessors.fromApplication(ctx.applicationContext, Deps::class.java).tmdbApi()
        val tvLabel = ctx.getString(R.string.app_search_suggestion_tv)
        val movieLabel = ctx.getString(R.string.app_search_suggestion_movie)
        val language = Locale.getDefault().let { "${it.language}-${it.country.ifEmpty { "US" }}" }
        val results = runBlocking {
            withTimeoutOrNull(4_000) { runCatching { api.searchMulti(query, language, 1).results }.getOrNull() }
        }.orEmpty()
        results.mapNotNull { it.toSearchResult() as? SearchResult.Title }.map { it.title }.filter { !it.adult }.take(20).forEachIndexed { i, t ->
            cursor.addRow(
                arrayOf<Any?>(
                    i.toLong(),
                    t.title,
                    listOfNotNull(t.year?.toString(), if (t.mediaType.apiValue == "tv") tvLabel else movieLabel).joinToString(" · "),
                    TmdbImages.poster(t.posterPath),
                    "jazzy://title/${t.mediaType.apiValue}/${t.tmdbId}",
                    t.year?.toString(),
                    if (t.mediaType.apiValue == "tv") "vnd.android.cursor.item/tv_series" else "vnd.android.cursor.item/movie",
                ),
            )
        }
        return cursor
    }

    override fun getType(uri: Uri): String = SearchManager.SUGGEST_MIME_TYPE
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    private companion object {
        val COLUMNS = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
            SearchManager.SUGGEST_COLUMN_CONTENT_TYPE,
        )
    }
}
