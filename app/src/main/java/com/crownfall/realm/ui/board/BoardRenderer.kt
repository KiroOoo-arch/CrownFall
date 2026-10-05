package com.crownfall.realm.ui.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.crownfall.realm.domain.chess.Square
import com.crownfall.realm.ui.theme.CrownfallPalette
import kotlin.math.abs

/** Everything the board needs to know about the current interaction. */
data class BoardHighlights(
    val selected: Square? = null,
    val legalMoves: List<Square> = emptyList(),
    val captureTargets: List<Square> = emptyList(),
    val lastMoveFrom: Square? = null,
    val lastMoveTo: Square? = null,
    val checkSquare: Square? = null,
    val focusSquare: Square? = null
)

/**
 * Draws the ancient stone battlefield: weathered tiles, grout, moss, banners
 * and every gameplay highlight. Kept separate from the pieces so the board
 * never re-draws piece geometry and vice versa.
 */
object BoardRenderer {

    private val DawnTile = Color(0xFF6E6353)
    private val DuskTile = Color(0xFF4C443A)
    private val DawnTileCool = Color(0xFF6A6459)
    private val DuskTileCool = Color(0xFF443F39)

    fun DrawScope.drawBoard(
        geometry: BoardGeometry,
        highlights: BoardHighlights,
        checkPulse: Float,
        showCoordinates: Boolean
    ) {
        drawTiles(geometry)
        drawHighlights(geometry, highlights, checkPulse)
        if (showCoordinates) drawCoordinates(geometry)
    }

