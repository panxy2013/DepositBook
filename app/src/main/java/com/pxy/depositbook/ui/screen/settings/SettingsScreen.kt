package com.pxy.depositbook.ui.screen.settings

import com.pxy.depositbook.util.I18n

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
    var privacyDefaultOn by remember { mutableStateOf(optionsStore.privacyDefault()) }
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
                    I18n.s("导入失败：JSON 格式不正确或没有数据", "Import failed: invalid JSON or no data")
                } else {
                    I18n.s("导入失败：未识别到「总账」表头或数据", "Import failed: “General Ledger” header not recognized")
                })
            } else {
                result.deposits.forEach { repository.save(it) }
                repository.addLog(
                    "IMPORT",
                    I18n.s("从 ${result.source} 导入 ${result.deposits.size} 笔存款", "Imported ${result.deposits.size} deposits from ${result.source}") +
                            if (result.skipped > 0) I18n.s("，跳过 ${result.skipped} 条", ", skipped ${result.skipped} rows") else ""
                )
                showStatus(I18n.s("从 ${result.source} 成功导入 ${result.deposits.size} 笔存款", "Imported ${result.deposits.size} deposits from ${result.source}"))
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
        Text(I18n.s("设置", "Settings"), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))

        SettingsGroup(I18n.s("语言 Language", "Language 语言")) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(I18n.s("显示语言", "Display language"))
                        Text(
                            I18n.s("中英文可随时切换，界面立即生效", "Switch anytime; applies instantly"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        TextButton(onClick = { I18n.switchTo(I18n.ZH); message = I18n.s("已切换为中文", "Switched to Chinese") }) {
                            Text(if (I18n.isZh) "● 中文" else "中文")
                        }
                        TextButton(onClick = { I18n.switchTo(I18n.EN); message = I18n.s("已切换为英文", "Switched to English") }) {
                            Text(if (!I18n.isZh) "● English" else "English")
                        }
                    }
                }
            }
        }

        SettingsGroup(I18n.s("导入", "Import")) {
            SettingsItem(
                icon = { Icon(Icons.Outlined.UploadFile, null) },
                title = I18n.s("从 Excel 导入", "Import from Excel"),
                subtitle = I18n.s("选择账本 .xlsx，读取「总账」sheet", "Pick a .xlsx and read the “General Ledger” sheet"),
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
                title = I18n.s("从 JSON 导入", "Import from JSON"),
                subtitle = I18n.s("恢复本 App 导出的备份文件", "Restore a backup exported by this app"),
                onClick = {
                    LockManager.suspend()
                    jsonPicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
            )
        }

        SettingsGroup(I18n.s("导出", "Export")) {
            SettingsItem(
                icon = { Icon(Icons.Outlined.FileDownload, null) },
                title = I18n.s("导出 Excel（.xlsx）", "Export Excel (.xlsx)"),
                subtitle = I18n.s("生成「总账」表格，可直接用 Excel 打开", "Generates a “General Ledger” sheet, openable in Excel"),
                onClick = {
                    scope.launch {
                        val path = exportXlsx(context, repository)
                        showStatus(if (path != null) I18n.s("导出成功，已保存到 $path", "Exported to $path") else I18n.s("导出失败", "Export failed"))
                    }
                },
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.FileDownload, null) },
                title = I18n.s("导出 JSON 备份", "Export JSON backup"),
                subtitle = I18n.s("包含全部存款与日志，可再次导入", "Contains all deposits and logs; re-importable"),
                onClick = {
                    scope.launch {
                        val path = exportJson(context, repository)
                        showStatus(if (path != null) I18n.s("导出成功，已保存到 $path", "Exported to $path") else I18n.s("导出失败", "Export failed"))
                    }
                },
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.Share, null) },
                title = I18n.s("通过微信等分享导出", "Share export (WeChat etc.)"),
                subtitle = I18n.s("将 Excel 或 JSON 分享到微信、邮件等", "Share the Excel or JSON via WeChat, email, etc."),
                onClick = { showShareDialog = true },
            )
            SettingsItem(
                icon = { Icon(Icons.Outlined.Storage, null) },
                title = I18n.s("清空数据", "Clear all data"),
                subtitle = I18n.s("删除所有存款与日志，不可恢复", "Delete all deposits and logs. Irreversible."),
                onClick = { showClearDialog = true },
            )
        }

        SettingsGroup(I18n.s("选项维护", "Options")) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        I18n.s("银行名称默认留空，可从已有存款导入", "Bank list starts empty; import from existing deposits"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            optionsStore.resetToDefaults()
                            refresh()
                            showStatus(I18n.s("已恢复默认选项", "Options reset to defaults"))
                        }
                    ) { Text(I18n.s("恢复默认", "Reset")) }
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
                                showStatus(I18n.s("已从已有存款导入选项", "Options imported from existing deposits"))
                            }
                        }
                    ) { Text(I18n.s("从已有存款导入", "Import from deposits")) }
                }
            }
            OptionsSection(
                title = I18n.s("银行名称", "Banks"),
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
                        showStatus(I18n.s("已重命名为「$new」，同步 $rows 条存款记录", "Renamed to “$new”; updated $rows deposits"))
                    }
                },
                placeholder = I18n.s("新增银行名称", "New bank name"),
            )
            OptionsSection(
                title = I18n.s("产品名称", "Products"),
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
                placeholder = I18n.s("新增产品名称", "New product name"),
            )
        }

        SettingsGroup(I18n.s("提醒", "Reminders")) {
            SettingsItem(
                icon = { Icon(Icons.Outlined.Notifications, null) },
                title = I18n.s("到期提醒", "Maturity reminders"),
                subtitle = I18n.s("到期前 30 天内每天检查并推送通知", "Daily check and notify within 30 days before maturity"),
                onClick = {
                    ReminderScheduler.schedule(context)
                    showStatus(I18n.s("已安排每日到期检查", "Daily maturity check scheduled"))
                },
            )
        }

        SettingsGroup(I18n.s("隐私", "Privacy")) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(I18n.s("总览默认隐藏金额", "Hide amounts on Home by default"))
                        Text(
                            I18n.s("开启后进入总览页默认显示 ***，点右上角眼睛可随时切换", "When on, Home shows *** by default; tap the eye icon to toggle"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = privacyDefaultOn,
                        onCheckedChange = { checked ->
                            privacyDefaultOn = checked
                            optionsStore.setPrivacyDefault(checked)
                            message = if (checked) I18n.s("已开启默认隐藏金额", "Hide-by-default: on") else I18n.s("已关闭默认隐藏金额", "Hide-by-default: off")
                        },
                    )
                }
            }
        }

        SecuritySection()

        SettingsGroup(I18n.s("关于", "About")) {
            Column(Modifier.padding(16.dp)) {
                Text(I18n.s("存款账本 v${BuildConfig.VERSION_NAME}", "DepositBook v${BuildConfig.VERSION_NAME}"), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    I18n.s(
                        "本地离线存储，数据只保存在本机。\n利息按「本金 × 年化 ÷ 365 × 已存天数」计算，与 Excel 账本口径一致。",
                        "Local offline storage; data stays on this device only.\nInterest = principal × annual rate ÷ 365 × days held, same as the Excel ledger.",
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                val mailContext = LocalContext.current
                Text(
                    I18n.s("问题反馈请发邮件到 23831644@qq.com", "Feedback email: 23831644@qq.com"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        try {
                            mailContext.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:23831644@qq.com"))
                            )
                        } catch (e: Exception) {
                            showStatus(I18n.s("未找到邮件应用，邮箱：23831644@qq.com", "No email app found; contact 23831644@qq.com"))
                        }
                    },
                )
            }
        }

        if (importing) {
            Text(I18n.s("正在导入…", "Importing…"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        message?.let {
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(I18n.s("确认清空所有数据？", "Clear all data?")) },
            text = { Text(I18n.s("此操作不可恢复，请确保已导出备份。", "This cannot be undone. Make sure you have exported a backup.")) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    scope.launch {
                        repository.clearAllData()
                        showStatus(I18n.s("已清空", "Cleared"))
                    }
                }) { Text(I18n.s("清空", "Clear"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(I18n.s("取消", "Cancel")) }
            },
        )
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(I18n.s("分享到微信等", "Share to WeChat etc.")) },
            text = { Text(I18n.s("选择要分享的文件格式，分享到微信、QQ、邮件等", "Pick a format to share via WeChat, QQ, email, etc.")) },
            confirmButton = {
                TextButton(onClick = {
                    showShareDialog = false
                    scope.launch {
                        val uri = shareFile(context, repository, "xlsx")
                        if (uri == null) showStatus(I18n.s("分享失败", "Share failed")) else shareViaIntent(context, uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    }
                }) { Text("Excel (.xlsx)") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showShareDialog = false
                        scope.launch {
                            val uri = shareFile(context, repository, "json")
                            if (uri == null) showStatus(I18n.s("分享失败", "Share failed")) else shareViaIntent(context, uri, "application/json")
                        }
                    }) { Text(I18n.s("JSON 备份", "JSON backup")) }
                    TextButton(onClick = { showShareDialog = false }) { Text(I18n.s("取消", "Cancel")) }
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
                    }, enabled = editInput.isNotBlank()) { Text(I18n.s("保存", "Save")) }
                    TextButton(onClick = { editing = null }) { Text(I18n.s("取消", "Cancel")) }
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
                            contentDescription = I18n.s("编辑 $item", "Edit $item"),
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
                            contentDescription = I18n.s("删除 $item", "Delete $item"),
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
            ) { Text(I18n.s("添加", "Add")) }
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
    context.startActivity(Intent.createChooser(intent, I18n.s("分享到...", "Share to…")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}


@Composable
private fun SecuritySection() {
    val context = LocalContext.current
    val app = context.applicationContext as com.pxy.depositbook.DepositApp
    val security = app.security
    var enabled by remember { mutableStateOf(security.isEnabled()) }
    var lockType by remember { mutableStateOf(security.getLockType()) }
    var pinConfigured by remember { mutableStateOf(security.hasPin()) }
    var patternConfigured by remember { mutableStateOf(security.hasPattern()) }
    var showSet by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(0) }
    var tempPin by remember { mutableStateOf("") }
    var tempPattern by remember { mutableStateOf("") }
    // 当前设置弹窗针对的类型与设置完成后的动作（pendingEnable / pendingSwitch）
    var setType by remember { mutableStateOf("pin") }
    var pendingEnable by remember { mutableStateOf(false) }
    var pendingSwitch by remember { mutableStateOf(false) }
    var setupHint by remember { mutableStateOf("") }

    fun hint(): String = when {
        pinConfigured && patternConfigured -> I18n.s("两种密码均已配置，可在解锁界面切换", "Both methods configured; switch on the lock screen")
        pinConfigured -> I18n.s("数字密码已配置；切换手势前会先要求设置手势", "PIN configured; you'll set a pattern before switching")
        else -> I18n.s("尚未配置密码", "No PIN or pattern configured")
    }

    SettingsGroup(I18n.s("安全", "Security")) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(I18n.s("应用锁", "App lock"))
                    Text(I18n.s("从后台切回或首次进入需要验证", "Verification required when returning from background or first launch"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked && !pinConfigured) {
                            // 规则：数字密码必须先配置
                            setType = "pin"
                            pendingEnable = true
                            pendingSwitch = false
                            showSet = true
                            step = 0
                            setupHint = I18n.s("开启应用锁前，请先设置数字密码", "Set a PIN before enabling app lock")
                        } else {
                            enabled = checked
                            security.setEnabled(checked)
                        }
                    },
                )
            }
            if (enabled) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(I18n.s("解锁方式", "Unlock method"), modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val target = if (lockType == "pin") "pattern" else "pin"
                        val configured = if (target == "pin") pinConfigured else patternConfigured
                        if (configured) {
                            lockType = target
                            security.setLockType(target)
                        } else {
                            // 规则：切换前必须先配置目标方式
                            setType = target
                            pendingEnable = false
                            pendingSwitch = true
                            showSet = true
                            step = 0
                            setupHint = if (target == "pattern") I18n.s("切换到手势密码前，请先设置手势密码", "Set a pattern before switching to pattern unlock") else I18n.s("切换到数字密码前，请先设置数字密码", "Set a PIN before switching to PIN unlock")
                        }
                    }) {
                        Text(if (lockType == "pin") I18n.s("数字密码", "PIN") else I18n.s("手势密码", "Pattern"))
                    }
                }
                Text(
                    hint(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = {
                    setType = lockType
                    pendingEnable = false
                    pendingSwitch = false
                    showSet = true
                    step = 0
                    tempPin = ""
                    tempPattern = ""
                }) {
                    Text(I18n.s("修改${if (lockType == "pin") "数字密码" else "手势密码"}", "Change ${if (lockType == "pin") "PIN" else "pattern"}"))
                }
            }
        }
    }

    if (showSet) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {
            showSet = false
            pendingEnable = false
            pendingSwitch = false
        }) {
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(8.dp)) {
                    if (setupHint.isNotBlank()) {
                        Text(
                            setupHint,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    if (setType == "pin") {
                        if (step == 0) {
                            PinLock(title = I18n.s("设置数字密码（4位）", "Set PIN (4 digits)"), onSuccess = { pin -> tempPin = pin; step = 1 })
                        } else {
                            PinLock(
                                title = I18n.s("确认数字密码", "Confirm PIN"),
                                onVerify = { it == tempPin },
                                onSuccess = {
                                    security.setPin(tempPin)
                                    pinConfigured = true
                                    if (pendingEnable || pendingSwitch) {
                                        security.setEnabled(true)
                                        enabled = true
                                    }
                                    if (pendingSwitch || pendingEnable || !security.isReady()) {
                                        security.setLockType("pin")
                                        lockType = "pin"
                                    }
                                    showSet = false
                                    pendingEnable = false
                                    pendingSwitch = false
                                    setupHint = ""
                                },
                                onError = { step = 0 },
                            )
                        }
                    } else {
                        if (step == 0) {
                            PatternLock(title = I18n.s("设置手势密码（至少4点）", "Set pattern (at least 4 dots)"), onSuccess = { pattern -> tempPattern = pattern; step = 1 })
                        } else {
                            PatternLock(
                                title = I18n.s("确认手势密码", "Confirm pattern"),
                                onVerify = { it == tempPattern },
                                onSuccess = {
                                    security.setPattern(tempPattern)
                                    patternConfigured = true
                                    if (pendingEnable || pendingSwitch) {
                                        security.setEnabled(true)
                                        enabled = true
                                        security.setLockType("pattern")
                                        lockType = "pattern"
                                    }
                                    showSet = false
                                    pendingEnable = false
                                    pendingSwitch = false
                                    setupHint = ""
                                },
                                onError = { step = 0 },
                            )
                        }
                    }
                }
            }
        }
    }
}
