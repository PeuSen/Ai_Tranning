package com.example.ai_tranning.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests for [SessionManager].
 *
 * Rather than mocking DataStore (whose `edit` extension is awkward to fake), these tests run against
 * a real [PreferenceDataStoreFactory] instance backed by a throwaway file in a [TemporaryFolder].
 * A fresh file per test guarantees isolation and avoids the "multiple DataStores active for the same
 * file" error.
 *
 * Coverage:
 *  - Default state (no session) emits `null`.
 *  - A saved user id is persisted and read back.
 *  - Overwriting the user id replaces the previous value.
 *  - Clearing the session returns the state to `null`.
 *  - Clearing when nothing is stored is a safe no-op.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("session_test.preferences_pb") }
        )
        sessionManager = SessionManager(dataStore)
    }

    @Test
    fun `no session stored emits null`() = testScope.runTest {
        assertNull(sessionManager.loggedInUserId.first())
    }

    @Test
    fun `saveUserId persists the id`() = testScope.runTest {
        sessionManager.saveUserId(123L)

        assertEquals(123L, sessionManager.loggedInUserId.first())
    }

    @Test
    fun `saveUserId overwrites a previously stored id`() = testScope.runTest {
        sessionManager.saveUserId(1L)
        sessionManager.saveUserId(2L)

        assertEquals(2L, sessionManager.loggedInUserId.first())
    }

    @Test
    fun `clearSession removes the stored id`() = testScope.runTest {
        sessionManager.saveUserId(42L)
        assertEquals(42L, sessionManager.loggedInUserId.first())

        sessionManager.clearSession()

        assertNull(sessionManager.loggedInUserId.first())
    }

    @Test
    fun `clearSession on an empty store is a no-op`() = testScope.runTest {
        sessionManager.clearSession()

        assertNull(sessionManager.loggedInUserId.first())
    }
}