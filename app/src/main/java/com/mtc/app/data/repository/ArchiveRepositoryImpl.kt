package com.mtc.app.data.repository

import com.mtc.app.data.local.*
import com.mtc.app.data.model.*
import com.mtc.app.data.remote.MtcApiService
import com.mtc.app.domain.model.*
import com.mtc.app.domain.repository.ArchiveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 档案 Repository 实现
 */
@Singleton
class ArchiveRepositoryImpl @Inject constructor(
    private val apiService: MtcApiService,
    private val archiveDao: ArchiveDao,
    private val memberDao: MemberDao,
    private val memoryDao: MemoryDao
) : ArchiveRepository {

    override suspend fun getArchives(archiveType: String?): Result<List<Archive>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getArchives(archiveType)
            if (response.isSuccessful) {
                val archives = response.body()!!.map { it.toDomain() }
                archiveDao.insertArchives(response.body()!!.map { it.toEntity() })
                Result.success(archives)
            } else {
                val cached = archiveDao.getAllArchives().map { it.toDomain() }
                if (cached.isNotEmpty()) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("获取档案列表失败"))
                }
            }
        } catch (e: Exception) {
            val cached = archiveDao.getAllArchives().map { it.toDomain() }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getArchive(archiveId: Int): Result<Archive> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getArchive(archiveId)
            if (response.isSuccessful) {
                val archive = response.body()!!.toDomain()
                archiveDao.insertArchive(response.body()!!.toEntity())
                Result.success(archive)
            } else {
                val cached = archiveDao.getArchiveById(archiveId)?.toDomain()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("获取档案详情失败"))
                }
            }
        } catch (e: Exception) {
            val cached = archiveDao.getArchiveById(archiveId)?.toDomain()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createArchive(name: String, description: String?, archiveType: String): Result<Archive> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createArchive(CreateArchiveRequest(name, description, archiveType))
            if (response.isSuccessful) {
                val archive = response.body()!!.toDomain()
                archiveDao.insertArchive(response.body()!!.toEntity())
                Result.success(archive)
            } else {
                Result.failure(Exception("创建档案失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateArchive(archiveId: Int, name: String?, description: String?): Result<Archive> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateArchive(archiveId, UpdateArchiveRequest(name, description))
            if (response.isSuccessful) {
                val archive = response.body()!!.toDomain()
                archiveDao.insertArchive(response.body()!!.toEntity())
                Result.success(archive)
            } else {
                Result.failure(Exception("更新档案失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteArchive(archiveId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.deleteArchive(archiveId)
            archiveDao.getArchiveById(archiveId)?.let { archiveDao.deleteArchive(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMembers(archiveId: Int): Result<List<Member>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMembers(archiveId)
            if (response.isSuccessful) {
                val members = response.body()!!.map { it.toDomain() }
                memberDao.insertMembers(response.body()!!.map { it.toEntity() })
                Result.success(members)
            } else {
                val cached = memberDao.getMembersByArchive(archiveId).map { it.toDomain() }
                if (cached.isNotEmpty()) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("获取成员列表失败"))
                }
            }
        } catch (e: Exception) {
            val cached = memberDao.getMembersByArchive(archiveId).map { it.toDomain() }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getMember(archiveId: Int, memberId: Int): Result<Member> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMember(archiveId, memberId)
            if (response.isSuccessful) {
                val member = response.body()!!.toDomain()
                memberDao.insertMember(response.body()!!.toEntity())
                Result.success(member)
            } else {
                val cached = memberDao.getMemberById(memberId)?.toDomain()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("获取成员详情失败"))
                }
            }
        } catch (e: Exception) {
            val cached = memberDao.getMemberById(memberId)?.toDomain()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createMember(archiveId: Int, name: String, relationshipType: String, birthYear: Int?, endYear: Int?, status: String, bio: String?): Result<Member> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createMember(archiveId, CreateMemberRequest(name, relationshipType, birthYear, status, endYear, bio))
            if (response.isSuccessful) {
                val member = response.body()!!.toDomain()
                memberDao.insertMember(response.body()!!.toEntity())
                Result.success(member)
            } else {
                Result.failure(Exception("创建成员失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMember(archiveId: Int, memberId: Int, name: String?, relationshipType: String?, birthYear: Int?, endYear: Int?, status: String?, bio: String?): Result<Member> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateMember(archiveId, memberId, UpdateMemberRequest(name, relationshipType, birthYear, status, endYear, bio))
            if (response.isSuccessful) {
                val member = response.body()!!.toDomain()
                memberDao.insertMember(response.body()!!.toEntity())
                Result.success(member)
            } else {
                Result.failure(Exception("更新成员失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMember(archiveId: Int, memberId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.deleteMember(archiveId, memberId)
            memberDao.getMemberById(memberId)?.let { memberDao.deleteMember(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadMemberAvatar(archiveId: Int, memberId: Int, filename: String, mimeType: String, bytes: ByteArray): Result<Member> = withContext(Dispatchers.IO) {
        try {
            val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
            val fileBody = bytes.toRequestBody(mediaType)
            val filePart = okhttp3.MultipartBody.Part.createFormData("file", filename, fileBody)
            val response = apiService.uploadMemberAvatar(archiveId, memberId, filePart)
            if (response.isSuccessful) {
                // 后端返回 MemberResponse
                val memberResponse = response.body()!!
                val member = memberResponse.toDomain()
                memberDao.insertMember(memberResponse.toEntity())
                Result.success(member)
            } else {
                Result.failure(Exception("上传头像失败: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// DTO -> Domain Model
private fun ArchiveResponse.toDomain() = Archive(
    id = id,
    name = name,
    description = description,
    archiveType = archiveType,
    ownerId = ownerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    memberCount = memberCount,
    memoryCount = memoryCount
)

private fun ArchiveResponse.toEntity() = ArchiveEntity(
    id = id,
    name = name,
    description = description,
    archiveType = archiveType,
    ownerId = ownerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    memberCount = memberCount,
    memoryCount = memoryCount
)

private fun ArchiveEntity.toDomain() = Archive(
    id = id,
    name = name,
    description = description,
    archiveType = archiveType,
    ownerId = ownerId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    memberCount = memberCount,
    memoryCount = memoryCount
)

private fun MemberResponse.toDomain() = Member(
    id = id,
    archiveId = archiveId,
    name = name,
    relationshipType = relationshipType,
    birthYear = birthYear,
    endYear = endYear,
    bio = bio,
    status = status,
    voiceProfileId = voiceProfileId,
    emotionTags = emotionTags,
    memoryCount = memoryCount,
    createdAt = createdAt,
    avatarUrl = avatarUrl
)

private fun MemberResponse.toEntity() = MemberEntity(
    id = id,
    archiveId = archiveId,
    name = name,
    relationshipType = relationshipType,
    birthYear = birthYear,
    endYear = endYear,
    status = status,
    bio = bio,
    voiceProfileId = voiceProfileId,
    emotionTags = emotionTags.joinToString(","),
    memoryCount = memoryCount,
    createdAt = createdAt,
    avatarUrl = avatarUrl
)

private fun MemberEntity.toDomain() = Member(
    id = id,
    archiveId = archiveId,
    name = name,
    relationshipType = relationshipType,
    birthYear = birthYear,
    endYear = endYear,
    bio = bio,
    status = status,
    voiceProfileId = voiceProfileId,
    emotionTags = emotionTags.split(",").filter { it.isNotBlank() },
    memoryCount = memoryCount,
    createdAt = createdAt,
    avatarUrl = avatarUrl
)
