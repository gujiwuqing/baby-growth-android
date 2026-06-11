# AGENTS.md — AI 维护指南

> 本文件为 AI 编码助手（如 Copilot、Claude Code 等）提供项目上下文，帮助 AI 快速理解项目架构、代码规范和修改约束。

---

## 项目概览

**BabyGrowth** 是一款 Android 宝宝成长记录应用，帮助家长追踪喂养、睡眠、尿布、辅食、生长指标、疫苗接种和营养补充剂等日常记录，并提供 WHO 生长曲线对照和智能提醒。

- **包名**: `com.baby.growth`
- **Application ID**: `com.baby.growth`
- **最低 SDK**: 26 (Android 8.0)
- **目标 SDK**: 34 (Android 14)
- **编译 SDK**: 34
- **Java 兼容**: Java 17
- **Kotlin JVM Target**: 17
- **版本**: 1.0.0 (versionCode 1)

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 1.9.20 |
| UI 框架 | Jetpack Compose | BOM 2024.02.00, Compiler Ext 1.5.5 |
| 设计系统 | Material 3 | compose-bom 统管 |
| 导航 | Navigation Compose | 2.7.6 |
| 数据库 | Room | 2.6.1 (KSP 注解处理) |
| 序列化 | Gson | 2.10.1 |
| 构建工具 | Gradle Kotlin DSL | AGP 8.2.2 |
| 注解处理 | KSP | 1.9.20-1.0.14 |
| CI/CD | GitHub Actions | tag `v*` 触发构建+签名+发布 |

---

## 项目架构

### 单模块架构

项目为**单模块** (`:app`) 结构，所有代码在 `app/src/main/java/com/baby/growth/` 下。

### 分层结构

