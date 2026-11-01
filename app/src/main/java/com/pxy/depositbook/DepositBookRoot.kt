package com.pxy.depositbook

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pxy.depositbook.data.prefs.OptionsStore
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.ui.navigation.AppNavHost

/**
 * 根界面：启动时请求通知权限（Android 13+），用于到期提醒。
 */
@Composable
fun DepositBookRoot(repository: DepositRepository) {    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 结果无需处理，未授权时提醒静默跳过 */ }

    // 安装后只询问一次通知权限；拒绝后不再反复弹窗（可在系统设置里手动开启）
    val prefs = remember { OptionsStore(context) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted && !prefs.notifPermAsked()) {
                prefs.setNotifPermAsked()
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppNavHost(repository = repository)
    }
}
