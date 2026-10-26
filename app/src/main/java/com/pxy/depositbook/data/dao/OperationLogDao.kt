package com.pxy.depositbook.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pxy.depositbook.data.entity.OperationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationLogDao {

    @Query("SELECT * FROM operation_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<OperationLog>>

    @Insert
    suspend fun insert(log: OperationLog): Long

    @Query("DELETE FROM operation_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM operation_log")
    suspend fun clear()
}
