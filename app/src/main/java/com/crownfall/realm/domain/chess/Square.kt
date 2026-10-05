package com.crownfall.realm.domain.chess

/**
 * A square on the 8x8 battlefield.
 *
 * [file] is 0..7 for columns a..h, [rank] is 0..7 for rows 1..8.
 * Rank 0 is the Kingdom of Dawn's home rank, rank 7 is the Empire of Dusk's home rank.
 */
data class Square(val file: Int, val rank: Int) {

    val index: Int get() = rank * 8 + file

    val isValid: Boolean get() = file in 0..7 && rank in 0..7

    /** Standard algebraic name such as "e4". */
    val algebraic: String
        get() = "${('a' + file)}${rank + 1}"

    /** Colour-mapped square key used by the board renderer ("a1" .. "h8"). */
    val key: String get() = algebraic

    override fun toString(): String = algebraic

    companion object {
        fun of(index: Int): Square = Square(index % 8, index / 8)

        fun of(file: Int, rank: Int): Square = Square(file, rank)

        fun fromAlgebraic(text: String): Square? {
            if (text.length < 2) return null
            val file = text[0].lowercaseChar() - 'a'
            val rank = text[1] - '1'
            val sq = Square(file, rank)
            return if (sq.isValid) sq else null
        }

        val all: List<Square> by lazy { (0..63).map { Square.of(it) } }
    }
}
