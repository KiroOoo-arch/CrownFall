package com.crownfall.realm.domain.chess

/**
 * Immutable 8x8 piece placement. Square indexing is rank-major (rank 0 first).
 */
class Board private constructor(private val cells: Array<Piece?>) {

    /** Cached Zobrist hash of just the piece placement. */
    val placementHash: Long by lazy(LazyThreadSafetyMode.NONE) { Zobrist.hashPlacement(this) }

    fun pieceAt(square: Square): Piece? =
        if (square.isValid) cells[square.index] else null

    fun isEmpty(square: Square): Boolean = pieceAt(square) == null

    fun hasPieceAt(square: Square, faction: Faction? = null): Boolean {
        val p = pieceAt(square) ?: return false
        return faction == null || p.faction == faction
    }

    fun pieces(): List<Pair<Square, Piece>> {
        val out = ArrayList<Pair<Square, Piece>>(32)
        for (i in 0..63) {
            val p = cells[i] ?: continue
            out.add(Square.of(i) to p)
        }
        return out
    }

    fun kingSquare(faction: Faction): Square? {
        for (i in 0..63) {
            val p = cells[i] ?: continue
            if (p.type == PieceType.KING && p.faction == faction) return Square.of(i)
        }
        return null
    }

    fun count(faction: Faction): Int = cells.count { it?.faction == faction }

    /** Produces a new board with [move] applied, including castling rook travel and en passant capture. */
    fun apply(move: Move): Board {
        val next = cells.copyOf()
        val moving = next[move.from.index] ?: move.piece
        next[move.from.index] = null
        next[move.to.index] = if (move.promotion != null) {
            Piece(move.promotion!!, moving.faction)
        } else {
            moving
        }
        if (move.isEnPassant) {
            // The captured pawn sits beside the destination square, on the moving pawn's rank.
            next[Square(move.to.file, move.from.rank).index] = null
        }
        if (move.isCastleKingside) {
            val rank = move.from.rank
            next[Square(7, rank).index]?.let {
                next[Square(5, rank).index] = it
                next[Square(7, rank).index] = null
            }
        }
        if (move.isCastleQueenside) {
            val rank = move.from.rank
            next[Square(0, rank).index]?.let {
                next[Square(3, rank).index] = it
                next[Square(0, rank).index] = null
            }
        }
        return Board(next)
    }

    fun copy(): Board = Board(cells.copyOf())

    /** Places/removes a piece; used by tests and FEN loading. */
    fun withPiece(square: Square, piece: Piece?): Board {
        val next = cells.copyOf()
        next[square.index] = piece
        return Board(next)
    }

    override fun equals(other: Any?): Boolean =
        other is Board && cells.contentEquals(other.cells)

    override fun hashCode(): Int = cells.contentHashCode()

    override fun toString(): String = buildString {
        for (rank in 7 downTo 0) {
            append(rank + 1).append(' ')
            for (file in 0..7) {
                val p = cells[rank * 8 + file]
                append(p?.let { charFor(it) } ?: '.').append(' ')
            }
            append('\n')
        }
        append("  a b c d e f g h")
    }

    companion object {
        private fun charFor(piece: Piece): Char {
            val c = piece.type.symbol
            return if (piece.faction == Faction.DAWN) c.uppercaseChar() else c
        }

        fun empty(): Board = Board(arrayOfNulls(64))

        fun ofPieces(pieces: Map<Square, Piece>): Board {
            val cells = arrayOfNulls<Piece>(64)
            pieces.forEach { (sq, piece) -> cells[sq.index] = piece }
            return Board(cells)
        }

        /** Standard opening array for "Crownfall". */
        fun initial(): Board {
            val backRank = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
            )
            val cells = arrayOfNulls<Piece>(64)
            for (file in 0..7) {
                cells[Square(file, 0).index] = Piece(backRank[file], Faction.DAWN)
                cells[Square(file, 1).index] = Piece(PieceType.PAWN, Faction.DAWN)
                cells[Square(file, 6).index] = Piece(PieceType.PAWN, Faction.DUSK)
                cells[Square(file, 7).index] = Piece(backRank[file], Faction.DUSK)
            }
            return Board(cells)
        }
    }
}
