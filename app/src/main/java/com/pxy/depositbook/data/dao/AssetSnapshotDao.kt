package com.pxy.depositbook.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pxy.depositbook.data.entity.AssetSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetSnapshotDao {

    /** day 有唯一索引，REPLACE 会覆盖当天旧记录（当天取最新值） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: AssetSnapshot)

    @Query("SELECT * FROM asset_snapshot ORDER BY day ASC")
    fun observeAll(): Flow<List<AssetSnapshot>>

    @Query("SELECT * FROM asset_snapshot ORDER BY day DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AssetSnapshot>>

    @Query("SELECT MAX(day) FROM asset_snapshot")
    suspend fun latestDay(): String?

    @Query("SELECT * FROM asset_snapshot ORDER BY day ASC")
    suspend fun getAllOnce(): List<AssetSnapshot>

    @Query("DELETE FROM asset_snapshot")
    suspend fun clearAll()
}
