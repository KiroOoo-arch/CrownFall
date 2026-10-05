package com.crownfall.realm.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownfall.realm.data.repository.StatisticsRepository
import com.crownfall.realm.domain.models.PieceStat
import com.crownfall.realm.domain.models.PlayerProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Streams the player profile and the per-piece career records. */
class StatisticsViewModel(statistics: StatisticsRepository) : ViewModel() {

    private val profiles = statistics.observeProfile()

    val profile: StateFlow<PlayerProfile> = profiles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlayerProfile(
            name = "Realm Champion",
            gamesPlayed = 0,
            wins = 0,
            losses = 0,
            draws = 0,
            rating = 1200,
            favouritePiece = "Foot Soldier",
            totalCaptures = 0,
            bestWinStreak = 0,
            currentWinStreak = 0,
            pieceStats = emptyList()
        )
    )

    val pieceStats: StateFlow<List<PieceStat>> = profiles
        .map { it.pieceStats }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
