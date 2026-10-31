package com.pxy.depositbook.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pxy.depositbook.data.entity.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Profile>>

    @Query("SELECT * FROM profile ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllOnce(): List<Profile>

    @Query("SELECT * FROM profile WHERE id = :id")
    suspend fun findById(id: Long): Profile?

    @Query("SELECT * FROM profile WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Profile?

    @Query("SELECT COUNT(*) FROM profile")
    suspend fun count(): Int

    @Insert
    suspend fun insert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)
}
