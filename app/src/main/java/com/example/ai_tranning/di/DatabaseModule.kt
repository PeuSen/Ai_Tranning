package com.example.ai_tranning.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ai_tranning.data.local.AppDatabase
import com.example.ai_tranning.data.local.dao.ProjectDao
import com.example.ai_tranning.data.local.dao.TaskDao
import com.example.ai_tranning.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the Room database and its DAOs to the singleton component.
 *
 * The [AppDatabase] is a `@Singleton` (one instance per process); the DAOs are derived from it and
 * injected wherever a repository needs them.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migrates the schema from version 1 to 2 by adding the nullable `color` column to the
     * `projects` table. Existing rows get `NULL` (no color).
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects ADD COLUMN color TEXT")
        }
    }

    /** Builds the singleton [AppDatabase] backed by the `ai_tranning_db` file. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_tranning_db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    /** Provides the [UserDao] from the database. */
    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    /** Provides the [ProjectDao] from the database. */
    @Provides
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()

    /** Provides the [TaskDao] from the database. */
    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
}