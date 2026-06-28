package com.example.ai_tranning.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.relation.ProjectWithTasks
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `projects` table.
 *
 * Exposes reactive reads as [Flow] and one-shot reads/writes as `suspend` functions; Room runs the
 * actual I/O on its own dispatcher.
 */
@Dao
interface ProjectDao {

    /**
     * Observes all projects belonging to a user, newest first.
     *
     * @param userId id of the owning user.
     * @return a [Flow] emitting the user's projects (ordered by `createdAt` descending) whenever the
     *   table changes.
     */
    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY createdAt DESC")
    fun getProjectsByUser(userId: Long): Flow<List<ProjectEntity>>

    /**
     * Fetches a single project by id.
     *
     * @param id project primary key.
     * @return the matching [ProjectEntity], or `null` if none exists.
     */
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    /**
     * Fetches a single project by id **only if it is owned by the given user**.
     *
     * This is the ownership-enforcing read: a non-existent project and a project owned by another
     * user are indistinguishable (both return `null`), so callers cannot probe for projects they do
     * not own.
     *
     * @param id project primary key.
     * @param userId id of the user that must own the project.
     * @return the matching [ProjectEntity], or `null` if it does not exist or is owned by someone else.
     */
    @Query("SELECT * FROM projects WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getProjectByIdForUser(id: Long, userId: Long): ProjectEntity?

    /**
     * Observes a project together with all of its tasks.
     *
     * @param projectId project primary key.
     * @return a [Flow] emitting the [ProjectWithTasks] aggregate (or `null` if the project does not
     *   exist) whenever the project or its tasks change.
     */
    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectWithTasks(projectId: Long): Flow<ProjectWithTasks?>

    /**
     * Inserts a new project.
     *
     * @param project the project to insert.
     * @return the auto-generated row id of the inserted project.
     */
    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    /**
     * Updates an existing project (matched by primary key).
     *
     * @param project the project with updated values.
     */
    @Update
    suspend fun updateProject(project: ProjectEntity)

    /**
     * Deletes the given project entity (cascades to its tasks).
     *
     * @param project the project to delete.
     */
    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    /**
     * Deletes a project by id (cascades to its tasks).
     *
     * @param projectId id of the project to delete.
     */
    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    /**
     * Deletes a project by id **only if it is owned by the given user** (cascades to its tasks).
     *
     * @param projectId id of the project to delete.
     * @param userId id of the user that must own the project.
     * @return the number of rows deleted: `1` if the user owned the project, `0` otherwise.
     */
    @Query("DELETE FROM projects WHERE id = :projectId AND userId = :userId")
    suspend fun deleteProjectByIdForUser(projectId: Long, userId: Long): Int
}