package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.ProjectDao
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.relation.ProjectWithTasks
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for project data.
 *
 * In this fully-offline app the repository replaces a backend service: it owns project persistence,
 * delegating all I/O to [ProjectDao] (Room). ViewModels depend on this class, never on the DAO
 * directly. Reactive reads return a [Flow]; one-shot reads/writes are `suspend`.
 *
 * @property projectDao Room DAO used for all project persistence and queries.
 */
@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {

    /**
     * Observes all projects owned by a user.
     *
     * @param userId id of the owning user.
     * @return a [Flow] of the user's projects, newest first.
     */
    fun getProjects(userId: Long): Flow<List<ProjectEntity>> {
        return projectDao.getProjectsByUser(userId)
    }

    /**
     * Fetches a single project by id, enforcing ownership.
     *
     * Equivalent to a `GET /projects/{id}` guarded by ownership: a project that does not exist and a
     * project owned by another user are treated identically (both return `null`), so a user can never
     * view — or even confirm the existence of — another user's project.
     *
     * @param userId id of the user requesting the project.
     * @param id project primary key.
     * @return the matching [ProjectEntity], or `null` if it does not exist or the user does not own it.
     */
    suspend fun getProject(userId: Long, id: Long): ProjectEntity? {
        return projectDao.getProjectByIdForUser(id, userId)
    }

    /**
     * Observes a project together with all of its tasks.
     *
     * @param projectId project primary key.
     * @return a [Flow] of the [ProjectWithTasks] aggregate, or `null` if the project does not exist.
     */
    fun getProjectWithTasks(projectId: Long): Flow<ProjectWithTasks?> {
        return projectDao.getProjectWithTasks(projectId)
    }

    /**
     * Creates and persists a new project for a user.
     *
     * @param userId id of the owning user.
     * @param name project name.
     * @param description project description.
     * @param color optional display color (e.g. a hex string); `null` when unset.
     * @return the auto-generated row id of the new project.
     */
    suspend fun createProject(
        userId: Long,
        name: String,
        description: String,
        color: String? = null
    ): Long {
        val project = ProjectEntity(
            userId = userId,
            name = name,
            description = description,
            color = color
        )
        return projectDao.insertProject(project)
    }

    /**
     * Partially updates a project, enforcing ownership — the equivalent of `PATCH /projects/{id}`.
     *
     * Any subset of [name], [description] and [color] may be supplied; a `null` argument leaves that
     * field unchanged. The update is applied only if [userId] owns the project; otherwise nothing is
     * written and `false` is returned (the caller cannot edit another user's project, and a missing
     * project is indistinguishable from one owned by someone else).
     *
     * @param userId id of the user requesting the update.
     * @param id id of the project to update.
     * @param name new name, or `null` to leave it unchanged.
     * @param description new description, or `null` to leave it unchanged.
     * @param color new color, or `null` to leave it unchanged.
     * @return `true` if the project was found, owned by the user, and updated; `false` otherwise.
     */
    suspend fun updateProject(
        userId: Long,
        id: Long,
        name: String? = null,
        description: String? = null,
        color: String? = null
    ): Boolean {
        val existing = projectDao.getProjectByIdForUser(id, userId) ?: return false
        projectDao.updateProject(
            existing.copy(
                name = name ?: existing.name,
                description = description ?: existing.description,
                color = color ?: existing.color
            )
        )
        return true
    }

    /**
     * Deletes a project, enforcing ownership — the equivalent of `DELETE /projects/{id}`.
     *
     * The project is deleted only if [userId] owns it; its tasks are removed via cascade delete. A
     * missing project and a project owned by another user are treated identically (both return
     * `false`), so a user can never delete another user's project.
     *
     * @param userId id of the user requesting the deletion.
     * @param id id of the project to delete.
     * @return `true` if a project owned by the user was deleted; `false` otherwise.
     */
    suspend fun deleteProject(userId: Long, id: Long): Boolean {
        return projectDao.deleteProjectByIdForUser(id, userId) > 0
    }
}