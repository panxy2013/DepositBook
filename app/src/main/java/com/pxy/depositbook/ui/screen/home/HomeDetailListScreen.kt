package com.pxy.depositbook.ui.screen.home

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.util.FormatUtils
import com.pxy.depositbook.util.InterestCalculator
import java.time.LocalDate

enum class HomeDetailType { TODAY_INTEREST, TOTAL_INTEREST, EXPIRING_SOON, MATURITY_TOTAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDetailListScreen(
    repository: DepositRepository,
    type: HomeDetailType,
    onBack: () -> Unit,
    onOpenDeposit: (Long) -> Unit,
) {
    val deposits by repository.observeAll().collectAsStateWithLifecycle(emptyList())
    val today = LocalDate.now()
    val nowMs = System.currentTimeMillis()
    val dayMs = 1000L * 60 * 60 * 24

    val items = when (type) {
        HomeDetailType.TODAY_INTEREST -> {
            deposits.filter { it.isActive }.map { d ->
                val daily = InterestCalculator.dailyInterest(d.principal, d.annualRate)
                Triple(d, daily, I18n.s("今日新增利息 ${FormatUtils.money(daily)}", "Interest today ${FormatUtils.money(daily)}"))
            }.filter { it.second > 0 }.sortedByDescending { it.second }
        }
        HomeDetailType.TOTAL_INTEREST -> {
            deposits.filter { it.isActive }.map { d ->
                val earned = InterestCalculator.interestEarned(d, today)
                Triple(d, earned, I18n.s("累计已获利息 ${FormatUtils.money(earned)}", "Interest earned ${FormatUtils.money(earned)}"))
            }.sortedByDescending { it.second }
        }
        HomeDetailType.EXPIRING_SOON -> {
            deposits.filter { it.isActive }.map { d ->
                val left = ((d.endDate - nowMs) / dayMs).toLong()
                Triple(d, left.toDouble(), I18n.s("剩余 $left 天 · 到期 ${FormatUtils.date(d.endDate)}", "$left days left · matures ${FormatUtils.date(d.endDate)}"))
            }.filter { it.second >= 0 && it.second <= 30 }
                .sortedBy { it.second } // 30天内到期，以最近到期排序
        }
        HomeDetailType.MATURITY_TOTAL -> {
            deposits.filter { it.isActive }.map { d ->
                val value = InterestCalculator.valueAtMaturity(d)
                Triple(d, value, I18n.s("到期预估本息 ${FormatUtils.money(value)}", "Est. P+I ${FormatUtils.money(value)}"))
            }.sortedByDescending { it.second }
        }
    }

    val title = when (type) {
        HomeDetailType.TODAY_INTEREST -> I18n.s("今日新增利息明细", "Interest Earned Today")
        HomeDetailType.TOTAL_INTEREST -> I18n.s("累计已获利息明细", "Total Interest Earned")
        HomeDetailType.EXPIRING_SOON -> I18n.s("30天内到期明细", "Due in 30 Days")
        HomeDetailType.MATURITY_TOTAL -> I18n.s("到期预估总额明细", "Est. Total at Maturity")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = I18n.s("返回", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                val sumText = if (type == HomeDetailType.EXPIRING_SOON) {
                    I18n.s("共 ${items.size} 笔", "${items.size} items")
                } else {
                    I18n.s("共 ${items.size} 笔 · 合计 ${FormatUtils.money(items.sumOf { it.second })}", "${items.size} items · total ${FormatUtils.money(items.sumOf { it.second })}")
                }
                Text(
                    sumText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (items.isEmpty()) {
                item {
                    Text(I18n.s("暂无数据", "No data"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(items, key = { it.first.id }) { item ->
                val d = item.first
                val sub = item.third
                val valStr = when (type) {
                    HomeDetailType.EXPIRING_SOON -> {
                        val left = ((d.endDate - nowMs) / dayMs).toLong()
                        if (left >= 0) I18n.s("剩余 $left 天", "$left days left") else I18n.s("已到期", "Matured")
                    }
                    HomeDetailType.TODAY_INTEREST -> "+" + FormatUtils.moneyCompact(item.second)
                    HomeDetailType.TOTAL_INTEREST -> "+" + FormatUtils.moneyCompact(item.second)
                    HomeDetailType.MATURITY_TOTAL -> FormatUtils.moneyCompact(item.second)
                }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDeposit(d.id) },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${d.accountName} · ${d.productName ?: "-"}", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(valStr, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
