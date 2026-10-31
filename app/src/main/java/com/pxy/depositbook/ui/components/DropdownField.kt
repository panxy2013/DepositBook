package com.pxy.depositbook.ui.components

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.util.Locale

/** 下拉选项：label 为显示/返回值；icon 可选（内置银行图标用）；aliases 为关键字匹配别名 */
data class DropdownItem(val label: String, val icon: (@Composable () -> Unit)? = null, val aliases: List<String> = emptyList())

/**
 * 通用下拉选择框：只允许从配置好的选项里选。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    placeholder: String = I18n.s("请选择", "Select"),
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(I18n.s("暂无选项，请到「设置」里添加", "No options yet; add them in Settings")) },
                    onClick = { expanded = false },
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (option == value) {
                            Icon(Icons.Outlined.Check, null)
                        }
                    },
                )
            }
        }
    }
}

/**
 * 可搜索下拉选择框：输入关键字实时筛选选项（匹配显示名或别名）。
 * 用于内置银行清单一类的长列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdownField(
    label: String,
    value: String,
    options: List<DropdownItem>,
    placeholder: String = I18n.s("输入关键字筛选", "Type to filter"),
    emptyHint: String = I18n.s("没有匹配项", "No matches"),
    onFocusRequester: FocusRequester? = null,
    /** 输入名称无精确匹配时，在下拉顶部提供「添加自定义项」入口；参数为当前输入文本，返回菜单显示文字 */
    customAdd: ((String) -> String)? = null,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(value) }
    val kw = text.trim()

    val filtered = if (kw.isEmpty()) options else options.filter { item ->
        item.label.contains(kw, ignoreCase = true) ||
                item.aliases.any { it.contains(kw, ignoreCase = true) }
    }
    val allowCustom = customAdd != null && kw.isNotEmpty() && options.none { it.label == kw }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; expanded = true },
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onFocusRequester != null) Modifier.focusRequester(onFocusRequester) else Modifier)
                .menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (allowCustom) {
                DropdownMenuItem(
                    text = { Text(customAdd!!(kw)) },
                    onClick = { onSelect(kw); expanded = false },
                )
            }
            if (filtered.isEmpty() && !allowCustom) {
                DropdownMenuItem(text = { Text(emptyHint) }, onClick = { expanded = false })
            }
            filtered.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = item.icon,
                    onClick = {
                        onSelect(item.label)
                        expanded = false
                    },
                    trailingIcon = {
                        if (item.label == value) {
                            Icon(Icons.Outlined.Check, null)
                        }
                    },
                )
            }
        }
    }
}

/**
 * 中文输入法提示：把 EditorInfo.imeHintLocales 设为 zh-CN（并保留 en 备选），
 * 输入法支持该标记时会自动切到中文键盘。包住整个字段即只对该字段生效。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WithChineseImeHint(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val hintLocales = remember { android.os.LocaleList(Locale.CHINA, Locale.ENGLISH) }
    val interceptor = remember {
        PlatformTextInputInterceptor { request, nextHandler ->
            val wrapped = PlatformTextInputMethodRequest { outAttrs ->
                runCatching { outAttrs.hintLocales = hintLocales }
                request.createInputConnection(outAttrs)
            }
            nextHandler.startInputMethod(wrapped)
        }
    }
    InterceptPlatformTextInput(
        interceptor = interceptor,
        content = { Box(modifier) { content() } },
    )
}

/** 通用键盘收起动作：Done 按下时关闭输入法 */
@Composable
fun rememberHideKeyboardAction(): KeyboardActions {
    val controller = LocalSoftwareKeyboardController.current
    return remember { KeyboardActions(onDone = { controller?.hide() }) }
}
