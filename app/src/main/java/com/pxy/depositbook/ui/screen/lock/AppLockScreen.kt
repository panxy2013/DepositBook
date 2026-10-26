package com.pxy.depositbook.ui.screen.lock

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pxy.depositbook.ui.components.PatternLock
import com.pxy.depositbook.ui.components.PinLock

@Composable
fun AppLockScreen(
    lockType: String,
    savedPin: String,
    savedPattern: String,
    onUnlocked: () -> Unit,
) {
    if (lockType == "pattern") {
        PatternLock(
            title = "请输入手势密码",
            onVerify = { it == savedPattern },
            onSuccess = { onUnlocked() },
            onError = {},
        )
    } else {
        PinLock(
            title = "请输入数字密码",
            onVerify = { it == savedPin },
            onSuccess = { onUnlocked() },
            onError = {},
        )
    }
}
