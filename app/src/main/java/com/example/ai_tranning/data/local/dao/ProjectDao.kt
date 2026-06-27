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

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY createdAt DESC")
    fun getProjectsByUser(userId: Long): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectWithTasks(projectId: Long): Flow<ProjectWithTasks?>

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)
}