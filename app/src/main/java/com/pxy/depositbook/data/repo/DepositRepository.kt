package com.pxy.depositbook.data.repo

import com.pxy.depositbook.data.dao.DepositDao
import com.pxy.depositbook.data.dao.OperationLogDao
import com.pxy.depositbook.data.entity.Deposit
import com.pxy.depositbook.data.entity.OperationLog
import com.pxy.depositbook.util.FormatUtils
import kotlinx.coroutines.flow.Flow

class DepositRepository(
    private val depositDao: DepositDao,
    private val logDao: OperationLogDao,
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
