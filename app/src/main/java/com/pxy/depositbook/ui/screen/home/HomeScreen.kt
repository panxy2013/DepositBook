package com.pxy.depositbook.ui.screen.home

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.di.AppViewModelFactory
import com.pxy.depositbook.ui.components.DepositCard
import com.pxy.depositbook.ui.components.HeroStatCard
import com.pxy.depositbook.ui.components.MiniStat
import com.pxy.depositbook.ui.components.SectionHeader
import com.pxy.depositbook.ui.theme.Accent
import com.pxy.depositbook.ui.theme.Warning
import com.pxy.depositbook.util.FormatUtils
import com.pxy.depositbook.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.pxy.depositbook.DepositApp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 隐私模式下的金额占位符（不带单位） */
private const val HIDDEN = "***"

@Composable
fun HomeScreen(
    repository: DepositRepository,
    onAddDeposit: () -> Unit,
    onOpenDeposit: (Long) -> Unit,
    onOpenHomeDetail: (String) -> Unit = {},
) {
    val vm: HomeViewModel = viewModel(factory = AppViewModelFactory.provide(repository))
    val state by vm.state.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern(I18n.s("M月d日 EEEE", "MMM d, EEEE"), Locale.CHINA)) }

    val context = LocalContext.current
    val options = remember { (context.applicationContext as DepositApp).options }
    // 隐私模式：默认值来自设置页，可随时点眼睛切换（只作用于总览区，30天内到期笔数除外）
    var privacy by remember { mutableStateOf(options.privacyDefault()) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddDeposit,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(I18n.s("新增存款", "Add deposit")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            I18n.s("存款总览", "Deposit Overview"),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            today,
                            fontSize = 12.dp.let { MaterialTheme.typography.labelMedium.fontSize },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // 隐私开关：隐藏总览区金额，仅 30 天内到期笔数始终可见
                    IconButton(onClick = { privacy = !privacy }) {
                        Icon(
                            imageVector = if (privacy) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (privacy) I18n.s("显示金额", "Show amounts") else I18n.s("隐藏金额", "Hide amounts"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                HeroStatCard(
                    title = I18n.s("当前本金总数（${state.summary.activeCount} 笔在存）", "Total principal (${state.summary.activeCount} active)"),
                    value = if (privacy) HIDDEN else FormatUtils.money(state.summary.totalPrincipal),
                    subtitle = if (privacy) {
                        I18n.s("截止今日 本金+利息 $HIDDEN", "Principal + interest to date $HIDDEN")
                    } else {
                        I18n.s("截止今日 本金+利息 ", "Principal + interest to date ") + FormatUtils.money(state.summary.totalValueNow)
                    },
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat(
                        label = I18n.s("今日新增利息", "Interest today"),
                        value = if (privacy) HIDDEN else "+" + FormatUtils.moneyPlain(state.summary.todayInterest),
                        valueColor = Accent,
                        icon = { Icon(Icons.Outlined.TrendingUp, null, modifier = Modifier.height(14.dp), tint = Accent) },
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenHomeDetail("TODAY_INTEREST") },
                    )
                    MiniStat(
                        label = I18n.s("累计已获利息", "Interest earned"),
                        value = if (privacy) HIDDEN else "+" + FormatUtils.moneyPlain(state.summary.totalInterestEarned),
                        valueColor = Accent,
                        icon = { Icon(Icons.Outlined.Savings, null, modifier = Modifier.height(14.dp), tint = Accent) },
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenHomeDetail("TOTAL_INTEREST") },
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat(
                        label = I18n.s("30天内到期", "Due in 30 days"),
                        value = I18n.s("${state.summary.expiringSoonCount} 笔", "${state.summary.expiringSoonCount} items"),
                        valueColor = Warning,
                        icon = { Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.height(14.dp), tint = Warning) },
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenHomeDetail("EXPIRING_SOON") },
                    )
                    MiniStat(
                        label = I18n.s("到期预估总额", "Est. total at maturity"),
                        value = if (privacy) HIDDEN else FormatUtils.moneyCompact(state.summary.totalValueAtMaturity),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenHomeDetail("MATURITY_TOTAL") },
                    )
                }
            }

            if (state.expiringSoon.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(I18n.s("即将到期提醒", "Expiring soon"))
                }
                items(state.expiringSoon, key = { it.deposit.id }) { ui ->
                    DepositCard(ui = ui, onClick = { onOpenDeposit(ui.deposit.id) })
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(I18n.s("全部存款（${state.deposits.size} 笔）", "All deposits (${state.deposits.size})"))
            }

            if (state.deposits.isEmpty() && !state.loading) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        Text(I18n.s("还没有存款记录", "No deposits yet"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            I18n.s("点击右下角「新增存款」开始记录", "Tap \"Add deposit\" at bottom-right to start"),
                            fontSize = 12.dp.let { MaterialTheme.typography.labelMedium.fontSize },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(state.deposits.withIndex().toList(), key = { it.value.deposit.id }) { indexed ->
                val ui = indexed.value
                val idx = indexed.index + 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$idx.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        DepositCard(ui = ui, onClick = { onOpenDeposit(ui.deposit.id) })
                    }
                }
            }
        }
    }
}
