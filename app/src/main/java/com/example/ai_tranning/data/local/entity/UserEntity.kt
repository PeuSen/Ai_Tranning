package com.example.ai_tranning.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the `users` table — the root of the User → Project → Task hierarchy.
 *
 * Security: only the [passwordHash] (SHA-256) is stored; the plaintext password is never persisted.
 *
 * @property id auto-generated primary key (`0` until inserted).
 * @property username unique login name (treated case-sensitively by lookups).
 * @property email unique email address.
 * @property passwordHash hex-encoded SHA-256 digest of the user's password.
 * @property createdAt epoch-millis timestamp of account creation; defaults to "now".
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)