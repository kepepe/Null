package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SrsTaskDao {

    @Query("SELECT * FROM srs_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<SrsTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(task: SrsTaskEntity)

    @Query("DELETE FROM srs_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE srs_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompletion(id: String, isCompleted: Boolean)

    @Query("DELETE FROM srs_tasks")
    suspend fun clearAll()
}
