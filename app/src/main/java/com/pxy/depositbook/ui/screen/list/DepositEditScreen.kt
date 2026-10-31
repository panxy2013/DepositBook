package com.pxy.depositbook.ui.screen.list

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pxy.depositbook.DepositApp
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.model.BankCatalog
import com.pxy.depositbook.data.prefs.ProductDef
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.ui.components.BankIcon
import com.pxy.depositbook.ui.components.DropdownField
import com.pxy.depositbook.ui.components.DropdownItem
import com.pxy.depositbook.ui.components.SearchableDropdownField
import com.pxy.depositbook.ui.components.WithChineseImeHint
import com.pxy.depositbook.util.DateUtils
import com.pxy.depositbook.util.FormatUtils
import com.pxy.depositbook.util.InterestCalculator
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositEditScreen(
    repository: DepositRepository,
    depositId: Long?,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val optionsStore = remember { (context.applicationContext as DepositApp).options }
    var userBanks by remember { mutableStateOf(optionsStore.banks()) }
    var productDefs by remember { mutableStateOf(optionsStore.productDefs()) }
    // 有存单的银行排前面：按最近一次存入时间倒序（用于银行下拉排序）
    var depositBankOrder by remember { mutableStateOf<List<String>>(emptyList()) }

    // 存期锁定：产品不允许修改存期时，存期单位/数值不可编辑
    var termLocked by remember { mutableStateOf(false) }

    // 选完银行 → 焦点自动到产品；选完产品 → 焦点自动到本金
    val productFocus = remember { FocusRequester() }
    val principalFocus = remember { FocusRequester() }
    val rateFocus = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // origin 将在下面赋值
    val editing = remember { mutableStateOf<Deposit?>(null) }
    val loaded = remember { mutableStateOf(depositId == null) }

    LaunchedEffect(depositId) {
        if (depositId != null) {
            editing.value = repository.get(depositId)
        }
        loaded.value = true
    }

    if (!loaded.value) return
    val origin = editing.value

    // 读取所有存款，计算银行排序：最近存入的银行排最前
    LaunchedEffect(Unit) {
        val all = repository.getAllOnce()
        val latest = mutableMapOf<String, Long>()
        all.forEach { d ->
            val b = d.accountName?.trim().orEmpty()
            if (b.isNotBlank()) {
                val cur = latest[b]
                if (cur == null || d.createdAt > cur) latest[b] = d.createdAt
            }
        }
        depositBankOrder = latest.entries.sortedByDescending { it.value }.map { it.key }
    }

    LaunchedEffect(origin) {
        val b = origin?.accountName?.trim().orEmpty()
        if (b.isNotBlank() && !BankCatalog.isBuiltIn(b) && userBanks.none { it == b }) {
            userBanks = userBanks + b
        }
        // 编辑旧存款：若原始存期与产品定义不一致，保持原值（不加锁不覆盖）
        val def0 = origin?.productName?.let { p -> productDefs.firstOrNull { it.name == p } }
        if (def0 != null) {
            termLocked = !def0.allowModify && def0.termDays != null
        }
    }

    // 产品下拉选项（含编辑旧存款时临时引入的产品）
    val productItems = remember(productDefs, origin) {
        val list = productDefs
        val extra = origin?.productName
        if (!extra.isNullOrBlank() && list.none { it.name == extra }) {
            list + ProductDef(extra, null, true)
        } else list
    }

    var accountName by remember(origin) { mutableStateOf(origin?.accountName ?: "") }
    // 新增时产品默认留空，由用户搜索选择；编辑时保留原产品
    var productName by remember(origin) { mutableStateOf(origin?.productName ?: "") }
    var principal by remember(origin) {
        // 编辑时预填完整金额（含小数），避免重存时丢失小数部分
        mutableStateOf(
            origin?.principal?.takeIf { it > 0 }?.let { v ->
                if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
            } ?: "",
        )
    }
    var rate by remember(origin) {
        mutableStateOf(origin?.annualRate?.takeIf { it > 0 }?.let { (it * 100).toString().replace(".0", "") } ?: "")
    }
    var note by remember(origin) { mutableStateOf(origin?.note ?: "") }

    var startDate by remember(origin) {
        mutableStateOf(origin?.startDate?.let { DateUtils.toDate(it) } ?: LocalDate.now())
    }
    // 存入日期第 5 步：默认今天（系统自动填），用户改过才算确认；编辑旧存款视为已确认
    var dateModified by remember(origin) { mutableStateOf(origin != null) }
    // 存期第 6 步：仅在选择了可修改存期的产品后显示；默认 1 年，用户改过才算确认
    var termModified by remember(origin) { mutableStateOf(origin != null) }

    // 存期单位内部用固定代码（Y/M/D），显示时按当前语言翻译
    val termUnits = listOf(
        UnitOption("Y", "年", "Year(s)"),
        UnitOption("M", "月", "Month(s)"),
        UnitOption("D", "天", "Day(s)"),
    )
    var termUnit by remember(origin) {
        val d = origin?.termDays ?: 365
        mutableStateOf(
            when {
                d % 365 == 0 && d != 0 -> "Y"
                d % 30 == 0 && d != 0 -> "M"
                else -> "D"
            },
        )
    }
    var termCountInput by remember(origin, startDate) {
        val d = (origin?.termDays ?: 365).toLong()
        val actualDays = if (origin != null) {
            val s = origin.startDate?.let { DateUtils.toDate(it) } ?: startDate
            val e = origin.endDate?.let { DateUtils.toDate(it) } ?: startDate.plusDays(d)
            if (e.isBefore(s)) d else ChronoUnit.DAYS.between(s, e)
        } else d
        mutableStateOf(
            when {
                actualDays <= 0L -> d.toString()
                termUnit == "Y" && actualDays % 365L == 0L -> (actualDays / 365L).toString()
                termUnit == "M" && actualDays % 30L == 0L -> (actualDays / 30L).toString()
                termUnit == "D" -> actualDays.toString()
                else -> actualDays.toString()
            },
        )
    }

    val termCountVal = termCountInput.trim().toLongOrNull() ?: 0L
    val computedEndDate = remember(startDate, termUnit, termCountVal) {
        if (termCountVal <= 0) {
            startDate
        } else {
            when (termUnit) {
                "Y" -> startDate.plusYears(termCountVal)
                "M" -> startDate.plusMonths(termCountVal)
                "D" -> startDate.plusDays(termCountVal)
                else -> startDate.plusMonths(termCountVal)
            }
        }
    }
    val endDate = if (computedEndDate.isBefore(startDate)) startDate.plusDays(1) else computedEndDate

    var showStartPicker by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val principalVal = (parseAmountInput(principal) ?: 0.0)
    val rateVal = ((parseRateInput(rate) ?: 0.0) / 100.0)
    val daysLong = ChronoUnit.DAYS.between(startDate, endDate)
    val days = if (daysLong <= 0) 1L else daysLong
    val daily = InterestCalculator.dailyInterest(principalVal, rateVal)

    // 保存步骤：1 银行名称 2 产品名称 3 本金 4 年化利率；第 5 步为存入日期（默认今天，非必填）
    val rateInputVal = parseRateInput(rate)
    val rateValid = rateInputVal != null && rateInputVal >= 0.0
    val saveSteps = listOf(
        Triple(1, accountName.trim().isNotBlank(), I18n.s("银行名称", "Bank")),
        Triple(2, productName.isNotBlank(), I18n.s("存款期限", "Deposit term")),
        Triple(3, principalVal > 0.0, I18n.s("本金（元）", "Principal")),
        Triple(4, rateValid, I18n.s("年化利率", "Annual rate")),
    )
    val pendingSteps = saveSteps.filter { !it.second }
    val nextStepNum = pendingSteps.firstOrNull()?.first
    // 软步骤链：①-④ 硬步骤 → ⑤ 存入日期（默认今天可跳过）→ ⑥ 存期（默认 1 年可跳过）
    val dateIsCurrent = nextStepNum == null && !dateModified
    val termIsCurrent = nextStepNum == null && dateModified && !termModified &&
            productName.isNotBlank() && !termLocked

    val doSave: () -> Unit = {
        val errors = mutableListOf<String>()
        // 有漏步时，先提示还需确认的步骤（对照栏位左侧的圆圈数字）
        if (pendingSteps.isNotEmpty()) {
            val list = pendingSteps.joinToString("；") { I18n.s("第%d步 %s", "Step %d %s").format(it.first, it.third) }
            errors.add(I18n.s("请先完成：${list}（对应栏位左侧的圆圈数字）", "Complete first: ${list} (numbers on the left of each field)"))
        }
        val r = parseRateInput(rate)
        if (r == null || r < 0.0) errors.add(I18n.s("年化利率无效，当前：$rate", "Invalid annual rate, current: $rate"))
        val tc = termCountInput.trim().toLongOrNull()
        if (tc == null || tc <= 0L) errors.add(I18n.s("存期数值必须大于 0，当前：$termCountInput", "Term length must be > 0, current: $termCountInput"))
        val realDaysCalc = if (endDate.isBefore(startDate)) 0L else ChronoUnit.DAYS.between(startDate, endDate)
        if (realDaysCalc <= 0) errors.add(I18n.s("到期日期必须晚于存入日期（存入：$startDate，到期：$endDate）", "Maturity must be after start (start: $startDate, maturity: $endDate)"))

        if (errors.isNotEmpty()) {
            errorMsg = errors.joinToString("\n")
        } else {
            val now = System.currentTimeMillis()
            val finalDays = if (realDaysCalc <= 0) 1L else realDaysCalc
            val finalEnd = if (realDaysCalc <= 0) startDate.plusDays(1) else endDate
            val record = Deposit(
                id = origin?.id ?: 0L,
                accountName = accountName.trim(),
                productName = productName.trim().ifBlank { null },
                principal = principalVal,
                annualRate = (r!! / 100.0),
                startDate = DateUtils.toMillis(startDate),
                endDate = DateUtils.toMillis(finalEnd),
                termDays = finalDays.toInt(),
                note = note.trim().ifBlank { null },
                isActive = origin?.isActive ?: true,
                createdAt = origin?.createdAt ?: now,
                updatedAt = now,
            )
            scope.launch {
                try {
                    repository.save(record)
                    android.widget.Toast.makeText(context, if (origin == null) I18n.s("新增成功", "Added") else I18n.s("修改成功", "Saved"), android.widget.Toast.LENGTH_SHORT).show()
                    onBack()
                } catch (e: Exception) {
                    android.util.Log.d("DepositBook", "save failed id=${record.id}", e)
                    android.widget.Toast.makeText(context, I18n.s("保存失败：${e.message ?: e.javaClass.simpleName}", "Save failed: ${e.message ?: e.javaClass.simpleName}"), android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (origin == null) I18n.s("新增存款", "Add Deposit") else I18n.s("编辑存款", "Edit Deposit"), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, I18n.s("返回", "Back"))
                    }
                },
            )
        },
        floatingActionButton = {
            // 侧边浮动保存：不受键盘遮挡（imePadding 抬升）；旁边提示下一步序号
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 3.dp,
                ) {
                    Row(
                        Modifier.padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        if (nextStepNum != null) {
                            val label = pendingSteps.first { it.first == nextStepNum }.third
                            StepBadge(num = nextStepNum, done = false, highlight = true)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                I18n.s("下一步 %d %s", "Next %d %s").format(nextStepNum, label),
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                maxLines = 1,
                            )
                        } else if (!dateModified) {
                            // 前四步完成且存入日期仍是默认今天 → 第 5 步：确认日期（默认今天，可跳过，不阻碍保存）
                            StepBadge(num = 5, done = false, highlight = true)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                I18n.s("下一步 5 存入日期·默认今天可跳过", "Next 5 Deposit date (today by default)"),
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                maxLines = 1,
                            )
                        } else if (termIsCurrent) {
                            // 存期显示且可修改但未确认 → 第 6 步：默认 1 年，可跳过，不阻碍保存
                            StepBadge(num = 6, done = false, highlight = true)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                I18n.s("下一步 6 存期·默认1年可跳过", "Next 6 Term (1 year by default)"),
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                maxLines = 1,
                            )
                        } else {
                            Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                I18n.s("可以保存", "Ready to save"),
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.imePadding()) {
                    FloatingActionButton(
                        onClick = { doSave() },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.Filled.Check, I18n.s("保存", "Save"))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 银行选择：内置清单（带图标）+ 用户自定义银行，输入关键字筛选
            // 排序：有存单的银行在前（最近存入的最前），其余按原顺序
            // ① 银行名称：左侧步骤圆圈；提示文字「选择存款银行」
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                StepBadge(num = 1, done = accountName.trim().isNotBlank(), highlight = nextStepNum == 1)
                Spacer(Modifier.width(8.dp))
                WithChineseImeHint(modifier = Modifier.weight(1f)) {
                    SearchableDropdownField(
                        label = I18n.s("银行名称", "Bank"),
                        value = accountName,
                        options = remember(userBanks, depositBankOrder) {
                            (userBanks + BankCatalog.BANKS.filterNot { b -> userBanks.contains(b) })
                                .sortedWith(compareBy { bank ->
                                    depositBankOrder.indexOf(bank).let { if (it == -1) Int.MAX_VALUE else it }
                                })
                                .map { b ->
                                    val icon: (@Composable () -> Unit)? = if (BankCatalog.isBuiltIn(b)) {
                                        { BankIcon(b) }
                                    } else null
                                    DropdownItem(
                                        b,
                                        icon = icon,
                                        aliases = BankCatalog.ALIASES.filterValues { it == b }.keys.toList(),
                                    )
                                }
                        },
                        placeholder = I18n.s("选择存款银行", "Select bank"),
                        customAdd = { name -> I18n.s("添加自定义银行「${name}」", "Add custom bank \"$name\"") },
                        onClear = { accountName = "" },
                        onSelect = { sel ->
                            accountName = sel
                            // 非内置银行 → 记入用户自定义银行清单
                            if (!BankCatalog.isBuiltIn(sel) && userBanks.none { it == sel }) {
                                optionsStore.addBank(sel)
                                userBanks = optionsStore.banks()
                            }
                            productFocus.requestFocus()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ② 产品名称：选择后自动带出默认存期；预置整存整取不允许更改存期
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                StepBadge(num = 2, done = productName.isNotBlank(), highlight = nextStepNum == 2)
                Spacer(Modifier.width(8.dp))
                WithChineseImeHint(modifier = Modifier.weight(1f)) {
                    SearchableDropdownField(
                        label = I18n.s("存款期限", "Deposit term"),
                        value = productName,
                        options = productItems.map { DropdownItem(it.name) },
                        placeholder = I18n.s("选择存款期限", "Select deposit term"),
                        onClear = { productName = "" },
                        onFocusRequester = productFocus,
                        onSelect = { sel ->
                            productName = sel
                            val def = productDefs.firstOrNull { it.name == sel }
                            if (def != null && def.name.contains("活期") && def.termDays == null) {
                                // 用户自建的「活期」没填默认存期 → 按活期惯例默认 10 年（可修改）
                                termUnit = "Y"
                                termCountInput = "10"
                                termLocked = false
                            } else if (def?.termDays != null) {
                                val days = def.termDays!!
                                when {
                                    days > 0 && days % 365L == 0L -> {
                                        termUnit = "Y"
                                        termCountInput = (days / 365L).toString()
                                    }
                                    days % 30L == 0L -> {
                                        termUnit = "M"
                                        termCountInput = (days / 30L).toString()
                                    }
                                    else -> {
                                        termUnit = "D"
                                        termCountInput = days.toString()
                                    }
                                }
                            }
                            termLocked = def?.let { it.termDays != null && !it.allowModify } ?: false
                            principalFocus.requestFocus()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ③ 本金（元）：数字键盘，按「完成」跳到年化利率
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                StepBadge(num = 3, done = principalVal > 0.0, highlight = nextStepNum == 3)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = principal,
                    onValueChange = { principal = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(I18n.s("本金（元）", "Principal (CNY)")) },
                    placeholder = { Text(I18n.s("输入本金", "Enter principal")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { rateFocus.requestFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(principalFocus),
                )
            }

            // ④ 年化利率（%）：数字键盘，「完成」收起键盘
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                StepBadge(num = 4, done = rateValid, highlight = nextStepNum == 4)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(I18n.s("年化利率（%）", "Annual rate (%)")) },
                    placeholder = { Text(I18n.s("如：4 表示 4%", "e.g. 4 means 4%")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(rateFocus),
                )
            }

            // ⑤ 存入日期：默认今天（虚线圆圈=待确认），改过日期后变对勾；到期日期并入同行纯文本显示
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                StepBadge(
                    num = 5,
                    done = dateModified,
                    highlight = !dateModified && nextStepNum == null,
                    dashed = !dateModified && nextStepNum != null,
                )
                Spacer(Modifier.width(8.dp))
                DateField(I18n.s("存入日期", "Deposit date"), startDate, Modifier.weight(1f)) { showStartPicker = true }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        I18n.s("到期日期", "Maturity"),
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(FormatUtils.date(endDate), style = MaterialTheme.typography.titleMedium)
                }
            }

            // ⑥ 存期：未选产品或产品固定存期时不显示任何栏位；可修改存期的产品显示（默认1年，可跳过）
            if (productName.isNotBlank() && !termLocked) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        StepBadge(
                            num = 6,
                            done = termModified,
                            highlight = termIsCurrent,
                            dashed = !termModified && !termIsCurrent,
                        )
                        Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DropdownField(
                                label = I18n.s("存期单位", "Term unit"),
                                value = termUnits.first { it.code == termUnit }.display(),
                                options = termUnits.map { it.display() },
                                placeholder = I18n.s("请选择", "Select"),
                                onSelect = { sel ->
                                    termUnit = termUnits.first { it.display() == sel }.code
                                    termModified = true
                                },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = termCountInput,
                                onValueChange = {
                                    termCountInput = it.filter { c -> c.isDigit() }
                                    termModified = true
                                },
                                label = { Text(I18n.s("存期数值", "Term length")) },
                                placeholder = { Text(I18n.s("如：12", "e.g. 12")) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                         }
                     }
                 }
 
             OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(I18n.s("备注（可选）", "Note (optional)")) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toMillis(startDate)
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startDate = DateUtils.toDate(it) }
                    dateModified = true
                    showStartPicker = false
                }) { Text(I18n.s("确定", "OK")) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text(I18n.s("取消", "Cancel")) }
            },
        ) { DatePicker(state = state) }
    }

    errorMsg?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text(I18n.s("无法保存", "Can't save")) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMsg = null }) { Text(I18n.s("知道了", "Got it")) }
            },
        )
    }
}

