package com.crownfall.realm.domain.ai

import com.crownfall.realm.domain.chess.ChessRules
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.GameEngine
import com.crownfall.realm.domain.chess.GameState
import com.crownfall.realm.domain.chess.Move
import com.crownfall.realm.domain.chess.Piece
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.chess.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AIEngineTest {

    private val engine = GameEngine()

    private fun d(type: PieceType) = Piece(type, Faction.DAWN)
    private fun k(type: PieceType) = Piece(type, Faction.DUSK)

    private fun pos(
        vararg pieces: Pair<String, Piece>,
        side: Faction = Faction.DAWN
    ): GameState {
        val map = HashMap<Square, Piece>()
        for ((square, piece) in pieces) map[Square.fromAlgebraic(square)!!] = piece
        return GameState(
            board = com.crownfall.realm.domain.chess.Board.ofPieces(map),
            sideToMove = side,
            castling = com.crownfall.realm.domain.chess.CastlingRights.NONE,
            enPassantTarget = null,
            halfmoveClock = 0,
            fullmoveNumber = 1,
            status = com.crownfall.realm.domain.chess.GameStatus.ONGOING,
            winner = null,
            moveLog = com.crownfall.realm.domain.chess.MoveLog.empty
        )
    }

    @Test
    fun everyDifficultyProducesOnlyLegalMoves() {
        val ai = AIEngine()
        for (difficulty in Difficulty.entries) {
            val state = GameState.initial()
            val decision = ai.chooseMove(state, difficulty, Random(42))
            assertNotNull("$difficulty should produce a move", decision)
            val legal = ChessRules.allLegalMoves(state).map { it.notation() }
            assertTrue(
                "$difficulty produced an illegal move ${decision!!.move}",
                legal.contains(decision.move.notation())
            )
        }
    }

    @Test
    fun hardFindsMateInOne() {
        // Dusk king trapped in the corner, supported queen delivers mate in one.
        val mateState = pos(
            "h8" to k(PieceType.KING),
            "h6" to d(PieceType.KING),
            "g6" to d(PieceType.QUEEN)
        )
        val ai = AIEngine()
        val decision = ai.chooseMove(mateState, SearchConfig.deterministic(depth = 3), Random(7))
        assertNotNull(decision)
        val after = engine.play(mateState, decision!!.move)
        assertEquals(
            com.crownfall.realm.domain.chess.GameStatus.CHECKMATE,
            after.status
        )
        assertEquals(Faction.DAWN, after.winner)
        assertTrue(decision.score > 900_000)
    }

    @Test
    fun masterWinsAHangingQueen() {
        val state = pos(
            "h1" to d(PieceType.KING),
            "h8" to k(PieceType.KING),
            "d1" to d(PieceType.ROOK),
            "d5" to k(PieceType.QUEEN),
            "a7" to k(PieceType.PAWN),
            "h2" to d(PieceType.PAWN)
        )
        val ai = AIEngine()
        val decision = ai.chooseMove(state, SearchConfig.deterministic(depth = 3), Random(3))
        assertNotNull(decision)
        assertEquals("d1d5", com.crownfall.realm.domain.chess.MoveCodec.encode(decision!!.move))
    }

    @Test
    fun aiPlaysAWholeMiniGameWithoutIllegalMoves() {
        val ai = AIEngine()
        var state = GameState.initial()
        var plies = 0
        val random = Random(11)
        while (!state.isFinished && plies < 12) {
            val difficulty = if (plies % 2 == 0) Difficulty.HARD else Difficulty.EXPERT
            val decision = ai.chooseMove(state, difficulty, random, timeLimitOverrideMs = 120L)
            assertNotNull("AI must always find a move while the game is running", decision)
            val legal = ChessRules.allLegalMoves(state)
            assertTrue(
                "illegal move at ply $plies: ${decision!!.move}",
                legal.any { it.notation() == decision.move.notation() }
            )
            state = engine.play(state, decision.move)
            plies++
        }
        assertEquals(plies, state.moveLog.size)
    }

    @Test
    fun searchRespectsItsTimeBudget() {
        val ai = AIEngine()
        val config = SearchConfig(
            label = "Timed",
            maxDepth = 8,
            timeLimitMs = 400,
            choiceMarginCp = 0,
            blunderChance = 0.0,
            useQuiescence = true,
            useTranspositionTable = true,
            useIterativeDeepening = true
        )
        val decision = ai.chooseMove(GameState.initial(), config, Random(1))
        assertNotNull(decision)
        assertTrue("search took ${decision!!.elapsedMs}ms", decision.elapsedMs < 4_000)
    }

    @Test
    fun difficultiesHaveGenuinelyDifferentBehaviour() {
        val depths = Difficulty.entries.map { it.maxDepth }
        assertEquals(depths.size, depths.distinct().size)
        assertTrue(Difficulty.BEGINNER.choiceMarginCp > Difficulty.MASTER.choiceMarginCp)
        assertTrue(Difficulty.BEGINNER.blunderChance > Difficulty.MASTER.blunderChance)
        assertTrue(Difficulty.MASTER.timeLimitMs > Difficulty.BEGINNER.timeLimitMs)
    }

    @Test
    fun evaluationPrefersMaterial() {
        val equal = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "d1" to d(PieceType.QUEEN), "d8" to k(PieceType.QUEEN)
        )
        val upQueen = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "d1" to d(PieceType.QUEEN)
        )
        assertTrue(kotlin.math.abs(Evaluation.evaluate(equal)) <= 20)
        assertTrue(Evaluation.evaluate(upQueen) > 500)
        assertTrue(Evaluation.evaluate(upQueen.copy(sideToMove = Faction.DUSK)) < -500)
    }

    @Test
    fun decisionReportsItsSearchStatistics() {
        val ai = AIEngine()
        val decision = ai.chooseMove(
            GameState.initial(),
            SearchConfig.deterministic(depth = 3),
            Random(5)
        )
        assertNotNull(decision)
        assertTrue("expected at least one node", decision!!.nodes > 0)
        assertTrue(decision.depth >= 1)
    }

    @Test
    fun aiReturnsNullOnlyWhenTheGameIsOver() {
        val checkmate = pos(
            "a8" to d(PieceType.KING),
            "b7" to k(PieceType.QUEEN), "b6" to k(PieceType.KING)
        )
        val resolved = engine.resolve(checkmate)
        assertTrue(resolved.isFinished)
        assertEquals(null, AIEngine().chooseMove(resolved, Difficulty.MASTER, Random(0)))
    }

    @Test
    fun transpositionTableStoresAndRetrieves() {
        val table = TranspositionTable(8)
        table.store(12345L, 99, 250, 4, TranspositionTable.FLAG_EXACT)
        val entry = table.probe(12345L, 4, 0, 100)
        assertNotNull(entry)
        assertEquals(99, entry!!.move)
        assertEquals(250, entry.score)
        assertTrue(table.hits > 0)
        table.clear()
        assertEquals(null, table.probe(12345L, 4, 0, 100))
    }

    @Test
    fun moveOrderingPutsTheTranspositionMoveFirst() {
        val moves = ChessRules.allLegalMoves(GameState.initial())
        val chosen = moves[10]
        val history = IntArray(64 * 64)
        val ordered = MoveOrdering.ordered(moves, MoveOrdering.pack(chosen), history)
        assertEquals(chosen.notation(), ordered.first().notation())
        assertNotEquals(0, MoveOrdering.pack(chosen))
        assertTrue(ordered.size == moves.size)
    }
}
