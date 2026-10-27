package com.pxy.depositbook.ui.screen.lock

import com.pxy.depositbook.util.I18n

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pxy.depositbook.ui.components.PatternLock
import com.pxy.depositbook.ui.components.PinLock

/**
 * 应用锁界面。
 *
 * - 数字密码必须已配置；手势密码仅在已配置时作为可选方式
 * - 底部可在「数字密码」和「手势密码」之间切换
 * - 兜底：若应用锁开启但两种密码都没配置（历史遗留状态），临时放行并提示去设置，
 *   正常流程下设置页会保证至少配置数字密码
 */
@Composable
fun AppLockScreen(
    lockType: String,
    savedPin: String,
    savedPattern: String,
    onUnlocked: () -> Unit,
) {
    // Surface 铺满整个窗口（包括系统状态栏/导航栏后面的区域），避免露出黑色底色
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LockContent(lockType, savedPin, savedPattern, onUnlocked)
    }
}

@Composable
private fun LockContent(
    lockType: String,
    savedPin: String,
    savedPattern: String,
    onUnlocked: () -> Unit,
) {
    val pinConfigured = savedPin.isNotBlank()
    val patternConfigured = savedPattern.isNotBlank()

    // 可用方式：数字密码优先，其次手势
    val methods = buildList {
        if (pinConfigured) add("pin")
        if (patternConfigured) add("pattern")
    }

    var current by remember {
        mutableStateOf(if (lockType in methods) lockType else methods.firstOrNull() ?: "pin")
    }

    if (!pinConfigured && !patternConfigured) {
        // 理论上不会出现（开启前强制先设数字密码）；历史遗留时放行并提示
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))
            Text(I18n.s("应用锁已开启，但尚未设置任何密码", "App lock is on, but no PIN or pattern is set"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Text(I18n.s("请解锁后到 设置 → 安全 重新配置", "Unlock, then configure it in Settings → Security"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onUnlocked) { Text(I18n.s("本次临时解锁", "Temporarily unlocked this time")) }
        }
        return
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(0.18f))
        if (current == "pin") {
            PinLock(
                modifier = Modifier.weight(1f),
                title = I18n.s("请输入数字密码", "Enter your PIN"),
                subtitle = subtitleText(methods.size),
                onVerify = { it == savedPin },
                onSuccess = { onUnlocked() },
            )
        } else {
            PatternLock(
                modifier = Modifier.weight(1f),
                title = I18n.s("请绘制手势密码", "Draw your pattern"),
                subtitle = subtitleText(methods.size),
                onVerify = { it == savedPattern },
                onSuccess = { onUnlocked() },
            )
        }
        Spacer(Modifier.weight(0.05f))
        if (methods.size == 2) {
            TextButton(
                onClick = { current = if (current == "pin") "pattern" else "pin" },
            ) {
                Text(if (current == "pin") I18n.s("使用手势密码解锁", "Unlock with pattern") else I18n.s("使用数字密码解锁", "Unlock with PIN"))
            }
        }
        Spacer(Modifier.weight(0.1f))
    }
}

private fun subtitleText(methodCount: Int): String =
    if (methodCount >= 2) I18n.s("也可在下方切换验证方式", "You can switch the method below") else ""
