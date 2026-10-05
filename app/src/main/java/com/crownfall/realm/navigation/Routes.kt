package com.crownfall.realm.navigation

import com.crownfall.realm.domain.ai.Difficulty

/** All destinations in the app. */
object Routes {
    const val MENU = "menu"
    const val HISTORY = "history"
    const val STATISTICS = "statistics"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"

    const val SINGLE_PLAYER = "single_player/{difficulty}"
    const val TWO_PLAYER = "two_player"
    const val CONTINUE = "continue_game"

    const val ARG_DIFFICULTY = "difficulty"

    fun singlePlayer(difficulty: Difficulty): String = "single_player/${difficulty.name}"
}
