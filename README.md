# BabyGrowth 🍼

一款专为新手父母设计的宝宝成长记录 Android 应用，帮助您轻松追踪宝宝的日常护理和成长发育。

## ✨ 功能特性

### 📝 多维度日常记录

| 记录类型 | 功能说明 |
|---------|---------|
| 🤱 **喂养记录** | 支持母乳/配方奶/瓶喂，母乳喂养内置左右侧独立计时器 |
| 👶 **换尿布** | 记录小便/大便/混合，支持 12 种大便颜色 + 15 种形状追踪 |
| 😴 **睡眠记录** | 计时模式 + 手动模式，支持次日标记，AAP 睡眠建议参考 |
| 🍎 **辅食记录** | 6 大食物分类，过敏/拒绝反应追踪 |
| 💊 **营养补剂** | 维生素 AD/D3/益生菌/钙/锌/铁/DHA，支持一次添加多条 |
| 📏 **生长指标** | 身高/体重/头围，WHO 参考范围实时提示 |
| 💉 **疫苗接种** | 中国免疫规划（23 种免费 + 10 种自费），自动生成接种计划 |

### 📊 数据可视化

- **今日统计**: 首页一目了然查看喂奶次数、换尿布次数、睡眠时长等
- **记录总览**: 支持日/周/月三种维度切换，周视图柱状图，月视图日历
- **成长曲线**: 基于 WHO 标准的 P3/P50/P97 生长曲线，自动计算百分位数
- **智能提示**: 根据记录间隔自动提醒（如距上次喂奶过久）

### 🎨 个性化

- 6 种主题色：珊瑚粉、薰衣草紫、薄荷绿、天空蓝、暖阳橙、樱花粉
- 深色模式：手动切换或跟随系统

### 💾 数据管理

- 本地 Room 数据库存储，无需联网，隐私安全
- JSON 格式导出/导入，支持跨设备迁移
- 基于 uniqueId 去重，导入不怕数据重复

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 导航 | Navigation Compose |
| 数据库 | Room (KSP) |
| 序列化 | Gson |
| 构建 | Gradle Kotlin DSL + AGP 8.2.2 |
| CI/CD | GitHub Actions |
| 最低版本 | Android 8.0 (API 26) |
| 目标版本 | Android 14 (API 34) |

## 📁 项目结构

```
app/src/main/java/com/baby/growth/
├── BabyGrowthApp.kt              # Application 入口
├── MainActivity.kt               # 唯一 Activity + Navigation Host
│
├── data/                         # 数据层
│   ├── database/AppDatabase.kt   # Room 数据库定义
│   ├── entity/                   # 8 个数据实体
│   │   ├── BabyInfo.kt           # 宝宝信息
│   │   ├── FeedRecord.kt         # 喂养记录
│   │   ├── DiaperRecord.kt       # 尿布记录
│   │   ├── SleepRecord.kt        # 睡眠记录
│   │   ├── FoodRecord.kt         # 辅食记录
│   │   ├── SupplementRecord.kt   # 补充剂记录
│   │   ├── GrowthRecord.kt       # 生长记录
│   │   └── VaccineRecord.kt      # 疫苗记录
│   └── dao/                      # 8 个数据访问对象
│
├── ui/                           # UI 层 (Jetpack Compose)
│   ├── theme/Theme.kt            # 主题系统
│   ├── components/               # 通用 UI 组件
│   ├── home/                     # 首页
│   ├── records/                  # 记录总览
│   ├── record/                   # 各类记录录入/编辑
│   ├── growth/                   # 成长曲线
│   ├── vaccine/                  # 疫苗接种
│   ├── settings/                 # 设置
│   └── profile/                  # 宝宝资料
│
├── service/                      # 前台服务
│   ├── FeedingTimerService.kt    # 母乳喂养计时通知
│   └── SleepTimerService.kt      # 睡眠计时通知
│
└── utils/                        # 工具类
    ├── DateUtils.kt              # 日期处理
    ├── RecordTypes.kt            # 记录类型定义
    ├── SleepTimer.kt             # 睡眠计时器
    ├── BreastfeedingTimer.kt     # 母乳计时器
    ├── SleepAdvice.kt            # AAP 睡眠建议
    ├── VaccineData.kt            # 疫苗数据
    ├── GrowthCurveData.kt        # WHO 生长曲线数据
    ├── DataExporter.kt           # 数据导出/导入
    └── ThemeManager.kt           # 主题管理
```

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Kotlin 1.9.20

### 构建运行

```bash
# 克隆项目
git clone https://github.com/your-username/baby-growth-android.git
cd baby-growth-android

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 运行测试
./gradlew test
```

### 安装

1. 从 [Releases](../../releases) 页面下载最新的 APK
2. 在 Android 设备上安装（需开启"允许未知来源"）
3. 首次打开后设置宝宝信息即可开始使用

## 📱 应用截图

> 首页：宝宝信息概览、今日统计、智能提示、最近记录时间轴

> 记录总览：日/周/月维度切换、统计图表、类型筛选

> 成长曲线：WHO 标准 P3/P50/P97 曲线、百分位计算

> 疫苗接种：免费/自费分类、接种进度、一键标记

## 🔄 发布流程

项目使用 GitHub Actions 自动化发布：

1. 更新 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`
2. 创建并推送 tag：`git tag v1.x.x && git push origin v1.x.x`
3. GitHub Actions 自动：构建 → 签名 → 创建 Release → 上传 APK

签名所需 Secrets：`SIGNING_KEY`、`KEY_ALIAS`、`KEY_STORE_PASSWORD`、`KEY_PASSWORD`

## 📋 数据说明

### 存储方式

所有数据存储在本地 Room 数据库（`baby_growth_db`），无需网络连接。

### 数据导出格式

导出为 JSON 文件，结构如下：

```json
{
  "babyInfo": { ... },
  "feeds": [ ... ],
  "diapers": [ ... ],
  "sleeps": [ ... ],
  "foods": [ ... ],
  "supplements": [ ... ],
  "growthRecords": [ ... ],
  "vaccines": [ ... ]
}
```

导入时基于 `uniqueId` 去重，不会产生重复数据。

### 参考数据来源

- **生长曲线**: WHO 儿童生长标准（0-36 个月）
- **睡眠建议**: 美国儿科学会 (AAP) 推荐
- **疫苗计划**: 中国国家免疫规划程序

## 🤝 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'Add some feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 提交 Pull Request

## 📄 许可证

本项目仅供个人学习和使用。
