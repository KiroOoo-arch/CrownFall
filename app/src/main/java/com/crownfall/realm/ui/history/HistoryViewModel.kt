package com.crownfall.realm.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownfall.realm.data.repository.GameRepository
import com.crownfall.realm.domain.models.MatchDetail
import com.crownfall.realm.domain.models.MatchSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistoryState(
    val selected: MatchDetail? = null,
    val loading: Boolean = false
)

/** Lists finished matches and loads the full move list of one of them. */
class HistoryViewModel(private val games: GameRepository) : ViewModel() {

    val matches: StateFlow<List<MatchSummary>> = games.observeMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _detail = MutableStateFlow(HistoryState())
    val detail: StateFlow<HistoryState> = _detail.asStateFlow()

    fun open(gameId: Long) {
        _detail.update { it.copy(loading = true) }
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { games.matchDetail(gameId) }
            _detail.update { it.copy(selected = loaded, loading = false) }
        }
    }

    fun closeDetail() = _detail.update { it.copy(selected = null) }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { games.reset() }
        }
    }
}
