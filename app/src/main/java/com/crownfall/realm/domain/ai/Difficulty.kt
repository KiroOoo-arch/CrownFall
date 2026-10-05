package com.crownfall.realm.domain.ai

/**
 * The four AI commanders. Each one changes the real search behaviour:
 * depth, time budget and how often it tolerates a weaker move.
 *
 * None of them are given information the human player does not have: they all
 * search the exact same legal moves the engine produces.
 */
enum class Difficulty(
    val displayName: String,
    val blurb: String,
    val maxDepth: Int,
    val timeLimitMs: Long,
    /** Centipawn window of "equally good" moves used for variety. */
    val choiceMarginCp: Int,
    /** Chance of deliberately taking a random legal move (weak AI flavour, never illegal). */
    val blunderChance: Double,
    val useQuiescence: Boolean,
    val useTranspositionTable: Boolean,
    val useIterativeDeepening: Boolean
) {
    BEGINNER(
        displayName = "Beginner",
        blurb = "Learns the field: short sighted and forgiving.",
        maxDepth = 2,
        timeLimitMs = 250,
        choiceMarginCp = 220,
        blunderChance = 0.30,
        useQuiescence = false,
        useTranspositionTable = false,
        useIterativeDeepening = true
    ),
    HARD(
        displayName = "Hard",
        blurb = "Punishes loose pieces and avoids obvious blunders.",
        maxDepth = 3,
        timeLimitMs = 900,
        choiceMarginCp = 45,
        blunderChance = 0.05,
        useQuiescence = true,
        useTranspositionTable = true,
        useIterativeDeepening = true
    ),
    EXPERT(
        displayName = "Expert",
        blurb = "Deep tactical vision with positional understanding.",
        maxDepth = 4,
        timeLimitMs = 1600,
        choiceMarginCp = 12,
        blunderChance = 0.0,
        useQuiescence = true,
        useTranspositionTable = true,
        useIterativeDeepening = true
    ),
    MASTER(
        displayName = "Master",
        blurb = "The strongest search the realm can field, within a fair time budget.",
        maxDepth = 5,
        timeLimitMs = 2600,
        choiceMarginCp = 0,
        blunderChance = 0.0,
        useQuiescence = true,
        useTranspositionTable = true,
        useIterativeDeepening = true
    );

    companion object {
        fun fromName(name: String?): Difficulty =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: BEGINNER
    }
}
