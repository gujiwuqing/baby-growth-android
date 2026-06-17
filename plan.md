# UI 全面优化方案

> 本文档为 BabyGrowth 项目的 UI 全面优化计划，分为多个阶段，每阶段可独立交付。

---

## 现状问题总结

| 问题类别 | 具体表现 |
|---------|---------|
| 时间/日期选择器 | 直接使用 Material3 原生 DatePicker/TimePicker，样式原生粗糙，与应用整体温馨风格割裂 |
| 选择器使用不一致 | DiaperRecordScreen 将 DatePicker/TimePicker 内联在页面中，其余 5 个 Screen 使用 Dialog 包裹，体验不统一 |
| 大量重复代码 | 6 个 RecordScreen 中日期/时间选择器的状态管理和 Dialog 模板代码（50-70 行/页）完全拷贝粘贴 |
| 表单输入样式单一 | OutlinedTextField 只有 12dp 圆角一种样式，缺少 focus/error/label 等状态反馈 |
| 颜色 Token 未完全落地 | `TextSecondary`、`TextHint` 是顶层常量而非 CompositionLocal，深色模式下仍使用浅色值 |
| 排版 Token 未完全落地 | 大量页面硬编码 `13.sp`、`15.sp`、`FontWeight.Bold`，未使用 `BabyGrowthTypography` |
| 动效缺失 | 页面转场、列表增删、计时器切换等场景几乎没有动画，体感生硬 |
| 卡片缺乏层次 | BabyCard 全部为 0dp elevation + 1dp border，视觉扁平，缺少主次区分 |
| 底部导航单调 | 标准 Material3 NavigationBar，缺少选中动效和个性化设计 |
| 空状态过于简陋 | 三层圆环 + emoji 的方案虽温馨，但所有空状态长得一样，缺少差异化 |

---

## 阶段一：核心组件重构（优先级最高）

### 1.1 自定义日期时间选择器

**目标**：替换 Material3 原生 DatePicker/TimePicker，设计符合应用温馨风格的自定义选择器。

**当前问题**：
- M3 TimePicker 使用系统默认的表盘样式，与应用配色无关
- M3 DatePicker 日历样式受限于 Material3 规范，无法自定义圆角、间距、字体
- 两者弹出的 Dialog 边框、按钮样式与 BabyCard 体系不协调

**方案**：

```
ui/components/pickers/
├── BabyDatePicker.kt        # 自定义日期选择器
├── BabyTimePicker.kt        # 自定义时间选择器（滚轮式）
├── BabyDateTimePicker.kt    # 组合组件：日期 + 时间一体化
└── PickerDialog.kt          # 统一的选择器 Dialog 容器
```

**BabyTimePicker（滚轮式时间选择器）**：
- 采用双列滚轮（小时 0-23 + 分钟 0-59），替代 M3 表盘式
- 滚轮使用 `LazyColumn` + `SnapFlingBehavior`，惯性吸附到整点
- 选中项居中放大（`scale 1.2x`），上下项逐渐缩小 + 透明度衰减
- 选中项背景为 `primaryContainer` 圆角胶囊，文字色 `onPrimaryContainer`
- 中间分隔线为两条 `DividerColor` 水平线
- 整体包裹在 `BabyCard` 中，与应用卡片体系一致

**BabyDatePicker（日历式日期选择器）**：
- 顶部：年月切换，左右箭头按钮 + 居中年月文字（`titleMedium`）
- 中部：7 列网格日历，当天用 `primary` 圆点标记，选中日用 `primary` 圆形填充
- 星期标题行使用 `labelSmall`，灰色
- 日期格子 40dp × 40dp，选中态有 `scale` 动画
- 限制范围：宝宝生日 ~ 今天
- 整体包裹在 `BabyCard` 中

**PickerDialog（统一容器）**：
- 基于 `Dialog` + `BabyCard` 而非 M3 `AlertDialog`
- 圆角 `Radius.xl`（20dp），`shadow` 8dp
- 底部双按钮区：取消（`TextButton`）+ 确认（`PrimaryButton` 半宽）
- 入场动画：从底部滑入 + 渐显

**BabyDateTimePicker（一体化组件）**：
- 将日期选择 + 时间选择合并在同一个 Dialog 中
- 上部为紧凑日期选择（日期滚轮：月/日），下部为时间滚轮
- 减少用户需要弹出的 Dialog 数量（从 2 次减少为 1 次）

### 1.2 抽取通用日期时间触发器

**目标**：消除 6 个 RecordScreen 中 50-70 行的重复代码。

