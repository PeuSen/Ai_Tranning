package com.example.ai_tranning.data.repository

import com.example.ai_tranning.data.local.dao.UserDao
import com.example.ai_tranning.data.local.entity.UserEntity
import com.example.ai_tranning.data.local.relation.UserWithProjects
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {

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

    suspend fun loginUser(username: String, password: String): Result<UserEntity> {
        val user = userDao.login(username, hashPassword(password))
            ?: return Result.failure(Exception("Invalid username or password"))
        return Result.success(user)
    }

    suspend fun getCurrentUser(userId: Long): UserEntity? {
        return userDao.getUserById(userId)
    }

    fun getUserWithProjects(userId: Long): Flow<UserWithProjects?> {
        return userDao.getUserWithProjects(userId)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}