/** 存期单位：内部固定代码 + 按语言显示 */
private data class UnitOption(val code: String, val zh: String, val en: String) {
    fun display(): String = com.pxy.depositbook.util.I18n.s(zh, en)
}

/**
 * 步骤圆圈序号（22dp）：
 * - 已完成：淡绿底 + 对勾
 * - 当前（highlight）：主色实底 + 白字
 * - 未完成：透明底 + 灰色描边数字
 */
@Composable
private fun StepBadge(num: Int, done: Boolean, highlight: Boolean, dashed: Boolean = false) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val bg = when {
        done -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        highlight -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val fg = when {
        done -> MaterialTheme.colorScheme.tertiary
        highlight -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                when {
                    // 虚线描边：表示「系统已自动填默认值，待确认」（区别于实线=待填写）
                    dashed -> Modifier.drawBehind {
                        val w = 1.5.dp.toPx()
                        drawCircle(
                            color = outlineColor,
                            radius = size.minDimension / 2f - w / 2f,
                            style = Stroke(
                                width = w,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5f.dp.toPx())),
                            ),
                        )
                    }
                    !done && !highlight -> Modifier.border(1.dp, outlineColor, CircleShape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Filled.Check, null, tint = fg, modifier = Modifier.size(13.dp))
        } else {
            Text(
                num.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(FormatUtils.date(date), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * 解析金额输入：兼容千分位逗号（12,000.00）、全角逗号（12，000）、空格、￥/元/¥ 前后缀。
 */
private fun parseAmountInput(raw: String): Double? =
    raw.trim()
        .replace("，", "")
        .replace(",", "")
        .replace("¥", "")
        .replace("￥", "")
        .replace("元", "")
        .replace(" ", "")
        .trim()
        .takeIf { it.isNotEmpty() && it.none { c -> c.isLetter() } }
        ?.toDoubleOrNull()

/**
 * 解析利率输入：兼容百分号（3.5%、3.5％）、空格等。
 */
private fun parseRateInput(raw: String): Double? =
    raw.trim()
        .replace("%", "")
        .replace("％", "")
        .replace(" ", "")
        .trim()
        .takeIf { it.isNotEmpty() && it.none { c -> c.isLetter() } }
        ?.toDoubleOrNull()
