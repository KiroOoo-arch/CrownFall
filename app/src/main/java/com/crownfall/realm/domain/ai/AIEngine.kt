package com.crownfall.realm.domain.ai

import com.crownfall.realm.domain.chess.ChessRules
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.GameState
import com.crownfall.realm.domain.chess.Move
import com.crownfall.realm.domain.chess.PieceType
import java.util.Random

/**
 * The public face of the AI. Every difficulty searches the exact same legal
 * move list the human player sees - the AI has no extra information and never
 * plays an illegal move.
 *
 * The engine object is reusable across moves so the transposition table keeps
 * paying off during a match.
 */
class AIEngine(
    private val transpositionTable: TranspositionTable = TranspositionTable(16)
) {

    data class Decision(
        val move: Move,
        val score: Int,
        val depth: Int,
        val nodes: Long,
        val elapsedMs: Long,
        val blundered: Boolean
    )

    fun chooseMove(
        state: GameState,
        difficulty: Difficulty,
        random: Random = Random(),
        timeLimitOverrideMs: Long? = null
    ): Decision? = chooseMove(state, SearchConfig.from(difficulty, timeLimitOverrideMs ?: difficulty.timeLimitMs), random)

    fun chooseMove(state: GameState, config: SearchConfig, random: Random = Random()): Decision? {
        val legal = ChessRules.allLegalMoves(state)
        if (legal.isEmpty()) return null
        if (legal.size == 1) {
            return Decision(legal.first(), 0, 0, 0, 0, blundered = false)
        }

        // Weak difficulties sometimes simply react instead of calculating.
        if (config.blunderChance > 0.0 && random.nextDouble() < config.blunderChance) {
            val pick = legal[random.nextInt(legal.size)]
            return Decision(pick, 0, 0, 0, 0, blundered = true)
        }

        val table = if (config.useTranspositionTable) transpositionTable else null
        val result = SearchEngine(config, table).search(state)
        val chosen = result.move ?: legal.first()

        val candidates = if (config.choiceMarginCp > 0 && result.rootMoves.isNotEmpty()) {
            val bestScore = result.rootMoves.maxOf { it.score }
            result.rootMoves.filter { it.score >= bestScore - config.choiceMarginCp }
        } else {
            emptyList()
        }

        val finalMove = if (candidates.size > 1) {
            candidates[random.nextInt(candidates.size)].move
        } else {
            chosen
        }

        val score = result.rootMoves.firstOrNull { it.move == finalMove }?.score ?: result.score
        return Decision(finalMove, score, result.depthReached, result.nodes, result.elapsedMs, blundered = false)
    }

    fun clearCache() = transpositionTable.clear()

    companion object {
        /** Material-only helper used by the UI to show the balance of power. */
        fun materialBalance(state: GameState): Int {
            var balance = 0
            for ((_, piece) in state.board.pieces()) {
                val value = when (piece.type) {
                    PieceType.KING -> 0
                    PieceType.QUEEN -> 9
                    PieceType.ROOK -> 5
                    PieceType.BISHOP -> 3
                    PieceType.KNIGHT -> 3
                    PieceType.PAWN -> 1
                }
                balance += if (piece.faction == Faction.DAWN) value else -value
            }
            return balance
        }
    }
}
