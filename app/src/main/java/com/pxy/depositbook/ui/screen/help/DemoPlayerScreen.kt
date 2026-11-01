package com.pxy.depositbook.ui.screen.help
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas

import com.pxy.depositbook.util.I18n

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * 演示播放器：PPT 式分步图文演示。
 * - 每步一张真实界面截图 + 红框标注要关注的位置
 * - 自动演示：自动切换并朗读中文讲解；手动：点「下一步」推进
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoPlayerScreen(
    demoKey: String,
    onExit: () -> Unit,
) {
    val demo = DEMOS[demoKey] ?: DEMOS.getValue("add")
    val context = LocalContext.current

    var step by remember { mutableIntStateOf(0) }
    var auto by remember { mutableStateOf(true) }
    var ttsReady by remember { mutableStateOf(false) }
    var voiceOn by remember { mutableStateOf(true) }

    // 语音引擎（TTS）：不可用时仅显示文字
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        runCatching {
            engine = TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS }
            tts.value = engine
        }
        onDispose {
            runCatching { engine?.stop(); engine?.shutdown() }
            ttsReady = false
        }
    }

    val currentStep = demo.steps[step]
    // 每步：朗读 + 自动模式下延时切换
    LaunchedEffect(step, auto, ttsReady, voiceOn) {
        if (voiceOn && ttsReady) {
            runCatching {
                tts.value?.stop()
                tts.value?.language = if (I18n.isZh) Locale.SIMPLIFIED_CHINESE else Locale.US
                tts.value?.speak(
                    if (I18n.isZh) currentStep.zh else currentStep.en,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "demo-$step",
                )
            }
        }
        if (auto) {
            val text = if (I18n.isZh) currentStep.zh else currentStep.en
            val dur = if (ttsReady) (text.length * 220L + 1500L).coerceIn(3000L, 25000L) else 3500L
            delay(dur)
            if (step < demo.steps.lastIndex) {
                step += 1
            } else {
                delay(1500L)
                step = 0 // 自动循环播放
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(I18n.s(demo.titleZh, demo.titleEn), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, I18n.s("返回", "Back"))
                    }
                },
                actions = {
                    IconButton(onClick = { auto = !auto }) {
                        Icon(
                            if (auto) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (auto) I18n.s("暂停自动演示", "Pause auto demo") else I18n.s("继续演示", "Resume demo"),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // 截图 + 红框标注（占据剩余空间，等比缩放完整显示）
            DemoShot(stepImage = currentStep.image, rect = currentStep.rect, modifier = Modifier.weight(1f).fillMaxWidth())

            Spacer(Modifier.height(8.dp))

            // 进度点
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                demo.steps.indices.forEach { i ->
                    Box(
                        Modifier
                            .height(8.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (i == step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // 讲解卡
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                Text(
                    I18n.s(currentStep.titleZh, currentStep.titleEn),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (I18n.isZh) currentStep.zh else currentStep.en,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (step > 0) step -= 1 },
                        enabled = step > 0,
                    ) { Text(I18n.s("上一步", "Back")) }
                    if (step < demo.steps.lastIndex) {
                        Button(onClick = {
                            auto = false // 手动切换即暂停自动
                            step += 1
                        }) { Text(I18n.s("下一步", "Next")) }
                    } else {
                        Button(onClick = { step = 0 }) { Text(I18n.s("重新演示", "Replay")) }
                    }
                    Text(if (voiceOn) "🔊" else "🔇", fontSize = 16.sp)
                    Switch(checked = voiceOn, onCheckedChange = {
                        voiceOn = it
                        if (!it) runCatching { tts.value?.stop() }
                    })
                }
                if (!ttsReady) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        I18n.s(
                            "提示：设备未内置中文语音引擎时将不发声（文字讲解不影响使用）",
                            "Note: without a Chinese TTS engine, narration is text-only",
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * 截图 + 红框标注（rect 为相对原图的 0..1 归一化坐标 l, t, r, b）。
 * 图片等比缩放（ContentScale.Fit），红框按实际绘制区域对齐。
 */
@Composable
private fun DemoShot(stepImage: String, rect: FloatArray, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exists = remember(stepImage) {
        runCatching { context.assets.list("help/demo")?.contains("$stepImage.jpg") == true }.getOrDefault(false)
    }
    if (!exists) return
    // 截图统一为 720x1600（宽:高 = 0.45）
    BoxWithConstraints(modifier) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        // 与 ContentScale.Fit 完全一致：取能放进框内的最大等比尺寸，居中
        val ar = 0.45f // 素材 720x1600
        val fitW = minOf(cw, ch * ar)
        val fitH = fitW / ar
        val offX = (cw - fitW) / 2f
        val offY = (ch - fitH) / 2f
        val drawnW = fitW
        val drawnH = fitH
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data("file:///android_asset/help/demo/$stepImage.jpg")
                    .build(),
                contentDescription = I18n.s("演示步骤截图", "Demo step screenshot"),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (rect.size >= 4) {
                val l = offX + rect[0] * drawnW
                val t = offY + rect[1] * drawnH
                val r = offX + rect[2] * drawnW
                val b = offY + rect[3] * drawnH
                // 用 Spacer+drawBehind：一次性绘制遮罩和红框（不依赖多层 composable 的首帧同步）
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(Color(0x66000000), Offset(0f, 0f), Size(size.width, t))
                            drawRect(Color(0x66000000), Offset(0f, b), Size(size.width, size.height - b))
                            drawRect(Color(0x66000000), Offset(0f, t), Size(l, b - t))
                            drawRect(Color(0x66000000), Offset(r, t), Size(size.width - r, b - t))
                            drawRoundRect(
                                color = Color(0xFFE53935),
                                topLeft = Offset(l, t),
                                size = Size(r - l, b - t),
                                cornerRadius = CornerRadius(20f, 20f),
                                style = Stroke(width = 8f),
                            )
                        },
                )
            }
        }
    }
}

/** 演示动作定义 */
data class DemoStep(
    val image: String,
    val rect: FloatArray, // l, t, r, b — 归一化 0..1
    val titleZh: String,
    val titleEn: String,
    val zh: String,
    val en: String,
)

data class DemoData(
    val titleZh: String,
    val titleEn: String,
    val steps: List<DemoStep>,
)

/** 演示库（截图在 assets/help/demo/ 下，缺失的步骤自动跳过画面） */
val DEMOS: Map<String, DemoData> = mapOf(
    "add" to DemoData(
        titleZh = "新增存款演示",
        titleEn = "Add deposit demo",
        steps = listOf(
            DemoStep(
                "add_1", floatArrayOf(0.659f, 0.780f, 0.963f, 0.843f),
                "第 1 步：点「新增存款」", "Step 1: tap Add deposit",
                "在总览页右下角点击「＋」悬浮按钮，进入新增存款引导页。",
                "On Home, tap the ＋ button (bottom right) to open the add-deposit wizard.",
            ),
            DemoStep(
                "add_2", floatArrayOf(0.104f, 0.113f, 0.961f, 0.583f),
                "第 2 步：选择银行", "Step 2: pick a bank",
                "点击「银行名称」框，从弹出的列表里选择银行，也可以手动输入其他银行。",
                "Tap the bank field and pick one from the list, or type any custom bank name.",
            ),
            DemoStep(
                "add_3", floatArrayOf(0.104f, 0.104f, 0.961f, 0.233f),
                "第 3 步：确认选择", "Step 3: confirm your pick",
                "选中的银行出现在框内，左边圆点变 ✓；接着点「存款期限」选择产品，如 1 年期整存整取。",
                "The chosen bank shows with a ✓; then tap the term field and pick a product, e.g. 1-year fixed.",
            ),
            DemoStep(
                "add_4", floatArrayOf(0.104f, 0.275f, 0.961f, 0.554f),
                "第 4 步：选存款期限", "Step 4: pick the term",
                "在「存款期限」下拉里选择产品，例如「1年期整存整取」，也可以选 2 年、3 年、5 年或活期。",
                "Pick a product from the term list, e.g. 1-year fixed deposit; 2/3/5-year and demand options are available too.",
            ),
            DemoStep(
                "add_5", floatArrayOf(0.104f, 0.292f, 0.961f, 0.358f),
                "第 5 步：填本金", "Step 5: enter principal",
                "在「本金（元）」输入存款金额，例如 50000。这是计算利息的基础。",
                "Enter the deposit amount in the principal field, e.g. 50000.",
            ),
            DemoStep(
                "add_6", floatArrayOf(0.104f, 0.377f, 0.961f, 0.446f),
                "第 6 步：填年化利率", "Step 6: enter annual rate",
                "在「年化利率（%）」填 2.35，表示年化 2.35%。日利息 = 本金 × 利率 ÷ 365。",
                "Enter 2.35 for 2.35% p.a. Daily interest = principal × rate ÷ 365.",
            ),
            DemoStep(
                "add_7", floatArrayOf(0.759f, 0.746f, 0.935f, 0.804f),
                "第 7 步：日期弹窗点「确定」", "Step 7: tap OK in the dialog",
                "点存入日期会弹出日历；选好后在弹窗右下角点「确定」收起。",
                "Tapping the date opens a calendar; pick a day, then tap OK at the bottom right.",
            ),
            DemoStep(
                "add_8", floatArrayOf(0.736f, 0.823f, 0.977f, 0.927f),
                "最后：保存", "Finally: save",
                "看到「✓ 可以保存」后，点右下角圆勾保存。这笔存款立即出现在总览、统计与日历里。",
                "Once \"✓ ready to save\" appears, tap the round ✓. It shows up in Home, Stats and Calendar right away.",
            ),
        ),
    ),
    "delete" to DemoData(
        titleZh = "删除存款演示",
        titleEn = "Delete a deposit",
        steps = listOf(
            DemoStep(
                "home_1", floatArrayOf(0.039f, 0.517f, 0.961f, 0.833f),
                "第 1 步：在总览找到这笔", "Step 1: find the deposit on Home",
                "在总览页「全部存款」里找到要删除的那一笔，点击卡片打开详情。",
                "Find the deposit in All deposits on Home and tap it to open details.",
            ),
            DemoStep(
                "del_1", floatArrayOf(0.912f, 0.079f, 0.968f, 0.104f),
                "第 2 步：点右上角删除", "Step 2: tap delete at top right",
                "详情页右上角有三个按钮：编辑（铅笔）和删除（垃圾箱）。点垃圾桶。",
                "The detail page has edit (pencil) and delete (bin) icons at the top right. Tap the bin.",
            ),
            DemoStep(
                "del_2", floatArrayOf(0.648f, 0.533f, 0.782f, 0.567f),
                "第 3 步：确认删除", "Step 3: confirm",
                "弹窗会提醒「删除后不可恢复」，确认无误后点红色「删除」。点「取消」可以放弃。",
                "The dialog warns the action can't be undone. Tap the red Delete to confirm, or Cancel to abort.",
            ),
            DemoStep(
                "del_3", floatArrayOf(0.039f, 0.517f, 0.961f, 0.833f),
                "第 4 步：列表已更新", "Step 4: list updated",
                "删除后自动回到列表，这笔存款消失，总额同步减少。",
                "You return to the list automatically; the deposit is gone and totals update.",
            ),
            DemoStep(
                "logs_1", floatArrayOf(0.039f, 0.121f, 0.961f, 0.30f),
                "第 5 步：日志留痕", "Step 5: logged",
                "删除不是「无声」的：日志页最上面会记录一条红色的「删除 …」，时间与金额都在。",
                "Deletes are never silent: the log page shows a red Delete entry with time and amount.",
            ),
            DemoStep(
                "logs_1", floatArrayOf(0.039f, 0.517f, 0.961f, 0.833f),
                "小提示：删错了怎么办", "Tip: deleted by mistake?",
                "删除不可撤销。重要数据建议先在「设置 → 导出 JSON 备份」留一份，或直接改数据而不是删。",
                "Deletion can't be undone. Export a JSON backup first (Settings → Export), or edit instead of deleting.",
            ),
        ),
    ),
    "home" to DemoData(
        titleZh = "总览页怎么看",
        titleEn = "Understanding Home",
        steps = listOf(
            DemoStep(
                "home_1", floatArrayOf(0.039f, 0.121f, 0.961f, 0.300f),
                "第 1 步：本金总额", "Step 1: total principal",
                "最上方深色卡片是当前账户的在存本金合计；下面一行「截止今日 本金+利息」是算上利息后的实际价值。",
                "The dark card shows total principal in this account; the line below is principal plus interest accrued so far.",
            ),
            DemoStep(
                "home_1", floatArrayOf(0.039f, 0.310f, 0.961f, 0.504f),
                "第 2 步：四个指标卡", "Step 2: the four cards",
                "「今日新增利息」= 所有存款今天的利息总和；「累计已获利息」= 从存入到今天累计拿到多少。下面两张是「30 天内到期」笔数和「到期预估总额」。",
                "Today's interest is what all deposits earn today; accrued interest is the total so far. Below: deposits due within 30 days, and projected value at maturity.",
            ),
            DemoStep(
                "home_1", floatArrayOf(0.870f, 0.083f, 0.935f, 0.121f),
                "第 3 步：隐私模式", "Step 3: privacy mode",
                "点右上角眼睛可以把所有金额变成 ***，适合在公共场合看。也可在设置里设为默认隐藏。",
                "Tap the eye icon to mask all amounts with ***; you can also default to hidden in Settings.",
            ),
            DemoStep(
                "home_2", floatArrayOf(0.039f, 0.310f, 0.961f, 0.504f),
                "第 4 步：金额已隐藏", "Step 4: amounts hidden",
                "开启后金额显示为 ***，再点一次眼睛即恢复。",
                "Amounts show as *** once enabled; tap the eye again to restore.",
            ),
            DemoStep(
                "home_3", floatArrayOf(0.039f, 0.179f, 0.961f, 0.30f),
                "第 5 步：30 天内到期明细", "Step 5: due within 30 days",
                "点「30 天内到期」卡片，可看到每笔临期存款和剩余天数，按到期日排序，方便安排转存。",
                "Tap the due-soon card to list each deposit with days left, sorted by maturity date.",
            ),
            DemoStep(
                "home_4", floatArrayOf(0.039f, 0.042f, 0.961f, 0.833f),
                "第 6 步：滚动查看每笔状态", "Step 6: scroll through every deposit",
                "往下滑就是全部存款：每张卡显示银行、期限、本金、年化、日利息、已获利息和本金+利息；右上角「N 天后到期」会随临期变黄、7 天内变红。",
                "Scroll for all deposits: bank, term, principal, rate, daily/accrued interest and total. The due badge turns amber when near, red within 7 days.",
            ),
        ),
    ),
    "stats" to DemoData(
        titleZh = "统计与日历",
        titleEn = "Stats & Calendar",
        steps = listOf(
            DemoStep(
                "stats_1", floatArrayOf(0.039f, 0.113f, 0.961f, 0.208f),
                "第 1 步：三个汇总", "Step 1: the three totals",
                "统计页顶部汇总本金总额、累计利息、本息合计；标题行还会显示当前有几笔在存。",
                "The top summarizes total principal, accrued interest and combined value, plus how many deposits are active.",
            ),
            DemoStep(
                "stats_1", floatArrayOf(0.039f, 0.217f, 0.961f, 0.425f),
                "第 2 步：资产分布", "Step 2: asset mix",
                "环形图按银行显示占比，一眼看出钱主要压在哪家银行，集中度太高时可以考虑分散。",
                "The donut chart shows each bank's share, so you can spot concentration risk.",
            ),
            DemoStep(
                "stats_1", floatArrayOf(0.074f, 0.512f, 0.389f, 0.554f),
                "第 3 步：按月看趋势", "Step 3: monthly trend",
                "「本金趋势」默认按月，能看到每个月末的本金变化，适合看近期增减。",
                "The trend chart defaults to monthly, showing month-end principal changes.",
            ),
            DemoStep(
                "stats_2", floatArrayOf(0.074f, 0.512f, 0.389f, 0.554f),
                "第 4 步：按年看更方便", "Step 4: yearly is clearer",
                "点「按年」把时间轴拉长到年，适合看长期积累；配合每天自动记录的资产快照形成曲线。",
                "Switch to yearly for the long view; daily asset snapshots build the curve automatically.",
            ),
            DemoStep(
                "cal_1", floatArrayOf(0.685f, 0.192f, 0.963f, 0.229f),
                "第 5 步：日历按月", "Step 5: calendar by month",
                "统计页右上角「日历」进入。按月视图里青色 = 存入日、橙色 = 到期日，小图标标在对应日期的右上角。",
                "Open Calendar from Stats. In month view cyan marks deposited days and amber marks maturities.",
            ),
            DemoStep(
                "cal_2", floatArrayOf(0.039f, 0.179f, 0.961f, 0.50f),
                "第 6 步：点日期看明细", "Step 6: tap a date",
                "点任意一天，下方列出当天存入和到期的每一笔，方便回忆那天的操作。",
                "Tap any day to list every deposit that started or matures that day.",
            ),
            DemoStep(
                "cal_3", floatArrayOf(0.056f, 0.138f, 0.954f, 0.792f),
                "第 7 步：按年更方便", "Step 7: yearly overview",
                "切到「按年」，每年显示「存入 N 笔 / 到期 N 笔」，今年还会标上今天；一眼看多年节奏。",
                "In year view each year shows N deposited / N maturing, with today highlighted this year.",
            ),
        ),
    ),
    "logs" to DemoData(
        titleZh = "操作日志",
        titleEn = "Activity log",
        steps = listOf(
            DemoStep(
                "logs_1", floatArrayOf(0.039f, 0.042f, 0.961f, 0.100f),
                "第 1 步：日志总览", "Step 1: the log page",
                "底部导航「日志」：标题下显示共多少条记录，所有新增、修改、删除、结清都会留痕。",
                "The Log tab lists how many records exist; every add, edit, delete and settle is recorded.",
            ),
            DemoStep(
                "logs_1", floatArrayOf(0.039f, 0.121f, 0.961f, 0.30f),
                "第 2 步：看单条记录", "Step 2: read an entry",
                "每条包含：类型（红点删除 / 绿点新增）、时间、银行、产品、本金和利率，信息完整可核对。",
                "Each entry shows type (red = delete, green = add), time, bank, product, principal and rate.",
            ),
            DemoStep(
                "logs_1", floatArrayOf(0.861f, 0.058f, 0.935f, 0.092f),
                "第 3 步：清理日志", "Step 3: clear logs",
                "右上角图标可以清理日志（不影响存款数据）。建议定期导出备份后再清理。",
                "The top-right icon clears the log without touching deposit data. Export a backup first.",
            ),
            DemoStep(
                "logs_2", floatArrayOf(0.039f, 0.121f, 0.961f, 0.833f),
                "小提示：日志与账户隔离", "Tip: logs are per account",
                "每个账户有自己的日志，切换账户后日志随之切换，互不混淆。",
                "Each account keeps its own log; switching accounts switches the log too.",
            ),
        ),
    ),
    "accounts" to DemoData(
        titleZh = "多账户切换与改名",
        titleEn = "Switch & rename accounts",
        steps = listOf(
            DemoStep(
                "acc_1", floatArrayOf(0.035f, 0.078f, 0.135f, 0.122f),
                "第 1 步：点总览左上角头像", "Step 1: tap the avatar on Home",
                "每个账户有自己的头像和名字。点总览页左上角头像，会弹出「切换账户」。",
                "Each account has its own avatar and name. Tap the avatar at the top left of Home to open Switch account.",
            ),
            DemoStep(
                "acc_1", floatArrayOf(0.11f, 0.365f, 0.90f, 0.565f),
                "第 2 步：选另一个账户", "Step 2: pick another account",
                "列表里列出全部账户，打勾的是当前账户。点「默认账户」即可切换；下面还有「管理账户 / 新建账户」。",
                "All accounts are listed; the check mark is current. Tap Default to switch; Manage / New account are below.",
            ),
            DemoStep(
                "acc_2", floatArrayOf(0.035f, 0.078f, 0.135f, 0.122f),
                "第 3 步：数据整体切换", "Step 3: data switches with you",
                "切换后头像变成「默」，总览金额和存款列表都换成该账户的数据——各账户完全隔离，互不影响。",
                "After switching the avatar becomes 默 and totals/deposits change to that account. Accounts are fully isolated.",
            ),
            DemoStep(
                "acc_3", floatArrayOf(0.04f, 0.255f, 0.96f, 0.47f),
                "第 4 步：在设置里管理账户", "Step 4: manage accounts in Settings",
                "总览的「管理账户」会跳到设置。当前账户标「当前」，另一账户显示「切换」；每行右侧是「重命名」和「删除」。",
                "Manage opens Settings. The active account shows Current, others show Switch; each row has rename and delete.",
            ),
            DemoStep(
                "acc_4", floatArrayOf(0.11f, 0.385f, 0.90f, 0.61f),
                "第 5 步：给默认账户改名", "Step 5: rename the default account",
                "点「默认账户」右侧铅笔，输入新名字（如「我的工资卡」）后点「确定」，名字会立即更新到总览头像旁。",
                "Tap the pencil next to Default, type a new name (e.g. Salary card) and tap OK; the name updates on Home.",
            ),
        ),
    ),
    "settings" to DemoData(
        titleZh = "设置与选项维护",
        titleEn = "Settings & options",
        steps = listOf(
            DemoStep(
                "set_1", floatArrayOf(0.039f, 0.138f, 0.961f, 0.388f),
                "第 1 步：账户", "Step 1: accounts",
                "设置最上方是「使用教学」；账户区可以新建、切换、重命名、删除账户（至少保留一个）。",
                "Tutorial sits at the very top; Accounts lets you add, switch, rename or delete (keep at least one).",
            ),
            DemoStep(
                "set_1", floatArrayOf(0.039f, 0.104f, 0.961f, 0.188f),
                "第 2 步：隐私", "Step 2: privacy",
                "「总览默认隐藏金额」打开后，每次进入总览金额都显示 ***，点眼睛可临时查看。",
                "Enable Hide amounts on Home to mask ** * on entry; tap the eye to peek.",
            ),
            DemoStep(
                "lock_2", floatArrayOf(0.039f, 0.225f, 0.961f, 0.79f),
                "第 3 步：应用锁", "Step 3: app lock",
                "打开「应用锁」先设置 4 位数字密码；也支持手势密码。开启后首次进入和从后台切回都要验证。",
                "Turning on App lock asks for a 4-digit PIN (or a gesture pattern). You'll verify on launch and on return.",
            ),
            DemoStep(
                "set_3", floatArrayOf(0.039f, 0.025f, 0.961f, 0.125f),
                "第 4 步：找不到的银行，自己加", "Step 4: add a missing bank",
                "「选项维护 → 银行名称」输入银行名点「添加」，之后新增存款时就能在下拉里选到它。",
                "Under Options → Bank names, type the bank and tap Add; it then appears in the bank dropdown.",
            ),
            DemoStep(
                "set_3", floatArrayOf(0.039f, 0.138f, 0.961f, 0.583f),
                "第 5 步：修改存款期限", "Step 5: edit terms",
                "「产品名称」即新增存款时的「存款期限」选项：点×删除，点「添加产品」自定义名称与存期，「恢复默认」可一键还原。",
                "Product names are the term options when adding a deposit: delete with ×, add your own, or Reset to defaults.",
            ),
            DemoStep(
                "set_3", floatArrayOf(0.039f, 0.60f, 0.961f, 0.70f),
                "第 6 步：语言与关于", "Step 6: language & about",
                "「显示语言」可随时切中英文，立即生效；「关于」里能看到版本号并邮件反馈。",
                "Switch language instantly under Display language; About shows the version and feedback email.",
            ),
            DemoStep(
                "set_2", floatArrayOf(0.039f, 0.079f, 0.961f, 0.53f),
                "第 7 步：备份与导入", "Step 7: backup & import",
                "「导出 JSON 备份」包含全部存款和日志，换机或删改前建议先导一份；也能导出 Excel 用电脑核对。",
                "Export a JSON backup (all deposits and logs) before switching phones or big edits; Excel export is available too.",
            ),
        ),
    ),
)

/** 教学页里的演示入口（顺序即展示顺序） */
data class DemoIndex(
    val key: String,
    val emoji: String,
    val titleZh: String,
    val titleEn: String,
    val descZh: String,
    val descEn: String,
    val stepCount: Int,
)

val DEMO_INDEX: List<DemoIndex> = listOf(
    DemoIndex("add", "➕", "新增存款", "Add a deposit",
        "从总览点＋开始，选择银行、期限、本金、利率，保存", "Start from the + button: bank, term, principal, rate, save", 8),
    DemoIndex("delete", "🗑", "删除存款", "Delete a deposit",
        "打开一笔存款，点右上角删除并确认", "Open a deposit, tap delete at top right and confirm", 6),
    DemoIndex("home", "🏠", "总览页怎么看", "Understand Home",
        "本金总额、今日利息、累计利息、30 天到期与每笔状态", "Total principal, today's and accrued interest, due soon, per-deposit status", 6),
    DemoIndex("stats", "📊", "统计与日历", "Stats & Calendar",
        "按月/按年查看存入与到期，看懂统计页各项指标", "Month/year views of deposits and maturities, plus stats metrics", 7),
    DemoIndex("logs", "🧾", "操作日志", "Activity log",
        "每一笔新增/修改/删除都可回溯", "Every add/edit/delete is traceable", 4),
    DemoIndex("accounts", "👥", "多账户与改名", "Accounts & rename",
        "总览点头像切换账户，设置里重命名/删除账户", "Switch accounts from the Home avatar; rename or delete in Settings", 5),
    DemoIndex("settings", "⚙️", "设置与选项维护", "Settings & options",
        "新增银行、修改存款期限、隐私、应用锁（数字/手势）", "Add banks, edit terms, privacy, app lock (PIN or pattern)", 7),
)

/** 教学页演示入口卡片 */
@Composable
fun DemoEntryCard(
    emoji: String,
    titleZh: String,
    titleEn: String,
    descZh: String,
    descEn: String,
    stepCount: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(I18n.s(titleZh, titleEn), style = MaterialTheme.typography.titleSmall)
            Text(
                I18n.s(descZh, descEn),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(I18n.s("$stepCount 步 ▶", "$stepCount steps ▶"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
    }
}
