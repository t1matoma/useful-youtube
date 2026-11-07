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
    fun `migration from 1 to 2 keeps subscriptions and videos`() =
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

            helper.runMigrationsAndValidate(DATABASE_NAME, 2, true).close()

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

    private companion object {
        const val DATABASE_NAME = "migration-test.db"
    }
}
