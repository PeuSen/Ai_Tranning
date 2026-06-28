package com.example.ai_tranning.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the authentication session — i.e. which user is currently logged in — using DataStore.
 *
 * Only the user's id is stored (no token, since the app is offline). The id survives process death
 * and app restarts; `MainActivity` reads [loggedInUserId] on launch to decide whether to start on
 * the dashboard or the login screen. Logging out clears it via [clearSession].
 *
 * @property dataStore the preferences DataStore that backs the session (injected by Hilt).
 */
@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        /** Preference key under which the logged-in user's id is stored. */
        private val USER_ID_KEY = longPreferencesKey("logged_in_user_id")
    }

    /**
     * Reactive stream of the logged-in user's id, emitting `null` when no session is active.
     * Re-emits whenever the session is saved or cleared.
     */
    val loggedInUserId: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    /**
     * Starts (or replaces) the session by persisting [userId].
     *
     * @param userId the id of the user who just logged in or registered.
     */
    suspend fun saveUserId(userId: Long) {
        dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }

    /** Ends the session by removing the stored user id. Safe to call when no session exists. */
    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
        }
    }
}