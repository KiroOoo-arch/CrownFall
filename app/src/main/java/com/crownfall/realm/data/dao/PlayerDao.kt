package com.crownfall.realm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crownfall.realm.data.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players WHERE playerId = :playerId LIMIT 1")
    suspend fun get(playerId: Long = 1L): PlayerEntity?

    @Query("SELECT * FROM players WHERE playerId = :playerId LIMIT 1")
    fun observe(playerId: Long = 1L): Flow<PlayerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(player: PlayerEntity)

    @Query("DELETE FROM players")
    suspend fun clear()
}
