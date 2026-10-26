package com.pxy.depositbook.ui.screen.settings

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pxy.depositbook.BuildConfig
import com.pxy.depositbook.DepositApp
import com.pxy.depositbook.LockManager
import com.pxy.depositbook.data.backup.BackupCodec
import com.pxy.depositbook.data.backup.BackupFile
import com.pxy.depositbook.data.backup.DepositDto
import com.pxy.depositbook.data.backup.XlsxExporter
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.ui.components.PatternLock
import com.pxy.depositbook.ui.components.PinLock
import com.pxy.depositbook.util.XlsxImporter
import com.pxy.depositbook.worker.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(repository: DepositRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val optionsStore = remember { (context.applicationContext as DepositApp).options }
    var banks by remember { mutableStateOf(optionsStore.banks()) }
    var products by remember { mutableStateOf(optionsStore.products()) }
    fun refresh() {
        banks = optionsStore.banks()
        products = optionsStore.products()
    }
    var message by remember { mutableStateOf<String?>(null) }
    // 状态提醒：Toast 立即可见 + 页面底部横幅留痕
    fun showStatus(text: String) {
        message = text
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
    }
    var showClearDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var importingKind by remember { mutableStateOf("") }

    fun handleImport(uri: Uri, kind: String) {
        importing = true
        importingKind = kind
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext null
                    when (kind) {
                        "json" -> {
                            val backup = BackupCodec.decodeBackup(text.toString(Charsets.UTF_8))
                                ?: return@withContext null
                            val deposits = backup.deposits.map { dto ->
                                Deposit(
                                    accountName = dto.accountName,
                                    owner = dto.owner,
                                    productName = dto.productName,
                                    principal = dto.principal,
                                    annualRate = dto.annualRate,
                                    startDate = dto.startDate,
                                    endDate = dto.endDate,
                                    termDays = dto.termDays,
                                    note = dto.note,
                                    isActive = dto.isActive,
                                    createdAt = dto.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis(),
                                )
                            }
                            ImportBundle(deposits, 0, "JSON")
                        }
                        else -> {
                            val r = XlsxImporter.parse(text)
                            ImportBundle(r.deposits, r.skippedRows, "Excel")
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
            importing = false
            if (result == null || result.deposits.isEmpty()) {
                showStatus(if (kind == "json") {
                    "导入失败：JSON 格式不正确或没有数据"
                } else {
                    "导入失败：未识别到「总账」表头或数据"
                })
            } else {
                result.deposits.forEach { repository.save(it) }
                repository.addLog(
                    "IMPORT",
                    "从 ${result.source} 导入 ${result.deposits.size} 笔存款" +
                            if (result.skipped > 0) "，跳过 ${result.skipped} 条" else ""
                )
                showStatus("从 ${result.source} 成功导入 ${result.deposits.size} 笔存款")
            }
        }
    }

    val xlsxPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handleImport(uri, "xlsx")
    }

    val jsonPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handleImport(uri, "json")
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))

        SettingsGroup("导入") {
            SettingsItem(
                icon = { Icon(Icons.Outlined.UploadFile, null) },
                title = "从 Excel 导入",
                subtitle = "选择账本 .xlsx，读取「总账」sheet",
                onClick = {
                    LockManager.suspend()
                    xlsxPicker.launch(
                        arrayOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel",
                            "*/*",
                        )
                    )
                },
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.UploadFile, null) },
                title = "从 JSON 导入",
                subtitle = "恢复本 App 导出的备份文件",
                onClick = {
                    LockManager.suspend()
                    jsonPicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
            )
        }

        SettingsGroup("导出") {
            SettingsItem(
                icon = { Icon(Icons.Outlined.FileDownload, null) },
                title = "导出 Excel（.xlsx）",
                subtitle = "生成「总账」表格，可直接用 Excel 打开",
                onClick = {
                    scope.launch {
                        val path = exportXlsx(context, repository)
                        showStatus(if (path != null) "导出成功，已保存到 $path" else "导出失败")
                    }
                },
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.FileDownload, null) },
                title = "导出 JSON 备份",
                subtitle = "包含全部存款与日志，可再次导入",
                onClick = {
                    scope.launch {
                        val path = exportJson(context, repository)
                        showStatus(if (path != null) "导出成功，已保存到 $path" else "导出失败")
                    }
                },
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.Share, null) },
                title = "通过微信等分享导出",
                subtitle = "将 Excel 或 JSON 分享到微信、邮件等",
                onClick = { showShareDialog = true },
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.Storage, null) },
                title = "清空数据",
                subtitle = "删除所有存款与日志，不可恢复",
                onClick = { showClearDialog = true },
            )
        }

        SettingsGroup("选项维护") {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "银行名称默认留空，可从已有存款导入",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            optionsStore.resetToDefaults()
                            refresh()
                            showStatus("已恢复默认选项")
                        }
                    ) { Text("恢复默认") }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val all = repository.getAllOnce()
                                val bankSet = banks.toMutableSet()
                                val prodSet = products.toMutableSet()
                                all.forEach { d ->
                                    if (!d.accountName.isNullOrBlank()) bankSet.add(d.accountName)
                                    if (!d.productName.isNullOrBlank()) prodSet.add(d.productName)
                                }
                                bankSet.forEach { if (it !in banks) optionsStore.addBank(it) }
                                prodSet.forEach { if (it !in products) optionsStore.addProduct(it) }
                                refresh()
                                showStatus("已从已有存款导入选项")
                            }
                        }
                    ) { Text("从已有存款导入") }
                }
            }
            OptionsSection(
                title = "银行名称",
                items = banks,
                onAdd = {
                    optionsStore.addBank(it)
                    refresh()
                },
                onRemove = {
                    optionsStore.removeBank(it)
                    refresh()
                },
                onRename = { old, new ->
                    scope.launch {
                        val rows = repository.renameAccount(old, new)
                        optionsStore.renameBank(old, new)
                        refresh()
                        showStatus("已重命名为「$new」，同步 $rows 条存款记录")
                    }
                },
                placeholder = "新增银行名称",
            )
            OptionsSection(
                title = "产品名称",
                items = products,
                onAdd = {
                    optionsStore.addProduct(it)
                    refresh()
                },
                onRemove = {
                    optionsStore.removeProduct(it)
                    refresh()
                },
                onRename = { old, new ->
                    optionsStore.renameProduct(old, new)
                    refresh()
                },
                placeholder = "新增产品名称",
            )
        }

        SettingsGroup("提醒") {
            SettingsItem(
                icon = { Icon(Icons.Outlined.Notifications, null) },
                title = "到期提醒",
                subtitle = "到期前 30 天内每天检查并推送通知",
                onClick = {
                    ReminderScheduler.schedule(context)
                    showStatus("已安排每日到期检查")
                },
            )
        }

        SecuritySection()

        SettingsGroup("关于") {
            Column(Modifier.padding(16.dp)) {
                Text("存款账本 v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "本地离线存储，数据只保存在本机。\n" +
                            "利息按「本金 × 年化 ÷ 365 × 已存天数」计算，与 Excel 账本口径一致。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                val mailContext = LocalContext.current
                Text(
                    "问题反馈请发邮件到 23831644@qq.com",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        try {
                            mailContext.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:23831644@qq.com"))
                            )
                        } catch (e: Exception) {
                            showStatus("未找到邮件应用，邮箱：23831644@qq.com")
                        }
                    },
                )
            }
        }

        if (importing) {
            Text("正在导入…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        message?.let {
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空所有数据？") },
            text = { Text("此操作不可恢复，请确保已导出备份。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    scope.launch {
                        repository.getAllOnce().forEach { repository.delete(it) }
                        showStatus("已清空")
                    }
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("分享到微信等") },
            text = { Text("选择要分享的文件格式，分享到微信、QQ、邮件等") },
            confirmButton = {
                TextButton(onClick = {
                    showShareDialog = false
                    scope.launch {
                        val uri = shareFile(context, repository, "xlsx")
                        if (uri == null) showStatus("分享失败") else shareViaIntent(context, uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    }
                }) { Text("Excel (.xlsx)") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showShareDialog = false
                        scope.launch {
                            val uri = shareFile(context, repository, "json")
                            if (uri == null) showStatus("分享失败") else shareViaIntent(context, uri, "application/json")
                        }
                    }) { Text("JSON 备份") }
                    TextButton(onClick = { showShareDialog = false }) { Text("取消") }
                }
            },
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { content() }
    }
}

