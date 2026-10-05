package com.crownfall.realm.domain.chess

/**
 * Complete, immutable description of a match at one point in time.
 * Pure domain data: no Android imports, fully unit testable.
 */
data class GameState(
    val board: Board,
    val sideToMove: Faction,
    val castling: Int,
    val enPassantTarget: Square?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
    val status: GameStatus,
    val winner: Faction?,
    val moveLog: MoveLog
) {

    val positionKey: Long = board.placementHash xor
        Zobrist.sideKey(sideToMove) xor
        Zobrist.castlingKey(castling) xor
        Zobrist.enPassantKey(enPassantTarget)

    val moveCount: Int get() = moveLog.size

    val isFinished: Boolean get() = status.isFinished

    val isCheck: Boolean get() = status == GameStatus.CHECK

    val isInCheck: Boolean get() = isCheck

    fun pieceAt(square: Square): Piece? = board.pieceAt(square)

    val lastMove: Move? get() = moveLog.lastMove()

    /** Name of the side that is currently allowed to move. */
    val activeFactionName: String get() = sideToMove.factionName

    companion object {
        /** A fresh game in the standard starting array. */
        fun initial(): GameState = GameState(
            board = Board.initial(),
            sideToMove = Faction.DAWN,
            castling = CastlingRights.ALL,
            enPassantTarget = null,
            halfmoveClock = 0,
            fullmoveNumber = 1,
            status = GameStatus.ONGOING,
            winner = null,
            moveLog = MoveLog.empty
        )

        /** A blank battlefield, mainly used by tests. */
        fun empty(): GameState = GameState(
            board = Board.empty(),
            sideToMove = Faction.DAWN,
            castling = CastlingRights.NONE,
            enPassantTarget = null,
            halfmoveClock = 0,
            fullmoveNumber = 1,
            status = GameStatus.ONGOING,
            winner = null,
            moveLog = MoveLog.empty
        )
    }
}

/** Convenience for building positions in tests. */
fun position(
    pieces: Map<String, Pair<PieceType, Faction>>,
    sideToMove: Faction = Faction.DAWN,
    castling: Int = CastlingRights.NONE,
    enPassantTarget: Square? = null,
    halfmoveClock: Int = 0,
    fullmoveNumber: Int = 1
): GameState {
    val map = pieces.entries.associate { (text, value) ->
        requireNotNull(Square.fromAlgebraic(text)) { "bad square $text" } to Piece(value.first, value.second)
    }
    return GameState(
        board = Board.ofPieces(map),
        sideToMove = sideToMove,
        castling = castling,
        enPassantTarget = enPassantTarget,
        halfmoveClock = halfmoveClock,
        fullmoveNumber = fullmoveNumber,
        status = GameStatus.ONGOING,
        winner = null,
        moveLog = MoveLog.empty
    )
}
