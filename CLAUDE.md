# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 构建与开发命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 运行单元测试
./gradlew test

# 运行 Lint 检查
./gradlew lint

# 清理构建产物
./gradlew clean

# 安装 Debug APK 到已连接的设备
./gradlew installDebug
```

**环境要求**：JDK 17、Android SDK 34、Android Studio Hedgehog (2023.1.1) 或更高版本

## 架构概览

**单模块 Android 应用**（`:app`），技术栈为 Kotlin + Jetpack Compose + Material 3。

### 核心分层

```
com.baby.growth/
├── BabyGrowthApp.kt          # Application 入口，初始化数据库与计时器
├── MainActivity.kt           # 唯一 Activity + Navigation Host
├── data/                     # 数据层（Room 数据库、实体、DAO）
├── ui/                       # Compose UI 层（页面、ViewModel、通用组件）
├── service/                  # 前台服务（计时器通知）
└── utils/                    # 工具类、管理器、参考数据
```

### 关键架构模式

**数据库获取**：ViewModel 通过 `(application as BabyGrowthApp).database` 获取数据库实例。

**状态管理**：所有 ViewModel 使用 `MutableStateFlow` → `StateFlow` 暴露状态，Screen 通过 `collectAsState()` 观察。

**计时器持久化**：BreastfeedingTimer 与 SleepTimer 使用 SharedPreferences 存储**绝对时间戳**（而非相对时长），以确保应用被杀死或切入后台后恢复时计时仍然准确。每个计时器都配有一个前台服务来维持通知。

**主题系统**：6 种预设主题 + 深色模式，由 ThemeManager（SharedPreferences）统一管理。扩展颜色通过 `LocalBabyGrowthColors.current` 获取，排版通过 `LocalBabyGrowthTypography.current` 获取。

**导航**：路由以 sealed class `Screen` 的形式定义在 MainActivity.kt 中。底部导航共 5 个 Tab。记录页面的路由规则为：新增用 `record/{type}`，编辑用 `record/{type}/edit/{id}`。

## 新增记录类型

添加新记录类型时，请按以下顺序同步修改相关文件：

1. `data/entity/` — 新建 Entity 类，包含标准字段（uniqueId、recordTime、deviceId、createdAt、note）
2. `data/dao/` — 新建 DAO，提供标准查询方法
3. `data/database/AppDatabase.kt` — 注册 Entity 与 DAO，**并递增版本号**
4. `utils/RecordTypes.kt` — 在 `ALL_TYPES` 中添加类型元信息
5. `ui/record/` — 新建 RecordScreen（含内联 ViewModel）
6. `MainActivity.kt` — 添加 composable 路由（新增 + 编辑）
7. `ui/home/HomeScreen.kt` + `HomeViewModel.kt` — 添加快捷入口和统计项
8. `ui/records/RecordsScreen.kt` + `RecordsViewModel.kt` — 添加筛选 Tab 和统计
9. `utils/DataExporter.kt` — 在 `ExportData` 类中添加字段，并补充导入/导出逻辑

## 数据库 Schema 变更

**当前版本**：2，迁移策略为 `fallbackToDestructiveMigration()`（⚠️ 升级会清空所有数据）

修改 Schema 时需同步：
- 在 AppDatabase.kt 中更新 `version` 号
- 将 Entity 添加到 `entities` 数组
- 添加对应的 DAO 抽象方法
- **强烈建议**：将破坏性迁移替换为正式的 `Migration` 策略，避免生产环境数据丢失

所有记录类 Entity 的插入均使用 `OnConflictStrategy.IGNORE`（基于 uniqueId 去重），BabyInfo 使用 `REPLACE`。

## UI 组件规范

**优先复用** `ui/components/CommonComponents.kt` 中的现有组件：
- 卡片：`BabyCard`、`BabyTitledCard`、`BabyAccentCard`
- 按钮：`PrimaryButton`、`SecondaryButton`
- 导航：`BabyTopBar`
- 展示：`EmptyState`、`SectionHeader`、`FilterTag`、`TimelineItem`

**Compose 约定**：
- 所有 Screen 均为 `@Composable` 函数，接收 `navController: NavController` 参数
- 编辑模式通过 `editId: Long?` 参数区分（null 表示新增，非 null 表示编辑）
- 使用 Material 3 组件与 `MaterialTheme.colorScheme`
- 扩展主题色通过 `LocalBabyGrowthColors.current` 访问

## CI/CD

GitHub Actions 工作流在推送 `v*` 标签时触发：
- 构建 → 签名 → 创建 GitHub Release → 上传 APK
- 所需 Secrets：`SIGNING_KEY`、`KEY_ALIAS`、`KEY_STORE_PASSWORD`、`KEY_PASSWORD`

发布流程：
1. 在 `app/build.gradle.kts` 中更新 `versionCode` 和 `versionName`
2. 创建并推送标签：`git tag v1.x.x && git push origin v1.x.x`

## 权限声明

在 AndroidManifest.xml 中声明：
- `WRITE_EXTERNAL_STORAGE`（maxSdk=28）— 数据导出
- `READ_EXTERNAL_STORAGE`（maxSdk=32）— 数据导入
- `FOREGROUND_SERVICE` — 计时器前台服务
- `POST_NOTIFICATIONS` — 计时器通知
- `CAMERA`、`VIBRATE` — 已声明但暂未使用

## ProGuard

当前规则保留所有 Entity 类：
```
-keep class com.baby.growth.data.entity.** { *; }
```
新增 Entity 后请确认此规则仍然覆盖。
