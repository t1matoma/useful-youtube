package dev.satoshi.usefulyoutube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.satoshi.usefulyoutube.core.database.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY title COLLATE NOCASE")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY title COLLATE NOCASE")
    suspend fun getChannels(): List<ChannelEntity>

    /** Returns row ids; `-1` marks channels that already existed. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(channels: List<ChannelEntity>): List<Long>

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannel(id: String)
}
