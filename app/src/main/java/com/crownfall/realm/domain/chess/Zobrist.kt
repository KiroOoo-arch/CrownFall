package com.crownfall.realm.domain.chess

import java.util.Random

/**
 * Deterministic Zobrist hashing. The seed is fixed so a given position always
 * produces the same key, which makes the transposition table and repetition
 * detection stable across runs.
 */
object Zobrist {
    private const val SEED = 0x1F2E3D4C5B6A7988L

    private val random = Random(SEED)

    private val pieceSquareKeys: Array<Array<Array<Long>>> =
        Array(64) { Array(PieceType.entries.size) { Array(2) { random.nextLong() } } }

    private val sideKeys = Array(2) { random.nextLong() }

    private val castlingKeys = Array(16) { random.nextLong() }

    private val enPassantKeys = Array(8) { random.nextLong() }

    fun hashPlacement(board: Board): Long {
        var h = 0L
        for (square in Square.all) {
            val piece = board.pieceAt(square) ?: continue
            h = h xor pieceSquareKeys[square.index][piece.type.ordinal][piece.faction.ordinal]
        }
        return h
    }

    fun sideKey(faction: Faction): Long = sideKeys[faction.ordinal]

    fun castlingKey(rights: Int): Long = castlingKeys[rights and 0xF]

    fun enPassantKey(square: Square?): Long =
        if (square == null) 0L else enPassantKeys[square.file and 0x7]
}
