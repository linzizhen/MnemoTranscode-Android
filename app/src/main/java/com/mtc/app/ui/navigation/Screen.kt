package com.mtc.app.ui.navigation

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    // 认证
    object Login : Screen("login")
    object Register : Screen("register")

    // 首页
    object Home : Screen("home")

    // 档案
    object ArchiveList : Screen("archives")
    object ArchiveDetail : Screen("archives/{archiveId}") {
        fun createRoute(archiveId: Int) = "archives/$archiveId"
    }
    object CreateArchive : Screen("archives/create")

    // 成员
    object MemberDetail : Screen("archives/{archiveId}/members/{memberId}") {
        fun createRoute(archiveId: Int, memberId: Int) = "archives/$archiveId/members/$memberId"
    }
    object CreateMember : Screen("archives/{archiveId}/members/create") {
        fun createRoute(archiveId: Int) = "archives/$archiveId/members/create"
    }

    // 记忆
    object MemoryDetail : Screen("memories/{memoryId}") {
        fun createRoute(memoryId: Int) = "memories/$memoryId"
    }
    object CreateMemory : Screen("members/{memberId}/memories/create") {
        fun createRoute(memberId: Int) = "members/$memberId/memories/create"
    }

    // AI 对话
    object Dialogue : Screen("dialogue/{archiveId}/{memberId}") {
        fun createRoute(archiveId: Int, memberId: Int) = "dialogue/$archiveId/$memberId"
        // 不带参数的路由用于选择档案
        const val SELECT_ROUTE = "dialogue/select"
    }

    // 个人中心
    object Profile : Screen("profile")
    object AccountInfo : Screen("account_info")

    // 故事书
    object Storybook : Screen("storybook/{archiveId}") {
        fun createRoute(archiveId: Int) = "storybook/$archiveId"
    }

    // 记忆胶囊
    object CapsuleList : Screen("capsules")
    object CreateCapsule : Screen("capsules/create/{archiveId}/{memberId}") {
        fun createRoute(archiveId: Int, memberId: Int) = "capsules/create/$archiveId/$memberId"
    }

    // 媒体库
    object MediaGallery : Screen("media")

    // 设置
    object Preferences : Screen("preferences")
    object ModelSettings : Screen("settings/model")
    object Subscription : Screen("subscription")
    object ServerConfig : Screen("server_config")
    object Timeline : Screen("timeline/{archiveId}") {
        fun createRoute(archiveId: Int) = "timeline/$archiveId"
    }
}

/**
 * 底部导航项目
 */
enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: String
) {
    Home("home", "首页", "🏠"),
    Archives("archives", "档案", "📁"),
    Media("media", "媒体", "📷"),
    Profile("profile", "我的", "👤")
}
