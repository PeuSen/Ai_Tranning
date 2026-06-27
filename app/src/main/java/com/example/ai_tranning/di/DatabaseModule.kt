package com.example.ai_tranning.di

import android.content.Context
import androidx.room.Room
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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai_tranning_db"
        ).build()
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
}