@Composable
private fun SettingsItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            icon()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class ImportBundle(
    val deposits: List<Deposit>,
    val skipped: Int,
    val source: String,
)

private fun timestampName(ext: String): String =
    "deposit_book_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + "." + ext

/** 写入 Download 目录（Android 10+ 用 MediaStore，低版本用应用专属目录） */
private fun saveToDownloads(
    context: Context,
    name: String,
    mime: String,
    bytes: ByteArray,
): String? {
    return try {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return null
            "Download/$name"
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
            val file = java.io.File(dir, name)
            file.writeBytes(bytes)
            file.absolutePath
        }
    } catch (e: Exception) {
        null
    }
}

private suspend fun exportJson(context: Context, repository: DepositRepository): String? =
    withContext(Dispatchers.IO) {
        try {
            val deposits = repository.getAllOnce()
            val backup = BackupFile(
                appVersion = BuildConfig.VERSION_NAME,
                deposits = deposits.map { d ->
                    DepositDto(
                        accountName = d.accountName,
                        owner = d.owner,
                        productName = d.productName,
                        principal = d.principal,
                        annualRate = d.annualRate,
                        startDate = d.startDate,
                        endDate = d.endDate,
                        termDays = d.termDays,
                        note = d.note,
                        isActive = d.isActive,
                        createdAt = d.createdAt,
                        updatedAt = d.updatedAt,
                    )
                },
            )
            saveToDownloads(
                context,
                timestampName("json"),
                "application/json",
                BackupCodec.encodeBackup(backup).toByteArray(Charsets.UTF_8),
            )
        } catch (e: Exception) {
            null
        }
    }

