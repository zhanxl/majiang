package com.majiang.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majiang.model.PlayerStats
import com.majiang.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val sortBy: SortBy = SortBy.WIN_RATE,
    val selectedPlayer: PlayerStats? = null
)

enum class SortBy {
    WIN_RATE,
    TOTAL_GAMES,
    AVERAGE_SCORE
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    val playerStats: StateFlow<List<PlayerStats>> = statsRepository.getAllStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortBy(sortBy: SortBy) {
        _uiState.value = _uiState.value.copy(sortBy = sortBy)
    }

    fun selectPlayer(stats: PlayerStats?) {
        _uiState.value = _uiState.value.copy(selectedPlayer = stats)
    }
}
