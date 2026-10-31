package com.pxy.depositbook.ui.screen.stats

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
import com.pxy.depositbook.util.I18n

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDepositListScreen(
    repository: DepositRepository,
    bankName: String,
    onBack: () -> Unit,
    onOpenDeposit: (Long) -> Unit,
) {
    val deposits by repository.observeAll().collectAsStateWithLifecycle(emptyList())
    val filtered = deposits.filter { it.accountName == bankName && it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bankName, style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = I18n.s("返回", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        val nowMs = System.currentTimeMillis()
        val dayMsVal = 1000L * 60 * 60 * 24
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    I18n.s("共 ${filtered.size} 笔在存 · 本金 ${FormatUtils.money(filtered.sumOf { it.principal })}", "${filtered.size} active · principal ${FormatUtils.money(filtered.sumOf { it.principal })}"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (filtered.isEmpty()) {
                item {
                    Text(I18n.s("暂无在存记录", "No active deposits"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(filtered, key = { it.id }) { deposit ->
                val daysLeft = ((deposit.endDate - nowMs) / dayMsVal).toLong()
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDeposit(deposit.id) },
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(deposit.productName ?: "-", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                I18n.s("到期 ${FormatUtils.date(deposit.endDate)} · 年化 ${String.format("%.2f%%", deposit.annualRate * 100)}", "Matures ${FormatUtils.date(deposit.endDate)} · rate ${String.format("%.2f%%", deposit.annualRate * 100)}"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(FormatUtils.moneyCompact(deposit.principal), fontWeight = FontWeight.SemiBold)
                            Text(
                                if (daysLeft >= 0) I18n.s("剩余 ${daysLeft} 天", "${daysLeft} days left") else I18n.s("已到期", "Matured"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
