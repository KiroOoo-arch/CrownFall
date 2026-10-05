package com.crownfall.realm.domain.chess

/**
 * Drives a match. Keeps all rules in one auditable place and exposes only
 * immutable [GameState] transitions, so the UI and the AI share exactly the
 * same rules and the AI can never cheat.
 */
class GameEngine {

    fun newGame(): GameState = GameState.initial()

    /**
     * Fast, side-effect-free application of a move. Does not compute
     * check/mate status - call [resolve] for that. AI search uses this.
     */
    fun applyMove(state: GameState, move: Move): GameState {
        val board = state.board.apply(move)
        val castling = CastlingRights.updatedAfter(state.castling, move)
        val doublePush = move.piece.type == PieceType.PAWN &&
            kotlin.math.abs(move.to.rank - move.from.rank) == 2
        val enPassant = if (doublePush) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else {
            null
        }
        val halfmove = if (move.piece.type == PieceType.PAWN || move.isCapture) 0 else state.halfmoveClock + 1
        val fullmove = if (state.sideToMove == Faction.DUSK) state.fullmoveNumber + 1 else state.fullmoveNumber

        return state.copy(
            board = board,
            sideToMove = state.sideToMove.opponent,
            castling = castling,
            enPassantTarget = enPassant,
            halfmoveClock = halfmove,
            fullmoveNumber = fullmove,
            status = GameStatus.ONGOING,
            winner = null,
            moveLog = state.moveLog.append(move)
        )
    }

    /** Applies a move and evaluates the resulting status. Used for real gameplay. */
    fun play(state: GameState, move: Move): GameState = resolve(applyMove(state, move))

    /** Locates the fully legal move between two squares, if one exists. */
    fun findMove(state: GameState, from: Square, to: Square, promotion: PieceType? = null): Move? {
        val candidates = ChessRules.legalMoves(state, from).filter { it.to == to }
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()
        return candidates.firstOrNull { it.promotion == (promotion ?: PieceType.QUEEN) }
            ?: candidates.first()
    }

    /**
     * Evaluates check / checkmate / stalemate / draw conditions for the
     * side that is about to move.
     */
    fun resolve(state: GameState, detectRepetition: Boolean = true): GameState {
        if (state.isFinished) return state

        val legal = ChessRules.allLegalMoves(state)
        val inCheck = ChessRules.isInCheck(state.board, state.sideToMove)

        if (legal.isEmpty()) {
            return if (inCheck) {
                state.copy(status = GameStatus.CHECKMATE, winner = state.sideToMove.opponent)
            } else {
                state.copy(status = GameStatus.STALEMATE, winner = null)
            }
        }

        if (state.halfmoveClock >= 100) {
            return state.copy(status = GameStatus.DRAW_FIFTY_MOVE, winner = null)
        }

        if (ChessRules.isInsufficientMaterial(state.board)) {
            return state.copy(status = GameStatus.DRAW_INSUFFICIENT_MATERIAL, winner = null)
        }

        if (detectRepetition && repetitionCount(state) >= 3) {
            return state.copy(status = GameStatus.DRAW_REPETITION, winner = null)
        }

        return state.copy(status = if (inCheck) GameStatus.CHECK else GameStatus.ONGOING)
    }

    /** How many times the current position has occurred in this game (including the start). */
    fun repetitionCount(state: GameState): Int {
        val target = state.positionKey
        var cursor = GameState.initial()
        var count = if (cursor.positionKey == target) 1 else 0
        for (move in state.moveLog.toList()) {
            cursor = applyMove(cursor, move)
            if (cursor.positionKey == target) count++
        }
        return count.coerceAtLeast(1)
    }

    /** Steps back one half-move by replaying the log. Returns null at the start. */
    fun undo(state: GameState): GameState? {
        val moves = state.moveLog.toList()
        if (moves.isEmpty()) return null
        var cursor = GameState.initial()
        for (i in 0 until moves.size - 1) {
            cursor = applyMove(cursor, moves[i])
        }
        return resolve(cursor)
    }

    /** A faction surrenders; the opponent takes the crown. */
    fun resign(state: GameState, faction: Faction): GameState =
        if (state.isFinished) state
        else state.copy(status = GameStatus.RESIGNATION, winner = faction.opponent)

    fun agreeDraw(state: GameState): GameState =
        if (state.isFinished) state
        else state.copy(status = GameStatus.DRAW_AGREEMENT, winner = null)

    fun abandon(state: GameState): GameState =
        if (state.isFinished) state else state.copy(status = GameStatus.ABANDONED, winner = null)

    /** All legal destinations for a square, for the board highlights. */
    fun legalDestinations(state: GameState, from: Square): List<Square> =
        ChessRules.legalMoves(state, from).map { it.to }

    /** Captured pieces, grouped by the faction that lost them. */
    fun capturedPieces(state: GameState): Map<Faction, List<PieceType>> {
        val result = HashMap<Faction, MutableList<PieceType>>()
        for (move in state.moveLog.toList()) {
            val captured = move.captured ?: continue
            result.getOrPut(captured.faction) { ArrayList() }.add(captured.type)
        }
        return result
    }
}
