package com.pxy.depositbook.ui.screen.help

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pxy.depositbook.data.repo.DepositRepository
import kotlinx.coroutines.launch

/**
 * 使用教学：按板块说明 App 的工作流程。
 * - 每节配要点列表；assets/help/ 下有对应截图时自动展示
 * - 「填充示例数据」一键插入演示存款（备注带标记，可随时清除）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    repository: DepositRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var demoCount by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        demoCount = if (repository.hasDemoData()) 1 else 0
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(I18n.s("使用教学", "Tutorial"), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, I18n.s("返回", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "item-0") {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        I18n.s(
                            "这一页按板块讲解存款账本的完整工作流程；底部可以一键填充（或清除）示例数据，边看边学。",
                            "Each section walks you through how this app works. Use the demo data below to explore with real examples.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    // 示例数据按钮
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val n = repository.addDemoData()
                                demoCount = 1
                                android.widget.Toast.makeText(
                                    context,
                                    I18n.s("已插入 $n 笔示例存款（当前账户）", "$n demo deposits inserted (current account)"),
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                            }
                        }) {
                            Text(I18n.s("填充示例数据", "Insert demo data"))
                        }
                        if (demoCount > 0) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val n = repository.removeDemoData()
                                    demoCount = 0
                                    android.widget.Toast.makeText(
                                        context,
                                        I18n.s("已清除 $n 笔示例存款", "$n demo deposits removed"),
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                }
                            }) {
                                Text(I18n.s("清除示例数据", "Remove demo data"))
                            }
                        }
                    }
                }
            }

            item(key = "item-1") { TutorialShot("workflow") }

            item(key = "item-2") { HelpChapter(Icons.Outlined.Route, "工作流程一览", "How it works") {
                StepLine("1", I18n.s("新增存款：选择账户 → 填基本信息", "Add deposit: pick account, fill in details"))
                StepLine("2", I18n.s("利息自动计算：本金 × 年化 ÷ 365 × 已存天数", "Interest auto-calculated: principal × rate ÷ 365 × days"))
                StepLine("3", I18n.s("统计页与日历页随时查看分布与到期", "Check Stats and Calendar for mix and maturity"))
                StepLine("4", I18n.s("临期自动提醒：到期前 N 天通知你转存", "Reminder notifies N days before maturity"))
                StepLine("5", I18n.s("导出备份：JSON / Excel 随时带走", "Export backups (JSON / Excel) anytime"))
                StepLine("6", I18n.s("结清与删除自动留痕，操作日志可回溯", "Every settle/delete is logged"))
            } }

            item(key = "item-3") { TutorialShot("home") }

            item(key = "item-4") { HelpChapter(Icons.Outlined.Home, "总览页", "Home") {
                Bullet(I18n.s("左上角显示当前账户头像，点击可切换账户；各页面数据随之切换", "Top-left avatar switches accounts; all pages follow"))
                Bullet(I18n.s("右上角眼睛按钮可隐藏金额（隐私模式）", "Eye icon hides amounts (privacy mode)"))
                Bullet(I18n.s("「N天内到期」卡片可点击：查看已到期和临期存款，按到期日排序", "Tap the \"due in N days\" card: matured and upcoming deposits, sorted by date"))
                Bullet(I18n.s("每笔存款卡片上显示银行图标，临期变黄色、7天内变红色", "Each card shows the bank logo; near-maturity turns amber, within 7 days red"))
            } }

            item(key = "item-5") { TutorialShot("edit") }

            item(key = "item-6") { HelpChapter(Icons.Outlined.AccountBalance, "新增存款（六步引导）", "Add deposit (6 steps)") {
                StepLine("1", I18n.s("选择银行：内置银行带 logo，也支持自定义", "Pick a bank (built-in with logo, or custom)"))
                StepLine("2", I18n.s("选择存款期限（产品）：预置整存整取、自定义产品", "Pick the product/term"))
                StepLine("3", I18n.s("输入本金（必填）", "Principal (required)"))
                StepLine("4", I18n.s("输入年化利率（如 2.35 表示 2.35%）", "Annual rate (e.g. 2.35 = 2.35%)"))
                StepLine("5", I18n.s("存入日期：默认今天，可跳过", "Deposit date: today by default, skippable"))
                StepLine("6", I18n.s("存期：部分产品可修改，默认 1 年，可跳过", "Term: 1 year default when editable"))
                Bullet(I18n.s("右下角按钮提示下一步；漏填时保存会明确指出第几步", "The floating button shows the next step; save lists missing steps"))
            } }

            item(key = "item-7") { TutorialShot("calendar") }

            item(key = "item-8") { HelpChapter(Icons.Outlined.CalendarMonth, "日历页", "Calendar") {
                Bullet(I18n.s("月视图 / 年视图切换，绿色 = 存入日，橙色 = 到期日", "Month/year views; green = deposited, amber = maturing"))
                Bullet(I18n.s("点任意日期查看当天存入与到期的每笔存款", "Tap any date to list deposits that start/mature that day"))
            } }

            item(key = "item-9") { TutorialShot("stats") }

            item(key = "item-10") { HelpChapter(Icons.Outlined.BarChart, "统计与资产历史", "Stats & asset history") {
                Bullet(I18n.s("本金分布、今日利息、到期预估一目了然", "Asset mix, today's interest, maturity estimates at a glance"))
                Bullet(I18n.s("每天自动记录资产快照（历史曲线），删除/结清不影响已有记录", "Daily asset snapshots build the history curve; edits don't change history"))
            } }

            item(key = "item-11") { TutorialShot("reminder") }

            item(key = "item-12") { HelpChapter(Icons.Outlined.Notifications, "到期提醒", "Maturity reminders") {
                Bullet(I18n.s("设置 → 提醒：可调整提前提醒天数（默认 30 天，每天自动检查）", "Settings → Reminders: adjust lead days (default 30), checked daily"))
                Bullet(I18n.s("临期时系统通知「有 N 笔存款将在 N 天内到期」，点通知直达 App", "System notification lists how many deposits are due; tap to open the app"))
            } }

            item(key = "item-13") { TutorialShot("export") }

            item(key = "item-14") { HelpChapter(Icons.Outlined.FileDownload, "导入导出与备份", "Import & backup") {
                Bullet(I18n.s("导出 JSON：完整备份（含多账户），推荐换机前导出", "Export JSON: full backup incl. accounts — recommended before switching phones"))
                Bullet(I18n.s("导出 Excel：每个账户一个工作表，可用 Excel 打开核对", "Export Excel: one sheet per account, openable in Excel"))
                Bullet(I18n.s("支持微信 / 邮件分享导出文件", "Share exports via WeChat / email"))
                Bullet(I18n.s("导入：旧备份进入当前账户；本 App 导出的多账户备份自动按账户归位", "Import: old backups go to the current account; our multi-account backups map automatically"))
            } }

            item(key = "item-15") { TutorialShot("accounts") }

            item(key = "item-16") { HelpChapter(Icons.Outlined.Group, "多账户", "Multiple accounts") {
                Bullet(I18n.s("设置 → 账户：新建、重命名、删除账户（至少保留一个）", "Settings → Accounts: create, rename, delete (keep at least one)"))
                Bullet(I18n.s("点击头像可上传自定义头像；无头像时显示渐变首字默认头像", "Tap avatar to upload a photo; otherwise a gradient initial avatar"))
                Bullet(I18n.s("各账户存款、日志、资产历史完全隔离，互不干扰", "Deposits, logs and history are fully isolated per account"))
            } }

            item(key = "item-17") { HelpChapter(Icons.Outlined.Lock, "应用锁与隐私", "App lock & privacy") {
                Bullet(I18n.s("设置 → 安全：数字密码或手势密码；进入 App 和切回都要验证", "Settings → Security: PIN or pattern; verified on entry and resume"))
                Bullet(I18n.s("设置 → 隐私：总览默认隐藏金额", "Settings → Privacy: hide amounts on Home by default"))
            } }

            item(key = "item-18") {
                Text(
                    I18n.s(
                        "有疑问？设置 → 关于 里可以邮件反馈（23831644@qq.com）。",
                        "Questions? Contact via Settings → About (23831644@qq.com).",
                    ),
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** 章节卡片：图标 + 标题 + 要点 */
@Composable
private fun HelpChapter(icon: ImageVector, titleZh: String, titleEn: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(I18n.s(titleZh, titleEn), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** 要点：小圆点 + 文本 */
@Composable
private fun Bullet(text: String) {
    Row {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .align(Alignment.CenterVertically),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 步骤行：圆圈编号 + 文本 */
@Composable
private fun StepLine(num: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(num, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 截图展示：assets/help/<name>.jpg 存在时显示（模拟器截屏生成），不存在则跳过 */
@Composable
private fun TutorialShot(name: String) {
    val context = LocalContext.current
    val exists = remember(name) {
        runCatching { context.assets.list("help")?.contains("$name.jpg") == true }.getOrDefault(false)
    }
    if (!exists) return
    coil.compose.AsyncImage(
        model = coil.request.ImageRequest.Builder(context)
            .data("file:///android_asset/help/$name.jpg")
            .build(),
        contentDescription = I18n.s("页面截图", "Screenshot"),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
    )
}
