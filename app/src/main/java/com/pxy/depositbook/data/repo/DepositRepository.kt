package com.pxy.depositbook.data.repo

import com.pxy.depositbook.data.dao.AssetSnapshotDao
import com.pxy.depositbook.data.dao.DepositDao
import com.pxy.depositbook.data.dao.OperationLogDao
import com.pxy.depositbook.data.entity.AssetSnapshot
import com.pxy.depositbook.data.model.StatsCalculator
import com.pxy.depositbook.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.entity.OperationLog
import com.pxy.depositbook.util.FormatUtils

class DepositRepository(
    private val depositDao: DepositDao,
    private val logDao: OperationLogDao,
    private val snapshotDao: AssetSnapshotDao,
) {

    fun observeAll(): Flow<List<Deposit>> = depositDao.observeAll()
    fun observeActive(): Flow<List<Deposit>> = depositDao.observeActive()
    fun observeById(id: Long): Flow<Deposit?> = depositDao.observeById(id)
    fun observeLogs(): Flow<List<OperationLog>> = logDao.observeAll()

    suspend fun get(id: Long): Deposit? = depositDao.findById(id)
    suspend fun getAllOnce(): List<Deposit> = depositDao.getAllOnce()

    suspend fun save(deposit: Deposit): Long {
        return if (deposit.id == 0L) {
            val id = depositDao.insert(deposit)
            log(deposit.copy(id = id), "CREATE")
            id
        } else {
            depositDao.update(deposit)
            log(deposit, "UPDATE")
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

    suspend fun addLog(action: String, content: String) {
        logDao.insert(OperationLog(action = action, content = content))
    }

    /**
     * 重命名银行/账户：同步更新所有该名称的存款记录，并写日志。
     */
    suspend fun renameAccount(oldName: String, newName: String): Int {
        val rows = depositDao.renameAccount(oldName.trim(), newName.trim(), System.currentTimeMillis())
        logDao.insert(
            OperationLog(
                action = "RENAME",
                content = "银行「$oldName」重命名为「$newName」，已同步 $rows 条存款记录",
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
        val summary = StatsCalculator.summarize(depositDao.getAllOnce())
        val snapshot = AssetSnapshot(
            id = 0L,
            day = DateUtils.today().toString(),
            totalPrincipal = summary.totalPrincipal,
            totalInterest = summary.totalInterestEarned,
            totalValue = summary.totalValueNow,
            activeCount = summary.activeCount,
            recordedAt = System.currentTimeMillis(),
        )
        snapshotDao.upsert(snapshot)
    }

    /** 历史快照：按日期升序返回全部记录 */
    fun observeSnapshots(): Flow<List<AssetSnapshot>> = snapshotDao.observeAll()

    /** 一次性读取全部快照（导出用） */
    suspend fun getAllSnapshotsOnce(): List<AssetSnapshot> = snapshotDao.getAllOnce()

    /**
     * 清空所有数据：存款、日志、历史资产快照全部删除。
     */
    suspend fun clearAllData() {
        depositDao.getAllOnce().forEach { delete(it) }
        logDao.clear()
        snapshotDao.clearAll()
        addLog("CLEAR", "已清空全部数据（含历史资产快照）")
    }

    private suspend fun log(deposit: Deposit, action: String) {
        val text = when (action) {
            "CREATE" -> "新增 ${deposit.accountName}${deposit.productName?.let { "·$it" } ?: ""} " +
                    "本金 ${FormatUtils.moneyPlain(deposit.principal)}，年化 ${FormatUtils.rate(deposit.annualRate)}"
            "UPDATE" -> "修改 ${deposit.accountName} " +
                    "本金 ${FormatUtils.moneyPlain(deposit.principal)}，年化 ${FormatUtils.rate(deposit.annualRate)}"
            "DELETE" -> "删除 ${deposit.accountName} 本金 ${FormatUtils.moneyPlain(deposit.principal)}"
            "SETTLE" -> "结清 ${deposit.accountName} 本金 ${FormatUtils.moneyPlain(deposit.principal)}"
            else -> "$action ${deposit.accountName}"
        }
        logDao.insert(
            OperationLog(
                depositId = deposit.id.takeIf { it != 0L },
                action = action,
                content = text,
            )
        )
    }
}
