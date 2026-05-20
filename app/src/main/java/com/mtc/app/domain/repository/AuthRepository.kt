package com.mtc.app.domain.repository

import com.mtc.app.domain.model.User

/**
 * 认证 Repository 接口
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, username: String, password: String): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun uploadAvatar(filename: String, mimeType: String, bytes: ByteArray): Result<User>
    suspend fun deleteAvatar(): Result<User>
}
