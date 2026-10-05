package com.crownfall.realm.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.crownfall.realm.domain.chess.Square

/**
 * Maps between board squares, normalised board units (0..8, centre based) and
 * pixel offsets.
 *
 * Rank 0 is drawn at the bottom so the Kingdom of Dawn always starts nearest to
 * the player, exactly like a physical board.
 */
class BoardGeometry(val side: Float) {

    val squareSize: Float get() = side / 8f

    fun rectOf(square: Square): Rect {
        val size = squareSize
        val left = square.file * size
        val top = (7 - square.rank) * size
        return Rect(left, top, left + size, top + size)
    }

    fun centerOf(square: Square): Offset = rectOf(square).center

    /** Centre of a normalised board unit; file/rank may be fractional for tweens. */
    fun unitCenter(file: Float, rank: Float): Offset {
        val size = squareSize
        return Offset((file + 0.5f) * size, (7f - rank + 0.5f) * size)
    }

    fun squareSizeOf(file: Float, rank: Float): Float = squareSize

    fun squareAt(point: Offset): Square? {
        val size = squareSize
        if (size <= 0f) return null
        val file = (point.x / size).toInt()
        val visualRank = (point.y / size).toInt()
        if (file !in 0..7 || visualRank !in 0..7) return null
        return Square(file, 7 - visualRank)
    }

    /** Board units (file, rank) for a pixel offset, used by tap handling. */
    fun unitsAt(point: Offset): Pair<Float, Float> {
        val size = squareSize
        if (size <= 0f) return 0f to 0f
        return (point.x / size - 0.5f) to (7f - point.y / size + 0.5f)
    }
}
