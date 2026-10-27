package com.pxy.depositbook.ui.screen.log

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pxy.depositbook.data.entity.OperationLog
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.di.AppViewModelFactory
import com.pxy.depositbook.ui.theme.Accent
import com.pxy.depositbook.ui.theme.ChartColors
import com.pxy.depositbook.ui.theme.Danger
import com.pxy.depositbook.ui.theme.Warning
import com.pxy.depositbook.util.FormatUtils
import com.pxy.depositbook.viewmodel.LogViewModel
import kotlinx.coroutines.launch

@Composable
fun LogScreen(repository: DepositRepository) {
    val vm: LogViewModel = viewModel(factory = AppViewModelFactory.provide(repository))
    val logs by vm.logs.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showClear by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<OperationLog?>(null) }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text(I18n.s("清空日志", "Clear log")) },
            text = { Text(I18n.s("确定要清空全部操作日志吗？此操作不可恢复。", "Clear all operation logs? This cannot be undone.")) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.clearLogs()
                        android.widget.Toast.makeText(context, I18n.s("已清空日志", "Logs cleared"), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    showClear = false
                }) { Text(I18n.s("清空", "Clear"), color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text(I18n.s("取消", "Cancel")) }
            },
        )
    }

    pendingDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(I18n.s("删除日志", "Delete record")) },
            text = { Text(I18n.s("确定要删除这条操作日志吗？", "Delete this operation record?")) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteLog(log.id)
                        android.widget.Toast.makeText(context, I18n.s("已删除日志", "Record deleted"), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    pendingDelete = null
                }) { Text(I18n.s("删除", "Delete"), color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(I18n.s("取消", "Cancel")) }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(I18n.s("操作日志", "Operation Log"), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        I18n.s("共 ${logs.size} 条记录", "${logs.size} records"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (logs.isNotEmpty()) {
                    IconButton(onClick = { showClear = true }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = I18n.s("清空日志", "Clear log"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (logs.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(I18n.s("暂无操作记录", "No records yet"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        items(logs, key = { it.id }) { log ->
            LogItem(log, onDelete = { pendingDelete = log })
        }
    }
}

@Composable
private fun LogItem(log: OperationLog, onDelete: () -> Unit) {
    val (color, label) = when (log.action) {
        "CREATE" -> Accent to I18n.s("新增", "Add")
        "UPDATE" -> ChartColors[5] to I18n.s("修改", "Edit")
        "DELETE" -> Danger to I18n.s("删除", "Delete")
        "SETTLE" -> Warning to I18n.s("结清", "Settle")
        "IMPORT" -> ChartColors[4] to I18n.s("导入", "Import")
        else -> MaterialTheme.colorScheme.onSurfaceVariant to log.action
    }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        FormatUtils.dateTime(log.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(log.content, fontSize = 13.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = I18n.s("删除日志", "Delete record"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
