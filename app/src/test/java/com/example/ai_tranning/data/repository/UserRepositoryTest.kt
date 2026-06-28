package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.UserDao
import com.example.ai_tranning.data.local.entity.UserEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

/**
 * Unit tests for [UserRepository] — the core of the authentication module.
 *
 * The repository's only collaborator, [UserDao], is mocked with MockK so these tests exercise the
 * repository's own logic in isolation (duplicate detection, password hashing, result wrapping)
 * without touching a real database.
 *
 * Coverage:
 *  - Positive: successful registration and login.
 *  - Negative: duplicate username/email, wrong credentials, unknown user.
 *  - Edge cases: blank input is accepted by the repository (validation lives in the ViewModel layer).
 *  - Security: passwords are SHA-256 hashed (never stored or queried in plaintext), hashing is
 *    deterministic, and distinct passwords produce distinct hashes.
 */
class UserRepositoryTest {

    private lateinit var userDao: UserDao
    private lateinit var repository: UserRepository

    /** SHA-256 reference implementation used to assert what the repository *should* produce. */
    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    @Before
    fun setUp() {
        userDao = mockk()
        repository = UserRepository(userDao)
    }

    // ---------------------------------------------------------------------------------------------
    // registerUser — positive
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `registerUser succeeds when username and email are free`() = runTest {
        coEvery { userDao.getUserByUsername("alice") } returns null
        coEvery { userDao.getUserByEmail("alice@example.com") } returns null
        coEvery { userDao.insertUser(any()) } returns 42L

        val result = repository.registerUser("alice", "alice@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
    }

    @Test
    fun `registerUser stores the SHA-256 hash and never the plaintext password`() = runTest {
        val captured = slot<UserEntity>()
        coEvery { userDao.getUserByUsername(any()) } returns null
        coEvery { userDao.getUserByEmail(any()) } returns null
        coEvery { userDao.insertUser(capture(captured)) } returns 1L

        repository.registerUser("bob", "bob@example.com", "s3cr3t!")

        val stored = captured.captured
        assertNotEquals("s3cr3t!", stored.passwordHash)
        assertEquals(sha256("s3cr3t!"), stored.passwordHash)
        assertEquals(64, stored.passwordHash.length) // SHA-256 hex digest length
        assertTrue(stored.passwordHash.all { it in "0123456789abcdef" })
    }

    // ---------------------------------------------------------------------------------------------
    // registerUser — negative
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `registerUser fails when username already exists`() = runTest {
        coEvery { userDao.getUserByUsername("alice") } returns
            UserEntity(id = 1, username = "alice", email = "a@x.com", passwordHash = "h")

        val result = repository.registerUser("alice", "new@example.com", "password123")

        assertTrue(result.isFailure)
        assertEquals("Username already exists", result.exceptionOrNull()?.message)
        // Must short-circuit before inserting.
        coVerify(exactly = 0) { userDao.insertUser(any()) }
    }

    @Test
    fun `registerUser fails when email already exists`() = runTest {
        coEvery { userDao.getUserByUsername("alice") } returns null
        coEvery { userDao.getUserByEmail("taken@example.com") } returns
            UserEntity(id = 2, username = "other", email = "taken@example.com", passwordHash = "h")

        val result = repository.registerUser("alice", "taken@example.com", "password123")

        assertTrue(result.isFailure)
        assertEquals("Email already exists", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { userDao.insertUser(any()) }
    }

    @Test
    fun `registerUser checks username before email`() = runTest {
        // Both are taken; the username error must win because it is checked first.
        coEvery { userDao.getUserByUsername(any()) } returns
            UserEntity(id = 1, username = "alice", email = "a@x.com", passwordHash = "h")
        coEvery { userDao.getUserByEmail(any()) } returns
            UserEntity(id = 2, username = "other", email = "taken@example.com", passwordHash = "h")

        val result = repository.registerUser("alice", "taken@example.com", "password123")

        assertEquals("Username already exists", result.exceptionOrNull()?.message)
    }

    // ---------------------------------------------------------------------------------------------
    // loginUser
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `loginUser succeeds and queries the DAO with the hashed password`() = runTest {
        val user = UserEntity(id = 7, username = "alice", email = "a@x.com", passwordHash = sha256("pw"))
        val passwordSlot = slot<String>()
        coEvery { userDao.login(eq("alice"), capture(passwordSlot)) } returns user

        val result = repository.loginUser("alice", "pw")

        assertTrue(result.isSuccess)
        assertEquals(user, result.getOrNull())
        // The DAO must be queried with the hash, not the raw password.
        assertEquals(sha256("pw"), passwordSlot.captured)
        assertNotEquals("pw", passwordSlot.captured)
    }

    @Test
    fun `loginUser fails for wrong credentials`() = runTest {
        coEvery { userDao.login(any(), any()) } returns null

        val result = repository.loginUser("alice", "wrong-password")

        assertTrue(result.isFailure)
        assertEquals("Invalid username or password", result.exceptionOrNull()?.message)
    }

    @Test
    fun `loginUser fails for unknown user`() = runTest {
        coEvery { userDao.login("ghost", any()) } returns null

        val result = repository.loginUser("ghost", "whatever")

        assertTrue(result.isFailure)
        assertEquals("Invalid username or password", result.exceptionOrNull()?.message)
    }

    // ---------------------------------------------------------------------------------------------
    // getCurrentUser
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `getCurrentUser returns the user for a known id`() = runTest {
        val user = UserEntity(id = 5, username = "alice", email = "a@x.com", passwordHash = "h")
        coEvery { userDao.getUserById(5) } returns user

        assertEquals(user, repository.getCurrentUser(5))
    }

    @Test
    fun `getCurrentUser returns null for an unknown id`() = runTest {
        coEvery { userDao.getUserById(999) } returns null

        assertEquals(null, repository.getCurrentUser(999))
    }

    // ---------------------------------------------------------------------------------------------
    // Password hashing properties (security)
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `hashing is deterministic - same password yields same hash`() = runTest {
        val hashes = mutableListOf<String>()
        coEvery { userDao.getUserByUsername(any()) } returns null
        coEvery { userDao.getUserByEmail(any()) } returns null
        coEvery { userDao.insertUser(any()) } answers {
            hashes.add(firstArg<UserEntity>().passwordHash)
            hashes.size.toLong()
        }

        repository.registerUser("u1", "u1@x.com", "samePassword")
        repository.registerUser("u2", "u2@x.com", "samePassword")

        assertEquals(2, hashes.size)
        assertEquals(hashes[0], hashes[1])
    }

    @Test
    fun `different passwords yield different hashes`() = runTest {
        val hashes = mutableListOf<String>()
        coEvery { userDao.getUserByUsername(any()) } returns null
        coEvery { userDao.getUserByEmail(any()) } returns null
        coEvery { userDao.insertUser(any()) } answers {
            hashes.add(firstArg<UserEntity>().passwordHash)
            hashes.size.toLong()
        }

        repository.registerUser("u1", "u1@x.com", "passwordA")
        repository.registerUser("u2", "u2@x.com", "passwordB")

        assertNotEquals(hashes[0], hashes[1])
    }

    @Test
    fun `registration and login use the same hashing so credentials round-trip`() = runTest {
        // Capture what registration stores, then assert login would query with the identical hash.
        val stored = slot<UserEntity>()
        coEvery { userDao.getUserByUsername(any()) } returns null
        coEvery { userDao.getUserByEmail(any()) } returns null
        coEvery { userDao.insertUser(capture(stored)) } returns 1L
        repository.registerUser("alice", "a@x.com", "round-trip-pw")

        val loginHash = slot<String>()
        coEvery { userDao.login(eq("alice"), capture(loginHash)) } returns
            stored.captured.copy(id = 1)
        val result = repository.loginUser("alice", "round-trip-pw")

        assertTrue(result.isSuccess)
        assertEquals(stored.captured.passwordHash, loginHash.captured)
    }

    // ---------------------------------------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `registerUser hashes empty password without throwing`() = runTest {
        // The repository performs no validation; it must still hash an empty password safely.
        val stored = slot<UserEntity>()
        coEvery { userDao.getUserByUsername(any()) } returns null
        coEvery { userDao.getUserByEmail(any()) } returns null
        coEvery { userDao.insertUser(capture(stored)) } returns 1L

        val result = repository.registerUser("alice", "a@x.com", "")

        assertTrue(result.isSuccess)
        assertEquals(sha256(""), stored.captured.passwordHash)
    }

    @Test
    fun `registerUser handles unicode passwords using UTF-8 bytes`() = runTest {
        val stored = slot<UserEntity>()
        coEvery { userDao.getUserByUsername(any()) } returns null
        coEvery { userDao.getUserByEmail(any()) } returns null
        coEvery { userDao.insertUser(capture(stored)) } returns 1L

        repository.registerUser("alice", "a@x.com", "pässwörd-café-🔐")

        assertEquals(sha256("pässwörd-café-🔐"), stored.captured.passwordHash)
    }
}