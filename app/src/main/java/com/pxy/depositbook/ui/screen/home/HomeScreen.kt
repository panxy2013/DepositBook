package com.pxy.depositbook.ui.screen.home

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
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)) }

    val context = LocalContext.current
    val options = remember { (context.applicationContext as DepositApp).options }
    // 隐私模式：默认值来自设置页，可随时点眼睛切换（只作用于总览区，30天内到期笔数除外）
    var privacy by remember { mutableStateOf(options.privacyDefault()) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddDeposit,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("新增存款") },
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
                            "存款总览",
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
                            contentDescription = if (privacy) "显示金额" else "隐藏金额",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                HeroStatCard(
                    title = "当前本金总数（${state.summary.activeCount} 笔在存）",
                    value = if (privacy) HIDDEN else FormatUtils.money(state.summary.totalPrincipal),
                    subtitle = if (privacy) {
                        "截止今日 本金+利息 $HIDDEN"
                    } else {
                        "截止今日 本金+利息 " + FormatUtils.money(state.summary.totalValueNow)
                    },
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat(
                        label = "今日新增利息",
                        value = if (privacy) HIDDEN else "+" + FormatUtils.moneyPlain(state.summary.todayInterest),
                        valueColor = Accent,
                        icon = { Icon(Icons.Outlined.TrendingUp, null, modifier = Modifier.height(14.dp), tint = Accent) },
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenHomeDetail("TODAY_INTEREST") },
                    )
                    MiniStat(
                        label = "累计已获利息",
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
                        label = "30天内到期",
                        value = "${state.summary.expiringSoonCount} 笔",
                        valueColor = Warning,
                        icon = { Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.height(14.dp), tint = Warning) },
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenHomeDetail("EXPIRING_SOON") },
                    )
                    MiniStat(
                        label = "到期预估总额",
                        value = if (privacy) HIDDEN else FormatUtils.moneyCompact(state.summary.totalValueAtMaturity),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenHomeDetail("MATURITY_TOTAL") },
                    )
                }
            }

            if (state.expiringSoon.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader("即将到期提醒")
                }
                items(state.expiringSoon, key = { it.deposit.id }) { ui ->
                    DepositCard(ui = ui, onClick = { onOpenDeposit(ui.deposit.id) })
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader("全部存款（${state.deposits.size} 笔）")
            }

            if (state.deposits.isEmpty() && !state.loading) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        Text("还没有存款记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "点击右下角「新增存款」开始记录",
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
