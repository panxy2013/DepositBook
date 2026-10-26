package com.pxy.depositbook.ui.screen.calendar

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.di.AppViewModelFactory
import com.pxy.depositbook.util.FormatUtils
import com.pxy.depositbook.viewmodel.CalendarCell
import com.pxy.depositbook.viewmodel.CalendarItem
import com.pxy.depositbook.viewmodel.CalendarMode
import com.pxy.depositbook.viewmodel.CalendarViewModel
import com.pxy.depositbook.viewmodel.ItemType
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    repository: DepositRepository,
    onBack: () -> Unit,
    onOpenDeposit: (Long) -> Unit = {},
) {
    LaunchedEffect(Unit) { Log.d("DepositBook", "CalendarScreen: composed") }
    val vm: CalendarViewModel = viewModel(factory = AppViewModelFactory.provide(repository))
    val state by vm.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("存入/到期日历") },
                navigationIcon = {
                    IconButton(onClick = { Log.d("DepositBook", "CalendarScreen: onBack"); onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.prev() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowLeft, contentDescription = "上一个")
                        }
                        Text(
                            state.title,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(onClick = { vm.next() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowRight, contentDescription = "下一个")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = { vm.goToday() }) { Text("回到今天") }
                        Spacer(Modifier.weight(1f))
                        FilterChip(
                            selected = state.mode == CalendarMode.MONTH,
                            onClick = { if (state.mode != CalendarMode.MONTH) vm.toggleMode() },
                            label = { Text("按月") },
                        )
                        FilterChip(
                            selected = state.mode == CalendarMode.YEAR,
                            onClick = { if (state.mode != CalendarMode.YEAR) vm.toggleMode() },
                            label = { Text("按年") },
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(color = Color(0xFF22D3EE), text = "存入")
                    LegendDot(color = Color(0xFFF59E0B), text = "到期")
                }
            }

            if (state.mode == CalendarMode.MONTH) {
                item {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                            Text(
                                it,
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                item {
                    val first = state.cells.firstOrNull()?.date ?: today
                    val dow = first.dayOfWeek
                    val leading = when (dow) {
                        DayOfWeek.MONDAY -> 0
                        DayOfWeek.TUESDAY -> 1
                        DayOfWeek.WEDNESDAY -> 2
                        DayOfWeek.THURSDAY -> 3
                        DayOfWeek.FRIDAY -> 4
                        DayOfWeek.SATURDAY -> 5
                        DayOfWeek.SUNDAY -> 6
                        else -> 0
                    }
                    val cells = state.cells
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var index = 0
                        val total = leading + cells.size
                        val rows = (total + 6) / 7
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                for (c in 0..6) {
                                    val cellIndex = r * 7 + c
                                    if (cellIndex < leading) {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else if (index < cells.size) {
                                        val cell = cells[index++]
                                        MonthCell(
                                            cell = cell,
                                            isToday = cell.date == today,
                                            isSelected = state.selectedDate == cell.date,
                                            onClick = { vm.selectDate(cell.date) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    val cells = state.cells
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        cells.chunked(3).forEach { rowCells ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                rowCells.forEach { cell ->
                                    YearCell(
                                        cell = cell,
                                        isCurrentYear = cell.date.year == today.year,
                                        isSelected = state.selectedDate?.year == cell.date.year,
                                        onClick = { vm.selectDate(cell.date) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(3 - rowCells.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            if (state.selectedDate != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (state.mode == CalendarMode.MONTH) "当日记录 - ${state.selectedDate}" else "全年记录 - ${state.selectedDate?.year}年",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { vm.clearSelection() }) { Text("清除") }
                            }
                            if (state.selectedItems.isEmpty()) {
                                Text("暂无存入或到期记录", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    state.selectedItems.forEach { item ->
                                        DayItemRow(item) { onOpenDeposit(item.deposit.id) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.padding(4.dp)) }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthCell(
    cell: CalendarCell,
    isToday: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 1.dp else 0.dp),
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxSize().padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cell.date.dayOfMonth.toString(),
                    fontSize = 10.sp,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (cell.startCount > 0) {
                        Icon(Icons.Outlined.AddCircle, contentDescription = "存入", tint = Color(0xFF22D3EE), modifier = Modifier.size(10.dp))
                    }
                    if (cell.endCount > 0) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "到期", tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun YearCell(
    cell: CalendarCell,
    isCurrentYear: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                isCurrentYear -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .aspectRatio(1.1f)
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                ) else if (isCurrentYear) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                ) else Modifier,
            )
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cell.date.year.toString(),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrentYear) {
                    Text(
                        "今年",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (cell.startCount > 0) {
                        Icon(Icons.Outlined.AddCircle, contentDescription = null, tint = Color(0xFF22D3EE), modifier = Modifier.size(12.dp))
                    }
                    if (cell.endCount > 0) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (cell.startCount > 0) Text("存入 ${cell.startCount} 笔", fontSize = 10.sp, color = Color(0xFF22D3EE))
                if (cell.endCount > 0) Text("到期 ${cell.endCount} 笔", fontSize = 10.sp, color = Color(0xFFF59E0B))
                if (cell.startCount == 0 && cell.endCount == 0) Text("无记录", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayItemRow(item: CalendarItem, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (item.type == ItemType.START) Icons.Outlined.AddCircle else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (item.type == ItemType.START) Color(0xFF22D3EE) else Color(0xFFF59E0B),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(item.deposit.accountName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (item.type == ItemType.START) "存入" else "到期"} · ${FormatUtils.date(item.deposit.startDate)}→${FormatUtils.date(item.deposit.endDate)} · 本金 ${FormatUtils.money(item.deposit.principal)} · ${item.deposit.productName ?: ""}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
