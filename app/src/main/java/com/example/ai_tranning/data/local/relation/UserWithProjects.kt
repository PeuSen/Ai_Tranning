package com.example.ai_tranning.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.entity.UserEntity

data class UserWithProjects(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val projects: List<ProjectEntity>
)