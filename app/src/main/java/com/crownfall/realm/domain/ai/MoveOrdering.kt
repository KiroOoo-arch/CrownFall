package com.crownfall.realm.domain.ai

import com.crownfall.realm.domain.chess.Move

/**
 * Move ordering: transposition move first, then captures by MVV-LVA, then
 * promotions, then quiet moves scored by the history heuristic. Good ordering
 * is what makes alpha-beta pruning effective on a phone.
 */
object MoveOrdering {

    fun pack(move: Move): Int {
        val from = move.from.index and 0x3F
        val to = move.to.index and 0x3F
        val promo = (move.promotion?.ordinal ?: 0) and 0x7
        return (from shl 12) or (to shl 6) or promo or (if (move.piece.type == com.crownfall.realm.domain.chess.PieceType.PAWN) 0x8000 else 0)
    }

    fun unpackFrom(packed: Int): Int = (packed ushr 12) and 0x3F

    fun unpackTo(packed: Int): Int = (packed ushr 6) and 0x3F

    fun score(move: Move, ttMove: Int, history: IntArray): Int {
        if (ttMove != 0 && pack(move) == ttMove) return 2_000_000
        if (move.isCapture) {
            val victim = Evaluation.materialValue(move.captured?.type ?: com.crownfall.realm.domain.chess.PieceType.PAWN)
            val attacker = Evaluation.materialValue(move.piece.type)
            return 1_000_000 + victim * 12 - attacker
        }
        move.promotion?.let { return 900_000 + Evaluation.materialValue(it) }
        if (move.isCastle) return 800_000
        return history[move.from.index * 64 + move.to.index]
    }

    fun ordered(moves: List<Move>, ttMove: Int, history: IntArray): List<Move> {
        if (moves.size <= 1) return moves
        return moves.sortedByDescending { score(it, ttMove, history) }
    }
}
