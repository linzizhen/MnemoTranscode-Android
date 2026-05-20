package com.mtc.app.data.local

import androidx.room.*

/**
 * Room 数据库实体
 */
@Entity(tableName = "archives")
data class ArchiveEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val archiveType: String,
    val ownerId: Int,
    val createdAt: String,
    val updatedAt: String,
    val memberCount: Int,
    val memoryCount: Int
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: Int,
    val archiveId: Int,
    val name: String,
    val relationshipType: String,
    val birthYear: Int?,
    val endYear: Int?,
    val status: String,
    val bio: String?,
    val voiceProfileId: String?,
    val emotionTags: String,
    val memoryCount: Int,
    val createdAt: String,
    val avatarUrl: String? = null
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: Int,
    val memberId: Int,
    val archiveId: Int,
    val title: String,
    val contentText: String,
    val emotionLabel: String?,
    val timestamp: String?,
    val location: String?,
    val isCapsule: Boolean,
    val unlockDate: String?,
    val mediaRefs: String,
    val createdAt: String,
    val updatedAt: String
)

/**
 * DAO 接口
 */
@Dao
interface ArchiveDao {
    @Query("SELECT * FROM archives ORDER BY updatedAt DESC")
    suspend fun getAllArchives(): List<ArchiveEntity>

    @Query("SELECT * FROM archives WHERE id = :id")
    suspend fun getArchiveById(id: Int): ArchiveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchives(archives: List<ArchiveEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(archive: ArchiveEntity)

    @Delete
    suspend fun deleteArchive(archive: ArchiveEntity)

    @Query("DELETE FROM archives")
    suspend fun deleteAllArchives()
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE archiveId = :archiveId ORDER BY birthYear")
    suspend fun getMembersByArchive(archiveId: Int): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Int): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("DELETE FROM members WHERE archiveId = :archiveId")
    suspend fun deleteMembersByArchive(archiveId: Int)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE memberId = :memberId ORDER BY timestamp DESC")
    suspend fun getMemoriesByMember(memberId: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE archiveId = :archiveId ORDER BY timestamp DESC")
    suspend fun getMemoriesByArchive(archiveId: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Int): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MemoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE memberId = :memberId")
    suspend fun deleteMemoriesByMember(memberId: Int)
}

/**
 * Room 数据库
 */
@Database(
    entities = [ArchiveEntity::class, MemberEntity::class, MemoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MtcDatabase : RoomDatabase() {
    abstract fun archiveDao(): ArchiveDao
    abstract fun memberDao(): MemberDao
    abstract fun memoryDao(): MemoryDao
}
