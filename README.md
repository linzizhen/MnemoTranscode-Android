# MTC-Android

MTC（Memory To Code）AI 记忆银行 Android 客户端

## 项目简介

MTC-Android 是 MTC 项目的 Android 原生客户端，专注于为用户提供便捷的移动端记忆管理体验。

用 AI 技术将记忆（声音、照片、文字、情感）进行数字化存档、智能化整理和多模态还原，随时随地守护珍贵回忆。

## 核心功能

- **多类型档案**：支持家族、恋人、挚友、至亲、伟人等多种记忆档案
- **AI 对话交互**：与记忆中的亲人进行自然语言对话
- **AI 记忆整理**：LLM 自动总结、归类、时间线重建
- **情感识别**：NLP 情绪标注与情感分析
- **记忆管理**：创建、编辑、搜索记忆
- **多端同步**：与后端服务无缝对接，数据实时同步

## 技术栈

| 层级 | 技术 |
|------|------|
| 平台 | Android 8.0+ (API 26) |
| 语言 | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| DI | Hilt |
| 网络 | Retrofit + OkHttp |
| 本地数据库 | Room |
| 状态管理 | StateFlow / Compose State |
| 导航 | Navigation Compose |

## 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK 34
- Gradle 8.2+

### 配置后端连接

编辑 `app/build.gradle.kts` 中的 `BASE_URL`，或创建 `local.properties` 文件：

```properties
backend.url=http://10.0.2.2:8000/
```

### 在 Android Studio 中打开

1. 打开 Android Studio
2. 选择 "Open an existing project"
3. 选择 `android` 目录
4. 等待 Gradle 同步完成

### 运行应用

```bash
# 使用命令行构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

## 项目结构

```
android/
├── app/src/main/
│   ├── java/com/mtc/app/
│   │   ├── data/              # 数据层
│   │   │   ├── local/         # Room 数据库
│   │   │   ├── remote/        # Retrofit API
│   │   │   ├── model/         # DTO 模型
│   │   │   └── repository/    # Repository 实现
│   │   │
│   │   ├── domain/            # 领域层
│   │   │   ├── model/         # 领域模型
│   │   │   └── repository/    # Repository 接口
│   │   │
│   │   ├── ui/                # 展示层
│   │   │   ├── components/    # 通用组件
│   │   │   ├── navigation/   # 导航
│   │   │   ├── theme/         # 主题
│   │   │   └── screens/       # 页面
│   │   │
│   │   ├── di/                # 依赖注入
│   │   └── util/              # 工具类
│   │
│   ├── res/                   # 资源文件
│   └── AndroidManifest.xml
│
├── build.gradle.kts           # 根构建配置
├── settings.gradle.kts        # 项目设置
└── gradle.properties          # Gradle 属性
```

## API 对接

客户端通过 RESTful API 与 MTC 后端通信。确保后端服务已启动：

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8000 |
| API 文档 | http://localhost:8000/docs |

## 许可证

MIT License
