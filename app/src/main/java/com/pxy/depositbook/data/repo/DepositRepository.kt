package com.pxy.depositbook.data.repo

import com.pxy.depositbook.util.I18n

import com.pxy.depositbook.data.dao.AssetSnapshotDao
import com.pxy.depositbook.data.dao.DepositDao
import com.pxy.depositbook.data.dao.OperationLogDao
import com.pxy.depositbook.data.entity.AssetSnapshot
import com.pxy.depositbook.data.model.StatsCalculator
import com.pxy.depositbook.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.entity.OperationLog
import com.pxy.depositbook.util.FormatUtils

class DepositRepository(
    private val depositDao: DepositDao,
    private val logDao: OperationLogDao,
    private val snapshotDao: AssetSnapshotDao,
    /** 账户中枢：多账户数据隔离由仓库层统一过滤/打标 */
    val accounts: AccountStore,
) {
    private fun pid(): Long = accounts.currentId.value

    /** 按当前账户过滤的存款流（切换账户后自动重发） */
    fun observeAll(): Flow<List<Deposit>> =
        kotlinx.coroutines.flow.combine(depositDao.observeAll(), accounts.currentId) { list, p ->
            list.filter { it.profileId == p }
        }

    fun observeActive(): Flow<List<Deposit>> = observeAll().map { l -> l.filter { it.isActive } }

    fun observeById(id: Long): Flow<Deposit?> = depositDao.observeById(id)

    fun observeLogs(): Flow<List<OperationLog>> =
        kotlinx.coroutines.flow.combine(logDao.observeAll(), accounts.currentId) { list, p ->
            list.filter { it.profileId == p }
        }

    suspend fun get(id: Long): Deposit? = depositDao.findById(id)

    /** 当前账户的存款（一次性） */
    suspend fun getAllOnce(): List<Deposit> = depositDao.getAllOnce().filter { it.profileId == pid() }

    /** 全部账户的存款（一次性，导出用） */
    suspend fun getAllDepositsAllAccounts(): List<Deposit> = depositDao.getAllOnce()

    suspend fun save(deposit: Deposit): Long {
        val saveTime = System.currentTimeMillis()
        // 归属当前账户；编辑旧存款时保持其原归属
        val own = deposit.copy(
            profileId = deposit.profileId.takeIf { it != 0L } ?: pid(),
            updatedAt = saveTime,
        )
        return if (deposit.id == 0L) {
            val id = depositDao.insert(own)
            log(own.copy(id = id), "CREATE")
            id
        } else {
            depositDao.update(own)
            log(own, "UPDATE")
            deposit.id
        }
    }

    suspend fun delete(deposit: Deposit) {
        depositDao.delete(deposit)
        log(deposit, "DELETE")
    }

    suspend fun settle(deposit: Deposit) {
        depositDao.update(deposit.copy(isActive = false, updatedAt = System.currentTimeMillis()))
        log(deposit, "SETTLE")
    }

    /** 已结清 → 恢复计息（锁定解除，可再次修改） */
    suspend fun unsettle(deposit: Deposit) {
        depositDao.update(deposit.copy(isActive = true, updatedAt = System.currentTimeMillis()))
        log(deposit, "UNSETTLE")
    }

    suspend fun addLog(action: String, content: String) {
        logDao.insert(OperationLog(action = action, content = content, profileId = pid()))
    }

    /**
     * 重命名银行/账户：同步更新所有该名称的存款记录，并写日志。
     */
    suspend fun renameAccount(oldName: String, newName: String): Int {
        val rows = depositDao.renameAccount(oldName.trim(), newName.trim(), System.currentTimeMillis(), pid())
        logDao.insert(
            OperationLog(
                action = "RENAME",
                content = I18n.s("银行「$oldName」重命名为「$newName」，已同步 $rows 条存款记录", "Renamed “$oldName” to “$newName”; updated $rows deposits"),
            )
        )
        return rows
    }

    suspend fun deleteLog(id: Long) {
        logDao.deleteById(id)
    }

    suspend fun clearLogs() {
        logDao.clear()
    }

    /**
     * 记录今天的资产快照（当天多次记录取最新值）。
     * 历史快照不随后续删除/结清存单变化，只有「清空所有数据」才删除。
     */
    suspend fun recordTodaySnapshot() {
        val summary = StatsCalculator.summarize(getAllOnce())
        val snapshot = AssetSnapshot(
            id = 0L,
            day = DateUtils.today().toString(),
            profileId = pid(),
            totalPrincipal = summary.totalPrincipal,
            totalInterest = summary.totalInterestEarned,
            totalValue = summary.totalValueNow,
            activeCount = summary.activeCount,
            recordedAt = System.currentTimeMillis(),
        )
        snapshotDao.upsert(snapshot)
    }

    /**
     * 教学示例数据（备注带「示例数据」标记，可一键清除）：
     * 覆盖各种形态——在存、临期、已到期未结清、已结清，
     * 用户点着示例学习各页面。
     */
    suspend fun addDemoData(): Int {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        fun deposit(
            bank: String, product: String, principal: Double, rate: Double,
            startDaysAgo: Long, termDays: Int, settled: Boolean = false,
        ) = Deposit(
            profileId = pid(),
            accountName = bank,
            productName = product,
            principal = principal,
            annualRate = rate,
            startDate = now - startDaysAgo * dayMs,
            endDate = now - startDaysAgo * dayMs + termDays * dayMs,
            termDays = termDays,
            note = "示例数据 Demo",
            isActive = !settled,
        )
        val demos = listOf(
            deposit("中国工商银行", "3年期整存整取", 80000.0, 0.0265, 200, 1095),
            deposit("中国建设银行", "1年期整存整取", 50000.0, 0.0235, 30, 365),
            deposit("招商银行", "大额存单（20万起）", 200000.0, 0.0285, 10, 1095),
            deposit("中国银行", "1年期整存整取", 60000.0, 0.023, 355, 365),
            deposit("交通银行", "2年期整存整取", 30000.0, 0.0245, 400, 730),
            deposit("中国邮政储蓄银行", "1年期整存整取", 20000.0, 0.02, 500, 365, settled = true),
        )
        demos.forEach { save(it) }
        return demos.size
    }

    /** 是否已有示例数据（当前账户） */
    suspend fun hasDemoData(): Boolean =
        getAllOnce().any { it.note?.contains("示例数据") == true }

    /** 清除示例数据（仅删除备注带「示例数据」标记的记录），返回删除条数 */
    suspend fun removeDemoData(): Int {
        val demo = getAllOnce().filter { it.note?.contains("示例数据") == true }
        demo.forEach { delete(it) }
        return demo.size
    }

    /** 历史快照：按日期升序返回（当前账户） */    fun observeSnapshots(): Flow<List<AssetSnapshot>> =
        kotlinx.coroutines.flow.combine(snapshotDao.observeAll(), accounts.currentId) { list, p ->
            list.filter { it.profileId == p }
        }

    /** 一次性读取全部快照（导出用） */
    suspend fun getAllSnapshotsOnce(): List<AssetSnapshot> = snapshotDao.getAllOnce()

    /**
     * 清空所有数据：所有账户的存款、日志、历史资产快照全部删除。
     */
    suspend fun clearAllData() {
        depositDao.getAllOnce().forEach { delete(it) }
        logDao.clear()
        snapshotDao.clearAll()
        addLog("CLEAR", I18n.s("已清空全部数据（含历史资产快照）", "All data cleared (including snapshot history)"))
    }

    private suspend fun log(deposit: Deposit, action: String) {
        val text = when (action) {
            "CREATE" -> I18n.s(
                "新增 ${deposit.accountName}${deposit.productName?.let { "·$it" } ?: ""} 本金 ${FormatUtils.moneyPlain(deposit.principal)}，年化 ${FormatUtils.rate(deposit.annualRate)}",
                "Added ${deposit.accountName}${deposit.productName?.let { "·$it" } ?: ""} principal ${FormatUtils.moneyPlain(deposit.principal)}, rate ${FormatUtils.rate(deposit.annualRate)}",
            )
            "UPDATE" -> I18n.s("修改 ${deposit.accountName} ", "Updated ${deposit.accountName} ") +
                    I18n.s("本金 ${FormatUtils.moneyPlain(deposit.principal)}，年化 ${FormatUtils.rate(deposit.annualRate)}", "principal ${FormatUtils.moneyPlain(deposit.principal)}, rate ${FormatUtils.rate(deposit.annualRate)}")
            "DELETE" -> I18n.s("删除 ${deposit.accountName} 本金 ${FormatUtils.moneyPlain(deposit.principal)}", "Deleted ${deposit.accountName}, principal ${FormatUtils.moneyPlain(deposit.principal)}")
            "SETTLE" -> I18n.s("结清 ${deposit.accountName} 本金 ${FormatUtils.moneyPlain(deposit.principal)}", "Settled ${deposit.accountName}, principal ${FormatUtils.moneyPlain(deposit.principal)}")
            "UNSETTLE" -> I18n.s("恢复计息 ${deposit.accountName} 本金 ${FormatUtils.moneyPlain(deposit.principal)}", "Reactivated ${deposit.accountName}, principal ${FormatUtils.moneyPlain(deposit.principal)}")
            else -> "$action ${deposit.accountName}"
        }
        logDao.insert(
            OperationLog(
                depositId = deposit.id.takeIf { it != 0L },
                profileId = deposit.profileId,
                action = action,
                content = text,
            )
        )
    }
}
