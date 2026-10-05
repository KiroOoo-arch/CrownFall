package com.crownfall.realm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crownfall.realm.data.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(achievements: List<AchievementEntity>)

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun count(): Int

    @Query("DELETE FROM achievements")
    suspend fun clear()
}
