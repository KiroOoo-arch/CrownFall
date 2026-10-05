package com.crownfall.realm.domain.chess

/**
 * The rules of Crownfall. Pure functions over [GameState] / [Board]:
 * attack detection, pseudo-legal generation and full legal move generation.
 *
 * The engine never allows a move that leaves the mover's own king in check.
 */
object ChessRules {

    private val ORTHOGONAL = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
    private val DIAGONAL = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))
    private val ALL_DIRECTIONS = ORTHOGONAL + DIAGONAL
    private val KNIGHT_OFFSETS = arrayOf(
        intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
        intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)
    )

    /** True when any piece of [by] attacks [square]. */
    fun isSquareAttacked(board: Board, square: Square, by: Faction): Boolean {
        // Pawns: a pawn attacking [square] sits one rank behind it relative to its march.
        val pawnRank = square.rank - by.forward
        for (df in intArrayOf(-1, 1)) {
            val sq = Square(square.file + df, pawnRank)
            if (sq.isValid) {
                val p = board.pieceAt(sq)
                if (p != null && p.faction == by && p.type == PieceType.PAWN) return true
            }
        }

        for (offset in KNIGHT_OFFSETS) {
            val sq = Square(square.file + offset[0], square.rank + offset[1])
            if (!sq.isValid) continue
            val p = board.pieceAt(sq)
            if (p != null && p.faction == by && p.type == PieceType.KNIGHT) return true
        }

        for (offset in ALL_DIRECTIONS) {
            val sq = Square(square.file + offset[0], square.rank + offset[1])
            if (!sq.isValid) continue
            val p = board.pieceAt(sq)
            if (p != null && p.faction == by && p.type == PieceType.KING) return true
        }

        for (offset in ORTHOGONAL) {
            var f = square.file + offset[0]
            var r = square.rank + offset[1]
            while (f in 0..7 && r in 0..7) {
                val p = board.pieceAt(Square(f, r))
                if (p != null) {
                    if (p.faction == by && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                f += offset[0]; r += offset[1]
            }
        }

        for (offset in DIAGONAL) {
            var f = square.file + offset[0]
            var r = square.rank + offset[1]
            while (f in 0..7 && r in 0..7) {
                val p = board.pieceAt(Square(f, r))
                if (p != null) {
                    if (p.faction == by && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                f += offset[0]; r += offset[1]
            }
        }

        return false
    }

    fun isInCheck(board: Board, faction: Faction): Boolean {
        val king = board.kingSquare(faction) ?: return false
        return isSquareAttacked(board, king, faction.opponent)
    }

    /** Moves ignoring whether they leave the mover in check. */
    fun pseudoLegalMoves(state: GameState, from: Square): List<Move> {
        val piece = state.board.pieceAt(from) ?: return emptyList()
        if (piece.faction != state.sideToMove) return emptyList()
        val moves = ArrayList<Move>(16)
        when (piece.type) {
            PieceType.PAWN -> pawnMoves(state, from, piece, moves)
            PieceType.KNIGHT -> stepMoves(state, from, piece, KNIGHT_OFFSETS, moves)
            PieceType.KING -> {
                stepMoves(state, from, piece, ALL_DIRECTIONS, moves)
                castleMoves(state, from, piece, moves)
            }
            PieceType.BISHOP -> rayMoves(state, from, piece, DIAGONAL, moves)
            PieceType.ROOK -> rayMoves(state, from, piece, ORTHOGONAL, moves)
            PieceType.QUEEN -> rayMoves(state, from, piece, ALL_DIRECTIONS, moves)
        }
        return moves
    }

    /** Fully legal moves from [from] for the side to move. */
    fun legalMoves(state: GameState, from: Square): List<Move> =
        pseudoLegalMoves(state, from).filter { isLegal(state, it) }

    /** Every legal move for the side to move. */
    fun allLegalMoves(state: GameState): List<Move> {
        val out = ArrayList<Move>(64)
        for (square in Square.all) {
            val piece = state.board.pieceAt(square) ?: continue
            if (piece.faction != state.sideToMove) continue
            for (move in pseudoLegalMoves(state, square)) {
                if (isLegal(state, move)) out.add(move)
            }
        }
        return out
    }

    fun isLegal(state: GameState, move: Move): Boolean {
        val nextBoard = state.board.apply(move)
        return !isInCheck(nextBoard, move.piece.faction)
    }

    // ---------------------------------------------------------------- pawns

    private fun pawnMoves(state: GameState, from: Square, piece: Piece, out: MutableList<Move>) {
        val forward = piece.faction.forward
        val oneRank = from.rank + forward
        if (oneRank in 0..7) {
            val one = Square(from.file, oneRank)
            if (state.board.isEmpty(one)) {
                addPawnAdvance(state, from, one, piece, null, out)
                val twoRank = from.rank + forward * 2
                val pawnStartRank = piece.faction.homeRank + forward
                if (from.rank == pawnStartRank && twoRank in 0..7) {
                    val two = Square(from.file, twoRank)
                    if (state.board.isEmpty(two)) {
                        out.add(Move(from, two, piece))
                    }
                }
            }
            for (df in intArrayOf(-1, 1)) {
                val target = Square(from.file + df, oneRank)
                if (!target.isValid) continue
                val occupant = state.board.pieceAt(target)
                if (occupant != null && occupant.faction != piece.faction) {
                    addPawnAdvance(state, from, target, piece, occupant, out)
                } else if (occupant == null && state.enPassantTarget == target) {
                    val capturedPawn = Piece(PieceType.PAWN, piece.faction.opponent)
                    out.add(
                        Move(
                            from = from, to = target, piece = piece,
                            captured = capturedPawn, isEnPassant = true
                        )
                    )
                }
            }
        }
    }

    private fun addPawnAdvance(
        state: GameState,
        from: Square,
        to: Square,
        piece: Piece,
        captured: Piece?,
        out: MutableList<Move>
    ) {
        if (to.rank == piece.faction.promotionRank) {
            for (promotion in PROMOTION_CHOICES) {
                out.add(Move(from, to, piece, captured, promotion = promotion))
            }
        } else {
            out.add(Move(from, to, piece, captured))
        }
    }

    val PROMOTION_CHOICES = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)

    // ------------------------------------------------------- stepping pieces

    private fun stepMoves(
        state: GameState,
        from: Square,
        piece: Piece,
        offsets: Array<IntArray>,
        out: MutableList<Move>
    ) {
        for (offset in offsets) {
            val target = Square(from.file + offset[0], from.rank + offset[1])
            if (!target.isValid) continue
            val occupant = state.board.pieceAt(target)
            if (occupant == null) {
                out.add(Move(from, target, piece))
            } else if (occupant.faction != piece.faction) {
                out.add(Move(from, target, piece, occupant))
            }
        }
    }

    private fun rayMoves(
        state: GameState,
        from: Square,
        piece: Piece,
        directions: Array<IntArray>,
        out: MutableList<Move>
    ) {
        for (direction in directions) {
            var f = from.file + direction[0]
            var r = from.rank + direction[1]
            while (f in 0..7 && r in 0..7) {
                val target = Square(f, r)
                val occupant = state.board.pieceAt(target)
                if (occupant == null) {
                    out.add(Move(from, target, piece))
                } else {
                    if (occupant.faction != piece.faction) {
                        out.add(Move(from, target, piece, occupant))
                    }
                    break
                }
                f += direction[0]; r += direction[1]
            }
        }
    }

    // ----------------------------------------------------------- castling

    private fun castleMoves(state: GameState, from: Square, piece: Piece, out: MutableList<Move>) {
        val rank = piece.faction.homeRank
        if (from != Square(4, rank)) return
        // Cannot castle out of check.
        if (isInCheck(state.board, piece.faction)) return

        if (CastlingRights.canCastleKingside(state.castling, piece.faction)) {
            val f = Square(5, rank)
            val g = Square(6, rank)
            val rook = state.board.pieceAt(Square(7, rank))
            if (state.board.isEmpty(f) && state.board.isEmpty(g) &&
                rook?.type == PieceType.ROOK && rook.faction == piece.faction &&
                !isSquareAttacked(state.board, f, piece.faction.opponent) &&
                !isSquareAttacked(state.board, g, piece.faction.opponent)
            ) {
                out.add(Move(from, g, piece, isCastleKingside = true))
            }
        }

        if (CastlingRights.canCastleQueenside(state.castling, piece.faction)) {
            val b = Square(1, rank)
            val c = Square(2, rank)
            val d = Square(3, rank)
            val rook = state.board.pieceAt(Square(0, rank))
            if (state.board.isEmpty(b) && state.board.isEmpty(c) && state.board.isEmpty(d) &&
                rook?.type == PieceType.ROOK && rook.faction == piece.faction &&
                !isSquareAttacked(state.board, d, piece.faction.opponent) &&
                !isSquareAttacked(state.board, c, piece.faction.opponent)
            ) {
                out.add(Move(from, c, piece, isCastleQueenside = true))
            }
        }
    }

    // --------------------------------------------------- material draws

    /** Standard "dead position" draw detection. */
    fun isInsufficientMaterial(board: Board): Boolean {
        var knights = 0
        var bishops = 0
        var lightBishops = 0
        var darkBishops = 0
        val minorSides = HashSet<Faction>()

        for ((square, piece) in board.pieces()) {
            when (piece.type) {
                PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> return false
                PieceType.KNIGHT -> {
                    knights++
                    minorSides.add(piece.faction)
                }
                PieceType.BISHOP -> {
                    bishops++
                    minorSides.add(piece.faction)
                    if ((square.file + square.rank) % 2 == 0) darkBishops++ else lightBishops++
                }
                PieceType.KING -> Unit
            }
        }

        // King vs king, or king + single minor vs king.
        if (knights == 0 && bishops == 0) return true
        if (knights + bishops == 1) return true
        // Only bishops left and every bishop lives on the same colour complex.
        if (knights == 0 && bishops > 1 && (lightBishops == 0 || darkBishops == 0)) return true
        // King + bishop(s) vs king + bishop(s) with all bishops on one colour and no knights.
        if (knights == 0 && minorSides.size <= 2 && (lightBishops == 0 || darkBishops == 0)) return true

        return false
    }
}
