package com.mtc.app.data.remote

import com.mtc.app.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * MTC API 服务接口
 */
interface MtcApiService {

    // ========== 认证接口 ==========

    @FormUrlEncoded
    @POST("api/v1/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<TokenResponse>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @PATCH("api/v1/auth/me")
    suspend fun updateUser(@Body request: UpdateUserRequest): Response<UserResponse>

    @POST("api/v1/auth/refresh-token")
    suspend fun refreshToken(@Query("remember") remember: Boolean = false): Response<TokenResponse>

    @Multipart
    @POST("api/v1/auth/avatar")
    suspend fun uploadAvatar(@Part file: okhttp3.MultipartBody.Part): Response<AvatarUploadResponse>

    @DELETE("api/v1/auth/avatar")
    suspend fun deleteAvatar(): Response<UserResponse>

    @GET("api/v1/auth/subscription")
    suspend fun getSubscription(): Response<SubscriptionResponse>

    @POST("api/v1/auth/subscription")
    suspend fun updateSubscription(@Body request: SubscriptionTierUpdate): Response<SubscriptionResponse>

    // ========== 档案接口 ==========

    @GET("api/v1/archives")
    suspend fun getArchives(
        @Query("archive_type") archiveType: String? = null
    ): Response<List<ArchiveResponse>>

    @GET("api/v1/archives/{archiveId}")
    suspend fun getArchive(@Path("archiveId") archiveId: Int): Response<ArchiveResponse>

    @POST("api/v1/archives")
    suspend fun createArchive(@Body request: CreateArchiveRequest): Response<ArchiveResponse>

    @PATCH("api/v1/archives/{archiveId}")
    suspend fun updateArchive(
        @Path("archiveId") archiveId: Int,
        @Body request: UpdateArchiveRequest
    ): Response<ArchiveResponse>

    @DELETE("api/v1/archives/{archiveId}")
    suspend fun deleteArchive(@Path("archiveId") archiveId: Int)

    // ========== 成员接口 ==========

    @GET("api/v1/archives/{archiveId}/members")
    suspend fun getMembers(@Path("archiveId") archiveId: Int): Response<List<MemberResponse>>

    @GET("api/v1/archives/{archiveId}/members/{memberId}")
    suspend fun getMember(
        @Path("archiveId") archiveId: Int,
        @Path("memberId") memberId: Int
    ): Response<MemberResponse>

    @POST("api/v1/archives/{archiveId}/members")
    suspend fun createMember(
        @Path("archiveId") archiveId: Int,
        @Body request: CreateMemberRequest
    ): Response<MemberResponse>

    @PATCH("api/v1/archives/{archiveId}/members/{memberId}")
    suspend fun updateMember(
        @Path("archiveId") archiveId: Int,
        @Path("memberId") memberId: Int,
        @Body request: UpdateMemberRequest
    ): Response<MemberResponse>

    @DELETE("api/v1/archives/{archiveId}/members/{memberId}")
    suspend fun deleteMember(
        @Path("archiveId") archiveId: Int,
        @Path("memberId") memberId: Int
    )

    @Multipart
    @POST("api/v1/archives/{archiveId}/members/{memberId}/avatar")
    suspend fun uploadMemberAvatar(
        @Path("archiveId") archiveId: Int,
        @Path("memberId") memberId: Int,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<MemberResponse>

    @DELETE("api/v1/archives/{archiveId}/members/{memberId}/avatar")
    suspend fun deleteMemberAvatar(
        @Path("archiveId") archiveId: Int,
        @Path("memberId") memberId: Int
    ): Response<MemberAvatarResponse>

    @GET("api/v1/archives/{archiveId}/members/{memberId}/avatar-file")
    suspend fun getMemberAvatarFile(
        @Path("archiveId") archiveId: Int,
        @Path("memberId") memberId: Int
    ): Response<MemberAvatarFileResponse>

    // ========== 记忆接口 ==========

    @GET("api/v1/memories")
    suspend fun getMemories(
        @Query("archive_id") archiveId: Int? = null,
        @Query("member_id") memberId: Int? = null,
        @Query("emotion_label") emotionLabel: String? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): Response<List<MemoryResponse>>

    @GET("api/v1/memories/{memoryId}")
    suspend fun getMemory(@Path("memoryId") memoryId: Int): Response<MemoryResponse>

    @POST("api/v1/memories")
    suspend fun createMemory(@Body request: CreateMemoryRequest): Response<MemoryResponse>

    @PATCH("api/v1/memories/{memoryId}")
    suspend fun updateMemory(
        @Path("memoryId") memoryId: Int,
        @Body request: UpdateMemoryRequest
    ): Response<MemoryResponse>

    @DELETE("api/v1/memories/{memoryId}")
    suspend fun deleteMemory(@Path("memoryId") memoryId: Int)

    @POST("api/v1/memories/search")
    suspend fun searchMemories(@Body request: MemorySearchRequest): Response<MemorySearchResponse>

    @POST("api/v1/memories/batch-delete")
    suspend fun batchDeleteMemories(@Body request: BatchDeleteRequest): Response<BatchDeleteResponse>

    // ========== 记忆关系图接口 ==========

    @GET("api/v1/memories/mnemo-graph")
    suspend fun getMnemoGraph(
        @Query("member_id") memberId: Int
    ): Response<MnemoGraphResponse>

    // ========== 聊天导入接口 ==========

    @POST("api/v1/memories/chat-import")
    suspend fun chatImport(@Body request: ChatImportRequest): Response<ChatImportResponse>

    @POST("api/v1/memories/extract-from-conversation")
    suspend fun extractMemoriesFromConversation(@Body request: ExtractMemoriesRequest): Response<ExtractMemoriesResponse>

    // ========== AI 对话接口 ==========

    @POST("api/v1/dialogue/chat")
    suspend fun chat(@Body request: DialogueRequest): Response<DialogueResponse>

    @POST("api/v1/dialogue/history")
    suspend fun getDialogueHistory(@Body request: DialogueHistoryRequest): Response<DialogueHistoryResponse>

    @DELETE("api/v1/dialogue/history/{sessionId}")
    suspend fun clearDialogueHistory(@Path("sessionId") sessionId: String)

    @DELETE("api/v1/dialogue/history")
    suspend fun clearDialogueHistoryByMember(@Body request: DeleteHistoryRequest)

    @POST("api/v1/dialogue/messages")
    suspend fun getDialogueMessages(@Body request: DialogueHistoryRequest): Response<DialogueMessagesResponse>

    @POST("api/v1/dialogue/messages/bootstrap")
    suspend fun bootstrapMessages(@Body request: BootstrapMessagesRequest): Response<List<DialogueMessageDto>>

    // ========== 故事书接口 ==========

    @POST("api/v1/storybook/generate")
    suspend fun generateStory(@Body request: StorybookGenerateRequest): Response<StorybookResponse>

    // ========== 记忆胶囊接口 ==========

    @POST("api/v1/capsules")
    suspend fun createCapsule(
        @Query("member_id") memberId: Int,
        @Query("title") title: String,
        @Query("content") content: String,
        @Query("unlock_date") unlockDate: String,
        @Query("recipients") recipients: List<Int>? = null
    ): Response<CapsuleResponse>

    @GET("api/v1/capsules")
    suspend fun getCapsules(@Query("member_id") memberId: Int? = null): Response<List<CapsuleResponse>>

    @GET("api/v1/capsules/{capsuleId}")
    suspend fun getCapsule(@Path("capsuleId") capsuleId: Int): Response<CapsuleResponse>

    @POST("api/v1/capsules/{capsuleId}/force-unlock")
    suspend fun forceUnlockCapsule(@Path("capsuleId") capsuleId: Int): Response<CapsuleForceUnlockResponse>

    // ========== 媒体接口 ==========

    @GET("api/v1/media")
    suspend fun getMediaAssets(
        @Query("member_id") memberId: Int? = null,
        @Query("archive_id") archiveId: Int? = null,
        @Query("purpose") purpose: String? = null
    ): Response<List<MediaAssetResponse>>

    @POST("api/v1/media/uploads/init")
    suspend fun initMediaUpload(@Body request: MediaUploadInitRequest): Response<MediaUploadInitResponse>

    @POST("api/v1/media/uploads/complete")
    suspend fun completeMediaUpload(@Body request: MediaUploadCompleteRequest): Response<MediaUploadCompleteResponse>

    // Direct upload - multipart form, more reliable
    @Multipart
    @POST("api/v1/media/uploads/direct")
    suspend fun uploadMediaDirect(
        @Part file: okhttp3.MultipartBody.Part,
        @Part("purpose") purpose: okhttp3.RequestBody,
        @Part("archive_id") archiveId: okhttp3.RequestBody?,
        @Part("member_id") memberId: okhttp3.RequestBody?
    ): Response<MediaAssetResponse>

    @GET("api/v1/media/{mediaId}/download-url")
    suspend fun getMediaDownloadUrl(@Path("mediaId") mediaId: Int): Response<MediaDownloadUrlResponse>

    // 经 API 鉴权拉取媒体文件原始字节（用于图片展示）
    @GET("api/v1/media/{mediaId}/file")
    @Streaming
    suspend fun getMediaFileStream(@Path("mediaId") mediaId: Int): Response<ResponseBody>

    @DELETE("api/v1/media/{mediaId}")
    suspend fun deleteMediaAsset(@Path("mediaId") mediaId: Int)

    // ========== 用户偏好接口 ==========

    @GET("api/v1/preferences")
    suspend fun getPreferences(): Response<PreferencesResponse>

    @PUT("api/v1/preferences")
    suspend fun updatePreferences(@Body request: PreferencesUpdateRequest): Response<PreferencesResponse>

    // ========== 用量统计接口 ==========

    @GET("api/v1/usage/stats")
    suspend fun getUsageStats(): Response<UsageStatsResponse>

    @GET("api/v1/usage/history")
    suspend fun getUsageHistory(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("action_type") actionType: String? = null
    ): Response<UsageHistoryResponse>

    @GET("api/v1/usage/quota")
    suspend fun getQuota(): Response<QuotaResponse>

    @POST("api/v1/usage/subscription-tier")
    suspend fun updateSubscriptionTier(@Body request: SubscriptionTierUpdate): Response<SubscriptionResponse>

    // ========== AI 记忆同步接口 ==========

    @GET("api/v1/ai-memory")
    suspend fun getAIMemory(): Response<AIMemoryContextResponse>

    @PUT("api/v1/ai-memory")
    suspend fun updateAIMemory(@Body request: AIMemoryUpdateRequest): Response<AIMemoryContextResponse>

    @DELETE("api/v1/ai-memory")
    suspend fun clearAIMemory(): Response<AIMemoryContextResponse>

    // ========== LLM 探测接口 ==========

    @POST("api/v1/llm-probe/check")
    suspend fun checkLlmEndpoint(@Body request: LlmCheckRequest): Response<LlmCheckResponse>

    // ========== 健康检查 ==========

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>
}
