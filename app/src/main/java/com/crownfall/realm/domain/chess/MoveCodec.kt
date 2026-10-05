package com.crownfall.realm.domain.chess

/**
 * Compact, replayable move encoding used by the save/resume system.
 * Example: "e2e4", "e7e8q", "g1f3".
 */
object MoveCodec {

    fun encode(move: Move): String = buildString {
        append(move.from.algebraic)
        append(move.to.algebraic)
        move.promotion?.let { append(it.symbol) }
    }

    fun encodeAll(moves: List<Move>): String = moves.joinToString(",") { encode(it) }

    /** Resolves an encoded move against the position it was played in. */
    fun decode(state: GameState, code: String, engine: GameEngine = GameEngine()): Move? {
        if (code.length < 4) return null
        val from = Square.fromAlgebraic(code.substring(0, 2)) ?: return null
        val to = Square.fromAlgebraic(code.substring(2, 4)) ?: return null
        val promotion = code.getOrNull(4)?.let { PieceType.fromSymbol(it) }
        return engine.findMove(state, from, to, promotion)
    }

    /** Rebuilds a full game state by replaying an encoded move list. */
    fun replay(encoded: String, engine: GameEngine = GameEngine()): GameState {
        var state = GameState.initial()
        if (encoded.isBlank()) return state
        for (code in encoded.split(",")) {
            val move = decode(state, code.trim(), engine) ?: break
            state = engine.play(state, move)
            if (state.isFinished) break
        }
        return state
    }
}
