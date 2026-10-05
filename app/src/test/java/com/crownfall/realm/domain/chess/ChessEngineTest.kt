package com.crownfall.realm.domain.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineTest {

    private val engine = GameEngine()

    // ------------------------------------------------------------- helpers

    private fun d(type: PieceType) = Piece(type, Faction.DAWN)
    private fun k(type: PieceType) = Piece(type, Faction.DUSK)

    private fun pos(
        vararg pieces: Pair<String, Piece>,
        side: Faction = Faction.DAWN,
        castling: Int = CastlingRights.NONE,
        ep: String? = null,
        halfmove: Int = 0
    ): GameState {
        val map = HashMap<Square, Piece>()
        for ((square, piece) in pieces) {
            map[requireNotNull(Square.fromAlgebraic(square)) { "bad square $square" }] = piece
        }
        return GameState(
            board = Board.ofPieces(map),
            sideToMove = side,
            castling = castling,
            enPassantTarget = ep?.let { Square.fromAlgebraic(it) },
            halfmoveClock = halfmove,
            fullmoveNumber = 1,
            status = GameStatus.ONGOING,
            winner = null,
            moveLog = MoveLog.empty
        )
    }

    private fun destinations(state: GameState, from: String): Set<String> {
        val square = requireNotNull(Square.fromAlgebraic(from))
        return ChessRules.legalMoves(state, square).map { it.to.algebraic }.toSet()
    }

    private fun move(state: GameState, from: String, to: String): Move =
        requireNotNull(
            engine.findMove(
                state,
                Square.fromAlgebraic(from)!!,
                Square.fromAlgebraic(to)!!
            )
        ) { "expected legal $from-$to" }

    // ------------------------------------------------------------------ pawns

    @Test
    fun pawnMovesOneAndTwoFromHomeRank() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "e2" to d(PieceType.PAWN)
        )
        assertEquals(setOf("e3", "e4"), destinations(state, "e2"))
    }

    @Test
    fun pawnCannotDoubleAfterLeavingHomeRank() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "e3" to d(PieceType.PAWN)
        )
        assertEquals(setOf("e4"), destinations(state, "e3"))
    }

    @Test
    fun pawnIsBlockedByEnemyDirectlyAhead() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "e2" to d(PieceType.PAWN), "e3" to k(PieceType.PAWN)
        )
        assertTrue(destinations(state, "e2").isEmpty())
    }

    @Test
    fun pawnCapturesDiagonallyOnly() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "e4" to d(PieceType.PAWN), "d5" to k(PieceType.PAWN), "f5" to k(PieceType.PAWN)
        )
        assertEquals(setOf("e5", "d5", "f5"), destinations(state, "e4"))
    }

    @Test
    fun duskPawnsMarchTheOtherWay() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "e7" to k(PieceType.PAWN), side = Faction.DUSK
        )
        assertEquals(setOf("e6", "e5"), destinations(state, "e7"))
    }

    // ------------------------------------------------------------ en passant

    @Test
    fun enPassantIsOfferedOnlyWhenTargetMatches() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "e5" to d(PieceType.PAWN), "d5" to k(PieceType.PAWN),
            ep = "d6"
        )
        val moves = ChessRules.legalMoves(state, Square.fromAlgebraic("e5")!!)
        val epMove = moves.firstOrNull { it.isEnPassant }
        assertNotNull("en passant capture should be generated", epMove)
        assertEquals("d6", epMove!!.to.algebraic)

        val after = engine.applyMove(state, epMove)
        assertNull("captured pawn removed from d5", after.board.pieceAt(Square.fromAlgebraic("d5")!!))
        assertEquals(d(PieceType.PAWN), after.board.pieceAt(Square.fromAlgebraic("d6")!!))
    }

    @Test
    fun enPassantNotAllowedWithoutTarget() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "e5" to d(PieceType.PAWN), "d5" to k(PieceType.PAWN)
        )
        assertFalse(destinations(state, "e5").contains("d6"))
    }

    // ------------------------------------------------------------ promotion

    @Test
    fun pawnPromotionOffersAllFourChoices() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "a7" to d(PieceType.PAWN)
        )
        val moves = ChessRules.legalMoves(state, Square.fromAlgebraic("a7")!!)
            .filter { it.to.algebraic == "a8" }
        assertEquals(4, moves.size)
        assertEquals(
            setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT),
            moves.mapNotNull { it.promotion }.toSet()
        )
    }

    @Test
    fun promotedPieceReplacesPawn() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "a7" to d(PieceType.PAWN)
        )
        val promote = ChessRules.legalMoves(state, Square.fromAlgebraic("a7")!!)
            .first { it.promotion == PieceType.ROOK }
        val after = engine.applyMove(state, promote)
        assertEquals(Piece(PieceType.ROOK, Faction.DAWN), after.board.pieceAt(Square.fromAlgebraic("a8")!!))
    }

    @Test
    fun promotionByCaptureIsGenerated() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "a7" to d(PieceType.PAWN), "b8" to k(PieceType.ROOK)
        )
        val capturePromotions = ChessRules.legalMoves(state, Square.fromAlgebraic("a7")!!)
            .filter { it.to.algebraic == "b8" }
        assertEquals(4, capturePromotions.size)
        assertTrue(capturePromotions.all { it.captured?.type == PieceType.ROOK })
    }

    // ---------------------------------------------------------- piece movement

    @Test
    fun knightMovesInAnLShape() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "d4" to d(PieceType.KNIGHT)
        )
        val expected = setOf("b3", "b5", "c2", "c6", "e2", "e6", "f3", "f5")
        assertEquals(expected, destinations(state, "d4"))
    }

    @Test
    fun bishopSlidesOnDiagonals() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "d4" to d(PieceType.BISHOP)
        )
        val expected = setOf(
            "a1", "b2", "c3", "e5", "f6", "g7", "h8",
            "a7", "b6", "c5", "e3", "f2", "g1"
        )
        assertEquals(expected, destinations(state, "d4"))
    }

    @Test
    fun rookSlidesOnFilesAndRanks() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "d4" to d(PieceType.ROOK)
        )
        val expected = setOf(
            "d1", "d2", "d3", "d5", "d6", "d7", "d8",
            "a4", "b4", "c4", "e4", "f4", "g4", "h4"
        )
        assertEquals(expected, destinations(state, "d4"))
    }

    @Test
    fun rookStopsBeforeBlockingPiece() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "d4" to d(PieceType.ROOK), "d6" to d(PieceType.PAWN), "f4" to k(PieceType.PAWN)
        )
        val dests = destinations(state, "d4")
        assertFalse("own pawn blocks d5/d6/d7/d8", dests.contains("d6"))
        assertTrue(dests.contains("d5"))
        assertTrue("may capture enemy on f4", dests.contains("f4"))
        assertFalse("must stop after capture", dests.contains("g4"))
    }

    @Test
    fun queenCombinesRookAndBishop() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "d4" to d(PieceType.QUEEN)
        )
        assertEquals(27, destinations(state, "d4").size)
    }

    @Test
    fun kingMovesOneSquare() {
        val state = pos(
            "d4" to d(PieceType.KING), "e8" to k(PieceType.KING)
        )
        val expected = setOf("c3", "c4", "c5", "d3", "d5", "e3", "e4", "e5")
        assertEquals(expected, destinations(state, "d4"))
    }

    @Test
    fun piecesMayNotCaptureTheirOwnFaction() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
            "a1" to d(PieceType.ROOK), "a4" to d(PieceType.PAWN)
        )
        val dests = destinations(state, "a1")
        assertTrue(dests.contains("a3"))
        assertFalse(dests.contains("a4"))
    }

    // -------------------------------------------------------------- castling

    @Test
    fun kingsideCastlingIsGenerated() {
        val state = pos(
            "e1" to d(PieceType.KING), "h1" to d(PieceType.ROOK), "e8" to k(PieceType.KING),
            castling = CastlingRights.DAWN_KINGSIDE
        )
        val move = ChessRules.legalMoves(state, Square.fromAlgebraic("e1")!!)
            .first { it.isCastleKingside }
        val after = engine.applyMove(state, move)
        assertEquals(d(PieceType.KING), after.board.pieceAt(Square.fromAlgebraic("g1")!!))
        assertEquals(d(PieceType.ROOK), after.board.pieceAt(Square.fromAlgebraic("f1")!!))
        assertNull(after.board.pieceAt(Square.fromAlgebraic("h1")!!))
    }

    @Test
    fun queensideCastlingIsGenerated() {
        val state = pos(
            "e1" to d(PieceType.KING), "a1" to d(PieceType.ROOK), "e8" to k(PieceType.KING),
            castling = CastlingRights.DAWN_QUEENSIDE
        )
        val move = ChessRules.legalMoves(state, Square.fromAlgebraic("e1")!!)
            .first { it.isCastleQueenside }
        val after = engine.applyMove(state, move)
        assertEquals(d(PieceType.KING), after.board.pieceAt(Square.fromAlgebraic("c1")!!))
        assertEquals(d(PieceType.ROOK), after.board.pieceAt(Square.fromAlgebraic("d1")!!))
    }

    @Test
    fun castlingBlockedByPiecesIsIllegal() {
        val state = pos(
            "e1" to d(PieceType.KING), "h1" to d(PieceType.ROOK),
            "g1" to d(PieceType.KNIGHT), "e8" to k(PieceType.KING),
            castling = CastlingRights.DAWN_KINGSIDE
        )
        assertTrue(destinations(state, "e1").doesNotContain("g1"))
    }

    @Test
    fun castlingThroughAttackedSquareIsIllegal() {
        val state = pos(
            "e1" to d(PieceType.KING), "h1" to d(PieceType.ROOK),
            "f8" to k(PieceType.ROOK), "e8" to k(PieceType.KING),
            castling = CastlingRights.DAWN_KINGSIDE
        )
        assertFalse("f1 is attacked so castling is illegal", destinations(state, "e1").contains("g1"))
    }

    @Test
    fun castlingOutOfCheckIsIllegal() {
        val state = pos(
            "e1" to d(PieceType.KING), "h1" to d(PieceType.ROOK),
            "e8" to k(PieceType.ROOK), "a8" to k(PieceType.KING),
            castling = CastlingRights.DAWN_KINGSIDE
        )
        assertTrue(ChessRules.isInCheck(state.board, Faction.DAWN))
        assertFalse(destinations(state, "e1").contains("g1"))
    }

    // --------------------------------------------------------- check & mate

    @Test
    fun checkIsDetected() {
        val state = pos(
            "e1" to d(PieceType.KING), "h8" to k(PieceType.KING), "e8" to k(PieceType.ROOK)
        )
        assertTrue(ChessRules.isInCheck(state.board, Faction.DAWN))
        assertEquals(GameStatus.CHECK, engine.resolve(state).status)
    }

    @Test
    fun checkmateIsDetected() {
        val state = pos(
            "a8" to d(PieceType.KING),
            "b7" to k(PieceType.QUEEN), "b6" to k(PieceType.KING)
        )
        val resolved = engine.resolve(state)
        assertEquals(GameStatus.CHECKMATE, resolved.status)
        assertEquals(Faction.DUSK, resolved.winner)
        assertTrue(resolved.isFinished)
    }

    @Test
    fun stalemateIsDetected() {
        val state = pos(
            "a1" to d(PieceType.KING),
            "b3" to k(PieceType.QUEEN), "g8" to k(PieceType.KING)
        )
        assertFalse(ChessRules.isInCheck(state.board, Faction.DAWN))
        val resolved = engine.resolve(state)
        assertEquals(GameStatus.STALEMATE, resolved.status)
        assertNull(resolved.winner)
    }

    @Test
    fun pinnedPieceMayNotLeaveThePinLine() {
        val state = pos(
            "e1" to d(PieceType.KING), "e2" to d(PieceType.ROOK),
            "h8" to k(PieceType.KING), "e8" to k(PieceType.ROOK)
        )
        val legal = ChessRules.legalMoves(state, Square.fromAlgebraic("e2")!!)
        assertTrue("pinned rook must stay on the e-file", legal.all { it.to.file == 4 })
        assertTrue(legal.isNotEmpty())
        assertFalse(destinations(state, "e2").contains("d2"))
    }

    @Test
    fun kingMayNotStepIntoCheck() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING), "d8" to k(PieceType.ROOK)
        )
        assertFalse(destinations(state, "e1").contains("d1"))
    }

    @Test
    fun illegalMoveIsRejectedByEngine() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING), "a1" to d(PieceType.ROOK)
        )
        assertNull(
            engine.findMove(
                state,
                Square.fromAlgebraic("a1")!!,
                Square.fromAlgebraic("b3")!!
            )
        )
    }

    @Test
    fun selectingEnemyPieceYieldsNoMoves() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING), "a7" to k(PieceType.PAWN)
        )
        assertTrue(ChessRules.legalMoves(state, Square.fromAlgebraic("a7")!!).isEmpty())
    }

    // ----------------------------------------------------------------- draws

    @Test
    fun insufficientMaterialDetected() {
        assertTrue(ChessRules.isInsufficientMaterial(pos("e1" to d(PieceType.KING), "e8" to k(PieceType.KING)).board))
        assertTrue(
            ChessRules.isInsufficientMaterial(
                pos("e1" to d(PieceType.KING), "e8" to k(PieceType.KING), "c1" to d(PieceType.BISHOP)).board
            )
        )
        assertFalse(
            ChessRules.isInsufficientMaterial(
                pos(
                    "e1" to d(PieceType.KING), "e8" to k(PieceType.KING),
                    "a1" to d(PieceType.ROOK)
                ).board
            )
        )
    }

    @Test
    fun fiftyMoveRuleTriggers() {
        val state = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING), "a1" to d(PieceType.ROOK),
            halfmove = 100
        )
        assertEquals(GameStatus.DRAW_FIFTY_MOVE, engine.resolve(state).status)
    }

    @Test
    fun halfmoveClockResetsOnPawnMoveAndCapture() {
        val start = pos(
            "e1" to d(PieceType.KING), "e8" to k(PieceType.KING), "e2" to d(PieceType.PAWN),
            halfmove = 7
        )
        val after = engine.applyMove(start, move(start, "e2", "e4"))
        assertEquals(0, after.halfmoveClock)
        assertEquals(Faction.DUSK, after.sideToMove)
        assertEquals(1, after.fullmoveNumber)

        val quiet = engine.applyMove(after, move(after, "e8", "d8"))
        assertEquals(1, quiet.halfmoveClock)
        assertEquals(2, quiet.fullmoveNumber)
    }

    // ------------------------------------------------------------ full games

    @Test
    fun foolsMateEndsTheGame() {
        var s = GameState.initial()
        s = engine.play(s, move(s, "f2", "f3"))
        s = engine.play(s, move(s, "e7", "e5"))
        s = engine.play(s, move(s, "g2", "g4"))
        s = engine.play(s, move(s, "d8", "h4"))
        assertEquals(GameStatus.CHECKMATE, s.status)
        assertEquals(Faction.DUSK, s.winner)
    }

    @Test
    fun undoStepsBackOneHalfMove() {
        var s = GameState.initial()
        s = engine.play(s, move(s, "e2", "e4"))
        s = engine.play(s, move(s, "e7", "e5"))
        val back = engine.undo(s)
        assertNotNull(back)
        assertEquals(1, back!!.moveLog.size)
        assertEquals(Faction.DUSK, back.sideToMove)
        assertNotNull("pawn should be on e4 after undo", back.board.pieceAt(Square.fromAlgebraic("e4")!!))
        assertNull("pawn left e2", back.board.pieceAt(Square.fromAlgebraic("e2")!!))
        assertNull(engine.undo(GameState.initial()))
    }

    @Test
    fun resignAndDrawEndTheGame() {
        val s = GameState.initial()
        assertEquals(Faction.DUSK, engine.resign(s, Faction.DAWN).winner)
        assertTrue(engine.agreeDraw(s).status == GameStatus.DRAW_AGREEMENT)
        assertTrue(engine.abandon(s).status == GameStatus.ABANDONED)
    }

    @Test
    fun repetitionIsDetected() {
        var s = GameState.initial()
        // Knights out and back twice for both sides.
        s = engine.play(s, move(s, "g1", "f3"))
        s = engine.play(s, move(s, "g8", "f6"))
        s = engine.play(s, move(s, "f3", "g1"))
        s = engine.play(s, move(s, "f6", "g8"))
        s = engine.play(s, move(s, "g1", "f3"))
        s = engine.play(s, move(s, "g8", "f6"))
        s = engine.play(s, move(s, "f3", "g1"))
        s = engine.play(s, move(s, "f6", "g8"))
        assertEquals(GameStatus.DRAW_REPETITION, s.status)
    }

    @Test
    fun capturedPiecesAreTracked() {
        var s = GameState.initial()
        s = engine.play(s, move(s, "e2", "e4"))
        s = engine.play(s, move(s, "d7", "d5"))
        s = engine.play(s, move(s, "e4", "d5"))
        val captured = engine.capturedPieces(s)
        assertEquals(listOf(PieceType.PAWN), captured[Faction.DUSK])
    }

    // --------------------------------------------------------------- codecs

    @Test
    fun fenRoundTripMatchesStandardStart() {
        val state = GameState.initial()
        val fen = Fen.toFen(state)
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fen)
        val parsed = Fen.parse(fen)
        assertEquals(fen, Fen.toFen(parsed))
        assertEquals(state.board, parsed.board)
    }

    @Test
    fun fenParsesEmptyRanksAndSide() {
        val parsed = Fen.parse("4k3/8/8/8/8/8/4P3/4K3 b - - 0 12")
        assertEquals(Faction.DUSK, parsed.sideToMove)
        assertEquals(12, parsed.fullmoveNumber)
        assertEquals(d(PieceType.PAWN), parsed.board.pieceAt(Square.fromAlgebraic("e2")!!))
    }

    @Test
    fun moveCodecReplaysAGame() {
        val engine2 = GameEngine()
        var s = GameState.initial()
        val moves = listOf(
            move(s, "e2", "e4").also { s = engine2.play(s, it) },
            move(s, "e7", "e5").also { s = engine2.play(s, it) },
            move(s, "g1", "f3").also { s = engine2.play(s, it) }
        )
        val encoded = MoveCodec.encodeAll(moves)
        assertEquals("e2e4,e7e5,g1f3", encoded)
        val replayed = MoveCodec.replay(encoded)
        assertEquals(s.board, replayed.board)
        assertEquals(s.sideToMove, replayed.sideToMove)
        assertEquals(3, replayed.moveLog.size)
    }

    @Test
    fun boardInitialHasThirtyTwoPieces() {
        assertEquals(16, Board.initial().count(Faction.DAWN))
        assertEquals(16, Board.initial().count(Faction.DUSK))
    }

    private fun Set<String>.doesNotContain(value: String): Boolean = !contains(value)
}
