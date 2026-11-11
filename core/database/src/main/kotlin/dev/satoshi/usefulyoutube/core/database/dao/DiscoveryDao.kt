package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.satoshi.usefulyoutube.core.database.entity.DiscoveredVideoEntity
import dev.satoshi.usefulyoutube.core.database.entity.FeaturedChannelsEntity
import dev.satoshi.usefulyoutube.core.model.VideoContentKind
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DiscoveryDao {
    @Query("SELECT * FROM discovered_videos WHERE content_kind IN (:kinds) ORDER BY published_at DESC")
    abstract fun observeVideos(kinds: Set<VideoContentKind>): Flow<List<DiscoveredVideoEntity>>

    @Query("SELECT * FROM discovered_videos WHERE video_id = :videoId")
    abstract fun observeVideo(videoId: String): Flow<DiscoveredVideoEntity?>

    /** Ids of all stored videos, Shorts included. */
    @Query("SELECT video_id FROM discovered_videos")
    abstract suspend fun getVideoIds(): List<String>

    /** Adds the [videos], keeping stored ones, and deletes the videos of channels outside [channelIds]. */
    @Transaction
    open suspend fun save(
        videos: List<DiscoveredVideoEntity>,
        channelIds: Set<String>,
    ) {
        insertOrIgnore(videos)
        deleteVideosOfOtherChannels(channelIds)
    }

    @Query("SELECT * FROM featured_channels WHERE source_channel_id = :sourceChannelId")
    abstract suspend fun getFeaturedChannels(sourceChannelId: String): FeaturedChannelsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertFeaturedChannels(featuredChannels: FeaturedChannelsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertOrIgnore(videos: List<DiscoveredVideoEntity>)

    @Query("DELETE FROM discovered_videos WHERE channel_id NOT IN (:channelIds)")
    protected abstract suspend fun deleteVideosOfOtherChannels(channelIds: Set<String>)
}
