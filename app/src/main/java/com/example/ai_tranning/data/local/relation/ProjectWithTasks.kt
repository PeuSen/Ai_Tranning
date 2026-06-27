package com.example.ai_tranning.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.entity.TaskEntity

data class ProjectWithTasks(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val tasks: List<TaskEntity>
)