    private fun DrawScope.drawTiles(geometry: BoardGeometry) {
        val size = geometry.squareSize
        val tileRadius = CornerRadius(size * 0.06f)
        for (rank in 0..7) {
            for (file in 0..7) {
                val square = Square(file, rank)
                val rect = geometry.rectOf(square)
                val light = (file + rank) % 2 == 0
                val variation = tileVariation(file, rank)
                val base = when {
                    light && variation > 0.5f -> DawnTileCool
                    light -> DawnTile
                    variation > 0.5f -> DuskTileCool
                    else -> DuskTile
                }
                // Shade tiles slightly toward the top of the board for depth.
                val depth = 1f - (7 - rank) * 0.018f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            base.copy(alpha = 0.94f * depth),
                            base.copy(alpha = 0.80f * depth)
                        ),
                        startY = rect.top,
                        endY = rect.bottom
                    ),
                    topLeft = rect.topLeft,
                    size = Size(rect.width, rect.height)
                )
                // Bevel highlight on the top-left edge.
                drawRoundRect(
                    color = Color.White.copy(alpha = if (light) 0.05f else 0.035f),
                    topLeft = Offset(rect.left + size * 0.06f, rect.top + size * 0.06f),
                    size = Size(size * 0.88f, size * 0.88f),
                    cornerRadius = tileRadius,
                    style = Stroke(width = size * 0.045f)
                )
                // Occasional crack or moss patch for weathering.
                if (variation > 0.86f) {
                    drawLine(
                        color = CrownfallPalette.Void.copy(alpha = 0.35f),
                        start = Offset(rect.left + size * 0.2f, rect.top + size * 0.25f),
                        end = Offset(rect.left + size * 0.75f, rect.top + size * 0.68f),
                        strokeWidth = size * 0.025f
                    )
                }
                if (variation in 0.10f..0.18f) {
                    drawCircle(
                        color = Color(0xFF4C6B3F).copy(alpha = 0.22f),
                        radius = size * 0.16f,
                        center = Offset(rect.left + size * 0.7f, rect.top + size * 0.72f)
                    )
                }
            }
        }
        // Grout lines between the stone slabs.
        for (i in 0..8) {
            val position = i * size
            drawLine(
                color = CrownfallPalette.Void.copy(alpha = 0.45f),
                start = Offset(position, 0f),
                end = Offset(position, geometry.side),
                strokeWidth = size * 0.035f
            )
            drawLine(
                color = CrownfallPalette.Void.copy(alpha = 0.45f),
                start = Offset(0f, position),
                end = Offset(geometry.side, position),
                strokeWidth = size * 0.035f
            )
        }
    }

    private fun DrawScope.drawHighlights(
        geometry: BoardGeometry,
        highlights: BoardHighlights,
        checkPulse: Float
    ) {
        val size = geometry.squareSize

        // Last move trail.
        listOfNotNull(highlights.lastMoveFrom, highlights.lastMoveTo).forEach { square ->
            val rect = geometry.rectOf(square)
            drawRect(
                color = CrownfallPalette.HighlightLastMove.copy(alpha = 0.22f),
                topLeft = rect.topLeft,
                size = Size(rect.width, rect.height)
            )
        }

        // Capture spotlight while a cinematic plays.
        highlights.focusSquare?.let { square ->
            val center = geometry.centerOf(square)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CrownfallPalette.GoldBright.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size * 1.4f
                ),
                radius = size * 1.4f,
                center = center
            )
        }

        // Legal destinations: dots for quiet moves, rings for captures.
        highlights.legalMoves.forEach { square ->
            drawCircle(
                color = CrownfallPalette.HighlightMove.copy(alpha = 0.75f),
                radius = size * 0.14f,
                center = geometry.centerOf(square)
            )
        }
        highlights.captureTargets.forEach { square ->
            val rect = geometry.rectOf(square)
            drawRect(
                color = CrownfallPalette.HighlightCapture.copy(alpha = 0.30f),
                topLeft = rect.topLeft,
                size = Size(rect.width, rect.height)
            )
            drawRoundRect(
                color = CrownfallPalette.HighlightCapture.copy(alpha = 0.9f),
                topLeft = rect.topLeft,
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(size * 0.12f),
                style = Stroke(width = size * 0.055f)
            )
            drawCircle(
                color = CrownfallPalette.HighlightCapture.copy(alpha = 0.85f),
                radius = size * 0.20f,
                center = geometry.centerOf(square),
                style = Stroke(width = size * 0.05f)
            )
        }

        // Selected piece: a strong gold frame.
        highlights.selected?.let { square ->
            val rect = geometry.rectOf(square)
            drawRoundRect(
                color = CrownfallPalette.HighlightSelect.copy(alpha = 0.85f),
                topLeft = rect.topLeft,
                size = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(size * 0.10f),
                style = Stroke(width = size * 0.07f)
            )
        }

        // Check warning: a pulsing crimson aura under the endangered king.
        highlights.checkSquare?.let { square ->
            val center = geometry.centerOf(square)
            val intensity = 0.35f + 0.45f * checkPulse.coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CrownfallPalette.HighlightCheck.copy(alpha = intensity),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size * 1.05f
                ),
                radius = size * 1.05f,
                center = center
            )
            drawRoundRect(
                color = CrownfallPalette.HighlightCheck.copy(alpha = intensity),
                topLeft = geometry.rectOf(square).topLeft,
                size = Size(size, size),
                cornerRadius = CornerRadius(size * 0.10f),
                style = Stroke(width = size * 0.06f)
            )
        }
    }

    private fun DrawScope.drawCoordinates(geometry: BoardGeometry) {
        // Coordinates are drawn as tiny iron studs rather than text so they never
        // fight with the pieces; the letters live in the wooden frame instead.
        val size = geometry.squareSize
        for (i in 0..7) {
            val light = i % 2 == 0
            if (light) {
                drawCircle(
                    color = CrownfallPalette.IronBright.copy(alpha = 0.28f),
                    radius = size * 0.035f,
                    center = Offset(i * size + size * 0.5f, geometry.side - size * 0.08f)
                )
                drawCircle(
                    color = CrownfallPalette.IronBright.copy(alpha = 0.28f),
                    radius = size * 0.035f,
                    center = Offset(size * 0.08f, i * size + size * 0.5f)
                )
            }
        }
    }

    /** Deterministic per-tile variation so the battlefield looks hand-laid. */
    private fun tileVariation(file: Int, rank: Int): Float {
        val hash = (file * 73856093) xor (rank * 19349663)
        val value = abs(hash % 1000)
        return value / 1000f
    }
}
