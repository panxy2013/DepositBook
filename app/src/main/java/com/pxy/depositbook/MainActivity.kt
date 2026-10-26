package com.pxy.depositbook

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pxy.depositbook.ui.screen.lock.AppLockScreen
import com.pxy.depositbook.ui.theme.DepositBookTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var isLocked by mutableStateOf(false)
    private var wasInBackground = false

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {
            if (activity != this@MainActivity) return
            val app = application as DepositApp
            val security = app.security
            if (!security.isEnabled()) {
                isLocked = false
                wasInBackground = false
                return
            }
            if (wasInBackground) {
                isLocked = true
            }
        }
        override fun onActivityPaused(activity: Activity) {
            if (activity != this@MainActivity) return
            val app = application as DepositApp
            // 文件选择/分享等挂起期间退后台，不标记需要锁
            if (app.security.isEnabled() && !LockManager.isSuspended()) {
                wasInBackground = true
            }
        }
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("DepositBook", "MainActivity onCreate action=${intent?.action} data=${intent?.data}")
        super.onCreate(savedInstanceState)
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        enableEdgeToEdge()
        val app = application as DepositApp
        val repo = app.repository
        val security = app.security
        if (security.isEnabled()) {
            isLocked = true
            wasInBackground = true
        }
        setContent {
            DepositBookTheme {
                if (isLocked && security.isEnabled()) {
                    AppLockScreen(
                        lockType = security.getLockType(),
                        savedPin = security.getPin(),
                        savedPattern = security.getPattern(),
                        onUnlocked = { isLocked = false },
                    )
                } else {
                    DepositBookRoot(repository = repo)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 系统文件选择器/分享返回结果，清除挂起状态
        LockManager.clear()
    }

    override fun onDestroy() {
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        super.onDestroy()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        Log.d("DepositBook", "MainActivity onNewIntent action=${intent.action} data=${intent.data}")
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        Log.d("DepositBook", "MainActivity onResume")
        super.onResume()
    }
}
