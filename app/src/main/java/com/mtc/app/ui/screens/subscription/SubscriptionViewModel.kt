package com.mtc.app.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtc.app.data.model.SubscriptionResponse
import com.mtc.app.data.model.QuotaResponse
import com.mtc.app.data.model.UsageStatsResponse
import com.mtc.app.data.remote.MtcApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val subscription: SubscriptionResponse? = null,
    val quota: QuotaResponse? = null,
    val usageStats: UsageStatsResponse? = null,
    val error: String? = null,
    val updateSuccess: Boolean = false
)

/**
 * 订阅等级信息
 */
data class SubscriptionTier(
    val id: String,
    val name: String,
    val description: String,
    val monthlyLimit: Long,
    val price: String
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val apiService: MtcApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    /**
     * 可用的订阅等级
     */
    val availableTiers = listOf(
        SubscriptionTier(
            id = "free",
            name = "免费版",
            description = "基础功能，适合尝鲜",
            monthlyLimit = 50000000,
            price = "免费"
        ),
        SubscriptionTier(
            id = "lite",
            name = "Lite",
            description = "适合个人用户",
            monthlyLimit = 100000000,
            price = "待定"
        ),
        SubscriptionTier(
            id = "pro",
            name = "Pro",
            description = "适合专业用户",
            monthlyLimit = 300000000,
            price = "待定"
        ),
        SubscriptionTier(
            id = "max",
            name = "Max",
            description = "适合重度用户",
            monthlyLimit = 800000000,
            price = "待定"
        )
    )

    init {
        loadSubscriptionData()
    }

    fun loadSubscriptionData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 加载订阅信息
                val subResponse = apiService.getSubscription()
                if (subResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(subscription = subResponse.body())
                }

                // 加载配额信息
                val quotaResponse = apiService.getQuota()
                if (quotaResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(quota = quotaResponse.body())
                }

                // 加载用量统计
                val usageResponse = apiService.getUsageStats()
                if (usageResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(usageStats = usageResponse.body())
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    fun updateSubscriptionTier(tier: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = apiService.updateSubscriptionTier(
                    com.mtc.app.data.model.SubscriptionTierUpdate(tier = tier)
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        subscription = response.body(),
                        updateSuccess = true
                    )
                    loadSubscriptionData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "更新订阅失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "更新失败: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearUpdateSuccess() {
        _uiState.value = _uiState.value.copy(updateSuccess = false)
    }
}
