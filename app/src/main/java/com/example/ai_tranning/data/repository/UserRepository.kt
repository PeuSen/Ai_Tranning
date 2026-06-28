package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.UserDao
import com.example.ai_tranning.data.local.entity.UserEntity
import com.example.ai_tranning.data.local.relation.UserWithProjects
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for authentication and user data.
 *
 * In this fully-offline app the repository plays the role a backend/auth service would normally
 * fill: it owns credential checking, password hashing, and uniqueness rules, delegating all
 * persistence to [UserDao] (Room). ViewModels depend on this class, never on the DAO directly.
 *
 * ### Password storage
 * Passwords are hashed with **SHA-256** before they ever reach the database; the plaintext password
 * is never stored. The same hashing function is used on both registration and login so credentials
 * round-trip correctly.
 *
 * > ⚠️ **Security note:** SHA-256 is a *fast, unsalted* digest. For an offline, single-device demo
 * > app that is an acceptable trade-off, but it is **not** suitable for a production system handling
 * > real credentials — there a slow, salted KDF such as **bcrypt**, **scrypt**, or **Argon2id**
 * > should be used instead. See `docs/AUTHENTICATION.md` for the full rationale.
 *
 * All methods are `suspend` (or return a [Flow]) and are safe to call from a coroutine; Room runs
 * the actual I/O on its own dispatcher.
 *
 * @property userDao Room DAO used for all user persistence and queries.
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {

    /**
     * Registers a new user.
     *
     * Enforces that both [username] and [email] are unique (username is checked first). The password
     * is hashed via [hashPassword] before being persisted. No format validation is performed here —
     * input validation (non-blank, password length, confirmation match) is the ViewModel's
     * responsibility.
     *
     * @param username desired unique username.
     * @param email desired unique email address.
     * @param password plaintext password; hashed before storage, never persisted as-is.
     * @return [Result.success] with the new row id on success, or [Result.failure] with a message of
     *   `"Username already exists"` / `"Email already exists"` if a uniqueness check fails.
     */
    suspend fun registerUser(username: String, email: String, password: String): Result<Long> {
        if (userDao.getUserByUsername(username) != null) {
            return Result.failure(Exception("Username already exists"))
        }
        if (userDao.getUserByEmail(email) != null) {
            return Result.failure(Exception("Email already exists"))
        }
        val user = UserEntity(
            username = username,
            email = email,
            passwordHash = hashPassword(password)
        )
        val id = userDao.insertUser(user)
        return Result.success(id)
    }

    /**
     * Authenticates a user by username and password.
     *
     * The supplied [password] is hashed and compared against the stored hash inside a single SQL
     * query ([UserDao.login]), so the plaintext password never leaves this method. To avoid leaking
     * which of the two was wrong, an unknown username and a wrong password return the **same**
     * generic error message.
     *
     * @param username the username to authenticate.
     * @param password the plaintext password to verify.
     * @return [Result.success] with the matching [UserEntity], or [Result.failure] with
     *   `"Invalid username or password"` if no user matches.
     */
    suspend fun loginUser(username: String, password: String): Result<UserEntity> {
        val user = userDao.login(username, hashPassword(password))
            ?: return Result.failure(Exception("Invalid username or password"))
        return Result.success(user)
    }

    /**
     * Loads the currently logged-in user by id (typically the id held by `SessionManager`).
     *
     * @param userId the user's primary key.
     * @return the [UserEntity], or `null` if no user has that id.
     */
    suspend fun getCurrentUser(userId: Long): UserEntity? {
        return userDao.getUserById(userId)
    }

    /**
     * Observes a user together with all of their projects as a reactive stream.
     *
     * @param userId the user's primary key.
     * @return a [Flow] emitting the latest [UserWithProjects] (or `null` if the user does not exist)
     *   whenever the underlying tables change.
     */
    fun getUserWithProjects(userId: Long): Flow<UserWithProjects?> {
        return userDao.getUserWithProjects(userId)
    }

    /**
     * Computes the lowercase hex-encoded SHA-256 digest of [password] (UTF-8 bytes).
     *
     * Deterministic: the same input always yields the same 64-character hex string, which is what
     * makes hash-based login comparisons work.
     */
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}