```
com.baby.growth/
├── BabyGrowthApp.kt          # Application 入口，初始化数据库和计时器
├── MainActivity.kt           # 唯一 Activity，Compose 入口 + Navigation Host
│
├── data/                     # 数据层
│   ├── database/
│   │   └── AppDatabase.kt    # Room 数据库定义（baby_growth_db, v2, 破坏性迁移）
│   ├── entity/               # 8 个 Room Entity
│   │   ├── BabyInfo.kt       # 宝宝信息表 (baby_info)
│   │   ├── FeedRecord.kt     # 喂养记录表 (feeds)
│   │   ├── DiaperRecord.kt   # 尿布记录表 (diapers)
│   │   ├── SleepRecord.kt    # 睡眠记录表 (sleeps)
│   │   ├── FoodRecord.kt     # 辅食记录表 (foods)
│   │   ├── SupplementRecord.kt # 补充剂记录表 (supplements)
│   │   ├── GrowthRecord.kt   # 生长记录表 (growth_records)
│   │   └ VaccineRecord.kt   # 疫苗记录表 (vaccines)
│   └── dao/                  # 8 个 DAO
│       ├── BabyInfoDao.kt     # REPLACE 冲突策略
│       ├── FeedDao.kt         # IGNORE 冲突策略 + 总奶量统计
│       ├── DiaperDao.kt       # IGNORE 冲突策略
│       ├── SleepDao.kt        # IGNORE 冲突策略 + 总时长统计
│       ├── FoodDao.kt         # IGNORE 冲击策略
│       ├── SupplementDao.kt   # IGNORE 冲击策略
│       ├── GrowthDao.kt       # IGNORE 冲击策略 + 升序查询(图表)
│       └ VaccineDao.kt       # IGNORE 冲击策略 + 按名/类型查询 + 进度统计
│
├── ui/                       # UI 层（Compose）
│   ├── theme/
│   │   └ Theme.kt            # 6种主题色 + 深色模式 + 扩展颜色/排版系统
│   ├── components/           # 通用 UI 组件
│   │   ├── CommonComponents.kt # BabyCard/BabyTitledCard/BabyAccentCard/PrimaryButton 等
│   │   ├── TimelineItem.kt   # 时间轴列表项
│   │   └ BabyCard.kt         # （可能合并到 CommonComponents.kt）
│   ├── home/                 # 首页模块
│   │   ├── HomeScreen.kt     # 宝宝概览 + 今日统计 + 智能提示 + 最近记录
│   │   └ HomeViewModel.kt   # MutableStateFlow 状态管理
│   ├── records/              # 记录总览模块
│   │   ├── RecordsScreen.kt  # 日/周/月维度切换 + 图表 + 时间轴
│   │   └ RecordsViewModel.kt # 维度/日期/筛选/统计计算
│   ├── record/               # 各类型记录录入/编辑
│   │   ├── FeedingRecordScreen.kt  # 喂养(母乳计时器/配方奶/瓶喂)
│   │   ├── DiaperRecordScreen.kt   # 尿布(大便颜色/形状/红屁屁)
│   │   ├── SleepRecordScreen.kt    # 睡眠(计时/手动模式)
│   │   ├── FoodRecordScreen.kt     # 辅食(分类/反应)
│   │   ├── SupplementRecordScreen.kt # 补充剂(批量添加)
│   │   └ GrowthRecordScreen.kt    # 生长指标(WHO参考范围提示)
│   ├── growth/               # 成长曲线模块
│   │   ├── GrowthScreen.kt   # WHO P3/P50/P97 曲线 + 百分位 + 历史记录
│   │   └ GrowthViewModel.kt  # Flow 收集记录 + 宝宝性别
│   ├── vaccine/              # 疫苗接种模块
│   │   ├── VaccineScreen.kt  # 免费/自费 Tab + 接种进度 + 标记已接种
│   │   └ VaccineViewModel.kt # 自动初始化接种计划 + 标记逻辑
│   ├── settings/             # 设置模块
│   │   ├── SettingsScreen.kt # 数据导出/导入/清空 + 深色模式 + 主题色
│   │   └ SettingsViewModel.kt # 数据操作 + DataExporter
│   ├── profile/              # 宝宝资料模块
│   │   ├── ProfileScreen.kt  # 编辑姓名/性别/生日/头像
│   │   └ ProfileViewModel.kt # 更新/插入宝宝信息
│
├── service/                  # 前台服务
│   ├── FeedingTimerService.kt # 母乳喂养计时通知(NOTIFICATION_ID=1001)
│   ├── SleepTimerService.kt  # 睡眠计时通知(NOTIFICATION_ID=1002)
│
├── utils/                    # 工具层
│   ├── DateUtils.kt          # 日期格式化/范围计算/月龄/唯一ID
│   ├── RecordTypes.kt        # 记录类型元信息(8种类型+大便颜色/形状等)
│   ├── SleepTimer.kt         # 睡眠计时器(SharedPreferences持久化+绝对时间戳)
│   ├── BreastfeedingTimer.kt # 母乳计时器(左右侧独立, SharedPreferences持久化)
│   ├── SleepAdvice.kt        # AAP睡眠建议(0-36月龄)
│   ├── VaccineData.kt        # 中国免疫规划数据(23种免费+10种自费)
│   ├── GrowthCurveData.kt    # WHO生长标准数据+百分位计算
│   ├── DataExporter.kt       # JSON导出/导入(基于uniqueId去重)
│   └ ThemeManager.kt        # 6种主题+深色模式(SharedPreferences)
```

---

## 路由系统

Navigation Compose 路由定义在 `MainActivity.kt` 中：

| 路由 | 页面 | 说明 |
|------|------|------|
| `home` | HomeScreen | 首页 |
| `records` | RecordsScreen | 记录总览 |
| `growth` | GrowthScreen | 成长曲线 |
| `vaccine` | VaccineScreen | 疫苗接种 |
| `settings` | SettingsScreen | 设置 |
| `record/feeding` | FeedingRecordScreen | 新增喂养 |
| `record/diaper` | DiaperRecordScreen | 新增尿布 |
| `record/sleep` | SleepRecordScreen | 新增睡眠 |
| `record/food` | FoodRecordScreen | 新增辅食 |
| `record/supplement` | SupplementRecordScreen | 新增补充剂 |
| `record/growth` | GrowthRecordScreen | 新增生长指标 |
| `record/{type}/edit/{id}` | 对应 RecordScreen | 编辑模式（id 为 Long） |
| `profile` | ProfileScreen | 宝宝资料编辑 |

