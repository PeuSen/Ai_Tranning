package com.example.ai_tranning.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ai_tranning.data.local.converter.Converters
import com.example.ai_tranning.data.local.dao.ProjectDao
import com.example.ai_tranning.data.local.dao.TaskDao
import com.example.ai_tranning.data.local.dao.UserDao
import com.example.ai_tranning.data.local.entity.ProjectEntity
import com.example.ai_tranning.data.local.entity.TaskEntity
import com.example.ai_tranning.data.local.entity.UserEntity

/**
 * Room database for the app — the local SQLite store that replaces a backend.
 *
 * Hosts the three entities ([UserEntity], [ProjectEntity], [TaskEntity]) that form the
 * User → Project → Task hierarchy, wired together with `ForeignKey.CASCADE` so deleting a parent
 * removes its children. Instantiated as a singleton via Hilt (`DatabaseModule`) with the db file
 * name `ai_tranning_db`.
 *
 * Schema version is `2`; `exportSchema = false`. Bump the version and supply a migration when the
 * schema changes. Version 2 added the nullable `color` column to the `projects` table
 * (see `DatabaseModule.MIGRATION_1_2`).
 */
@Database(
    entities = [UserEntity::class, ProjectEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /** DAO for user/auth queries. */
    abstract fun userDao(): UserDao

    /** DAO for project queries. */
    abstract fun projectDao(): ProjectDao

    /** DAO for task queries. */
    abstract fun taskDao(): TaskDao
}