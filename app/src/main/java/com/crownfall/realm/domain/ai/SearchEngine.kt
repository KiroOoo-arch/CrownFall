package com.crownfall.realm.domain.ai

import com.crownfall.realm.domain.chess.ChessRules
import com.crownfall.realm.domain.chess.GameEngine
import com.crownfall.realm.domain.chess.GameState
import com.crownfall.realm.domain.chess.Move

/**
 * Negamax search with alpha-beta pruning, quiescence, move ordering,
 * an optional transposition table and iterative deepening under a hard time
 * budget. Single threaded by design: on Android the caller runs one search on
 * a background dispatcher so the UI never freezes.
 */
class SearchEngine(
    private val config: SearchConfig,
    private val table: TranspositionTable?
) {

    private val engine = GameEngine()
    private val history = IntArray(64 * 64)
    private var nodes = 0L
    private var startNanos = 0L
    private var aborted = false

    data class RootMove(val move: Move, val score: Int)

    data class Result(
        val move: Move?,
        val score: Int,
        val depthReached: Int,
        val nodes: Long,
        val elapsedMs: Long,
        val rootMoves: List<RootMove>
    )

    fun search(state: GameState): Result {
        startNanos = System.nanoTime()
        nodes = 0
        aborted = false
        history.fill(0)

        val legal = ChessRules.allLegalMoves(state)
        if (legal.isEmpty()) {
            return Result(null, 0, 0, 0, 0, emptyList())
        }
        if (legal.size == 1) {
            return Result(legal.first(), 0, 0, 0, 0, listOf(RootMove(legal.first(), 0)))
        }

        val maxDepth = config.maxDepth.coerceAtLeast(1)
        val collectAll = config.choiceMarginCp > 0
        val depths = if (config.useIterativeDeepening) (1..maxDepth) else listOf(maxDepth)

        var best: Result? = null
        for (depth in depths) {
            val result = searchAtDepth(state, depth, collectAll)
            if (aborted && best != null) break
            if (result.move != null) best = result
            if (aborted) break
            if (result.score > Evaluation.MATE_SCORE - 100) break
        }

        val elapsed = elapsedMs()
        return (best ?: Result(legal.first(), 0, 0, nodes, elapsed, listOf(RootMove(legal.first(), 0))))
            .copy(nodes = nodes, elapsedMs = elapsed)
    }

    private fun searchAtDepth(state: GameState, depth: Int, collectAll: Boolean): Result {
        val ttMove = table?.bestMove(state.positionKey) ?: 0
        val ordered = orderedMoves(state, ttMove)
        if (ordered.isEmpty()) return Result(null, 0, depth, nodes, elapsedMs(), emptyList())

        val rootMoves = ArrayList<RootMove>(ordered.size)
        var alpha = -Evaluation.INFINITY
        val beta = Evaluation.INFINITY
        var bestMove = ordered.first()
        var bestScore = -Evaluation.INFINITY

        for (move in ordered) {
            val child = engine.applyMove(state, move)
            val score = if (collectAll) {
                // Full window so each root move receives a true score (used for variety).
                -negamax(child, depth - 1, -Evaluation.INFINITY, Evaluation.INFINITY, 1)
            } else {
                -negamax(child, depth - 1, -beta, -alpha, 1)
            }
            if (aborted) break
            rootMoves.add(RootMove(move, score))
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (!collectAll && score > alpha) alpha = score
        }

        if (rootMoves.isNotEmpty()) {
            table?.store(
                state.positionKey,
                MoveOrdering.pack(bestMove),
                bestScore,
                depth,
                TranspositionTable.FLAG_EXACT
            )
        }

        return Result(bestMove, bestScore, depth, nodes, elapsedMs(), rootMoves)
    }

    private fun negamax(state: GameState, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        if (aborted) return 0
        if (shouldStop()) return 0
        nodes++

        if (state.halfmoveClock >= 100) return 0

        if (depth <= 0) {
            return if (config.useQuiescence) quiescence(state, alphaIn, beta, ply)
            else Evaluation.evaluate(state)
        }

        val key = state.positionKey
        val ttMove = table?.bestMove(key) ?: 0
        var alpha = alphaIn

        if (table != null) {
            val entry = table.probe(key, depth, alpha, beta)
            if (entry != null && entry.depth >= depth && entry.isUsable) return entry.score
        }

        val moves = orderedMoves(state, ttMove)
        if (moves.isEmpty()) {
            val inCheck = ChessRules.isInCheck(state.board, state.sideToMove)
            return if (inCheck) -(Evaluation.MATE_SCORE - ply) else 0
        }

        var bestScore = -Evaluation.INFINITY
        var bestMove: Move? = null
        var flag = TranspositionTable.FLAG_UPPER

        for (move in moves) {
            val child = engine.applyMove(state, move)
            val score = -negamax(child, depth - 1, -beta, -alpha, ply + 1)
            if (aborted) return 0
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > alpha) {
                alpha = score
                flag = TranspositionTable.FLAG_EXACT
            }
            if (alpha >= beta) {
                flag = TranspositionTable.FLAG_LOWER
                if (!move.isCapture) {
                    history[move.from.index * 64 + move.to.index] += depth * depth
                }
                break
            }
        }

        table?.store(key, bestMove?.let { MoveOrdering.pack(it) } ?: 0, bestScore, depth, flag)
        return bestScore
    }

    private fun quiescence(state: GameState, alphaIn: Int, beta: Int, ply: Int): Int {
        if (aborted) return 0
        if (shouldStop()) return 0
        nodes++

        val standPat = Evaluation.evaluate(state)
        var alpha = alphaIn
        if (standPat >= beta) return standPat
        if (standPat > alpha) alpha = standPat
        if (ply > 24) return standPat

        val captures = ChessRules.allLegalMoves(state).filter { it.isCapture || it.promotion != null }
        for (move in MoveOrdering.ordered(captures, 0, history)) {
            val child = engine.applyMove(state, move)
            val score = -quiescence(child, -beta, -alpha, ply + 1)
            if (aborted) return 0
            if (score >= beta) return score
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun orderedMoves(state: GameState, ttMove: Int): List<Move> =
        MoveOrdering.ordered(ChessRules.allLegalMoves(state), ttMove, history)

    private fun shouldStop(): Boolean {
        if (config.timeLimitMs <= 0) return false
        if ((nodes and 0x1FFL) != 0L) return false
        if (elapsedMs() > config.timeLimitMs) {
            aborted = true
            return true
        }
        return false
    }

    private fun elapsedMs(): Long = (System.nanoTime() - startNanos) / 1_000_000
}