底部导航 5 个 Tab：首页、记录、成长、疫苗、我的

---

## 数据模型关键设计

### 通用字段模式

除 `BabyInfo` 外，所有记录实体都有以下共同字段：
- **`uniqueId`**: String — 数据去重标识（用于导入时避免重复）
- **`recordTime`**: Long — 业务记录时间（毫秒时间戳）
- **`deviceId`**: String — 设备标识
- **`createdAt`**: Long — 数据创建时间
- **`note`**: String — 备注

### 主键与冲突策略

- 所有表使用 `@PrimaryKey(autoGenerate = true)` 自增 Long id
- 记录表 INSERT 使用 `OnConflictStrategy.IGNORE`（基于 uniqueId 去重）
- BabyInfo INSERT 使用 `OnConflictStrategy.REPLACE`（单条更新）

### DAO 查询模式

所有 DAO（除 BabyInfoDao 和 VaccineDao）提供统一方法集：
- `getAll()`: Flow<List<T>> — 响应式查询（按 recordTime 降序）
- `getAllOnce()`: List<T> — 一次性查询
- `getByDateRange()/getByDateRangeOnce()` — 日期范围查询
- `getCountByDateRange()` — 范围内记录数
- `getLatest()` — 最近一条
- `insert/update/delete/deleteById/deleteAll`

---

## ViewModel 状态管理模式

所有 ViewModel 使用 **`MutableStateFlow` → `StateFlow`** 暴露状态，Screen 通过 `collectAsState()` 观察。

典型模式：
```kotlin
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as BabyGrowthApp).database
    private val _babyInfo = MutableStateFlow<BabyInfo?>(null)
    val babyInfo: StateFlow<BabyInfo?> = _babyInfo.asStateFlow()

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch { ... }
    }
}
```

**数据库获取方式**: `(application as BabyGrowthApp).database`

---

## 计时器持久化机制

| 计时器 | 持久化方式 | 前台服务 | 特点 |
|--------|-----------|---------|------|
| BreastfeedingTimer | SharedPreferences + 绝对时间戳 | FeedingTimerService | 左右侧独立计时，自动切换 |
| SleepTimer | SharedPreferences + 绝对时间戳 | SleepTimerService | 开始/停止，APP切后台可恢复 |

关键设计：计时器基于**绝对时间戳**而非相对时长，确保 APP 被杀或切后台后恢复时计时准确。

---

## 主题系统

6 种预设主题 + 深色模式，通过 `ThemeManager` 管理（SharedPreferences）：
- 珊瑚粉 (`coral`)、薰衣草紫 (`lavender`)、薄荷绿 (`mint`)
- 天空蓝 (`sky`)、暖阳橙 (`warm`)、樱花粉 (`sakura`)

`BabyGrowthTheme` Composable 接收 `themeKey` 和 `darkMode`，通过 `CompositionLocalProvider` 提供扩展颜色（`LocalBabyGrowthColors`）和排版（`LocalBabyGrowthTypography`）。

---

## CI/CD

GitHub Actions 工作流 `.github/workflows/release.yml`：
- **触发条件**: 推送 tag `v*`
- **流程**: checkout → JDK 17 → Gradle 构建 → APK 签名 → 重命名 → GitHub Release 发布
- **签名**: 使用 `r0adkll/sign-android-release` Action，密钥从 Secrets 读取

---

## 代码修改注意事项

### 1. 数据库变更

- Room 数据库当前版本 **2**，迁移策略为 **`fallbackToDestructiveMigration()`**（破坏性迁移，升级会清空数据）
- 如需新增表或字段，必须：
  - 在 `AppDatabase.kt` 中更新 `version` 号
  - 添加对应的 Entity 到 `entities` 数组
  - 添加对应的 DAO 到抽象方法
  - **强烈建议**改为正式迁移策略（`Migration`），避免用户数据丢失

