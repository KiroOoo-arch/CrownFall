package com.crownfall.realm.domain.chess

/** The six battlefield roles. Each has an original medieval identity in the UI layer. */
enum class PieceType(val symbol: Char, val medievalName: String, val baseValue: Int) {
    KING('k', "Armored King", 0),
    QUEEN('q', "Warrior Queen", 9),
    ROOK('r', "Castle Guardian", 5),
    BISHOP('b', "Battle Cleric", 3),
    KNIGHT('n', "Warhorse Knight", 3),
    PAWN('p', "Foot Soldier", 1);

    companion object {
        fun fromSymbol(symbol: Char): PieceType? =
            entries.firstOrNull { it.symbol == symbol.lowercaseChar() }
    }
}

/** The two medieval factions. DAWN is the "white" side, DUSK is the "black" side. */
enum class Faction(val factionName: String, val shortName: String) {
    DAWN("Kingdom of Dawn", "Dawn"),
    DUSK("Empire of Dusk", "Dusk");

    val opponent: Faction get() = if (this == DAWN) DUSK else DAWN

    /** Facing of the soldiers: +1 means the pawns march toward higher ranks. */
    val forward: Int get() = if (this == DAWN) 1 else -1

    val homeRank: Int get() = if (this == DAWN) 0 else 7

    val promotionRank: Int get() = if (this == DAWN) 7 else 0
}

/** A single soldier on the board. */
data class Piece(val type: PieceType, val faction: Faction) {
    val isRoyal: Boolean get() = type == PieceType.KING
}
