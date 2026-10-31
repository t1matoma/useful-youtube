package dev.satoshi.usefulyoutube.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.satoshi.usefulyoutube.core.database.entity.ChannelEntity
import dev.satoshi.usefulyoutube.core.database.entity.VideoEntity
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
abstract class DatabaseTest {
    protected lateinit var database: UsefulYouTubeDatabase

    @Before
    fun createDatabase() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), UsefulYouTubeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    protected fun channel(
        id: String,
        title: String = "Channel $id",
    ) = ChannelEntity(id = id, title = title)

    protected fun video(
        id: String,
        channelId: String,
        publishedAtEpochMillis: Long = 0,
        contentKind: VideoContentKind = VideoContentKind.REGULAR,
        detailsFetched: Boolean = true,
        durationSeconds: Long? = null,
    ) = VideoEntity(
        id = id,
        channelId = channelId,
        title = "Video $id",
        description = "",
        publishedAtEpochMillis = publishedAtEpochMillis,
        durationSeconds = durationSeconds,
        thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
        contentKind = contentKind,
        detailsFetched = detailsFetched,
    )
}