**方案**：在 `ui/components/` 中新增 `DateTimeInput.kt`：

```kotlin
@Composable
fun DateTimeInput(
    dateTime: Long,
    onDateTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "记录时间",
    minDate: Long? = null,
    maxDate: Long? = null,
)
```

- 内部封装 `showDatePicker` / `showTimePicker` 状态
- 展示为 `BabyCard`，左侧 emoji 图标 + 格式化日期时间文字 + 右侧编辑图标
- 点击直接弹出 `BabyDateTimePicker` 一体化 Dialog
- 所有 RecordScreen 统一替换，每个页面减少约 60 行代码

### 1.3 DiaperRecordScreen 内联选择器修正

- 删除内联的 `DatePicker` 和 `TimePicker`
- 替换为 `DateTimeInput` 组件，与其他页面保持一致

---

## 阶段二：表单输入体系升级

### 2.1 自定义输入框组件 `BabyTextField`

**目标**：替代直接使用 `OutlinedTextField`，统一样式和交互。

```
ui/components/BabyTextField.kt
```

**设计规范**：
- 圆角 `Radius.lg`（16dp），比当前 12dp 更柔和
- 未聚焦：`cardBorder` 色 1dp 边框 + `cardBackground` 填充
- 聚焦：`primary` 色 2dp 边框 + `primaryContainer` at 20% 填充，带 `animateBorderStroke` 过渡动画
- 错误态：`StatusColor.Error` 边框 + `errorLight` 填充 + 底部抖动动画（`shake` keyframes）
- 标签（label）放在输入框上方而非 Material3 的浮动标签，减少布局跳动
- 数字输入变体：内置 `KeyboardType.Number` + 输入过滤 + 单位后缀
- 备注输入变体：`minLines = 3`，右下角字数统计（可选）

### 2.2 数值滑块组件 `BabySlider`

- 用于生长记录（身高/体重/头围）等连续数值输入
- 替代当前的 OutlinedTextField 手动输入数字
- 轨道色 `primaryContainer`，进度色 `primary`，滑块为 20dp 圆形 + 阴影
- 滑块上方浮窗显示当前数值（tooltip 样式）
- 下方保留手动输入 TextField 作为精确输入备选

### 2.3 选择标签组件优化 `FilterTag` → `BabyChip`

**当前问题**：FilterTag 选中态仅改变 alpha，视觉反馈太弱。

**改进**：
- 选中态：`primary` 填充 + `onPrimary` 文字 + `scale 1.05` 动画
- 未选中态：`cardBackground` 填充 + `cardBorder` 边框
- 切换动画：`animateColorAsState` + `animateDpAsState` 平滑过渡
- 支持左侧 emoji 图标位（如 🍼 母乳、🌙 睡眠）

---

## 阶段三：卡片与布局体系升级

### 3.1 BabyCard 层次感增强

**当前问题**：所有 BabyCard 都是 0dp elevation + 1dp border，视觉完全扁平。

**方案**：引入三级卡片层级：

| 层级 | 用途 | 样式 |
|------|------|------|
| `elevation = 0` | 内嵌卡片、列表项 | 1dp `cardBorder`，无阴影（当前样式） |
| `elevation = 1` | 主要内容卡片 | 2dp 阴影 + `cardBorder`，轻微浮起 |
| `elevation = 2` | 强调卡片（今日统计等） | 4dp 阴影 + `primaryContainer` 渐变背景 |

- 在 `BabyCard` 中新增 `elevation` 参数（默认 0），保持向后兼容
- 阴影颜色使用 `BabyGrowthColors.shadow`
- 强调卡片使用从上到下的渐变：`primaryContainer` → `cardBackground`

### 3.2 首页布局重设计

**BabyInfoHeader 优化**：
- 头像区域增大到 64dp，添加主题色渐变环形边框（3dp）
- 宝宝名字使用 `titleLarge`，月龄使用 `bodyMedium` + `TextSecondary`
- 添加轻柔入场动画：头像 `fadeIn + scaleIn`，文字 `slideInHorizontally`

**TodayStatsCard 优化**：
- 统计数字添加数字翻转动画（`AnimatedContent` + `slideInVertically`）
- 每个统计项添加类型色小圆点标识
- 卡片使用 `elevation = 2` 强调

**SmartTipsBanner 优化**：
- 改为可滑动的横向卡片（`LazyRow`），每条提示一张小卡片
- 卡片使用对应类型的 `primaryContainer` 背景
- 左侧 emoji 放大到 24sp

### 3.3 TimelineItem 优化

