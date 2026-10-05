package com.crownfall.realm.data.repository

import com.crownfall.realm.data.dao.AchievementDao
import com.crownfall.realm.data.dao.GameDao
import com.crownfall.realm.data.dao.GameStatisticsDao
import com.crownfall.realm.data.dao.MoveDao
import com.crownfall.realm.data.dao.PieceStatisticsDao
import com.crownfall.realm.data.dao.PlayerDao
import com.crownfall.realm.data.dao.SavedGameDao
import com.crownfall.realm.data.dao.SettingsDao
import com.crownfall.realm.data.entities.AchievementEntity
import com.crownfall.realm.data.entities.GameEntity
import com.crownfall.realm.data.entities.GameStatisticsEntity
import com.crownfall.realm.data.entities.MoveEntity
import com.crownfall.realm.data.entities.PieceStatisticsEntity
import com.crownfall.realm.data.entities.PlayerEntity
import com.crownfall.realm.data.entities.SavedGameEntity
import com.crownfall.realm.data.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory implementations of every DAO. They obey the same contracts Room
 * generates, so the repository logic can be unit tested on the JVM without a
 * device, an emulator or Robolectric.
 */
class FakePlayerDao : PlayerDao {
    var stored: PlayerEntity? = null
    private val flow = MutableStateFlow<PlayerEntity?>(null)
    override suspend fun get(playerId: Long): PlayerEntity? = stored
    override fun observe(playerId: Long): Flow<PlayerEntity?> = flow
    override suspend fun upsert(player: PlayerEntity) {
        stored = player
        flow.value = player
    }
    override suspend fun clear() {
        stored = null
        flow.value = null
    }
}

class FakeGameDao : GameDao {
    private val rows = LinkedHashMap<Long, GameEntity>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<GameEntity>>(emptyList())

    override suspend fun insert(game: GameEntity): Long {
        val id = if (game.gameId == 0L) nextId++ else game.gameId
        rows[id] = game.copy(gameId = id)
        publish()
        return id
    }
    override fun observeAll(): Flow<List<GameEntity>> = flow
    override suspend fun getAll(): List<GameEntity> = rows.values.sortedByDescending { it.endTime }
    override suspend fun getById(gameId: Long): GameEntity? = rows[gameId]
    override suspend fun count(): Int = rows.size
    override suspend fun clear() {
        rows.clear()
        publish()
    }
    private fun publish() { flow.value = rows.values.sortedByDescending { it.endTime } }
}

class FakeMoveDao : MoveDao {
    private val rows = ArrayList<MoveEntity>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<MoveEntity>>(emptyList())

    override suspend fun insertAll(moves: List<MoveEntity>) {
        moves.forEach { rows.add(it.copy(moveId = nextId++)) }
        publish()
    }
    override suspend fun movesFor(gameId: Long): List<MoveEntity> =
        rows.filter { it.gameId == gameId }.sortedBy { it.moveNumber }
    override fun observeMovesFor(gameId: Long): Flow<List<MoveEntity>> = flow
    override suspend fun countFor(gameId: Long): Int = rows.count { it.gameId == gameId }
    override suspend fun clear() {
        rows.clear()
        publish()
    }
    private fun publish() { flow.value = rows.toList() }
}

class FakePieceStatisticsDao : PieceStatisticsDao {
    private val rows = LinkedHashMap<String, PieceStatisticsEntity>()
    private val flow = MutableStateFlow<List<PieceStatisticsEntity>>(emptyList())
    override suspend fun getAll(): List<PieceStatisticsEntity> = rows.values.toList()
    override fun observeAll(): Flow<List<PieceStatisticsEntity>> = flow
    override suspend fun get(pieceType: String): PieceStatisticsEntity? = rows[pieceType]
    override suspend fun upsert(stat: PieceStatisticsEntity) {
        rows[stat.pieceType] = stat
        publish()
    }
    override suspend fun upsertAll(stats: List<PieceStatisticsEntity>) {
        stats.forEach { rows[it.pieceType] = it }
        publish()
    }
    override suspend fun clear() {
        rows.clear()
        publish()
    }
    private fun publish() { flow.value = rows.values.toList() }
}

class FakeGameStatisticsDao : GameStatisticsDao {
    var stored: GameStatisticsEntity? = null
    private val flow = MutableStateFlow<GameStatisticsEntity?>(null)
    override suspend fun get(): GameStatisticsEntity? = stored
    override fun observe(): Flow<GameStatisticsEntity?> = flow
    override suspend fun upsert(stats: GameStatisticsEntity) {
        stored = stats
        flow.value = stats
    }
    override suspend fun clear() {
        stored = null
        flow.value = null
    }
}

class FakeAchievementDao : AchievementDao {
    private val rows = LinkedHashMap<String, AchievementEntity>()
    private val flow = MutableStateFlow<List<AchievementEntity>>(emptyList())
    override suspend fun getAll(): List<AchievementEntity> = rows.values.toList()
    override fun observeAll(): Flow<List<AchievementEntity>> = flow
    override suspend fun upsertAll(achievements: List<AchievementEntity>) {
        achievements.forEach { rows[it.achievementId] = it }
        publish()
    }
    override suspend fun count(): Int = rows.size
    override suspend fun clear() {
        rows.clear()
        publish()
    }
    private fun publish() { flow.value = rows.values.toList() }
}

class FakeSettingsDao : SettingsDao {
    var stored: SettingsEntity? = null
    private val flow = MutableStateFlow<SettingsEntity?>(null)
    override suspend fun get(): SettingsEntity? = stored
    override fun observe(): Flow<SettingsEntity?> = flow
    override suspend fun upsert(settings: SettingsEntity) {
        stored = settings
        flow.value = settings
    }
    override suspend fun clear() {
        stored = null
        flow.value = null
    }
}

class FakeSavedGameDao : SavedGameDao {
    var stored: SavedGameEntity? = null
    private val flow = MutableStateFlow<SavedGameEntity?>(null)
    override suspend fun get(): SavedGameEntity? = stored
    override fun observe(): Flow<SavedGameEntity?> = flow
    override suspend fun upsert(game: SavedGameEntity) {
        stored = game
        flow.value = game
    }
    override suspend fun clear() {
        stored = null
        flow.value = null
    }
}
