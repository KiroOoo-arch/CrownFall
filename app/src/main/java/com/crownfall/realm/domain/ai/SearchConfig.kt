package com.crownfall.realm.domain.ai

/**
 * The tunable parameters of one AI search. Kept separate from [Difficulty] so
 * tests (and future "custom difficulty" screens) can drive the engine directly.
 */
data class SearchConfig(
    val label: String = "Custom",
    val maxDepth: Int = 3,
    val timeLimitMs: Long = 1000,
    val choiceMarginCp: Int = 0,
    val blunderChance: Double = 0.0,
    val useQuiescence: Boolean = true,
    val useTranspositionTable: Boolean = true,
    val useIterativeDeepening: Boolean = true
) {
    companion object {
        fun from(difficulty: Difficulty, timeLimitMs: Long = difficulty.timeLimitMs): SearchConfig =
            SearchConfig(
                label = difficulty.displayName,
                maxDepth = difficulty.maxDepth,
                timeLimitMs = timeLimitMs,
                choiceMarginCp = difficulty.choiceMarginCp,
                blunderChance = difficulty.blunderChance,
                useQuiescence = difficulty.useQuiescence,
                useTranspositionTable = difficulty.useTranspositionTable,
                useIterativeDeepening = difficulty.useIterativeDeepening
            )

        /** Deterministic, unlimited-time configuration used by unit tests. */
        fun deterministic(depth: Int): SearchConfig = SearchConfig(
            label = "Test",
            maxDepth = depth,
            timeLimitMs = 0L,
            choiceMarginCp = 0,
            blunderChance = 0.0,
            useQuiescence = true,
            useTranspositionTable = true,
            useIterativeDeepening = false
        )
    }
}
