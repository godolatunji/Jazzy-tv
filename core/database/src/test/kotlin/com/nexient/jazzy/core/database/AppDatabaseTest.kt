package com.nexient.jazzy.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nexient.jazzy.core.database.app.AppDatabase
import com.nexient.jazzy.core.database.app.LikedTitleEntity
import com.nexient.jazzy.core.database.app.MyListItemEntity
import com.nexient.jazzy.core.database.app.PlaySessionEntity
import com.nexient.jazzy.core.database.app.ProfileEntity
import com.nexient.jazzy.core.database.app.TitleSnapshotEntity
import com.nexient.jazzy.core.database.app.WatchProgressEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun profile(name: String) = db.profileDao().insert(
        ProfileEntity(
            name = name, avatarId = 1, isKids = false, maturity = "ALL", language = "en-US",
            subtitleLanguage = null, audioLanguage = null, pinHash = null, pinSalt = null,
            autoplayNext = true, createdAt = 0,
        ),
    )

    private fun snapshot(id: Int, type: String = "movie") = TitleSnapshotEntity(
        tmdbId = id, mediaType = type, title = "T$id", originalTitle = "T$id", year = 2020,
        posterPath = null, backdropPath = null, genreIds = listOf(28, 12), imdbId = null, updatedAt = 0,
    )

    private fun progress(profileId: Long, id: Int, season: Int, episode: Int, updatedAt: Long, type: String = "tv") =
        WatchProgressEntity(
            profileId = profileId, tmdbId = id, mediaType = type, season = season, episode = episode,
            positionMs = 600_000, durationMs = 3_000_000, completed = false, lastInfoHash = "abc",
            lastFileIndex = 0, lastSourceName = "x", lastQuality = "1080p", episodeTitle = null, updatedAt = updatedAt,
        )

    @Test
    fun deletingProfileCascadesToItsDataOnly() = runBlocking {
        val a = profile("A")
        val b = profile("B")
        db.titleSnapshotDao().upsert(snapshot(1))
        db.libraryDao().addToMyList(MyListItemEntity(a, 1, "movie", 1))
        db.libraryDao().addToMyList(MyListItemEntity(b, 1, "movie", 1))
        db.libraryDao().like(LikedTitleEntity(a, 1, "movie", 1))
        db.progressDao().upsert(progress(a, 1, -1, -1, 5, "movie"))

        db.profileDao().delete(a)

        assertThat(db.libraryDao().myListFor(a)).isEmpty()
        assertThat(db.libraryDao().likedFor(a)).isEmpty()
        assertThat(db.progressDao().allFor(a)).isEmpty()
        assertThat(db.libraryDao().myListFor(b)).hasSize(1)
    }

    @Test
    fun genreListRoundTripsThroughConverter() = runBlocking {
        val p = profile("A")
        db.titleSnapshotDao().upsert(snapshot(7))
        db.libraryDao().like(LikedTitleEntity(p, 7, "movie", 3))
        val liked = db.libraryDao().observeLikedTitles(p).first()
        assertThat(liked.single().genreIds).containsExactly(28, 12).inOrder()
    }

    @Test
    fun latestProgressPerTitleReturnsNewestEpisodeOnly() = runBlocking {
        val p = profile("A")
        db.progressDao().upsert(progress(p, 10, 1, 1, updatedAt = 100))
        db.progressDao().upsert(progress(p, 10, 1, 2, updatedAt = 200))
        db.progressDao().upsert(progress(p, 20, -1, -1, updatedAt = 150, type = "movie"))

        val latest = db.progressDao().observeLatestPerTitle(p, 20).first()

        assertThat(latest.map { it.tmdbId to it.episode }).containsExactly(10 to 2, 20 to -1).inOrder()
    }

    @Test
    fun unreferencedSnapshotsAreRemovedButReferencedOnesKept() = runBlocking {
        val p = profile("A")
        db.titleSnapshotDao().upsert(snapshot(1))
        db.titleSnapshotDao().upsert(snapshot(2))
        db.titleSnapshotDao().upsert(snapshot(3))
        db.libraryDao().like(LikedTitleEntity(p, 1, "movie", 1))
        db.playSessionDao().insert(
            PlaySessionEntity(
                profileId = p, tmdbId = 2, mediaType = "movie", season = -1, episode = -1, episodeTitle = null,
                infoHash = null, fileIndex = null, sourceName = null, quality = null, providerId = null,
                startedAt = 1, endedAt = 2, startPositionMs = 0, stopPositionMs = 10, durationMs = 100,
                watchedMs = 10, endReason = null,
            ),
        )

        val removed = db.titleSnapshotDao().deleteUnreferenced()

        assertThat(removed).isEqualTo(1)
        assertThat(db.titleSnapshotDao().get(3, "movie")).isNull()
        assertThat(db.titleSnapshotDao().get(1, "movie")).isNotNull()
    }
}
