package com.crownfall.realm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crownfall.realm.data.entities.SavedGameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {

    @Query("SELECT * FROM saved_games WHERE id = 1 LIMIT 1")
    suspend fun get(): SavedGameEntity?

    @Query("SELECT * FROM saved_games WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SavedGameEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: SavedGameEntity)

    @Query("DELETE FROM saved_games")
    suspend fun clear()
}
