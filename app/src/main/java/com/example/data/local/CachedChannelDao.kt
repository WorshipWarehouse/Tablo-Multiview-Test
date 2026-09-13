package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedChannelDao {
    @Query("SELECT * FROM cached_channels ORDER BY major ASC, minor ASC")
    fun getAllChannels(): Flow<List<CachedChannelEntity>>

    @Query("SELECT * FROM cached_channels ORDER BY major ASC, minor ASC")
    suspend fun getAllChannelsList(): List<CachedChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<CachedChannelEntity>)

    @Query("DELETE FROM cached_channels")
    suspend fun clearAll()
}
