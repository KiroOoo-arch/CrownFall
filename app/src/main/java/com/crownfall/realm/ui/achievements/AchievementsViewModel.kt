package com.crownfall.realm.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownfall.realm.data.entities.AchievementEntity
import com.crownfall.realm.data.repository.AchievementRepository
import com.crownfall.realm.data.repository.StatisticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AchievementsState(
    val unlocked: Int = 0,
    val total: Int = 0
)

/**
 * Achievements screen state. Progress is re-synced from the lifetime
 * statistics on entry, so a replay or a restored install shows correct values.
 */
class AchievementsViewModel(
    private val achievements: AchievementRepository,
    private val statistics: StatisticsRepository
) : ViewModel() {

    val items: StateFlow<List<AchievementEntity>> = achievements.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _summary = MutableStateFlow(AchievementsState())
    val summary: StateFlow<AchievementsState> = _summary.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                // Recompute from the current aggregates so the list is never stale
                // (this also picks up progress earned before achievements existed).
                val stats = statistics.currentStatistics()
                achievements.syncFromStatistics(stats)
            }
            _summary.update {
                it.copy(unlocked = list.count { item -> item.unlocked }, total = list.size)
            }
        }
    }
}
