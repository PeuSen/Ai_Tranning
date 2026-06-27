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

@Database(
    entities = [UserEntity::class, ProjectEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
}