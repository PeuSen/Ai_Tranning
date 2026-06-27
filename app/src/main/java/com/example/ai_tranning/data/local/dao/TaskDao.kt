package com.example.ai_tranning.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.ai_tranning.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND status = :status ORDER BY createdAt DESC")
    fun filterByStatus(projectId: Long, status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND priority = :priority ORDER BY createdAt DESC")
    fun filterByPriority(projectId: Long, priority: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    fun getTaskCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND isCompleted = 1")
    fun getCompletedTaskCount(projectId: Long): Flow<Int>

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
}