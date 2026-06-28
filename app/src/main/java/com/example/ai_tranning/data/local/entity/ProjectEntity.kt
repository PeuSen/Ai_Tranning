package com.example.ai_tranning.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a project owned by a user (`projects` table).
 *
 * A project is the middle level of the app's hierarchy (User 1:N Project 1:N Task). It is linked to
 * its owner via a [ForeignKey] on [userId] with `ON DELETE CASCADE`, so deleting a user
 * automatically removes all of their projects (and, transitively, their tasks). The [userId] column
 * is indexed to keep per-user lookups fast.
 *
 * @property id auto-generated primary key (0 until inserted).
 * @property userId id of the owning [UserEntity]; foreign key with cascade delete.
 * @property name human-readable project name.
 * @property description optional longer description; empty by default.
 * @property color optional display color (e.g. a hex string like `#FF6200EE`); `null` when unset.
 * @property createdAt creation timestamp in epoch milliseconds; defaults to now.
 */
@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val description: String = "",
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)