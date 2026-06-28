package com.example.ai_tranning.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ai_tranning.data.local.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end integration tests for [UserRepository] wired to a real (in-memory) Room database.
 *
 * Unlike [UserRepositoryTest] (which mocks the DAO), these tests verify the full authentication
 * slice — repository logic + SHA-256 hashing + Room persistence + SQL queries — works together.
 *
 * Coverage:
 *  - Register then log in with the same credentials (the critical happy path).
 *  - Login fails with the wrong password even for a real, registered user.
 *  - Duplicate username / email are rejected end-to-end.
 *  - The persisted password hash is never the plaintext password.
 */
@RunWith(AndroidJUnit4::class)
class UserRepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = UserRepository(db.userDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun registerThenLogin_succeeds() = runTest {
        val registerResult = repository.registerUser("alice", "alice@example.com", "password123")
        assertTrue(registerResult.isSuccess)

        val loginResult = repository.loginUser("alice", "password123")

        assertTrue(loginResult.isSuccess)
        assertEquals("alice", loginResult.getOrNull()?.username)
        assertEquals(registerResult.getOrNull(), loginResult.getOrNull()?.id)
    }

    @Test
    fun login_failsWithWrongPasswordForRealUser() = runTest {
        repository.registerUser("alice", "alice@example.com", "password123")

        val loginResult = repository.loginUser("alice", "wrong-password")

        assertTrue(loginResult.isFailure)
        assertEquals("Invalid username or password", loginResult.exceptionOrNull()?.message)
    }

    @Test
    fun register_rejectsDuplicateUsername() = runTest {
        repository.registerUser("alice", "alice@example.com", "password123")

        val second = repository.registerUser("alice", "different@example.com", "password123")

        assertTrue(second.isFailure)
        assertEquals("Username already exists", second.exceptionOrNull()?.message)
    }

    @Test
    fun register_rejectsDuplicateEmail() = runTest {
        repository.registerUser("alice", "shared@example.com", "password123")

        val second = repository.registerUser("bob", "shared@example.com", "password123")

        assertTrue(second.isFailure)
        assertEquals("Email already exists", second.exceptionOrNull()?.message)
    }

    @Test
    fun persistedPasswordHash_isNotPlaintext() = runTest {
        val id = repository.registerUser("alice", "alice@example.com", "password123").getOrThrow()

        val stored = db.userDao().getUserById(id)

        assertNotEquals("password123", stored?.passwordHash)
        assertEquals(64, stored?.passwordHash?.length) // SHA-256 hex digest
    }

    @Test
    fun getCurrentUser_returnsRegisteredUser() = runTest {
        val id = repository.registerUser("alice", "alice@example.com", "password123").getOrThrow()

        val user = repository.getCurrentUser(id)

        assertEquals("alice", user?.username)
        assertFalse(user?.email.isNullOrBlank())
    }
}