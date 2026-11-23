package com.pxy.depositbook.ui.screen.stats

import com.pxy.depositbook.util.I18n

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

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
            var changeMode by remember { mutableStateOf(false) }
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
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !changeMode,
                        onClick = { changeMode = false },
                        label = { Text(I18n.s("累计金额", "Cumulative")) },
                    )
                    FilterChip(
                        selected = changeMode,
                        onClick = { changeMode = true },
                        label = {
                            Text(
                                if (trendMode == "month") I18n.s("月度变动", "Monthly change")
                                else I18n.s("年度变动", "Yearly change")
                            )
                        },
                    )
                }
                Spacer(Modifier.height(10.dp))
                val points = if (trendMode == "month") state.monthTrend else state.yearTrend
                if (points.isEmpty()) {
                    EmptyHint(I18n.s("暂无数据", "No data"))
                } else {
                    TrendChart(points, changeMode)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        I18n.s("按住图表可查看每期金额", "Touch and hold the chart to inspect a period"),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                            // 银行图标（内置显示 logo，否则彩色首字圆标）+ 原配色小点
                            com.pxy.depositbook.ui.components.BankIcon(g.name, size = 22.dp)
                            Spacer(Modifier.width(8.dp))
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
private fun TrendChart(points: List<TrendPoint>, changeMode: Boolean) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val negColor = MaterialTheme.colorScheme.error
    val dotFill = MaterialTheme.colorScheme.surface
    val bubbleBg = MaterialTheme.colorScheme.inverseSurface
    val bubbleFg = MaterialTheme.colorScheme.inverseOnSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    var scrub by remember(points, changeMode) { mutableStateOf<Int?>(null) }

    // 变化量序列：首期取当期累计值（此前视作 0），之后为相邻两期之差
    val deltas = if (changeMode) {
        points.mapIndexed { i, p -> if (i == 0) p.total else p.total - points[i - 1].total }
    } else emptyList()
    val values = if (changeMode) deltas else points.map { it.total }

    fun valueText(v: Double): String =
        if (changeMode && v > 0) "+" + FormatUtils.moneyPlain(v) else FormatUtils.moneyPlain(v)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(points, changeMode) {
                detectDragGestures(
                    onDragStart = { o -> scrub = nearestIndex(o.x, 54.dp.toPx(), size.width.toFloat(), points.size) },
                    onDrag = { ch, _ -> scrub = nearestIndex(ch.position.x, 54.dp.toPx(), size.width.toFloat(), points.size) },
                    onDragEnd = { scrub = null },
                    onDragCancel = { scrub = null },
                )
            }
            .pointerInput(points, changeMode) {
                detectTapGestures(
                    onPress = { o ->
                        scrub = nearestIndex(o.x, 54.dp.toPx(), size.width.toFloat(), points.size)
                        tryAwaitRelease()
                        scrub = null
                    },
                )
            },
    ) {
        val n = points.size
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 9.5.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            color = labelColor.toArgb()
        }
        val axisPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 8.5.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            color = subtleColor.copy(alpha = 0.85f).toArgb()
        }
        val bubblePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 9.5.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            color = bubbleFg.toArgb()
        }

        val leftGutter = 54.dp.toPx()
        val bottomLabels = 22.dp.toPx()
        val topPad = 24.dp.toPx()
        val plotLeft = leftGutter
        val plotRight = size.width - 2.dp.toPx()
        val plotTop = topPad
        val plotBottom = size.height - bottomLabels
        val plotH = (plotBottom - plotTop).coerceAtLeast(1f)

        // 纵轴自适应：不再从 0 起算，小幅度变化也能看出趋势
        val lo: Double
        val hi: Double
        if (changeMode) {
            val lowV = minOf(0.0, values.min())
            val highV = maxOf(0.0, values.max())
            val span = (highV - lowV).coerceAtLeast(maxOf(abs(highV), abs(lowV)) * 0.05).coerceAtLeast(1.0)
            lo = lowV - span * 0.18
            hi = highV + span * 0.18
        } else {
            val vMin = values.min()
            val vMax = values.max()
            val span = (vMax - vMin).coerceAtLeast(abs(vMax) * 0.04).coerceAtLeast(1.0)
            lo = vMin - span * 0.2
            hi = vMax + span * 0.2
        }
        fun yOf(v: Double): Float = (plotBottom - ((v - lo) / (hi - lo) * plotH)).toFloat()

        val stepX = if (n == 1) 0f else (plotRight - plotLeft) / (n - 1)
        val cx = List(n) { i -> if (n == 1) (plotLeft + plotRight) / 2f else plotLeft + stepX * i }

        // 网格线 + 左侧刻度（最小值/中间/最大值）
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
        val gridColor = subtleColor.copy(alpha = 0.22f)
        val axisLabels = listOf(lo, (lo + hi) / 2, hi)
        axisLabels.forEach { v ->
            val y = yOf(v)
            drawLine(gridColor, Offset(plotLeft, y), Offset(plotRight, y), strokeWidth = 1f, pathEffect = dash)
        }
        // 刻度文字在数据绘制后再画（带底色，避免与柱状/标签文字叠在一起）
        val drawAxisLabels = {
            axisLabels.forEach { v ->
                val y = yOf(v)
                val t = FormatUtils.moneyPlain(kotlin.math.round(v).toDouble())
                val tw = axisPaint.measureText(t)
                val right = leftGutter - 8.dp.toPx()
                val baseline = y + axisPaint.textSize * 0.35f
                drawRect(
                    color = surfaceColor,
                    topLeft = Offset(right - tw - 2.dp.toPx(), baseline - axisPaint.textSize - 1.dp.toPx()),
                    size = Size(tw + 4.dp.toPx(), axisPaint.textSize + 4.dp.toPx()),
                )
                drawIntoCanvas { it.nativeCanvas.drawText(t, right, baseline, axisPaint) }
            }
        }

        if (!changeMode) {
            // 累计金额：面积渐变 + 折线
            if (n >= 2) {
                val fill = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx[0], plotBottom)
                    for (i in 0 until n) lineTo(cx[i], yOf(values[i]))
                    lineTo(cx[n - 1], plotBottom)
                    close()
                }
                drawPath(
                    fill,
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.18f), lineColor.copy(alpha = 0.01f)),
                        startY = plotTop,
                        endY = plotBottom,
                    ),
                )
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx[0], yOf(values[0]))
                    for (i in 1 until n) lineTo(cx[i], yOf(values[i]))
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            for (i in 0 until n) {
                val cy = yOf(values[i])
                drawCircle(color = dotFill, radius = 5.5.dp.toPx(), center = Offset(cx[i], cy))
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(cx[i], cy))
            }
        } else {
            // 变动量：零轴 + 柱状图（存为正、取为负）
            val zeroY = yOf(0.0)
            drawLine(
                subtleColor.copy(alpha = 0.45f),
                Offset(plotLeft, zeroY),
                Offset(plotRight, zeroY),
                strokeWidth = 1.2.dp.toPx(),
            )
            val barW = min(if (n > 1) stepX * 0.55f else 18.dp.toPx(), 16.dp.toPx())
            for (i in 0 until n) {
                val v = values[i]
                if (abs(v) < 1e-6) continue // 无变动不画柱，保持零轴干净
                val y = yOf(v)
                val top = min(y, zeroY)
                val h = abs(y - zeroY).coerceAtLeast(1.5.dp.toPx())
                drawRoundRect(
                    color = if (v >= 0) lineColor else negColor,
                    topLeft = Offset(cx[i] - barW / 2, top),
                    size = Size(barW, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(min(barW / 2, 3.dp.toPx())),
                )
            }
        }

        drawAxisLabels()

        // 关键点标注：点数少时全标；点多时只标首尾、最高、最低与大幅变动点
        val keyIndices: List<Int> = if (n <= 6) {
            (0 until n).toList()
        } else {
            val s = sortedSetOf(0, n - 1, values.indices.maxBy { values[it] }, values.indices.minBy { values[it] })
            val thr = (hi - lo) * 0.12
            for (i in 1 until n) if (abs(values[i] - values[i - 1]) >= thr) s.add(i)
            s.toList()
        }
        var lastText = ""
        var lastRect: android.graphics.RectF? = null
        for (i in keyIndices) {
            val v = values[i]
            val text = valueText(v)
            val halfW = textPaint.measureText(text) / 2f
            val labelX = cx[i].coerceIn(leftGutter + halfW + 2.dp.toPx(), size.width - halfW - 2.dp.toPx())
            val labelY = if (!changeMode) {
                (yOf(v) - 8.dp.toPx()).coerceAtLeast(textPaint.textSize)
            } else if (v >= 0) {
                (min(yOf(v), yOf(0.0)) - 6.dp.toPx()).coerceAtLeast(textPaint.textSize)
            } else {
                (yOf(v) + textPaint.textSize + 5.dp.toPx()).coerceAtMost(size.height - 2.dp.toPx())
            }
            val rect = android.graphics.RectF(
                labelX - halfW,
                labelY - textPaint.textSize,
                labelX + halfW,
                labelY + 2.dp.toPx(),
            )
            val prev = lastRect
            if (text == lastText || (prev != null && android.graphics.RectF.intersects(prev, rect))) continue
            drawIntoCanvas { it.nativeCanvas.drawText(text, labelX, labelY, textPaint) }
            lastText = text
            lastRect = rect
        }

        // 横轴标签：最多 5 个，均匀取点
        val tickCount = if (n <= 5) n else 5
        val ticks = (0 until tickCount)
            .map { if (tickCount == 1) 0 else it * (n - 1) / (tickCount - 1) }
            .distinct()
        for (i in ticks) {
            val text = points[i].monthLabel
            val halfW = textPaint.measureText(text) / 2f
            val x = cx[i].coerceIn(halfW + 2.dp.toPx(), size.width - halfW - 2.dp.toPx())
            drawIntoCanvas { it.nativeCanvas.drawText(text, x, size.height - 6.dp.toPx(), textPaint) }
        }

        // 按住查看：竖线 + 气泡
        scrub?.let { raw ->
            val i = raw.coerceIn(0, n - 1)
            val x = cx[i]
            val dotY = yOf(values[i])
            drawLine(
                lineColor.copy(alpha = 0.35f),
                Offset(x, plotTop),
                Offset(x, plotBottom),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dash,
            )
            drawCircle(color = lineColor, radius = 5.5.dp.toPx(), center = Offset(x, dotY))
            val txt = points[i].monthLabel + "  " + valueText(values[i])
            val pad = 8.dp.toPx()
            val bw = bubblePaint.measureText(txt) + pad * 2
            val bh = bubblePaint.textSize + pad
            val bx = (x - bw / 2).coerceIn(2.dp.toPx(), size.width - bw - 2.dp.toPx())
            val by = (dotY - bh - 12.dp.toPx()).coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                color = bubbleBg,
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bh / 2),
            )
            drawIntoCanvas {
                it.nativeCanvas.drawText(txt, bx + bw / 2, by + bh / 2 + bubblePaint.textSize * 0.35f, bubblePaint)
            }
        }
    }
}

/** 手指 x 坐标 → 最近的数据点下标（左侧刻度栏之外的绘图区均匀分布） */
private fun nearestIndex(x: Float, leftPx: Float, width: Float, n: Int): Int {
    if (n <= 1) return 0
    val step = (width - leftPx) / (n - 1)
    return ((x - leftPx) / step).roundToInt().coerceIn(0, n - 1)
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
