package com.pxy.depositbook

import android.app.Application
import com.pxy.depositbook.data.db.AppDatabase
import com.pxy.depositbook.data.prefs.OptionsStore
import com.pxy.depositbook.data.prefs.SecurityStore
import com.pxy.depositbook.data.repo.DepositRepository
import com.pxy.depositbook.util.I18n
import com.pxy.depositbook.worker.ReminderScheduler

class DepositApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: DepositRepository by lazy {
        DepositRepository(database.depositDao(), database.operationLogDao(), database.assetSnapshotDao())
    }
    val options: OptionsStore by lazy { OptionsStore(this) }
    val security: SecurityStore by lazy { SecurityStore(this) }

    override fun onCreate() {
        super.onCreate()
        // 读取已保存的显示语言（默认中文）
        I18n.init(options.language()) { options.setLanguage(it) }
        ReminderScheduler.schedule(this)
    }
}
