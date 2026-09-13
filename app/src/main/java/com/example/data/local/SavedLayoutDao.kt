package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLayoutDao {
    @Query("SELECT * FROM saved_layouts ORDER BY createdAt DESC")
    fun getAllLayouts(): Flow<List<SavedLayoutEntity>>

    @Query("SELECT * FROM saved_layouts WHERE id = :id LIMIT 1")
    suspend fun getLayoutById(id: Long): SavedLayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLayout(layout: SavedLayoutEntity): Long

    @Update
    suspend fun updateLayout(layout: SavedLayoutEntity)

    @Delete
    suspend fun deleteLayout(layout: SavedLayoutEntity)

    @Query("DELETE FROM saved_layouts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
