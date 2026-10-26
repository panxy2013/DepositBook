package com.pxy.depositbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.model.DepositUi
import com.pxy.depositbook.data.model.StatsCalculator
import com.pxy.depositbook.data.model.SummaryStats
import com.pxy.depositbook.data.repo.DepositRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val summary: SummaryStats = SummaryStats(),
    val deposits: List<DepositUi> = emptyList(),
    val expiringSoon: List<DepositUi> = emptyList(),
)

class HomeViewModel(private val repo: DepositRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> = repo.observeAll()
        .map { list ->
            val ui = StatsCalculator.toUiList(list)
            HomeUiState(
                loading = false,
                summary = StatsCalculator.summarize(list),
                deposits = ui.sortedWith(
                    compareBy<DepositUi> { it.expired }
                        .thenBy { it.daysLeft }
                ),
                expiringSoon = ui.filter { it.deposit.isActive && it.daysLeft in 0..30 }
                    .sortedBy { it.daysLeft },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun delete(deposit: Deposit) = viewModelScope.launch { repo.delete(deposit) }

    fun settle(deposit: Deposit) = viewModelScope.launch { repo.settle(deposit) }

    fun save(deposit: Deposit, onDone: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repo.save(deposit)
        onDone(id)
    }
}

class StatsViewModel(private val repo: DepositRepository) : ViewModel() {

    data class StatsUiState(
        val summary: SummaryStats = SummaryStats(),
        val groups: List<com.pxy.depositbook.data.model.GroupStat> = emptyList(),
        val trend: List<com.pxy.depositbook.data.model.TrendPoint> = emptyList(),
    )

    val state: StateFlow<StatsUiState> = repo.observeAll()
        .map { list ->
            StatsUiState(
                summary = StatsCalculator.summarize(list),
                groups = StatsCalculator.groupByAccount(list),
                trend = StatsCalculator.monthlyTrend(list, months = 6),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}

class LogViewModel(private val repo: DepositRepository) : ViewModel() {

    val logs = repo.observeLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLog(action: String, content: String) = viewModelScope.launch {
        repo.addLog(action, content)
    }
}
