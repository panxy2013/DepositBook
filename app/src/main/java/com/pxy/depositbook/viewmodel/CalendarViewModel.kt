package com.pxy.depositbook.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarMode { MONTH, YEAR }

data class CalendarCell(
    val date: LocalDate,
    val isCurrentPeriod: Boolean,
    val startCount: Int = 0,
    val endCount: Int = 0,
    val hasData: Boolean = false,
)

data class CalendarItem(
    val deposit: Deposit,
    val type: ItemType,
)

enum class ItemType { START, END }

data class CalendarUiState(
    val mode: CalendarMode = CalendarMode.MONTH,
    val anchor: LocalDate = DateUtils.today(),
    val cells: List<CalendarCell> = emptyList(),
    val title: String = "",
    val selectedDate: LocalDate? = null,
    val selectedItems: List<CalendarItem> = emptyList(),
)

class CalendarViewModel(private val repo: DepositRepository) : ViewModel() {

    private val _mode = MutableStateFlow(CalendarMode.MONTH)
    private val _anchor = MutableStateFlow(DateUtils.today())
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private var allDeposits: List<Deposit> = emptyList()

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.observeAll(), _mode, _anchor, _selectedDate) { deposits, mode, anchor, selected ->
                allDeposits = deposits
                buildState(deposits, mode, anchor, selected)
            }.collect { _state.value = it }
        }
    }

    fun toggleMode() {
        _mode.value = if (_mode.value == CalendarMode.MONTH) CalendarMode.YEAR else CalendarMode.MONTH
        _selectedDate.value = null
    }

    fun prev() {
        _selectedDate.value = null
        _anchor.value = if (_mode.value == CalendarMode.MONTH) {
            _anchor.value.minusMonths(1)
        } else {
            _anchor.value.minusYears(12)
        }
    }

    fun next() {
        _selectedDate.value = null
        _anchor.value = if (_mode.value == CalendarMode.MONTH) {
            _anchor.value.plusMonths(1)
        } else {
            _anchor.value.plusYears(12)
        }
    }

    fun goToday() {
        val today = DateUtils.today()
        _anchor.value = today
        // 年视图下选中当年格子（1月1日），便于高亮显示
        _selectedDate.value = if (_mode.value == CalendarMode.YEAR) {
            LocalDate.of(today.year, 1, 1)
        } else {
            today
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = if (_selectedDate.value == date) null else date
    }

    fun clearSelection() {
        _selectedDate.value = null
    }

    private fun buildState(
        deposits: List<Deposit>,
        mode: CalendarMode,
        anchor: LocalDate,
        selected: LocalDate?,
    ): CalendarUiState {
        return if (mode == CalendarMode.MONTH) {
            buildMonth(deposits, anchor, selected)
        } else {
            buildYear(deposits, anchor, selected)
        }
    }

    private fun buildMonth(
        deposits: List<Deposit>,
        anchor: LocalDate,
        selected: LocalDate?,
    ): CalendarUiState {
        val ym = YearMonth.from(anchor)
        val days = ym.lengthOfMonth()
        val startCounts = deposits.groupBy { DateUtils.toDate(it.startDate) }.mapValues { it.value.size }
        val endCounts = deposits.groupBy { DateUtils.toDate(it.endDate) }.mapValues { it.value.size }

        val cells = (1..days).map { d ->
            val date = ym.atDay(d)
            CalendarCell(
                date = date,
                isCurrentPeriod = true,
                startCount = startCounts[date] ?: 0,
                endCount = endCounts[date] ?: 0,
                hasData = (startCounts[date] ?: 0) > 0 || (endCounts[date] ?: 0) > 0,
            )
        }

        val items = if (selected != null) {
            deposits.filter { DateUtils.toDate(it.startDate) == selected || DateUtils.toDate(it.endDate) == selected }
                .map { d ->
                    val isStart = DateUtils.toDate(d.startDate) == selected
                    val isEnd = DateUtils.toDate(d.endDate) == selected
                    if (isStart && !isEnd) CalendarItem(d, ItemType.START)
                    else if (isEnd && !isStart) CalendarItem(d, ItemType.END)
                    else if (isStart && isEnd) CalendarItem(d, ItemType.START)
                    else CalendarItem(d, ItemType.END)
                }
        } else emptyList()

        return CalendarUiState(
            mode = CalendarMode.MONTH,
            anchor = anchor,
            cells = cells,
            title = com.pxy.depositbook.util.I18n.s(
                "${ym.year}年${ym.monthValue}月",
                "${ym.year}-${ym.monthValue}",
            ),
            selectedDate = selected,
            selectedItems = items,
        )
    }

    private fun buildYear(
        deposits: List<Deposit>,
        anchor: LocalDate,
        selected: LocalDate?,
    ): CalendarUiState {
        val year = anchor.year
        val start = year - 11
        val end = year
        val startCounts = deposits.groupBy { DateUtils.toDate(it.startDate).year }.mapValues { it.value.size }
        val endCounts = deposits.groupBy { DateUtils.toDate(it.endDate).year }.mapValues { it.value.size }
        val cells = (0 until 12).map { offset ->
            val y = start + offset
            val date = LocalDate.of(y, 1, 1)
            CalendarCell(
                date = date,
                isCurrentPeriod = y == year,
                startCount = startCounts[y] ?: 0,
                endCount = endCounts[y] ?: 0,
                hasData = (startCounts[y] ?: 0) > 0 || (endCounts[y] ?: 0) > 0,
            )
        }

        val items = if (selected != null) {
            deposits.filter { DateUtils.toDate(it.startDate).year == selected.year || DateUtils.toDate(it.endDate).year == selected.year }
                .flatMap { d ->
                    val list = mutableListOf<CalendarItem>()
                    val sd = DateUtils.toDate(d.startDate)
                    val ed = DateUtils.toDate(d.endDate)
                    if (sd.year == selected.year) list.add(CalendarItem(d, ItemType.START))
                    if (ed.year == selected.year && ed != sd) list.add(CalendarItem(d, ItemType.END))
                    list
                }
                .sortedBy { item ->
                    val d = if (item.type == ItemType.START) DateUtils.toDate(item.deposit.startDate) else DateUtils.toDate(item.deposit.endDate)
                    d
                }
        } else emptyList()

        return CalendarUiState(
            mode = CalendarMode.YEAR,
            anchor = anchor,
            cells = cells,
            title = com.pxy.depositbook.util.I18n.s(
                "${year}年 · $start–$end",
                "$year · $start–$end",
            ),
            selectedDate = selected,
            selectedItems = items,
        )
    }
}
