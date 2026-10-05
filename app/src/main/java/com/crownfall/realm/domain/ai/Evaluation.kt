package com.crownfall.realm.domain.ai

import com.crownfall.realm.domain.chess.Board
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.GameState
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.chess.Square

/**
 * Positional + material evaluation used by every difficulty.
 * Scores are centipawns from the point of view of [GameState.sideToMove].
 */
object Evaluation {

    const val MATE_SCORE = 1_000_000
    const val INFINITY = 10_000_000

    const val KING = 0
    const val QUEEN = 950
    const val ROOK = 520
    const val BISHOP = 340
    const val KNIGHT = 330
    const val PAWN = 100

    // Tables are written like standard references: index 0 = a8 ... index 63 = h1.
    private val PAWN_TABLE = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5, 5, 10, 25, 25, 10, 5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, -5, -10, 0, 0, -10, -5, 5,
        5, 10, 10, -20, -20, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0
    )

    private val KNIGHT_TABLE = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    )

    private val BISHOP_TABLE = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10,
        -10, 0, 10, 10, 10, 10, 0, -10,
        -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 5, 0, 0, 0, 0, 5, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    )

    private val ROOK_TABLE = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        5, 10, 10, 10, 10, 10, 10, 5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        0, 0, 0, 5, 5, 0, 0, 0
    )

    private val QUEEN_TABLE = intArrayOf(
        -20, -10, -10, -5, -5, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 5, 5, 5, 0, -10,
        -5, 0, 5, 5, 5, 5, 0, -5,
        0, 0, 5, 5, 5, 5, 0, -5,
        -10, 5, 5, 5, 5, 5, 0, -10,
        -10, 0, 5, 0, 0, 0, 0, -10,
        -20, -10, -10, -5, -5, -10, -10, -20
    )

    private val KING_MIDDLE_TABLE = intArrayOf(
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -20, -30, -30, -40, -40, -30, -30, -20,
        -10, -20, -20, -20, -20, -20, -20, -10,
        20, 20, 0, 0, 0, 0, 20, 20,
        20, 30, 10, 0, 0, 10, 30, 20
    )

    private val KING_END_TABLE = intArrayOf(
        -50, -40, -30, -20, -20, -30, -40, -50,
        -30, -20, -10, 0, 0, -10, -20, -30,
        -30, -10, 20, 30, 30, 20, -10, -30,
        -30, -10, 30, 40, 40, 30, -10, -30,
        -30, -10, 30, 40, 40, 30, -10, -30,
        -30, -10, 20, 30, 30, 20, -10, -30,
        -30, -30, 0, 0, 0, 0, -30, -30,
        -50, -30, -30, -30, -30, -30, -30, -50
    )

    fun materialValue(type: PieceType): Int = when (type) {
        PieceType.KING -> KING
        PieceType.QUEEN -> QUEEN
        PieceType.ROOK -> ROOK
        PieceType.BISHOP -> BISHOP
        PieceType.KNIGHT -> KNIGHT
        PieceType.PAWN -> PAWN
    }

    fun evaluate(state: GameState): Int {
        val board = state.board
        val scoreForDawn = evaluateFor(board, Faction.DAWN)
        val scoreForDusk = evaluateFor(board, Faction.DUSK)
        val diff = scoreForDawn - scoreForDusk
        val tempo = 8
        return if (state.sideToMove == Faction.DAWN) diff + tempo else -diff + tempo
    }

    private fun evaluateFor(board: Board, faction: Faction): Int {
        var score = 0
        var materialWithoutKings = 0
        for ((_, piece) in board.pieces()) {
            if (piece.type != PieceType.KING && piece.type != PieceType.PAWN) {
                materialWithoutKings += materialValue(piece.type)
            }
        }
        val endgame = materialWithoutKings <= 2 * (ROOK + KNIGHT)

        var bishops = 0
        var pawns = 0
        for ((square, piece) in board.pieces()) {
            if (piece.faction != faction) continue
            val value = materialValue(piece.type)
            score += value
            score += when (piece.type) {
                PieceType.PAWN -> {
                    pawns++
                    table(PAWN_TABLE, square, faction)
                }
                PieceType.KNIGHT -> table(KNIGHT_TABLE, square, faction)
                PieceType.BISHOP -> {
                    bishops++
                    table(BISHOP_TABLE, square, faction)
                }
                PieceType.ROOK -> table(ROOK_TABLE, square, faction)
                PieceType.QUEEN -> table(QUEEN_TABLE, square, faction)
                PieceType.KING -> table(if (endgame) KING_END_TABLE else KING_MIDDLE_TABLE, square, faction)
            }
        }

        // Bishop pair.
        if (bishops >= 2) score += 35

        // Pawn structure: doubled pawns are a liability.
        for (file in 0..7) {
            var count = 0
            for (rank in 0..7) {
                val piece = board.pieceAt(Square(file, rank)) ?: continue
                if (piece.faction == faction && piece.type == PieceType.PAWN) count++
            }
            if (count > 1) score -= 18 * (count - 1)
        }

        // King safety: reward a surviving pawn shield in the opening/middlegame.
        if (!endgame) {
            val king = board.kingSquare(faction)
            if (king != null) {
                var shield = 0
                val ahead = king.rank + faction.forward
                for (df in -1..1) {
                    val sq = Square(king.file + df, ahead)
                    if (sq.isValid) {
                        val p = board.pieceAt(sq)
                        if (p != null && p.faction == faction && p.type == PieceType.PAWN) shield++
                    }
                }
                score += shield * 12
                if (pawns > 0 && shield == 0) score -= 25
            }
        }

        return score
    }

    private fun table(values: IntArray, square: Square, faction: Faction): Int {
        val index = if (faction == Faction.DAWN) square.index xor 56 else square.index
        return values[index]
    }
}
