package com.mtc.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mtc.app.ui.screens.archive.*
import com.mtc.app.ui.screens.auth.LoginScreen
import com.mtc.app.ui.screens.auth.RegisterScreen
import com.mtc.app.ui.screens.auth.ServerConfigScreen
import com.mtc.app.ui.screens.capsule.CapsuleListScreen
import com.mtc.app.ui.screens.capsule.CreateCapsuleScreen
import com.mtc.app.ui.screens.dialogue.DialogueScreen
import com.mtc.app.ui.screens.home.HomeScreen
import com.mtc.app.ui.screens.media.MediaGalleryScreen
import com.mtc.app.ui.screens.memory.CreateMemoryScreen
import com.mtc.app.ui.screens.memory.MemoryDetailScreen
import com.mtc.app.ui.screens.member.CreateMemberScreen
import com.mtc.app.ui.screens.member.MemberDetailScreen
import com.mtc.app.ui.screens.preferences.PreferencesScreen
import com.mtc.app.ui.screens.profile.AccountInfoScreen
import com.mtc.app.ui.screens.profile.ProfileScreen
import com.mtc.app.ui.screens.settings.ModelSettingsScreen
import com.mtc.app.ui.screens.subscription.SubscriptionScreen
import com.mtc.app.ui.screens.storybook.StorybookScreen
import com.mtc.app.ui.screens.timeline.TimelineScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MtcNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 底部导航是否显示
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.ArchiveList.route,
        Screen.CapsuleList.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding)
        ) {
            // 认证
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            // 首页
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToArchive = { archiveId ->
                        navController.navigate(Screen.ArchiveDetail.createRoute(archiveId))
                    },
                    onNavigateToCreateArchive = {
                        navController.navigate(Screen.CreateArchive.route)
                    },
                    onNavigateToArchiveList = {
                        navController.navigate(Screen.ArchiveList.route)
                    },
                    onNavigateToDialogue = { archiveId, memberId ->
                        navController.navigate(Screen.Dialogue.createRoute(archiveId, memberId))
                    },
                    onNavigateToTimeline = { archiveId ->
                        navController.navigate(Screen.Timeline.createRoute(archiveId))
                    },
                    onNavigateToStorybook = { archiveId ->
                        navController.navigate(Screen.Storybook.createRoute(archiveId))
                    },
                    onNavigateToCapsules = {
                        navController.navigate(Screen.CapsuleList.route)
                    }
                )
            }

            // 档案列表
            composable(Screen.ArchiveList.route) {
                ArchiveListScreen(
                    onNavigateToArchive = { archiveId ->
                        navController.navigate(Screen.ArchiveDetail.createRoute(archiveId))
                    },
                    onNavigateToCreateArchive = {
                        navController.navigate(Screen.CreateArchive.route)
                    }
                )
            }

            // 档案详情
            composable(
                route = Screen.ArchiveDetail.route,
                arguments = listOf(navArgument("archiveId") { type = NavType.IntType })
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                ArchiveDetailScreen(
                    archiveId = archiveId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMember = { memberId ->
                        navController.navigate(Screen.MemberDetail.createRoute(archiveId, memberId))
                    },
                    onNavigateToCreateMember = {
                        navController.navigate(Screen.CreateMember.createRoute(archiveId))
                    },
                    onNavigateToDialogue = { memberId ->
                        navController.navigate(Screen.Dialogue.createRoute(archiveId, memberId))
                    },
                    onNavigateToTimeline = {
                        navController.navigate(Screen.Timeline.createRoute(archiveId))
                    },
                    onNavigateToStorybook = {
                        navController.navigate(Screen.Storybook.createRoute(archiveId))
                    }
                )
            }

            // 创建档案
            composable(Screen.CreateArchive.route) {
                CreateArchiveScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onArchiveCreated = { archiveId ->
                        navController.navigate(Screen.ArchiveDetail.createRoute(archiveId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // 成员详情
            composable(
                route = Screen.MemberDetail.route,
                arguments = listOf(
                    navArgument("archiveId") { type = NavType.IntType },
                    navArgument("memberId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                val memberId = backStackEntry.arguments?.getInt("memberId") ?: return@composable
                MemberDetailScreen(
                    archiveId = archiveId,
                    memberId = memberId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMemory = { memoryId ->
                        navController.navigate(Screen.MemoryDetail.createRoute(memoryId))
                    },
                    onNavigateToCreateMemory = {
                        navController.navigate(Screen.CreateMemory.createRoute(memberId))
                    },
                    onNavigateToDialogue = {
                        navController.navigate(Screen.Dialogue.createRoute(archiveId, memberId))
                    }
                )
            }

            // 创建成员
            composable(
                route = Screen.CreateMember.route,
                arguments = listOf(navArgument("archiveId") { type = NavType.IntType })
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                CreateMemberScreen(
                    archiveId = archiveId,
                    onNavigateBack = { navController.popBackStack() },
                    onMemberCreated = { memberId ->
                        navController.navigate(Screen.MemberDetail.createRoute(archiveId, memberId)) {
                            popUpTo(Screen.ArchiveDetail.createRoute(archiveId))
                        }
                    }
                )
            }

            // 记忆详情
            composable(
                route = Screen.MemoryDetail.route,
                arguments = listOf(navArgument("memoryId") { type = NavType.IntType })
            ) { backStackEntry ->
                val memoryId = backStackEntry.arguments?.getInt("memoryId") ?: return@composable
                MemoryDetailScreen(
                    memoryId = memoryId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 创建记忆
            composable(
                route = Screen.CreateMemory.route,
                arguments = listOf(navArgument("memberId") { type = NavType.IntType })
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getInt("memberId") ?: return@composable
                CreateMemoryScreen(
                    memberId = memberId,
                    onNavigateBack = { navController.popBackStack() },
                    onMemoryCreated = { navController.popBackStack() }
                )
            }

            // AI 对话
            composable(
                route = Screen.Dialogue.route,
                arguments = listOf(
                    navArgument("archiveId") { type = NavType.IntType },
                    navArgument("memberId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                val memberId = backStackEntry.arguments?.getInt("memberId") ?: return@composable
                DialogueScreen(
                    archiveId = archiveId,
                    memberId = memberId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 个人中心
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToPreferences = {
                        navController.navigate(Screen.Preferences.route)
                    },
                    onNavigateToAccountInfo = {
                        navController.navigate(Screen.AccountInfo.route)
                    },
                    onNavigateToModelSettings = {
                        navController.navigate(Screen.ModelSettings.route)
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    onNavigateToServerConfig = {
                        navController.navigate(Screen.ServerConfig.route)
                    }
                )
            }

            // 账号详情
            composable(Screen.AccountInfo.route) {
                AccountInfoScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 故事书
            composable(
                route = Screen.Storybook.route,
                arguments = listOf(navArgument("archiveId") { type = NavType.IntType })
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                StorybookScreen(
                    archiveId = archiveId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 记忆胶囊列表
            composable(Screen.CapsuleList.route) {
                CapsuleListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCreateCapsule = { archiveId, memberId ->
                        navController.navigate(Screen.CreateCapsule.createRoute(archiveId, memberId))
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // 创建记忆胶囊
            composable(
                route = Screen.CreateCapsule.route,
                arguments = listOf(
                    navArgument("archiveId") { type = NavType.IntType },
                    navArgument("memberId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                val memberId = backStackEntry.arguments?.getInt("memberId") ?: return@composable
                CreateCapsuleScreen(
                    memberId = memberId,
                    archiveId = archiveId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 媒体库
            composable(Screen.MediaGallery.route) {
                MediaGalleryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 个人偏好设置
            composable(Screen.Preferences.route) {
                PreferencesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 模型设置
            composable(Screen.ModelSettings.route) {
                ModelSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 服务器设置
            composable(Screen.ServerConfig.route) {
                ServerConfigScreen(
                    onServerConfigured = {
                        navController.popBackStack()
                    },
                    onNavigateToLogin = {
                        // 不允许从这里跳转到登录
                    }
                )
            }

            // 订阅管理
            composable(Screen.Subscription.route) {
                SubscriptionScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 时间线
            composable(
                route = Screen.Timeline.route,
                arguments = listOf(navArgument("archiveId") { type = NavType.IntType })
            ) { backStackEntry ->
                val archiveId = backStackEntry.arguments?.getInt("archiveId") ?: return@composable
                TimelineScreen(
                    archiveId = archiveId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private data class BottomNavItemData(
    val route: String,
    val title: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItemData(Screen.Home.route, "首页", Icons.Default.Home),
    BottomNavItemData(Screen.ArchiveList.route, "档案", Icons.Default.Folder),
    BottomNavItemData(Screen.CapsuleList.route, "胶囊", Icons.Default.Schedule),
    BottomNavItemData(Screen.Profile.route, "我的", Icons.Default.AccountCircle)
)