private suspend fun exportXlsx(context: Context, repository: DepositRepository): String? =
    withContext(Dispatchers.IO) {
        try {
            val deposits = repository.getAllOnce()
            val bytes = XlsxExporter.export(deposits)
            saveToDownloads(
                context,
                timestampName("xlsx"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes,
            )
        } catch (e: Exception) {
            null
        }
    }

@Composable
private fun OptionsSection(
    title: String,
    items: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRename: (String, String) -> Unit,
    placeholder: String,
) {
    var input by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var editInput by remember { mutableStateOf("") }

    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))

        items.forEach { item ->
            if (editing == item) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = editInput,
                        onValueChange = { editInput = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        if (editInput.isNotBlank()) {
                            onRename(item, editInput)
                            editing = null
                        }
                    }, enabled = editInput.isNotBlank()) { Text("保存") }
                    TextButton(onClick = { editing = null }) { Text("取消") }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { editing = item; editInput = item },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "编辑 $item",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(
                        onClick = { onRemove(item) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "删除 $item",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(placeholder, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = {
                    if (input.isNotBlank()) {
                        onAdd(input)
                        input = ""
                    }
                },
                enabled = input.isNotBlank(),
            ) { Text("添加") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private suspend fun shareFile(context: Context, repository: DepositRepository, type: String): Uri? = withContext(Dispatchers.IO) {
    try {
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val name = timestampName(if (type == "xlsx") "xlsx" else "json")
        val file = File(dir, name)
        if (type == "xlsx") {
            val deposits = repository.getAllOnce()
            file.writeBytes(XlsxExporter.export(deposits))
        } else {
            val deposits = repository.getAllOnce()
            val backup = BackupFile(
                appVersion = "1.0",
                deposits = deposits.map { d ->
                    DepositDto(
                        accountName = d.accountName,
                        owner = d.owner,
                        productName = d.productName,
                        principal = d.principal,
                        annualRate = d.annualRate,
                        startDate = d.startDate,
                        endDate = d.endDate,
                        termDays = d.termDays,
                        note = d.note,
                        isActive = d.isActive,
                        createdAt = d.createdAt,
                        updatedAt = d.updatedAt,
                    )
                },
            )
            file.writeText(BackupCodec.encodeBackup(backup), Charsets.UTF_8)
        }
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    } catch (e: Exception) {
        null
    }
}

private fun shareViaIntent(context: Context, uri: Uri, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    // 分享面板短暂离开应用，挂起锁避免返回时要求重新验证
    LockManager.suspend()
    context.startActivity(Intent.createChooser(intent, "分享到...").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}


@Composable
private fun SecuritySection() {
    val context = LocalContext.current
    val app = context.applicationContext as com.pxy.depositbook.DepositApp
    val security = app.security
    var enabled by remember { mutableStateOf(security.isEnabled()) }
    var lockType by remember { mutableStateOf(security.getLockType()) }
    var showSet by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(0) }
    var tempPin by remember { mutableStateOf("") }
    var tempPattern by remember { mutableStateOf("") }

    SettingsGroup("安全") {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("应用锁")
                    Text("从后台切回或首次进入需要验证", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked && security.getPin().isEmpty() && security.getPattern().isEmpty()) {
                            showSet = true
                            step = 0
                            enabled = false
                        } else {
                            enabled = checked
                            security.setEnabled(checked)
                        }
                    },
                )
            }
            if (enabled) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("解锁方式", modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        lockType = if (lockType == "pin") "pattern" else "pin"
                        security.setLockType(lockType)
                    }) {
                        Text(if (lockType == "pin") "数字密码" else "手势密码")
                    }
                }
                TextButton(onClick = { showSet = true; step = 0; tempPin = ""; tempPattern = "" }) {
                    Text("修改密码")
                }
            }
        }
    }

    if (showSet) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSet = false }) {
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.large) {
                if (lockType == "pin") {
                    if (step == 0) {
                        PinLock(title = "设置数字密码（4位）", onSuccess = { pin -> tempPin = pin; step = 1 })
                    } else {
                        PinLock(
                            title = "确认数字密码",
                            onVerify = { it == tempPin },
                            onSuccess = {
                                security.setPin(tempPin)
                                security.setLockType("pin")
                                security.setEnabled(true)
                                enabled = true
                                showSet = false
                            },
                            onError = { step = 0 },
                        )
                    }
                } else {
                    if (step == 0) {
                        PatternLock(title = "设置手势密码（至少4点）", onSuccess = { pattern -> tempPattern = pattern; step = 1 })
                    } else {
                        PatternLock(
                            title = "确认手势密码",
                            onVerify = { it == tempPattern },
                            onSuccess = {
                                security.setPattern(tempPattern)
                                security.setLockType("pattern")
                                security.setEnabled(true)
                                enabled = true
                                showSet = false
                            },
                            onError = { step = 0 },
                        )
                    }
                }
            }
        }
    }
}