- 时间列改用 `bodySmall` + `TextSecondary`，对齐方式改为 `top`
- 圆点添加脉冲光晕动画（最近的 1-2 条记录），暗示"最新"
- 卡片内 emoji 图标改为 36dp，增加识别度
- 添加左滑手势：滑出"编辑"和"删除"操作按钮（`SwipeToDismissBox`）

---

## 阶段四：动效与转场

### 4.1 页面转场动画

在 `MainActivity.kt` 的 `NavHost` 中为每个 composable 添加 `enterTransition` / `exitTransition`：

```kotlin
composable(
    route = "home",
    enterTransition = { fadeIn(animationSpec = tween(300)) },
    exitTransition = { fadeOut(animationSpec = tween(200)) }
)
```

- Tab 间切换：`fadeIn` + `fadeOut`（200-300ms）
- 进入子页面：`slideInHorizontally(initialOffsetX = { it })` + `fadeOut`
- 返回：`slideOutHorizontally(targetOffsetX = { it })` + `fadeIn`
- Dialog 页面：`scaleIn(0.9f → 1f)` + `fadeIn`

### 4.2 列表动画

- 记录列表使用 `AnimatedVisibility` 包裹每个 item，新增时 `fadeIn + slideInVertically`
- 删除时使用 `shrinkVertically` + `fadeOut`
- 列表项间距添加 `animateItemPlacement()` (LazyColumn)

### 4.3 计时器动画

- 计时器数字使用 `AnimatedContent` 实现翻牌效果
- 计时器运行时，卡片边框添加呼吸灯效果（`primary` 色 `alpha 0.3 ↔ 0.6` 循环）
- 开始/停止按钮添加 `scale` 点击反馈

### 4.4 按钮与交互反馈

- `PrimaryButton` 点击时 `scale(0.96f)` 按压效果 + `animateScale` 回弹
- `QuickActionButton` 点击时图标 `rotate(15deg)` 微旋转
- `FilterTag` 切换时添加 `scale(1.0 → 1.05 → 1.0)` 弹跳动画

---

## 阶段五：深色模式修复与主题打磨

### 5.1 修复深色模式颜色泄露

**当前 Bug**：`TextSecondary`、`TextHint` 是顶层 `val` 常量（浅色值），在深色模式下被直接引用的地方仍显示浅色灰。

**涉及文件**：
- `TimelineItem.kt` — `color = TextSecondary`（第 63、139 行）
- `CommonComponents.kt` — `FilterTag` 的 `textColor`（第 356 行）、`EmptyState` 的 subtitle color（第 217 行）
- `BabyGrowthTypography` 的 `statLabel` 默认 color = `TextSecondary`

**修复方案**：
- 将 `TextSecondary`、`TextHint`、`DividerColor` 移入 `BabyGrowthColors` 扩展颜色系统
- 通过 `BabyGrowthTheme.colors.textSecondary` 访问，确保深色模式自动切换
- 全局搜索替换直接引用

### 5.2 状态栏适配

- 当前 `window.statusBarColor = colorScheme.background.toArgb()` 在 Android 15+ 已废弃
- 改为使用 `enableEdgeToEdge()` + `WindowInsets` 系统，已在 MainActivity 调用但缺少 padding 适配
- 为 TopAppBar 和底部 NavigationBar 添加 `WindowInsets.navigationBars` padding

### 5.3 主题色对比度优化

- 部分主题色（如 `sakura` #FFB6C1、`sunshine` #F5C5A3）作为文字或图标色时对比度不足
- 为每个主题计算一个 `primaryDark` 变体（用于文字场景），确保 WCAG AA 对比度

---

## 阶段六：底部导航与页面框架

### 6.1 自定义底部导航栏

**目标**：替换标准 M3 NavigationBar，设计更具辨识度的底部导航。

**设计**：
- 导航栏背景：`cardBackground` + 顶部 1dp `cardBorder` 分隔线
- 未选中项：`TextSecondary` 图标 + 文字
- 选中项：`primary` 色 32dp 圆形药丸背景（alpha 15%）+ `primary` 图标 + `primary` 文字
- 切换动画：药丸背景 `animateDpAsState` 滑移，图标 `scale(1.0 → 1.2)` 弹跳
- 整体高度 64dp，图标 24dp，文字 `labelSmall`

### 6.2 TopAppBar 优化

- 添加返回页面的渐变背景（从 `surfaceTint` 到 `background` 的 80dp 渐变）
- 标题使用 `titleMedium`，返回按钮添加圆形涟漪效果
- 滚动时 TopAppBar 添加 `shadowElevation` 提升效果

---

## 阶段七：特殊页面打磨

