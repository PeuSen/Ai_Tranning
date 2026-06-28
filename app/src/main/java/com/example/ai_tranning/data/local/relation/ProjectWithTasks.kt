package com.example.ai_tranning.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.entity.TaskEntity

/**
 * Room one-to-many relation pairing a project with all of its tasks.
 *
 * Room populates [tasks] by matching the embedded project's `id` against each task's `projectId`.
 *
 * @property project the embedded parent [ProjectEntity].
 * @property tasks all [TaskEntity] rows belonging to [project].
 */
data class ProjectWithTasks(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val tasks: List<TaskEntity>
)