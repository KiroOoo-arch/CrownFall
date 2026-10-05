package com.crownfall.realm.data.repository

import com.crownfall.realm.data.dao.GameDao
import com.crownfall.realm.data.dao.MoveDao
import com.crownfall.realm.data.dao.SavedGameDao
import com.crownfall.realm.data.entities.GameEntity
import com.crownfall.realm.data.entities.MoveEntity
import com.crownfall.realm.data.entities.SavedGameEntity
import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.models.FinishedGame
import com.crownfall.realm.domain.models.GameMode
import com.crownfall.realm.domain.models.GameResult
import com.crownfall.realm.domain.models.MatchDetail
import com.crownfall.realm.domain.models.MatchSummary
import com.crownfall.realm.domain.models.PlayedMove
import com.crownfall.realm.domain.models.SavedGame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists finished matches (game row + one row per move), exposes the match
 * history and owns the single "resume match" slot.
 *
 * A finished match writes a bounded number of rows and is normally called once
 * at the end of a game, never per animation frame.
 */
class GameRepository(
    private val gameDao: GameDao,
    private val moveDao: MoveDao,
    private val savedGameDao: SavedGameDao,
    private val statisticsRepository: StatisticsRepository,
    private val achievementRepository: AchievementRepository
) {

    data class SaveOutcome(
        val gameId: Long,
        val statistics: com.crownfall.realm.data.entities.GameStatisticsEntity,
        val achievements: List<com.crownfall.realm.data.entities.AchievementEntity>,
        val newlyUnlocked: List<com.crownfall.realm.data.entities.AchievementEntity>
    )

    /** Persists a completed match and folds it into the profile + achievements. */
    suspend fun recordFinishedGame(game: FinishedGame): SaveOutcome {
        val unlockedBefore = achievementRepository.all().filter { it.unlocked }.map { it.achievementId }.toSet()

        val gameId = gameDao.insert(
            GameEntity(
                player1 = game.playerName,
                player2 = game.opponentName,
                gameMode = game.mode.name,
                difficulty = (game.difficulty ?: Difficulty.HARD).name,
                startTime = game.startTime,
                endTime = game.endTime,
                winner = game.winnerName,
                result = game.result.name,
                totalMoves = game.moves.size
            )
        )

        if (game.moves.isNotEmpty()) {
            moveDao.insertAll(game.moves.map { it.toEntity(gameId) })
        }

        val stats = statisticsRepository.applyFinishedGame(game)
        val achievements = achievementRepository.syncFromStatistics(stats)
        val newlyUnlocked = achievements.filter { it.unlocked && it.achievementId !in unlockedBefore }

        // The match is finished, so there is nothing left to resume.
        clearInProgress()

        return SaveOutcome(gameId, stats, achievements, newlyUnlocked)
    }

    fun observeMatches(): Flow<List<MatchSummary>> =
        gameDao.observeAll().map { list -> list.map { it.toSummary() } }

    suspend fun matches(): List<MatchSummary> = gameDao.getAll().map { it.toSummary() }

    suspend fun match(gameId: Long): MatchSummary? = gameDao.getById(gameId)?.toSummary()

    suspend fun matchDetail(gameId: Long): MatchDetail? {
        val summary = gameDao.getById(gameId)?.toSummary() ?: return null
        val moves = moveDao.movesFor(gameId).map { it.toPlayedMove() }
        return MatchDetail(
            summary = summary,
            moves = moves,
            capturedByDawn = moves.count { it.side == Faction.DAWN && it.captured != null },
            capturedByDusk = moves.count { it.side == Faction.DUSK && it.captured != null }
        )
    }

    suspend fun movesFor(gameId: Long): List<PlayedMove> =
        moveDao.movesFor(gameId).map { it.toPlayedMove() }

    // ------------------------------------------------------- save / resume

    suspend fun saveInProgress(save: SavedGame) {
        savedGameDao.upsert(
            SavedGameEntity(
                id = 1L,
                fen = save.fen,
                movesEncoded = save.movesEncoded,
                sideToMove = save.sideToMove,
                gameMode = save.mode.name,
                difficulty = (save.difficulty ?: Difficulty.HARD).name,
                playerName = save.playerName,
                opponentName = save.opponentName,
                dawnTimeMs = save.dawnTimeMs,
                duskTimeMs = save.duskTimeMs,
                startedAt = save.startedAt,
                savedAt = save.savedAt
            )
        )
    }

    fun observeSavedGame(): Flow<SavedGame?> =
        savedGameDao.observe().map { it?.toSavedGame() }

    suspend fun loadInProgress(): SavedGame? = savedGameDao.get()?.toSavedGame()

    suspend fun clearInProgress() = savedGameDao.clear()

    suspend fun reset() {
        moveDao.clear()
        gameDao.clear()
        savedGameDao.clear()
    }

    // ------------------------------------------------------------- mapping

    private fun GameEntity.toSummary() = MatchSummary(
        gameId = gameId,
        player1 = player1,
        player2 = player2,
        mode = GameMode.fromName(gameMode),
        difficulty = difficulty?.let { Difficulty.fromName(it) },
        startTime = startTime,
        endTime = endTime,
        winner = winner,
        result = GameResult.fromName(result),
        totalMoves = totalMoves
    )

    private fun PlayedMove.toEntity(gameId: Long) = MoveEntity(
        gameId = gameId,
        moveNumber = moveNumber,
        player = if (side == Faction.DAWN) "White" else "Black",
        piece = piece.medievalName,
        startingPosition = from,
        destinationPosition = to,
        capturedPiece = captured?.medievalName,
        promotion = promotion?.medievalName,
        notation = notation,
        timestamp = timestamp
    )

    private fun MoveEntity.toPlayedMove() = PlayedMove(
        moveNumber = moveNumber,
        side = if (player.equals("Black", true)) Faction.DUSK else Faction.DAWN,
        piece = PieceType.entries.firstOrNull { it.medievalName == piece } ?: PieceType.PAWN,
        from = startingPosition,
        to = destinationPosition,
        captured = capturedPiece?.let { name -> PieceType.entries.firstOrNull { it.medievalName == name } },
        promotion = promotion?.let { name -> PieceType.entries.firstOrNull { it.medievalName == name } },
        notation = notation,
        timestamp = timestamp
    )

    private fun SavedGameEntity.toSavedGame() = SavedGame(
        fen = fen,
        movesEncoded = movesEncoded,
        sideToMove = sideToMove,
        mode = GameMode.fromName(gameMode),
        difficulty = Difficulty.fromName(difficulty),
        playerName = playerName,
        opponentName = opponentName,
        dawnTimeMs = dawnTimeMs,
        duskTimeMs = duskTimeMs,
        startedAt = startedAt,
        savedAt = savedAt
    )
}
