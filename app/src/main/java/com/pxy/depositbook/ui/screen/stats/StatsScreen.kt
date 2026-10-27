package com.pxy.depositbook.ui.screen.stats

import com.pxy.depositbook.util.I18n

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pxy.depositbook.data.model.GroupStat
import com.pxy.depositbook.data.model.TrendPoint
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.di.AppViewModelFactory
import com.pxy.depositbook.ui.theme.Accent
import com.pxy.depositbook.ui.theme.ChartColors
import com.pxy.depositbook.util.FormatUtils
import com.pxy.depositbook.viewmodel.StatsViewModel

@Composable
fun StatsScreen(
    repository: DepositRepository,
    onOpenBank: (String) -> Unit = {},
    onOpenCalendar: () -> Unit = {},
) {
    val vm: StatsViewModel = viewModel(factory = AppViewModelFactory.provide(repository))
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(I18n.s("统计分析", "Statistics"), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        I18n.s("共 ${state.summary.activeCount} 笔在存 · 本金 ${FormatUtils.money(state.summary.totalPrincipal)}", "${state.summary.activeCount} active · principal ${FormatUtils.money(state.summary.totalPrincipal)}"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { Log.d("DepositBook", "StatsScreen: click calendar"); onOpenCalendar() }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(I18n.s("日历", "Calendar"))
                }
            }
        }

        item {
            SummaryStrip(
                principal = state.summary.totalPrincipal,
                interest = state.summary.totalInterestEarned,
                valueNow = state.summary.totalValueNow,
            )
        }

        item {
            SectionCard(I18n.s("资产分布（按账户）", "Asset Mix (by account)")) {
                if (state.groups.isEmpty()) {
                    EmptyHint(I18n.s("暂无数据", "No data"))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            groups = state.groups.take(7),
                            modifier = Modifier.size(130.dp).clickable { if (state.groups.isNotEmpty()) onOpenBank(state.groups.first().name) },
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.groups.take(7).forEachIndexed { i, g ->
                                LegendRow(ChartColors[i % ChartColors.size], g.name, g.ratio) { onOpenBank(g.name) }
                            }
                        }
                    }
                }
            }
        }

        item {
            var trendMode by remember { mutableStateOf("month") }
            SectionCard(I18n.s("本金趋势", "Principal Trend")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = trendMode == "month",
                        onClick = { trendMode = "month" },
                        label = { Text(I18n.s("按月", "Monthly")) },
                    )
                    FilterChip(
                        selected = trendMode == "year",
                        onClick = { trendMode = "year" },
                        label = { Text(I18n.s("按年", "Yearly")) },
                    )
                }
                Spacer(Modifier.height(10.dp))
                val points = if (trendMode == "month") state.monthTrend else state.yearTrend
                if (points.isEmpty()) {
                    EmptyHint(I18n.s("暂无数据", "No data"))
                } else {
                    TrendLineChart(points)
                }
            }
        }

        item {
            SectionCard(I18n.s("历史资产记录（每日快照）", "Asset History (daily snapshots)")) {
                if (state.history.isEmpty()) {
                    EmptyHint(I18n.s("暂无记录，每天会自动记录一次", "No records yet; one snapshot is recorded daily"))
                } else {
                    // 按月归并：每月取最后一天的快照，倒序展示
                    val monthly = state.history
                        .groupBy { it.day.take(7) }
                        .map { (_, list) -> list.last() }
                        .sortedByDescending { it.day }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        monthly.take(24).forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    I18n.s("${s.day.take(4)}年${s.day.take(7).takeLast(2)}月", "${s.day.take(7)}"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.width(78.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        I18n.s("本金 ${FormatUtils.moneyPlain(s.totalPrincipal)} · 本息 ${FormatUtils.moneyPlain(s.totalValue)}", "Principal ${FormatUtils.moneyPlain(s.totalPrincipal)} · P+I ${FormatUtils.moneyPlain(s.totalValue)}"),
                                        fontSize = 12.sp,
                                    )
                                    Text(
                                        I18n.s("已获利息 +${FormatUtils.moneyPlain(s.totalInterest)} · 在存 ${s.activeCount} 笔", "Interest +${FormatUtils.moneyPlain(s.totalInterest)} · ${s.activeCount} active"),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        Text(
                            I18n.s("快照为历史数据，之后删除/结清存单不会改变已有记录；清空所有数据才会删除", "Snapshots are immutable history: deleting or settling deposits won't change them; only Clear All Data removes them"),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }

        item {
            SectionCard(I18n.s("账户明细（点击可查看明细）", "Account Details (tap to view)")) {
                if (state.groups.isEmpty()) {
                    EmptyHint(I18n.s("暂无数据", "No data"))
                } else {
                    state.groups.forEachIndexed { i, g ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBank(g.name) }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ChartColors[i % ChartColors.size])
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(
                                    I18n.s("本金 ${FormatUtils.moneyPlain(g.principal)}", "Principal ${FormatUtils.moneyPlain(g.principal)}"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("+" + FormatUtils.moneyPlain(g.interest), fontSize = 12.sp, color = Accent)
                                Text(
                                    FormatUtils.moneyPlain(g.valueNow),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStrip(principal: Double, interest: Double, valueNow: Double) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp)) {
            StripItem(I18n.s("本金总额", "Total principal"), FormatUtils.moneyCompact(principal), Modifier.weight(1f))
            StripItem(I18n.s("累计利息", "Interest earned"), "+" + FormatUtils.moneyCompact(interest), Modifier.weight(1f), Accent)
            StripItem(I18n.s("本息合计", "P + I total"), FormatUtils.moneyCompact(valueNow), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StripItem(label: String, value: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun DonutChart(groups: List<GroupStat>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 26.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        var start = -90f
        groups.forEachIndexed { i, g ->
            val sweep = g.ratio * 360f
            drawArc(
                color = ChartColors[i % ChartColors.size],
                startAngle = start,
                sweepAngle = sweep - 1.5f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            start += sweep
        }
    }
}

@Composable
private fun LegendRow(color: Color, name: String, ratio: Float, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(name, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
        Text(
            "%.1f%%".format(ratio * 100),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrendLineChart(points: List<TrendPoint>) {
    val max = points.maxOf { it.total }.coerceAtLeast(1.0)
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val dotFill = MaterialTheme.colorScheme.surface

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val n = points.size

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                color = labelColor.toArgb()
            }
            val topPad = 20.dp.toPx() // 点上方金额标签的空间
            val chartBottom = size.height
            val chartH = chartBottom - topPad

            val stepX = size.width / (n - 1).coerceAtLeast(1)
            val centers = points.mapIndexed { i, _ ->
                if (n == 1) size.width / 2f else stepX * i
            }
            val ys = points.map { p ->
                chartBottom - ((p.total / max) * (chartH - 8.dp.toPx())).toFloat()
            }

            // 折线（两点以上）
            if (n >= 2) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centers.first(), ys.first())
                    for (i in 1 until n) lineTo(centers[i], ys[i])
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            // 每个点：圆点 + 点上方金额
            // 去重规则：与上一个已显示金额相同的点（同一段水平线）不重复标注；
            // 相邻两个标签矩形重叠时也跳过，避免挤在一起
            var lastText = ""
            var lastRect: android.graphics.RectF? = null
            points.forEachIndexed { i, p ->
                val cx = centers[i]
                val cy = ys[i]
                drawCircle(color = dotFill, radius = 6.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = lineColor, radius = 4.5f.dp.toPx(), center = Offset(cx, cy))

                val text = FormatUtils.moneyCompact(p.total)
                val halfW = paint.measureText(text) / 2f
                val labelX = cx.coerceIn(halfW + 2.dp.toPx(), size.width - halfW - 2.dp.toPx())
                val labelY = (cy - 12.dp.toPx()).coerceAtLeast(paint.textSize)
                val rect = android.graphics.RectF(
                    labelX - halfW,
                    labelY - paint.textSize,
                    labelX + halfW,
                    labelY + 2.dp.toPx(),
                )
                val prev = lastRect
                if (text == lastText || (prev != null && android.graphics.RectF.intersects(prev, rect))) {
                    return@forEachIndexed
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(text, labelX, labelY, paint)
                }
                lastText = text
                lastRect = rect
            }
        }
        Spacer(Modifier.height(6.dp))
        // 横轴标签（点太多时隔一个显示，避免重叠）
        Row(Modifier.fillMaxWidth()) {
            points.forEachIndexed { i, p ->
                Text(
                    if (points.size > 8 && i % 2 == 1) "" else p.monthLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
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
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
