package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.TaskDao
import com.example.ai_tranning.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    fun getTasks(projectId: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksByProject(projectId)
    }

    suspend fun getTask(id: Long): TaskEntity? {
        return taskDao.getTaskById(id)
    }

    fun filterByStatus(projectId: Long, status: String): Flow<List<TaskEntity>> {
        return taskDao.filterByStatus(projectId, status)
    }

    fun filterByPriority(projectId: Long, priority: String): Flow<List<TaskEntity>> {
        return taskDao.filterByPriority(projectId, priority)
    }

    fun getTaskCount(projectId: Long): Flow<Int> {
        return taskDao.getTaskCount(projectId)
    }

    fun getCompletedTaskCount(projectId: Long): Flow<Int> {
        return taskDao.getCompletedTaskCount(projectId)
    }

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

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }
}