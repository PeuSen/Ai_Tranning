package com.example.ai_tranning.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val USER_ID_KEY = longPreferencesKey("logged_in_user_id")
    }

    val loggedInUserId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    suspend fun saveUserId(userId: Long) {
        dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
        }
    }
}