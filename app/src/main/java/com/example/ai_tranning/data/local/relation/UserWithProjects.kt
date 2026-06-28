package com.example.ai_tranning.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.entity.UserEntity

/**
 * Room one-to-many relation pairing a user with all of their projects.
 *
 * Room populates [projects] by matching the embedded user's `id` against each project's `userId`.
 *
 * @property user the embedded parent [UserEntity].
 * @property projects all [ProjectEntity] rows belonging to [user].
 */
data class UserWithProjects(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val projects: List<ProjectEntity>
)