package com.mtc.app.domain.repository

import com.mtc.app.domain.model.Archive
import com.mtc.app.domain.model.Member

/**
 * 档案 Repository 接口
 */
interface ArchiveRepository {
    suspend fun getArchives(archiveType: String? = null): Result<List<Archive>>
    suspend fun getArchive(archiveId: Int): Result<Archive>
    suspend fun createArchive(name: String, description: String?, archiveType: String): Result<Archive>
    suspend fun updateArchive(archiveId: Int, name: String?, description: String?): Result<Archive>
    suspend fun deleteArchive(archiveId: Int): Result<Unit>

    suspend fun getMembers(archiveId: Int): Result<List<Member>>
    suspend fun getMember(archiveId: Int, memberId: Int): Result<Member>
    suspend fun createMember(archiveId: Int, name: String, relationshipType: String, birthYear: Int?, endYear: Int?, status: String, bio: String?): Result<Member>
    suspend fun updateMember(archiveId: Int, memberId: Int, name: String?, relationshipType: String?, birthYear: Int?, endYear: Int?, status: String?, bio: String?): Result<Member>
    suspend fun uploadMemberAvatar(archiveId: Int, memberId: Int, filename: String, mimeType: String, bytes: ByteArray): Result<Member>
    suspend fun deleteMember(archiveId: Int, memberId: Int): Result<Unit>
}
