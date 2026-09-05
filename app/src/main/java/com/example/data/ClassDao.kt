package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM schedule_classes ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM schedule_classes ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllClassesSync(): List<ClassEntity>

    @Query("SELECT * FROM schedule_classes WHERE id = :id LIMIT 1")
    suspend fun getClassById(id: String): ClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(classEntity: ClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classEntities: List<ClassEntity>)

    @Query("DELETE FROM schedule_classes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM schedule_classes")
    suspend fun clearAll()
}
