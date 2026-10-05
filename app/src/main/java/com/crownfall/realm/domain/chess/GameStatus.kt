package com.crownfall.realm.domain.chess

/** Lifecycle state of a match. */
enum class GameStatus {
    ONGOING,
    CHECK,
    CHECKMATE,
    STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_REPETITION,
    DRAW_AGREEMENT,
    RESIGNATION,
    ABANDONED;

    val isFinished: Boolean
        get() = this != ONGOING && this != CHECK

    val isDraw: Boolean
        get() = this == STALEMATE || this == DRAW_FIFTY_MOVE ||
            this == DRAW_INSUFFICIENT_MATERIAL || this == DRAW_REPETITION ||
            this == DRAW_AGREEMENT
}

/** Bitmask castling rights so state copies stay cheap. */
object CastlingRights {
    const val NONE = 0
    const val DAWN_KINGSIDE = 1
    const val DAWN_QUEENSIDE = 2
    const val DUSK_KINGSIDE = 4
    const val DUSK_QUEENSIDE = 8
    const val ALL = DAWN_KINGSIDE or DAWN_QUEENSIDE or DUSK_KINGSIDE or DUSK_QUEENSIDE

    fun canCastleKingside(rights: Int, faction: Faction): Boolean =
        (rights and if (faction == Faction.DAWN) DAWN_KINGSIDE else DUSK_KINGSIDE) != 0

    fun canCastleQueenside(rights: Int, faction: Faction): Boolean =
        (rights and if (faction == Faction.DAWN) DAWN_QUEENSIDE else DUSK_QUEENSIDE) != 0

    /** Removes rights invalidated by [move] (king/rook moves or a rook being captured). */
    fun updatedAfter(rights: Int, move: Move): Int {
        var r = rights
        val f = move.piece.faction
        if (move.piece.type == PieceType.KING) {
            val clear = if (f == Faction.DAWN) DAWN_KINGSIDE or DAWN_QUEENSIDE
            else DUSK_KINGSIDE or DUSK_QUEENSIDE
            r = r and (ALL xor clear)
        }
        // A rook leaving its home square.
        if (move.piece.type == PieceType.ROOK) {
            r = r and removeRightsForSquare(move.from)
        }
        // A rook being captured on its home square.
        r = r and removeRightsForSquare(move.to)
        return r
    }

    private fun removeRightsForSquare(square: Square): Int = when {
        square == Square(0, 0) -> ALL xor DAWN_QUEENSIDE
        square == Square(7, 0) -> ALL xor DAWN_KINGSIDE
        square == Square(0, 7) -> ALL xor DUSK_QUEENSIDE
        square == Square(7, 7) -> ALL xor DUSK_KINGSIDE
        else -> ALL
    }
}
