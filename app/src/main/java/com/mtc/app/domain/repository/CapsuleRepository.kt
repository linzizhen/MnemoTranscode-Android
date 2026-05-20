package com.mtc.app.domain.repository

import com.mtc.app.domain.model.Capsule

/**
 * 记忆胶囊 Repository 接口
 */
interface CapsuleRepository {
    suspend fun getCapsules(memberId: Int? = null): Result<List<Capsule>>
    suspend fun getCapsule(capsuleId: Int): Result<Capsule>
    suspend fun createCapsule(memberId: Int, title: String, content: String, unlockDate: String, recipients: List<Int>? = null): Result<Capsule>
    suspend fun forceUnlockCapsule(capsuleId: Int): Result<Capsule>
}
