package dev.satoshi.usefulyoutube.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), UsefulYouTubeDatabase::class.java)

    @Test
    fun `migration from 1 to the latest version keeps subscriptions and videos`() =
        runTest {
            helper.createDatabase(DATABASE_NAME, 1).use { database ->
                database.execSQL("INSERT INTO channels (id, title) VALUES ('UCaaaaaaaaaaaaaaaaaaaaaa', 'First')")
                database.execSQL(
                    """
                    INSERT INTO videos (id, channel_id, title, description, published_at, duration_seconds,
                    thumbnail_url, content_kind, details_fetched)
                    VALUES ('video00001', 'UCaaaaaaaaaaaaaaaaaaaaaa', 'Title', '', 0, 600, 'url', 'REGULAR', 1)
                    """.trimIndent(),
                )
            }

            helper.runMigrationsAndValidate(DATABASE_NAME, LATEST_VERSION, true).close()

            val database =
                Room
                    .databaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        UsefulYouTubeDatabase::class.java,
                        DATABASE_NAME,
                    ).allowMainThreadQueries()
                    .build()
            helper.closeWhenFinished(database)
            assertThat(database.channelDao().getChannels().map { it.title }).containsExactly("First")
            assertThat(database.videoDao().getIdsWithoutDetails()).isEmpty()
            assertThat(
                database
                    .videoDao()
                    .observeVideo("video00001")
                    .first()
                    ?.video
                    ?.title,
            ).isEqualTo("Title")
            assertThat(database.searchDao().observeSession().first()).isNull()
        }

    @Test
    fun `migration from 2 to the latest version keeps search results`() =
        runTest {
            helper.createDatabase(DATABASE_NAME, 2).use { database ->
                database.execSQL(
                    "INSERT INTO recent_search_queries (query_text, used_order) VALUES ('kotlin', 1)",
                )
            }

            helper.runMigrationsAndValidate(DATABASE_NAME, LATEST_VERSION, true).close()

            val database =
                Room
                    .databaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        UsefulYouTubeDatabase::class.java,
                        DATABASE_NAME,
                    ).allowMainThreadQueries()
                    .build()
            helper.closeWhenFinished(database)
            assertThat(database.recentSearchQueryDao().observeRecentQueries().first()).containsExactly("kotlin")
            assertThat(database.playlistDao().observePlaylists().first()).isEmpty()
        }

    @Test
    fun `migration from 3 to the latest version keeps playlists`() =
        runTest {
            helper.createDatabase(DATABASE_NAME, 3).use { database ->
                database.execSQL(
                    "INSERT INTO playlists (id, title, remote_id, synced_at) VALUES (1, 'Later', NULL, NULL)",
                )
            }

            helper.runMigrationsAndValidate(DATABASE_NAME, LATEST_VERSION, true).close()

            val database =
                Room
                    .databaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        UsefulYouTubeDatabase::class.java,
                        DATABASE_NAME,
                    ).allowMainThreadQueries()
                    .build()
            helper.closeWhenFinished(database)
            assertThat(
                database
                    .playlistDao()
                    .observePlaylists()
                    .first()
                    .map { it.playlist.title },
            ).containsExactly("Later")
            assertThat(database.watchHistoryDao().observeHistory().first()).isEmpty()
        }

    @Test
    fun `migration from 4 to the latest version keeps the history without playback positions`() =
        runTest {
            helper.createDatabase(DATABASE_NAME, 4).use { database ->
                database.execSQL(
                    """
                    INSERT INTO watch_history (video_id, channel_id, channel_title, title, description, published_at,
                    duration_seconds, thumbnail_url, content_kind, watched_at)
                    VALUES ('video00001', 'UCaaaaaaaaaaaaaaaaaaaaaa', 'First', 'Title', '', 0, 600, 'url', 'REGULAR', 1)
                    """.trimIndent(),
                )
            }

            helper.runMigrationsAndValidate(DATABASE_NAME, LATEST_VERSION, true).close()

            val database =
                Room
                    .databaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        UsefulYouTubeDatabase::class.java,
                        DATABASE_NAME,
                    ).allowMainThreadQueries()
                    .build()
            helper.closeWhenFinished(database)
            val entry = database.watchHistoryDao().getEntry("video00001")
            assertThat(entry?.video?.title).isEqualTo("Title")
            assertThat(entry?.playbackPositionSeconds).isEqualTo(0L)
        }

    @Test
    fun `migration from 5 to the latest version keeps the history and adds empty tables`() =
        runTest {
            helper.createDatabase(DATABASE_NAME, 5).use { database ->
                database.execSQL(
                    """
                    INSERT INTO watch_history (video_id, channel_id, channel_title, title, description, published_at,
                    duration_seconds, thumbnail_url, content_kind, watched_at, playback_position_seconds,
                    playback_duration_seconds)
                    VALUES ('video00001', 'UCaaaaaaaaaaaaaaaaaaaaaa', 'First', 'Title', '', 0, 600, 'url', 'REGULAR', 1,
                    120, 600)
                    """.trimIndent(),
                )
            }

            helper.runMigrationsAndValidate(DATABASE_NAME, LATEST_VERSION, true).close()

            val database =
                Room
                    .databaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        UsefulYouTubeDatabase::class.java,
                        DATABASE_NAME,
                    ).allowMainThreadQueries()
                    .build()
            helper.closeWhenFinished(database)
            assertThat(database.watchHistoryDao().getEntry("video00001")?.playbackPositionSeconds).isEqualTo(120L)
            assertThat(database.videoRatingDao().observeRatings().first()).isEmpty()
            assertThat(database.discoveryDao().getVideoIds()).isEmpty()
        }

    private companion object {
        const val LATEST_VERSION = 6
        const val DATABASE_NAME = "migration-test.db"
    }
}
