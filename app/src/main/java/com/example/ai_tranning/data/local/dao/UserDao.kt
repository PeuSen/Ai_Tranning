package com.example.ai_tranning.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.ai_tranning.data.local.entity.UserEntity
import com.example.ai_tranning.data.local.relation.UserWithProjects
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for the `users` table.
 *
 * Defines the SQL contract behind the authentication module. Consumed exclusively by
 * `UserRepository`; other layers should not reference it directly. All single-shot reads/writes are
 * `suspend`; the projection query exposes a reactive [Flow].
 *
 * Note: string comparisons use SQLite's default `BINARY` collation, so username/email lookups are
 * **case-sensitive**.
 */
@Dao
interface UserDao {

    /**
     * Inserts a new user row.
     *
     * @param user the user to insert; its `id` is auto-generated and may be left at its default.
     * @return the auto-generated primary key of the inserted row.
     */
    @Insert
    suspend fun insertUser(user: UserEntity): Long

    /**
     * Looks up a user by exact username **and** password hash — the credential check used at login.
     *
     * @param username the username to match.
     * @param passwordHash the SHA-256 hash to match (callers must hash before calling).
     * @return the matching [UserEntity], or `null` if the pair does not match any row.
     */
    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(username: String, passwordHash: String): UserEntity?

    /**
     * @param id primary key to look up.
     * @return the user with that id, or `null` if none exists.
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    /**
     * Used by registration to enforce username uniqueness.
     *
     * @param username username to look up.
     * @return the matching user, or `null` if the username is free.
     */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    /**
     * Used by registration to enforce email uniqueness.
     *
     * @param email email to look up.
     * @return the matching user, or `null` if the email is free.
     */
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Observes a user and their related projects as a single graph.
     *
     * `@Transaction` guarantees the user row and its projects are read consistently.
     *
     * @param userId the user's primary key.
     * @return a [Flow] of [UserWithProjects] (or `null`) that re-emits on relevant table changes.
     */
    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserWithProjects(userId: Long): Flow<UserWithProjects?>
}