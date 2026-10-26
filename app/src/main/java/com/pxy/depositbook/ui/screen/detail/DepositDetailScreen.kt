package com.pxy.depositbook.ui.screen.detail

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
                title = { Text("存款详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { current?.let { onEdit(it.deposit.id) } }) {
                        Icon(Icons.Outlined.Edit, "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Outlined.DeleteOutline, "删除", tint = Danger)
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            current.expired -> Pill("已到期", Color(0xFF6B7B8A), Color(0xFFE8EBEF))
                            current.urgent -> Pill("${current.daysLeft}天后到期", Danger, DangerLight)
                            current.expiringSoon -> Pill("${current.daysLeft}天后到期", Warning, WarningLight)
                            else -> Pill("${current.daysLeft}天后到期", Accent, AccentLight)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("本金", color = Color(0xFFB9C8D5), fontSize = 12.sp)
                    Text(FormatUtils.money(d.principal), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text(
                            "本金 + 利息 " + FormatUtils.money(current.valueNow),
                            color = Color(0xFFD5DEE6),
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("年化 ${FormatUtils.rate(d.annualRate)}", color = Color(0xFF9DB0BF), fontSize = 13.sp)
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
                        Text("已存 ${current.daysPassed} 天", color = Color(0xFFB9C8D5), fontSize = 11.sp)
                        Text("共 ${current.daysPassed + maxOf(current.daysLeft, 0)} 天", color = Color(0xFFB9C8D5), fontSize = 11.sp)
                    }
                }
            }

            // 收益明细
            SectionCard("收益明细") {
                DetailRow("日利息", "+" + FormatUtils.moneyPlain(current.dailyInterest), Accent)
                DetailRow("累计已获利息", "+" + FormatUtils.moneyPlain(current.interestEarned), Accent)
                DetailRow("当前本金+利息", FormatUtils.money(current.valueNow))
                DetailRow("到期预估本息", FormatUtils.money(current.valueAtMaturity), Accent)
            }

            // 基本信息
            SectionCard("基本信息") {
                d.productName?.let { DetailRow("产品名称", it) }
                DetailRow("年化利率", FormatUtils.rate(d.annualRate))
                DetailRow("存入日期", FormatUtils.date(d.startDate))
                DetailRow("到期日期", FormatUtils.date(d.endDate))
                d.termDays?.let { DetailRow("期限", "$it 天") }
                DetailRow("状态", if (d.isActive) "计息中" else "已结清")
                d.note?.takeIf { it.isNotBlank() }?.let { DetailRow("备注", it) }
            }

            // 时间线
            SectionCard("时间线") {
                TimelineRow("存入", FormatUtils.date(d.startDate), false)
                TimelineRow("今天", FormatUtils.date(System.currentTimeMillis()), true)
                TimelineRow(
                    if (current.expired) "已到期" else "到期",
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
                    Text("标记为已结清", color = Accent)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这条存款？") },
            text = { Text("删除后不可恢复，操作会记录到日志中。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    current?.let {
                        scope.launch {
                            repository.delete(it.deposit)
                            onBack()
                        }
                    }
                }) { Text("删除", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
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
