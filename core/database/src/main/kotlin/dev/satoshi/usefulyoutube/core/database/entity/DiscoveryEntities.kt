package dev.satoshi.usefulyoutube.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A video of a channel outside the subscriptions, found for the recommendations. [suggestedBy] is the channel
 * that features the video's channel, `NULL` when the user watched or liked the channel themselves.
 */
@Entity(tableName = "discovered_videos", primaryKeys = ["video_id"], indices = [Index("channel_id")])
data class DiscoveredVideoEntity(
    @Embedded val video: VideoSnapshot,
    @ColumnInfo(name = "suggested_by") val suggestedBy: String?,
)

/** The channels a channel features on its page, as answered by the Data API at [fetchedAtEpochMillis]. */
@Entity(tableName = "featured_channels")
data class FeaturedChannelsEntity(
    @PrimaryKey @ColumnInfo(name = "source_channel_id") val sourceChannelId: String,
    /** Channel ids separated by commas; empty when the channel features nobody. */
    @ColumnInfo(name = "channel_ids") val channelIds: String,
    @ColumnInfo(name = "fetched_at") val fetchedAtEpochMillis: Long,
)
