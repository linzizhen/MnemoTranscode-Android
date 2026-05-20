package com.mtc.app.domain.repository

import com.mtc.app.data.model.ClientLlmConfig
import com.mtc.app.domain.model.Storybook

/**
 * 故事书 Repository 接口
 */
interface StorybookRepository {
    suspend fun generateStory(
        archiveId: Int,
        memberId: Int? = null,
        style: String = "nostalgic",
        clientLlm: ClientLlmConfig? = null
    ): Result<Storybook>
}