### 7.1 成长曲线页面（GrowthScreen）

- 曲线图使用 Canvas 自绘，添加渐变填充（`primary` at 20% alpha → transparent）
- 数据点添加 `scaleIn` 入场动画
- P3/P50/P97 参考线使用虚线，各用不同透明度区分
- 当前测量点高亮：脉冲光晕 + 数值标签

### 7.2 疫苗接种页面（VaccineScreen）

- 已接种项：文字添加 ~~删除线~~ + `alpha 0.5f`
- 进度环：将百分比数字改为 `CircularProgressIndicator` 可视化进度环
- Tab 切换添加 `HorizontalPager` 滑动手势支持

### 7.3 设置页面（SettingsScreen）

- 主题色选择器：色块改为 32dp 圆形，选中态添加 3dp 白色内环 + `primary` 外环
- 数据操作按钮添加图标前缀（导出→📤，导入→📥，清空→🗑️）
- 各 Section 之间添加 `Spacing.xxl` 间距 + `SectionHeader` 分隔

---

## 阶段八：代码质量与一致性

### 8.1 排版 Token 全面替换

**目标**：消除所有硬编码字号，统一使用 `BabyGrowthTypography`。

**需要补充的 Token**：
```kotlin
data class BabyGrowthTypography(
    // 现有...
    val caption: TextStyle,      // 13sp Normal — 补充，用于辅助说明
    val chip: TextStyle,         // 13sp Medium — 补充，用于标签/Chip
)
```

**替换范围**：全局搜索 `fontSize = X.sp` 和 `fontWeight = FontWeight.X`，逐一替换为对应 Token。

### 8.2 清理死代码

- 删除 `HomeScreen.kt` 中未使用的 `QuickRecordGrid` composable
- 删除 `Icons.Default.ArrowBack` 的冗余导入（已有 `AutoMirrored` 版本）
- 修正 `SupplementRecordScreen` 中 "📷 备注" 按钮的 emoji（改为 "📝 备注"）

### 8.3 废弃 API 替换

- `Divider` → `HorizontalDivider`（ProfileScreen 等）
- `window.statusBarColor` → Edge-to-Edge + `WindowInsets`

### 8.4 输入校验 UI 反馈

- 数值型 TextField 添加实时校验：空值、超范围时显示 `isError = true` + 红色提示文字
- 保存按钮在必填字段未填时 `enabled = false` + 颜色衰减

---

## 实施优先级建议

| 优先级 | 阶段 | 预计工作量 | 视觉提升 |
|-------|------|-----------|---------|
| P0 | 阶段一：日期时间选择器重构 | 3-4 天 | 极高（用户高频接触） |
| P0 | 阶段五 5.1：深色模式修复 | 0.5 天 | 高（修复 Bug） |
| P1 | 阶段二：表单输入体系 | 2 天 | 高 |
| P1 | 阶段四：动效与转场 | 2-3 天 | 高（质感提升） |
| P2 | 阶段三：卡片与布局 | 2 天 | 中高 |
| P2 | 阶段六：底部导航 | 1-2 天 | 中 |
| P3 | 阶段七：特殊页面 | 2-3 天 | 中 |
| P3 | 阶段八：代码质量 | 1-2 天 | 低（内部质量） |

**建议执行顺序**：P0 → P1 → P2 → P3，每阶段完成后构建验证。

---

## 涉及的核心文件清单

```
# 新增文件
ui/components/pickers/BabyDatePicker.kt
ui/components/pickers/BabyTimePicker.kt
ui/components/pickers/BabyDateTimePicker.kt
ui/components/pickers/PickerDialog.kt
ui/components/DateTimeInput.kt
ui/components/BabyTextField.kt
ui/components/BabySlider.kt
ui/components/BabyChip.kt

# 重点修改文件
ui/theme/Theme.kt                    — 扩展颜色系统、排版 Token
ui/components/CommonComponents.kt    — 按钮动效、FilterTag 升级
ui/components/TimelineItem.kt        — 动画、深色模式修复
ui/components/BabyCard.kt            — 层级系统
MainActivity.kt                      — 页面转场、底部导航
ui/home/HomeScreen.kt                — 首页布局重设计
ui/record/*.kt (6个文件)              — 替换选择器、表单组件
ui/records/RecordsScreen.kt          — 列表动画
ui/growth/GrowthScreen.kt            — 曲线图优化
ui/vaccine/VaccineScreen.kt          — 进度可视化
ui/settings/SettingsScreen.kt        — 主题选择器优化
ui/profile/ProfileScreen.kt          — 废弃 API 替换
```
