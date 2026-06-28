package com.example.ai_tranning.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ai_tranning.data.local.dao.UserDao
import com.example.ai_tranning.data.local.entity.UserEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [UserDao] against a real (in-memory) Room database.
 *
 * These run as instrumented tests (`connectedAndroidTest`) because Room needs the Android SQLite
 * runtime. An in-memory database is used so each test starts from a clean slate and nothing is
 * persisted to disk.
 *
 * Coverage:
 *  - Insert returns an auto-generated row id.
 *  - Lookups by id / username / email (hit and miss).
 *  - The login query matches only on the exact username + password-hash pair.
 *  - Username/email lookups are case-sensitive (documents current behaviour).
 */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = db.userDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleUser(
        username: String = "alice",
        email: String = "alice@example.com",
        passwordHash: String = "hash"
    ) = UserEntity(username = username, email = email, passwordHash = passwordHash)

    @Test
    fun insertUser_returnsGeneratedId() = runTest {
        val id = userDao.insertUser(sampleUser())
        assertEquals(1L, id)
    }

    @Test
    fun getUserById_returnsInsertedUser() = runTest {
        val id = userDao.insertUser(sampleUser())

        val loaded = userDao.getUserById(id)

        assertNotNull(loaded)
        assertEquals("alice", loaded?.username)
        assertEquals("alice@example.com", loaded?.email)
    }

    @Test
    fun getUserById_returnsNullForUnknownId() = runTest {
        assertNull(userDao.getUserById(999))
    }

    @Test
    fun getUserByUsername_findsMatch() = runTest {
        userDao.insertUser(sampleUser(username = "bob"))

        assertNotNull(userDao.getUserByUsername("bob"))
        assertNull(userDao.getUserByUsername("nobody"))
    }

    @Test
    fun getUserByEmail_findsMatch() = runTest {
        userDao.insertUser(sampleUser(email = "bob@example.com"))

        assertNotNull(userDao.getUserByEmail("bob@example.com"))
        assertNull(userDao.getUserByEmail("missing@example.com"))
    }

    @Test
    fun login_succeedsWithMatchingUsernameAndHash() = runTest {
        userDao.insertUser(sampleUser(username = "alice", passwordHash = "correct-hash"))

        val user = userDao.login("alice", "correct-hash")

        assertNotNull(user)
        assertEquals("alice", user?.username)
    }

    @Test
    fun login_failsWithWrongHash() = runTest {
        userDao.insertUser(sampleUser(username = "alice", passwordHash = "correct-hash"))

        assertNull(userDao.login("alice", "wrong-hash"))
    }

    @Test
    fun login_failsWithWrongUsername() = runTest {
        userDao.insertUser(sampleUser(username = "alice", passwordHash = "correct-hash"))

        assertNull(userDao.login("mallory", "correct-hash"))
    }

    @Test
    fun lookups_areCaseSensitive() = runTest {
        userDao.insertUser(sampleUser(username = "Alice", email = "Alice@example.com"))

        // Exact case matches; differing case does not (default SQLite BINARY collation).
        assertNotNull(userDao.getUserByUsername("Alice"))
        assertNull(userDao.getUserByUsername("alice"))
    }

    @Test
    fun multipleUsers_areStoredIndependently() = runTest {
        val id1 = userDao.insertUser(sampleUser(username = "u1", email = "u1@x.com"))
        val id2 = userDao.insertUser(sampleUser(username = "u2", email = "u2@x.com"))

        assertEquals("u1", userDao.getUserById(id1)?.username)
        assertEquals("u2", userDao.getUserById(id2)?.username)
    }
}