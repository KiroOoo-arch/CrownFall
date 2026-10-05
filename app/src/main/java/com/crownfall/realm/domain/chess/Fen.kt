package com.crownfall.realm.domain.chess

/** Forsyth-Edwards notation for Crownfall positions. */
object Fen {

    fun toFen(state: GameState): String {
        val placement = buildString {
            for (rank in 7 downTo 0) {
                var empty = 0
                for (file in 0..7) {
                    val piece = state.board.pieceAt(Square(file, rank))
                    if (piece == null) {
                        empty++
                    } else {
                        if (empty > 0) {
                            append(empty); empty = 0
                        }
                        val symbol = piece.type.symbol
                        append(if (piece.faction == Faction.DAWN) symbol.uppercaseChar() else symbol)
                    }
                }
                if (empty > 0) append(empty)
                if (rank > 0) append('/')
            }
        }

        val active = if (state.sideToMove == Faction.DAWN) "w" else "b"

        val castling = buildString {
            if (CastlingRights.canCastleKingside(state.castling, Faction.DAWN)) append('K')
            if (CastlingRights.canCastleQueenside(state.castling, Faction.DAWN)) append('Q')
            if (CastlingRights.canCastleKingside(state.castling, Faction.DUSK)) append('k')
            if (CastlingRights.canCastleQueenside(state.castling, Faction.DUSK)) append('q')
            if (isEmpty()) append('-')
        }

        val ep = state.enPassantTarget?.algebraic ?: "-"
        return "$placement $active $castling $ep ${state.halfmoveClock} ${state.fullmoveNumber}"
    }

    fun parse(fen: String): GameState {
        val parts = fen.trim().split(" ").filter { it.isNotEmpty() }
        require(parts.size >= 4) { "Invalid FEN: $fen" }

        val cells = arrayOfNulls<Piece>(64)
        val rows = parts[0].split('/')
        require(rows.size == 8) { "Invalid FEN board: $fen" }
        for ((rowIndex, row) in rows.withIndex()) {
            val rank = 7 - rowIndex
            var file = 0
            for (ch in row) {
                if (ch.isDigit()) {
                    file += ch.digitToInt()
                } else {
                    val faction = if (ch.isUpperCase()) Faction.DAWN else Faction.DUSK
                    val type = requireNotNull(PieceType.fromSymbol(ch)) { "Bad piece '$ch' in FEN" }
                    if (file in 0..7) cells[Square(file, rank).index] = Piece(type, faction)
                    file++
                }
            }
        }

        var castling = CastlingRights.NONE
        if (parts[2] != "-") {
            for (ch in parts[2]) {
                castling = castling or when (ch) {
                    'K' -> CastlingRights.DAWN_KINGSIDE
                    'Q' -> CastlingRights.DAWN_QUEENSIDE
                    'k' -> CastlingRights.DUSK_KINGSIDE
                    'q' -> CastlingRights.DUSK_QUEENSIDE
                    else -> 0
                }
            }
        }

        val state = GameState(
            board = Board.ofPieces(cells.mapIndexedNotNull { index, piece ->
                if (piece == null) null else Square.of(index) to piece
            }.toMap()),
            sideToMove = if (parts[1] == "b") Faction.DUSK else Faction.DAWN,
            castling = castling,
            enPassantTarget = Square.fromAlgebraic(parts[3]),
            halfmoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0,
            fullmoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1,
            status = GameStatus.ONGOING,
            winner = null,
            moveLog = MoveLog.empty
        )
        return GameEngine().resolve(state, detectRepetition = false)
    }
}
