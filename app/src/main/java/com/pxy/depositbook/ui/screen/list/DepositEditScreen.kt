package com.pxy.depositbook.ui.screen.list

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (origin == null) I18n.s("新增存款", "Add Deposit") else I18n.s("编辑存款", "Edit Deposit")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, I18n.s("返回", "Back"))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 银行选择：内置清单（带图标）+ 用户自定义银行，输入关键字筛选
            // 排序：有存单的银行在前（最近存入的最前），其余按原顺序
            WithChineseImeHint {
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
                    placeholder = I18n.s("输入关键字筛选，如「工行」「微众」", "Type to filter, e.g. ICBC"),
                    onFocusRequester = null,
                    onSelect = { accountName = it; productFocus.requestFocus() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 手动添加自定义银行（不带图标）
            var showAddBank by remember { mutableStateOf(false) }
            TextButton(onClick = { showAddBank = !showAddBank }) {
                Text(
                    if (showAddBank) I18n.s("收起自定义银行", "Hide custom bank")
                    else I18n.s("没有找到？手动添加银行（自定义银行不带图标）", "Not listed? Add a custom bank (no icon)"),
                )
            }
            if (showAddBank) {
                var newBank by remember { mutableStateOf("") }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    WithChineseImeHint(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = newBank,
                            onValueChange = { newBank = it },
                            placeholder = { Text(I18n.s("输入银行名称", "Bank name"), fontSize = 12.dp.let { MaterialTheme.typography.labelMedium.fontSize }) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val name = newBank.trim()
                            if (name.isNotEmpty()) {
                                optionsStore.addBank(name)
                                userBanks = optionsStore.banks()
                                accountName = name
                                newBank = ""
                            }
                        },
                        enabled = newBank.isNotBlank(),
                    ) { Text(I18n.s("添加", "Add")) }
                }
            }

            // 产品选择：选择后自动带出默认存期；预置整存整取不允许更改存期
            WithChineseImeHint {
                SearchableDropdownField(
                label = I18n.s("产品名称", "Product"),
                value = productName,
                options = productItems.map { DropdownItem(it.name) },
                placeholder = I18n.s("输入关键字筛选", "Type to filter"),
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

            OutlinedTextField(
                value = principal,
                onValueChange = { principal = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(I18n.s("本金（元）", "Principal (CNY)")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(principalFocus),
            )

            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(I18n.s("年化利率（%）", "Annual rate (%)")) },
                placeholder = { Text(I18n.s("如：4 表示 4%", "e.g. 4 means 4%")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(I18n.s("存入日期", "Deposit date"), startDate, Modifier.fillMaxWidth()) { showStartPicker = true }

            if (termLocked) {
                // 存期锁定：只读展示
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            I18n.s("存期（按产品固定，不可修改）", "Term (fixed by product, not editable)"),
                            fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        val u = termUnits.first { it.code == termUnit }
                        Text(
                            I18n.s("%s %s（%d 天）", "%s %s (%d days)").format(termCountInput, u.display(), days),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DropdownField(
                        label = I18n.s("存期单位", "Term unit"),
                        value = termUnits.first { it.code == termUnit }.display(),
                        options = termUnits.map { it.display() },
                        placeholder = I18n.s("请选择", "Select"),
                        onSelect = { sel ->
                            termUnit = termUnits.first { it.display() == sel }.code
                        },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = termCountInput,
                        onValueChange = { termCountInput = it.filter { c -> c.isDigit() } },
                        label = { Text(I18n.s("存期数值", "Term length")) },
                        placeholder = { Text(I18n.s("如：12", "e.g. 12")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        I18n.s("到期日期（自动计算，不可手动修改）", "Maturity date (auto-calculated, not editable)"),
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(FormatUtils.date(endDate), style = MaterialTheme.typography.titleMedium)
                }
            }

            Text(
                I18n.s("期限 ${days} 天 · 日利息约 ${FormatUtils.moneyPlain(daily)} 元", "${days} days · est. daily interest ${FormatUtils.moneyPlain(daily)} CNY"),
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(I18n.s("备注（可选）", "Note (optional)")) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val errors = mutableListOf<String>()
                    if (accountName.trim().isBlank()) errors.add(I18n.s("请填写或选择银行名称", "Select or enter a bank name"))
                    val p = parseAmountInput(principal)
                    if (p == null || p <= 0.0) errors.add(I18n.s("本金必须大于 0，当前：$principal", "Principal must be > 0, current: $principal"))
                    val r = parseRateInput(rate)
                    if (r == null || r < 0.0) errors.add(I18n.s("年化利率无效，当前：$rate", "Invalid annual rate, current: $rate"))
                    val tc = termCountInput.trim().toLongOrNull()
                    if (tc == null || tc <= 0L) errors.add(I18n.s("存期数值必须大于 0，当前：$termCountInput", "Term length must be > 0, current: $termCountInput"))
                    val realDaysCalc = if (endDate.isBefore(startDate)) 0L else ChronoUnit.DAYS.between(startDate, endDate)
                    if (realDaysCalc <= 0) errors.add(I18n.s("到期日期必须晚于存入日期（存入：$startDate，到期：$endDate）", "Maturity must be after start (start: $startDate, maturity: $endDate)"))

                    if (errors.isNotEmpty()) {
                        errorMsg = errors.joinToString("\n")
                        return@Button
                    }

                    val now = System.currentTimeMillis()
                    val finalDays = if (realDaysCalc <= 0) 1L else realDaysCalc
                    val finalEnd = if (realDaysCalc <= 0) startDate.plusDays(1) else endDate
                    val record = Deposit(
                        id = origin?.id ?: 0L,
                        accountName = accountName.trim(),
                        productName = productName.trim().ifBlank { null },
                        principal = p!!,
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
                },
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(I18n.s("保存", "Save"))
            }
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
            Text(label, fontSize = MaterialTheme.typography.labelMedium.fontSize,
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
