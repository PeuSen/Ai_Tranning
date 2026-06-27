package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.ProjectDao
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.relation.ProjectWithTasks
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {

    fun getProjects(userId: Long): Flow<List<ProjectEntity>> {
        return projectDao.getProjectsByUser(userId)
    }

    suspend fun getProject(id: Long): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    fun getProjectWithTasks(projectId: Long): Flow<ProjectWithTasks?> {
        return projectDao.getProjectWithTasks(projectId)
    }

    suspend fun createProject(userId: Long, name: String, description: String): Long {
        val project = ProjectEntity(
            userId = userId,
            name = name,
            description = description
        )
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }
}