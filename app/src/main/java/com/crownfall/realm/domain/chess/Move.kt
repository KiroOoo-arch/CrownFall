package com.crownfall.realm.domain.chess

/**
 * A fully described move. Every special rule (promotion, en passant, castling)
 * is captured here so the engine can replay a move without extra context.
 */
data class Move(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val captured: Piece? = null,
    val promotion: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastleKingside: Boolean = false,
    val isCastleQueenside: Boolean = false
) {
    val isCastle: Boolean get() = isCastleKingside || isCastleQueenside
    val isCapture: Boolean get() = captured != null || isEnPassant

    /** Compact long-algebraic style text, e.g. "Ng1-f3" or "e2-e4". */
    fun notation(): String = buildString {
        append(piece.type.symbol.uppercaseChar())
        append(from.algebraic)
        append(if (isCapture) "x" else "-")
        append(to.algebraic)
        promotion?.let { append('=').append(it.symbol.uppercaseChar()) }
    }

    override fun toString(): String = notation()
}
