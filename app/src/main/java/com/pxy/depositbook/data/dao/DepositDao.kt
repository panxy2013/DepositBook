package com.pxy.depositbook.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pxy.depositbook.data.entity.Deposit
import kotlinx.coroutines.flow.Flow

@Dao
interface DepositDao {

    @Query("SELECT * FROM deposit ORDER BY endDate ASC")
    fun observeAll(): Flow<List<Deposit>>

    @Query("SELECT * FROM deposit WHERE isActive = 1 ORDER BY endDate ASC")
    fun observeActive(): Flow<List<Deposit>>

    @Query("SELECT * FROM deposit WHERE id = :id")
    suspend fun findById(id: Long): Deposit?

    @Query("SELECT * FROM deposit WHERE id = :id")
    fun observeById(id: Long): Flow<Deposit?>

    @Insert
    suspend fun insert(deposit: Deposit): Long

    @Update
    suspend fun update(deposit: Deposit)

    @Delete
    suspend fun delete(deposit: Deposit)

    @Query("SELECT * FROM deposit")
    suspend fun getAllOnce(): List<Deposit>

    @Query("UPDATE deposit SET accountName = :newName, updatedAt = :now WHERE accountName = :oldName")
    suspend fun renameAccount(oldName: String, newName: String, now: Long): Int
}
