package com.example.ai_tranning.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Process-wide Preferences DataStore, persisted to the `user_prefs` file. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Hilt module that provides the Preferences [DataStore] used for app preferences and the auth
 * session (see `SessionManager`).
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /** Provides the singleton Preferences [DataStore] bound to the application context. */
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}