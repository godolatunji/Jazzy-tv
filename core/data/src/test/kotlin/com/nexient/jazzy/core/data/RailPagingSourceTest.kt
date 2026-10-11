package com.nexient.jazzy.core.data

import androidx.paging.PagingSource
import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.data.catalog.RailPagingSource
import com.nexient.jazzy.core.data.catalog.TitlePage
import com.nexient.jazzy.core.model.MediaType
import com.nexient.jazzy.core.model.TitleSummary
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RailPagingSourceTest {
    private fun title(id: Int, type: MediaType = MediaType.MOVIE) = TitleSummary(tmdbId = id, mediaType = type, title = "t$id")

    private val pages = mapOf(
        1 to listOf(title(1), title(2), title(3)),
        // TMDB shifted between requests: 3 shows up again, and a TV show shares id 1 with a movie.
        2 to listOf(title(3), title(4), title(1, MediaType.TV)),
        3 to listOf(title(4)),
    )

    private val source = RailPagingSource(maxPages = 5) { page -> TitlePage(pages.getValue(page), totalPages = 3) }

    private suspend fun load(key: Int?) =
        source.load(PagingSource.LoadParams.Append(key ?: 1, loadSize = 20, placeholdersEnabled = false))
            as PagingSource.LoadResult.Page

    @Test
    fun dropsTitlesAlreadyLoadedOnEarlierPages() = runTest {
        val first = load(1)
        val second = load(2)
        val third = load(3)

        assertThat(first.data.map { it.tmdbId }).containsExactly(1, 2, 3).inOrder()
        assertThat(second.data.map { it.tmdbId to it.mediaType })
            .containsExactly(4 to MediaType.MOVIE, 1 to MediaType.TV).inOrder()
        assertThat(third.data).isEmpty()
    }

    @Test
    fun aPageThatIsAllDuplicatesStillPointsToTheNextOne() = runTest {
        val repeats = RailPagingSource(maxPages = 5) { TitlePage(listOf(title(9)), totalPages = 3) }
        repeats.load(PagingSource.LoadParams.Refresh(null, loadSize = 20, placeholdersEnabled = false))

        val second = repeats.load(PagingSource.LoadParams.Append(2, loadSize = 20, placeholdersEnabled = false))
            as PagingSource.LoadResult.Page

        assertThat(second.data).isEmpty()
        assertThat(second.nextKey).isEqualTo(3)
    }
}
