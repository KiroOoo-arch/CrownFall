package com.crownfall.realm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crownfall.realm.data.entities.GameStatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStatisticsDao {

    @Query("SELECT * FROM game_statistics WHERE id = 1 LIMIT 1")
    suspend fun get(): GameStatisticsEntity?

    @Query("SELECT * FROM game_statistics WHERE id = 1 LIMIT 1")
    fun observe(): Flow<GameStatisticsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: GameStatisticsEntity)

    @Query("DELETE FROM game_statistics")
    suspend fun clear()
}
