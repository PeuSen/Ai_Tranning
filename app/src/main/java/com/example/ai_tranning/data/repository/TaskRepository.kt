package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.TaskDao
import com.example.ai_tranning.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for task data.
 *
 * In this fully-offline app the repository replaces a backend service: it owns task persistence,
 * filtering, and progress counts, delegating all I/O to [TaskDao] (Room). ViewModels depend on this
 * class, never on the DAO directly. Reactive reads return a [Flow]; one-shot reads/writes are
 * `suspend`.
 *
 * @property taskDao Room DAO used for all task persistence and queries.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    /**
     * Observes all tasks in a project.
     *
     * @param projectId id of the owning project.
     * @return a [Flow] of the project's tasks, newest first.
     */
    fun getTasks(projectId: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksByProject(projectId)
    }

    /**
     * Fetches a single task by id.
     *
     * @param id task primary key.
     * @return the matching [TaskEntity], or `null` if none exists.
     */
    suspend fun getTask(id: Long): TaskEntity? {
        return taskDao.getTaskById(id)
    }

    /**
     * Observes a project's tasks filtered by workflow status.
     *
     * @param projectId id of the owning project.
     * @param status status to match (`"TODO"`, `"IN_PROGRESS"`, or `"DONE"`).
     * @return a [Flow] of the matching tasks.
     */
    fun filterByStatus(projectId: Long, status: String): Flow<List<TaskEntity>> {
        return taskDao.filterByStatus(projectId, status)
    }

    /**
     * Observes a project's tasks filtered by priority.
     *
     * @param projectId id of the owning project.
     * @param priority priority to match (`"LOW"`, `"MEDIUM"`, or `"HIGH"`).
     * @return a [Flow] of the matching tasks.
     */
    fun filterByPriority(projectId: Long, priority: String): Flow<List<TaskEntity>> {
        return taskDao.filterByPriority(projectId, priority)
    }

    /**
     * Observes the total number of tasks in a project.
     *
     * @param projectId id of the owning project.
     * @return a [Flow] of the task count.
     */
    fun getTaskCount(projectId: Long): Flow<Int> {
        return taskDao.getTaskCount(projectId)
    }

    /**
     * Observes the number of completed tasks in a project.
     *
     * @param projectId id of the owning project.
     * @return a [Flow] of the completed-task count.
     */
    fun getCompletedTaskCount(projectId: Long): Flow<Int> {
        return taskDao.getCompletedTaskCount(projectId)
    }

    /**
     * Creates and persists a new task in a project.
     *
     * @param projectId id of the owning project.
     * @param title task title.
     * @param description optional description; empty by default.
     * @param priority priority level; defaults to `"MEDIUM"`.
     * @param dueDate optional due date in epoch milliseconds; `null` by default.
     * @return the auto-generated row id of the new task.
     */
    suspend fun createTask(
        projectId: Long,
        title: String,
        description: String = "",
        priority: String = "MEDIUM",
        dueDate: Long? = null
    ): Long {
        val task = TaskEntity(
            projectId = projectId,
            title = title,
            description = description,
            priority = priority,
            dueDate = dueDate
        )
        return taskDao.insertTask(task)
    }

    /**
     * Updates an existing task.
     *
     * @param task the task with updated values.
     */
    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    /**
     * Deletes a task by id.
     *
     * @param taskId id of the task to delete.
     */
    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }
}