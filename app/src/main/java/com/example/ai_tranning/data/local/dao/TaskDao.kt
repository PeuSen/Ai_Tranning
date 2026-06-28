package com.example.ai_tranning.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.ai_tranning.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `tasks` table.
 *
 * Exposes reactive reads/counts as [Flow] and one-shot reads/writes as `suspend` functions; Room
 * runs the actual I/O on its own dispatcher.
 */
@Dao
interface TaskDao {

    /**
     * Observes all tasks within a project, newest first.
     *
     * @param projectId id of the owning project.
     * @return a [Flow] emitting the project's tasks (ordered by `createdAt` descending) whenever the
     *   table changes.
     */
    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>>

    /**
     * Fetches a single task by id.
     *
     * @param id task primary key.
     * @return the matching [TaskEntity], or `null` if none exists.
     */
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    /**
     * Observes a project's tasks filtered by workflow status, newest first.
     *
     * @param projectId id of the owning project.
     * @param status status to match (`"TODO"`, `"IN_PROGRESS"`, or `"DONE"`).
     * @return a [Flow] emitting the matching tasks whenever the table changes.
     */
    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND status = :status ORDER BY createdAt DESC")
    fun filterByStatus(projectId: Long, status: String): Flow<List<TaskEntity>>

    /**
     * Observes a project's tasks filtered by priority, newest first.
     *
     * @param projectId id of the owning project.
     * @param priority priority to match (`"LOW"`, `"MEDIUM"`, or `"HIGH"`).
     * @return a [Flow] emitting the matching tasks whenever the table changes.
     */
    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND priority = :priority ORDER BY createdAt DESC")
    fun filterByPriority(projectId: Long, priority: String): Flow<List<TaskEntity>>

    /**
     * Observes the total number of tasks in a project.
     *
     * @param projectId id of the owning project.
     * @return a [Flow] emitting the task count whenever the table changes.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    fun getTaskCount(projectId: Long): Flow<Int>

    /**
     * Observes the number of completed tasks in a project.
     *
     * @param projectId id of the owning project.
     * @return a [Flow] emitting the count of tasks whose `isCompleted` flag is set, whenever the
     *   table changes.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND isCompleted = 1")
    fun getCompletedTaskCount(projectId: Long): Flow<Int>

    /**
     * Inserts a new task.
     *
     * @param task the task to insert.
     * @return the auto-generated row id of the inserted task.
     */
    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    /**
     * Updates an existing task (matched by primary key).
     *
     * @param task the task with updated values.
     */
    @Update
    suspend fun updateTask(task: TaskEntity)

    /**
     * Deletes the given task entity.
     *
     * @param task the task to delete.
     */
    @Delete
    suspend fun deleteTask(task: TaskEntity)

    /**
     * Deletes a task by id.
     *
     * @param taskId id of the task to delete.
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
}