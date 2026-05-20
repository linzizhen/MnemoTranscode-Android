package com.mtc.app.ui.screens.subscription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            snackbarHostState.showSnackbar("订阅已更新")
            viewModel.clearUpdateSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("订阅管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadSubscriptionData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 当前订阅状态
                item {
                    CurrentSubscriptionCard(
                        subscription = uiState.subscription,
                        quota = uiState.quota
                    )
                }

                // 用量进度条
                item {
                    UsageProgressCard(
                        quota = uiState.quota
                    )
                }

                // 订阅等级选择
                item {
                    Text(
                        text = "选择订阅等级",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(viewModel.availableTiers) { tier ->
                    SubscriptionTierCard(
                        tier = tier,
                        isCurrentTier = uiState.subscription?.tier == tier.id,
                        onSelect = {
                            if (uiState.subscription?.tier != tier.id) {
                                viewModel.updateSubscriptionTier(tier.id)
                            }
                        }
                    )
                }

                // 说明
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "用量说明",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Token 用量按月统计，每月自动重置",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• 使用自己的 API Key 不消耗订阅额度",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• 切换订阅等级即时生效",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentSubscriptionCard(
    subscription: com.mtc.app.data.model.SubscriptionResponse?,
    quota: com.mtc.app.data.model.QuotaResponse?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val tierName = when (subscription?.tier ?: quota?.subscriptionTier ?: "free") {
                "free" -> "免费版"
                "lite" -> "Lite"
                "pro" -> "Pro"
                "max" -> "Max"
                else -> subscription?.tier ?: quota?.subscriptionTier ?: "免费版"
            }

            Icon(
                imageVector = when (subscription?.tier ?: quota?.subscriptionTier) {
                    "max" -> Icons.Default.Stars
                    "pro" -> Icons.Default.Star
                    "lite" -> Icons.Default.WorkspacePremium
                    else -> Icons.Default.FreeBreakfast
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tierName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (subscription?.expiresAt != null || quota?.resetAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "重置日期: ${subscription?.expiresAt ?: quota?.resetAt ?: ""}".take(20),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun UsageProgressCard(
    quota: com.mtc.app.data.model.QuotaResponse?
) {
    if (quota == null) return

    val progress = (quota.usagePercent / 100f).coerceIn(0f, 1f)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本月用量",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${formatTokens(quota.monthlyUsed)} / ${formatTokens(quota.monthlyLimit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    progress > 0.9f -> MaterialTheme.colorScheme.error
                    progress > 0.7f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "剩余: ${formatTokens(quota.remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        progress > 0.9f -> MaterialTheme.colorScheme.error
                        progress > 0.7f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@Composable
fun SubscriptionTierCard(
    tier: SubscriptionTier,
    isCurrentTier: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrentTier, onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTier)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentTier) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tier.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrentTier) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("当前", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tier.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "每月 ${formatTokens(tier.monthlyLimit)} Token",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = tier.price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isCurrentTier) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "当前",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 格式化 Token 数量
 */
private fun formatTokens(tokens: Long): String {
    return when {
        tokens >= 1_000_000_000 -> String.format("%.1fB", tokens / 1_000_000_000.0)
        tokens >= 1_000_000 -> String.format("%.1fM", tokens / 1_000_000.0)
        tokens >= 1_000 -> String.format("%.1fK", tokens / 1_000.0)
        else -> tokens.toString()
    }
}
