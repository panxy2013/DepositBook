package com.pxy.depositbook.ui.screen.detail

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pxy.depositbook.data.model.DepositUi
import com.pxy.depositbook.data.model.StatsCalculator
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.ui.components.Pill
import com.pxy.depositbook.ui.theme.Accent
import com.pxy.depositbook.ui.theme.AccentLight
import com.pxy.depositbook.ui.theme.Danger
import com.pxy.depositbook.ui.theme.DangerLight
import com.pxy.depositbook.ui.theme.Warning
import com.pxy.depositbook.ui.theme.WarningLight
import com.pxy.depositbook.util.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositDetailScreen(
    repository: DepositRepository,
    depositId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var ui by remember { mutableStateOf<DepositUi?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(depositId) {
        repository.get(depositId)?.let { ui = StatsCalculator.toUi(it) }
    }

    val current = ui
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(I18n.s("存款详情", "Deposit Details")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, I18n.s("返回", "Back"))
                    }
                },
                actions = {
                    IconButton(onClick = { current?.let { onEdit(it.deposit.id) } }) {
                        Icon(Icons.Outlined.Edit, I18n.s("编辑", "Edit"))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.DeleteOutline, I18n.s("删除", "Delete"), tint = Danger)
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(I18n.s("加载中…", "Loading…"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val d = current.deposit
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 主卡片
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2C3E50), Color(0xFF46617A))))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(d.accountName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        when {
                            current.expired -> Pill(I18n.s("已到期", "Matured"), Color(0xFF6B7B8A), Color(0xFFE8EBEF))
                            current.urgent -> Pill(I18n.s("${current.daysLeft}天后到期", "Due in ${current.daysLeft} days"), Danger, DangerLight)
                            current.expiringSoon -> Pill(I18n.s("${current.daysLeft}天后到期", "Due in ${current.daysLeft} days"), Warning, WarningLight)
                            else -> Pill(I18n.s("${current.daysLeft}天后到期", "Due in ${current.daysLeft} days"), Accent, AccentLight)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(I18n.s("本金", "Principal"), color = Color(0xFFB9C8D5), fontSize = 12.sp)
                    Text(FormatUtils.money(d.principal), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text(
                            I18n.s("本金 + 利息 ", "Principal + interest ") + FormatUtils.money(current.valueNow),
                            color = Color(0xFFD5DEE6),
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(I18n.s("年化 ${FormatUtils.rate(d.annualRate)}", "Rate ${FormatUtils.rate(d.annualRate)}"), color = Color(0xFF9DB0BF), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { current.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF6FCF97),
                        trackColor = Color(0x33FFFFFF),
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(I18n.s("已存 ${current.daysPassed} 天", "${current.daysPassed} days held"), color = Color(0xFFB9C8D5), fontSize = 11.sp)
                        Text(I18n.s("共 ${current.daysPassed + maxOf(current.daysLeft, 0)} 天", "${current.daysPassed + maxOf(current.daysLeft, 0)} days total"), color = Color(0xFFB9C8D5), fontSize = 11.sp)
                    }
                }
            }

            // 收益明细
            SectionCard(I18n.s("收益明细", "Earnings")) {
                DetailRow(I18n.s("日利息", "Daily interest"), "+" + FormatUtils.moneyPlain(current.dailyInterest), Accent)
                DetailRow(I18n.s("累计已获利息", "Interest earned"), "+" + FormatUtils.moneyPlain(current.interestEarned), Accent)
                DetailRow(I18n.s("当前本金+利息", "Current P+I"), FormatUtils.money(current.valueNow))
                DetailRow(I18n.s("到期预估本息", "Est. P+I at maturity"), FormatUtils.money(current.valueAtMaturity), Accent)
            }

            // 基本信息
            SectionCard(I18n.s("基本信息", "Details")) {
                d.productName?.let { DetailRow(I18n.s("产品名称", "Product"), it) }
                DetailRow(I18n.s("年化利率", "Annual rate"), FormatUtils.rate(d.annualRate))
                DetailRow(I18n.s("存入日期", "Deposit date"), FormatUtils.date(d.startDate))
                DetailRow(I18n.s("到期日期", "Maturity date"), FormatUtils.date(d.endDate))
                d.termDays?.let { DetailRow(I18n.s("期限", "Term"), I18n.s("$it 天", "$it days")) }
                DetailRow(I18n.s("状态", "Status"), if (d.isActive) I18n.s("计息中", "Earning interest") else I18n.s("已结清", "Settled"))
                d.note?.takeIf { it.isNotBlank() }?.let { DetailRow(I18n.s("备注", "Note"), it) }
            }

            // 时间线
            SectionCard(I18n.s("时间线", "Timeline")) {
                TimelineRow(I18n.s("存入", "Deposited"), FormatUtils.date(d.startDate), false)
                TimelineRow(I18n.s("今天", "Today"), FormatUtils.date(System.currentTimeMillis()), true)
                TimelineRow(
                    if (current.expired) I18n.s("已到期", "Matured") else I18n.s("到期", "Maturity"),
                    FormatUtils.date(d.endDate),
                    !current.expired,
                )
            }

            if (d.isActive) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = {
                    scope.launch {
                        repository.settle(d)
                        repository.get(depositId)?.let { ui = StatsCalculator.toUi(it) }
                    }
                }) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Accent)
                    Spacer(Modifier.width(6.dp))
                    Text(I18n.s("标记为已结清", "Mark as settled"), color = Accent)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(I18n.s("删除这条存款？", "Delete this deposit?")) },
            text = { Text(I18n.s("删除后不可恢复，操作会记录到日志中。", "This cannot be undone; the action will be logged.")) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    current?.let {
                        scope.launch {
                            repository.delete(it.deposit)
                            onBack()
                        }
                    }
                }) { Text(I18n.s("删除", "Delete"), color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(I18n.s("取消", "Cancel")) }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun TimelineRow(label: String, date: String, highlight: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(if (highlight) 10.dp else 8.dp)
                .clip(CircleShape)
                .background(if (highlight) Accent else MaterialTheme.colorScheme.outline)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(date, fontSize = 13.sp, fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal)
    }
    Spacer(Modifier.height(10.dp))
}
