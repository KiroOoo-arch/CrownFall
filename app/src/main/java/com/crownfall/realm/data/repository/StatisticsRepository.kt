package com.crownfall.realm.data.repository

import com.crownfall.realm.data.dao.GameStatisticsDao
import com.crownfall.realm.data.dao.PieceStatisticsDao
import com.crownfall.realm.data.dao.PlayerDao
import com.crownfall.realm.data.entities.GameStatisticsEntity
import com.crownfall.realm.data.entities.PieceStatisticsEntity
import com.crownfall.realm.data.entities.PlayerEntity
import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.models.FinishedGame
import com.crownfall.realm.domain.models.GameResult
import com.crownfall.realm.domain.models.PieceStat
import com.crownfall.realm.domain.models.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Owns the player profile, lifetime aggregates and per-piece career records.
 * All counters are plain integers so a finished match only touches a handful
 * of rows regardless of how long the game was.
 */
class StatisticsRepository(
    private val playerDao: PlayerDao,
    private val statisticsDao: GameStatisticsDao,
    private val pieceStatisticsDao: PieceStatisticsDao
) {

    suspend fun ensureSeeded(name: String = "Realm Champion", difficulty: Difficulty = Difficulty.HARD) {
        if (playerDao.get(1L) == null) {
            playerDao.upsert(PlayerEntity(name = name, selectedDifficulty = difficulty.name))
        }
        if (statisticsDao.get() == null) {
            statisticsDao.upsert(GameStatisticsEntity())
        }
        seedPieceStatistics()
    }

    private suspend fun seedPieceStatistics() {
        for (type in PieceType.entries) {
            if (pieceStatisticsDao.get(type.name) == null) {
                pieceStatisticsDao.upsert(
                    PieceStatisticsEntity(pieceType = type.name, medievalName = type.medievalName)
                )
            }
        }
    }

    fun observeProfile(): Flow<PlayerProfile> =
        combine(
            playerDao.observe(),
            statisticsDao.observe(),
            pieceStatisticsDao.observeAll()
        ) { player, stats, pieces ->
            buildProfile(player, stats, pieces)
        }

    suspend fun profile(): PlayerProfile = buildProfile(
        playerDao.get(1L),
        statisticsDao.get(),
        pieceStatisticsDao.getAll()
    )

    /** Current aggregate row, creating the defaults if the table is empty. */
    suspend fun currentStatistics(): GameStatisticsEntity {
        val existing = statisticsDao.get()
        if (existing != null) return existing
        val seeded = GameStatisticsEntity()
        statisticsDao.upsert(seeded)
        return seeded
    }

    suspend fun pieceStats(): List<PieceStat> = pieceStatisticsDao.getAll()
        .sortedByDescending { it.captures * 3 + it.moves }
        .map { it.toModel() }

    suspend fun updatePlayerName(name: String) {
        val current = playerDao.get(1L) ?: PlayerEntity(name = name)
        playerDao.upsert(current.copy(name = name))
    }

    suspend fun selectDifficulty(difficulty: Difficulty) {
        val current = playerDao.get(1L) ?: PlayerEntity(name = "Realm Champion")
        playerDao.upsert(current.copy(selectedDifficulty = difficulty.name))
    }

    /**
     * Applies a completed match to every counter and returns the fresh
     * aggregate row, which the achievement layer then evaluates.
     */
    suspend fun applyFinishedGame(game: FinishedGame): GameStatisticsEntity {
        // --- per piece career records -------------------------------------
        val perPiece = LinkedHashMap<PieceType, PieceStatisticsEntity>()
        for (type in PieceType.entries) {
            perPiece[type] = pieceStatisticsDao.get(type.name)
                ?: PieceStatisticsEntity(pieceType = type.name, medievalName = type.medievalName)
        }
        val dawnMoves = HashMap<PieceType, Int>()
        var dawnCaptures = 0
        var knightCaptures = 0

        for (move in game.moves) {
            val mover = perPiece.getValue(move.piece)
            perPiece[move.piece] = mover.copy(moves = mover.moves + 1)
            if (move.side == Faction.DAWN) {
                dawnMoves[move.piece] = (dawnMoves[move.piece] ?: 0) + 1
            }
            move.captured?.let { victim ->
                val victimStat = perPiece.getValue(victim)
                perPiece[victim] = victimStat.copy(deaths = victimStat.deaths + 1)
                val attacker = perPiece.getValue(move.piece)
                perPiece[move.piece] = attacker.copy(captures = attacker.captures + 1)
                if (move.side == Faction.DAWN) {
                    dawnCaptures++
                    if (move.piece == PieceType.KNIGHT) knightCaptures++
                }
            }
        }
        // gamesUsed: a piece was "used" in any game where it moved at least once.
        val usedThisGame = game.moves.map { it.piece }.toSet()
        for (type in usedThisGame) {
            val stat = perPiece.getValue(type)
            perPiece[type] = stat.copy(gamesUsed = stat.gamesUsed + 1)
        }
        pieceStatisticsDao.upsertAll(perPiece.values.toList())

        // --- aggregates ----------------------------------------------------
        val stats = statisticsDao.get() ?: GameStatisticsEntity()
        val isDawnPlayer = game.mode != com.crownfall.realm.domain.models.GameMode.TWO_PLAYER
        val isSinglePlayer = game.mode == com.crownfall.realm.domain.models.GameMode.SINGLE_PLAYER
        val win = game.result == GameResult.VICTORY
        val loss = game.result == GameResult.DEFEAT
        val draw = game.result == GameResult.DRAW

        var streak = stats.currentWinStreak
        streak = when {
            win -> streak + 1
            loss -> 0
            else -> streak
        }

        val favourite = (if (isDawnPlayer) dawnMoves else emptyMap())
            .maxByOrNull { it.value }?.key?.medievalName
            ?: stats.favouritePiece

        val updated = stats.copy(
            gamesPlayed = stats.gamesPlayed + 1,
            wins = stats.wins + if (win) 1 else 0,
            losses = stats.losses + if (loss) 1 else 0,
            draws = stats.draws + if (draw) 1 else 0,
            totalCaptures = stats.totalCaptures + dawnCaptures,
            knightCaptures = stats.knightCaptures + knightCaptures,
            checkmatesDelivered = stats.checkmatesDelivered + if (game.checkmateDelivered) 1 else 0,
            winsWithoutQueenLoss = stats.winsWithoutQueenLoss +
                if (win && !game.dawnLostQueen) 1 else 0,
            expertWins = stats.expertWins +
                if (win && isSinglePlayer && game.difficulty == Difficulty.EXPERT) 1 else 0,
            masterWins = stats.masterWins +
                if (win && isSinglePlayer && game.difficulty == Difficulty.MASTER) 1 else 0,
            currentWinStreak = streak,
            bestWinStreak = maxOf(stats.bestWinStreak, streak),
            favouritePiece = favourite,
            totalMoves = stats.totalMoves + game.moves.size
        )
        statisticsDao.upsert(updated)

        // --- player row ----------------------------------------------------
        val player = playerDao.get(1L) ?: PlayerEntity(name = game.playerName)
        playerDao.upsert(
            player.copy(
                // A local duel has two commanders, so it must not rename the
                // single persistent profile; only solo battles carry a name.
                name = if (isSinglePlayer) game.playerName else player.name,
                gamesPlayed = player.gamesPlayed + 1,
                gamesWon = player.gamesWon + if (win) 1 else 0,
                gamesLost = player.gamesLost + if (loss) 1 else 0,
                draws = player.draws + if (draw) 1 else 0,
                rating = player.rating + ratingDelta(game)
            )
        )

        return updated
    }

    private fun ratingDelta(game: FinishedGame): Int {
        val base = when (game.result) {
            GameResult.VICTORY -> 25
            GameResult.DEFEAT -> -20
            GameResult.DRAW -> 5
            GameResult.IN_PROGRESS -> 0
        }
        val bonus = when (game.difficulty) {
            Difficulty.MASTER -> 25
            Difficulty.EXPERT -> 15
            Difficulty.HARD -> 8
            Difficulty.BEGINNER -> 0
            null -> 0
        }
        return if (game.result == GameResult.VICTORY) base + bonus else base
    }

    suspend fun reset() {
        playerDao.clear()
        statisticsDao.clear()
        pieceStatisticsDao.clear()
        ensureSeeded()
    }

    private fun buildProfile(
        player: PlayerEntity?,
        stats: GameStatisticsEntity?,
        pieces: List<PieceStatisticsEntity>
    ): PlayerProfile = PlayerProfile(
        name = player?.name ?: "Realm Champion",
        gamesPlayed = stats?.gamesPlayed ?: 0,
        wins = stats?.wins ?: 0,
        losses = stats?.losses ?: 0,
        draws = stats?.draws ?: 0,
        rating = player?.rating ?: 1200,
        favouritePiece = stats?.favouritePiece ?: "Foot Soldier",
        totalCaptures = stats?.totalCaptures ?: 0,
        bestWinStreak = stats?.bestWinStreak ?: 0,
        currentWinStreak = stats?.currentWinStreak ?: 0,
        pieceStats = pieces.sortedByDescending { it.captures * 3 + it.moves }.map { it.toModel() }
    )

    private fun PieceStatisticsEntity.toModel() = PieceStat(
        pieceType = pieceType,
        medievalName = medievalName,
        gamesUsed = gamesUsed,
        moves = moves,
        captures = captures,
        deaths = deaths
    )
}

