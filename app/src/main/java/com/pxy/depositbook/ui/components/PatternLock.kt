package com.pxy.depositbook.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PatternLock(
    modifier: Modifier = Modifier,
    title: String = "请绘制手势密码",
    subtitle: String = "",
    onSuccess: (String) -> Unit = {},
    onVerify: ((String) -> Boolean)? = null,
    onError: () -> Unit = {},
) {
    var error by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Int>() }
    var currentPos by remember { mutableStateOf<Offset?>(null) }

    fun getPatternString(): String = selected.joinToString("")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 320.dp)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        if (error) {
            Text("手势密码错误", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        // 手势区随可用空间放大（不再固定 280dp）
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // 取可用区域的较短边作为九宫格边长，尽量吃满（点本身在 25%~75% 位置，边缘天然有留白）
            val side = minOf(maxWidth, maxHeight) * 0.98f
            Box(
                modifier = Modifier
                    .size(side)
                    .pointerInput(side) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selected.clear()
                                error = false
                                currentPos = offset
                                val hit = side.toPx() / 8f
                                val pts = getPoints(this.size.width.toFloat(), this.size.height.toFloat())
                                for (i in pts.indices) {
                                    if ((pts[i] - offset).getDistance() < hit) {
                                        if (!selected.contains(i)) selected.add(i)
                                        break
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPos = change.position
                                val hit = side.toPx() / 8f
                                val pts = getPoints(this.size.width.toFloat(), this.size.height.toFloat())
                                for (i in pts.indices) {
                                    if ((pts[i] - change.position).getDistance() < hit && !selected.contains(i)) {
                                        selected.add(i)
                                        break
                                    }
                                }
                            },
                            onDragEnd = {
                                currentPos = null
                                if (selected.size >= 4) {
                                    val pattern = getPatternString()
                                    if (onVerify != null) {
                                        if (onVerify(pattern)) {
                                            onSuccess(pattern)
                                        } else {
                                            error = true
                                            selected.clear()
                                            onError()
                                        }
                                    } else {
                                        onSuccess(pattern)
                                    }
                                } else {
                                    selected.clear()
                                    error = true
                                    onError()
                                }
                            },
                        )
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = getPoints(size.width, size.height)
                    val normalR = size.width / 30f
                    val activeR = size.width / 21f
                    val lineW = size.width / 26f
                    val primary = Color(0xFF6366F1)
                    val outline = Color(0xFF9CA3AF)
                    for (i in points.indices) {
                        drawCircle(
                            color = if (selected.contains(i)) primary else outline,
                            radius = if (selected.contains(i)) activeR else normalR,
                            center = points[i],
                        )
                    }
                    for (i in 0 until selected.size - 1) {
                        val from = points[selected[i]]
                        val to = points[selected[i + 1]]
                        drawLine(color = primary, start = from, end = to, strokeWidth = lineW, cap = StrokeCap.Round)
                    }
                    val last = selected.lastOrNull()
                    if (last != null && currentPos != null) {
                        drawLine(color = primary.copy(alpha = 0.6f), start = points[last], end = currentPos!!, strokeWidth = lineW, cap = StrokeCap.Round)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("至少连接 4 个点", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getPoints(w: Float, h: Float): List<Offset> {
    val cx = w / 2f
    val cy = h / 2f
    val r = w / 4f
    return listOf(
        Offset(cx - r, cy - r),
        Offset(cx, cy - r),
        Offset(cx + r, cy - r),
        Offset(cx - r, cy),
        Offset(cx, cy),
        Offset(cx + r, cy),
        Offset(cx - r, cy + r),
        Offset(cx, cy + r),
        Offset(cx + r, cy + r),
    )
}
