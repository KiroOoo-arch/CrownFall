package com.crownfall.realm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.crownfall.realm.data.entities.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Insert
    suspend fun insert(game: GameEntity): Long

    @Query("SELECT * FROM games ORDER BY endTime DESC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games ORDER BY endTime DESC")
    suspend fun getAll(): List<GameEntity>

    @Query("SELECT * FROM games WHERE gameId = :gameId LIMIT 1")
    suspend fun getById(gameId: Long): GameEntity?

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int

    @Query("DELETE FROM games")
    suspend fun clear()
}
