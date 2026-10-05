package com.crownfall.realm.domain.models

import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.PieceType

/** How a match was played. */
enum class GameMode(val displayName: String) {
    SINGLE_PLAYER("Single Player"),
    TWO_PLAYER("Two Player"),
    CONTINUED("Continued Match");

    companion object {
        fun fromName(name: String?): GameMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SINGLE_PLAYER
    }
}

/** The result from the local (Dawn) player's point of view. */
enum class GameResult(val displayName: String) {
    VICTORY("Victory"),
    DEFEAT("Defeat"),
    DRAW("Draw"),
    IN_PROGRESS("In Progress");

    companion object {
        fun fromName(name: String?): GameResult =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: IN_PROGRESS
    }
}

/** One recorded half-move, strongly typed so statistics can be aggregated from it. */
data class PlayedMove(
    val moveNumber: Int,
    val side: Faction,
    val piece: PieceType,
    val from: String,
    val to: String,
    val captured: PieceType?,
    val promotion: PieceType?,
    val notation: String,
    val timestamp: Long
) {
    /** "White Knight -> C3" style text used by the history screen. */
    val displayLine: String
        get() = buildString {
            append(if (side == Faction.DAWN) "White" else "Black")
            append(' ')
            append(piece.medievalName)
            append(" → ")
            append(to.uppercase())
            if (captured != null) append(" (captures ${captured.medievalName})")
            promotion?.let { append(" promotes to ${it.medievalName}") }
        }
}

/** Everything the repository needs to persist a completed match. */
data class FinishedGame(
    val mode: GameMode,
    val difficulty: Difficulty?,
    val playerName: String,
    val opponentName: String,
    val startTime: Long,
    val endTime: Long,
    val winnerName: String?,
    val result: GameResult,
    val moves: List<PlayedMove>,
    val dawnLostQueen: Boolean,
    val checkmateDelivered: Boolean
)

/** A resumable match. */
data class SavedGame(
    val fen: String,
    val movesEncoded: String,
    val sideToMove: String,
    val mode: GameMode,
    val difficulty: Difficulty?,
    val playerName: String,
    val opponentName: String,
    val dawnTimeMs: Long,
    val duskTimeMs: Long,
    val startedAt: Long,
    val savedAt: Long
)

/** A match plus its full move list, shown on the history detail screen. */
data class MatchDetail(
    val summary: MatchSummary,
    val moves: List<PlayedMove>,
    val capturedByDawn: Int,
    val capturedByDusk: Int
)

/** Row model for the match history list. */
data class MatchSummary(
    val gameId: Long,
    val player1: String,
    val player2: String,
    val mode: GameMode,
    val difficulty: Difficulty?,
    val startTime: Long,
    val endTime: Long,
    val winner: String?,
    val result: GameResult,
    val totalMoves: Int
) {
    val durationMs: Long get() = (endTime - startTime).coerceAtLeast(0L)

    val opponentLine: String
        get() = if (mode == GameMode.TWO_PLAYER) "$player1 vs $player2" else "$player1 vs $player2"
}

/** One row on the statistics screen. */
data class PieceStat(
    val pieceType: String,
    val medievalName: String,
    val gamesUsed: Int,
    val moves: Int,
    val captures: Int,
    val deaths: Int
)

/** The player profile shown on the statistics screen. */
data class PlayerProfile(
    val name: String,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val rating: Int,
    val favouritePiece: String,
    val totalCaptures: Int,
    val bestWinStreak: Int,
    val currentWinStreak: Int,
    val pieceStats: List<PieceStat>
) {
    val winRate: Float
        get() = if (gamesPlayed <= 0) 0f else (wins.toFloat() * 100f / gamesPlayed)

    val winRateText: String get() = String.format(java.util.Locale.US, "%.1f%%", winRate)
}