### 2. 新增记录类型

如需新增一种记录类型，需同步修改以下文件：
1. `data/entity/` — 新增 Entity 类
2. `data/dao/` — 新增 DAO 类
3. `data/database/AppDatabase.kt` — 注册 Entity 和 DAO
4. `utils/RecordTypes.kt` — 在 `ALL_TYPES` 中添加类型元信息
5. `ui/record/` — 新增 RecordScreen + 内嵌 ViewModel
6. `MainActivity.kt` — 添加 composable 路由（新增 + 编辑）
7. `ui/home/HomeScreen.kt` / `HomeViewModel.kt` — 添加快捷入口和统计
8. `ui/records/RecordsScreen.kt` / `RecordsViewModel.kt` — 添加筛选 Tab 和统计
9. `utils/DataExporter.kt` — 在 `ExportData` 中添加字段和导入导出逻辑

### 3. UI 组件复用

优先使用 `ui/components/CommonComponents.kt` 中的组件：
- `BabyCard` / `BabyTitledCard` / `BabyAccentCard` — 卡片容器
- `PrimaryButton` / `SecondaryButton` — 按钮
- `BabyTopBar` — 顶部导航栏
- `EmptyState` — 空状态
- `SectionHeader` — 段标题
- `FilterTag` — 筛选标签
- `TimelineItem` — 时间轴条目

### 4. Compose 规范

- 使用 Material 3 组件和 `MaterialTheme.colorScheme`
- 扩展颜色通过 `LocalBabyGrowthColors.current` 获取
- 排版通过 `LocalBabyGrowthTypography.current` 获取
- 所有 Screen 都是 `@Composable` 函数，接收 `navController: NavController` 参数
- 编辑模式通过 `editId: Long?` 参数区分（null=新增，非null=编辑）

### 5. ProGuard

当前规则仅保留 Entity 类：
```
-keep class com.baby.growth.data.entity.** { *; }
```
新增 Entity 后需确认此规则仍然覆盖。

### 6. 权限

AndroidManifest.xml 中声明的权限：
- `WRITE_EXTERNAL_STORAGE` (maxSdk=28) — 数据导出
- `READ_EXTERNAL_STORAGE` (maxSdk=32) — 数据导入
- `CAMERA` — 暂未使用
- `VIBRATE` — 暂未使用
- `FOREGROUND_SERVICE` — 计时器前台服务
- `POST_NOTIFICATIONS` — 计时器通知

---

## 常见修改场景指引

| 场景 | 需修改的文件 | 注意事项 |
|------|-------------|---------|
| 修改首页统计逻辑 | HomeViewModel.kt | 注意 DAO 方法是否已存在 |
| 新增筛选维度 | RecordsScreen.kt + RecordsViewModel.kt | 维度/日期/筛选联动 |
| 修改生长曲线数据 | GrowthCurveData.kt + GrowthScreen.kt | WHO 数据为硬编码常量 |
| 修改疫苗计划 | VaccineData.kt + VaccineViewModel.kt | 初始化逻辑在 init 块 |
| 添加新主题色 | Theme.kt + ThemeManager.kt | 同步 THEMES 列表和 ColorScheme |
| 修改数据导出格式 | DataExporter.kt | ExportData 数据结构 + uniqueId 去重 |
| 修改计时器逻辑 | SleepTimer.kt / BreastfeedingTimer.kt | SharedPreferences key 和前台服务联动 |
| 修改导航路由 | MainActivity.kt | Screen sealed class + NavHost composable |

---

## 项目依赖完整清单

```kotlin
// Compose BOM 统管版本
composeBom = platform("androidx.compose:compose-bom:2024.02.00")

// 核心
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.activity:activity-compose:1.8.2")

// Compose UI
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.6")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Gson
implementation("com.google.code.gson:gson:2.10.1")

// Debug
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```