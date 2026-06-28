package com.example.ai_tranning.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a task within a project (`tasks` table).
 *
 * A task is the leaf level of the app's hierarchy (User 1:N Project 1:N Task). It is linked to its
 * parent via a [ForeignKey] on [projectId] with `ON DELETE CASCADE`, so deleting a project
 * automatically removes all of its tasks. The [projectId] column is indexed for fast per-project
 * lookups.
 *
 * @property id auto-generated primary key (0 until inserted).
 * @property projectId id of the owning [ProjectEntity]; foreign key with cascade delete.
 * @property title short task title.
 * @property description optional longer description; empty by default.
 * @property status workflow state stored as a string: `"TODO"`, `"IN_PROGRESS"`, or `"DONE"`;
 *   defaults to `"TODO"`.
 * @property priority importance level stored as a string: `"LOW"`, `"MEDIUM"`, or `"HIGH"`;
 *   defaults to `"MEDIUM"`.
 * @property dueDate optional due date in epoch milliseconds; `null` when no date is set.
 * @property isCompleted convenience completion flag, kept in sync with a [status] of `"DONE"`;
 *   used for progress counts and the checkbox UI.
 * @property createdAt creation timestamp in epoch milliseconds; defaults to now.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val description: String = "",
    val status: String = "TODO",       // TODO, IN_PROGRESS, DONE
    val priority: String = "MEDIUM",   // LOW, MEDIUM, HIGH
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)