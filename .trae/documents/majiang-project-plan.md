# 麻将分析系统 - 项目完善计划

## 项目现状分析

### 已完成
- ✅ 项目配置文件（build.gradle, settings.gradle, gradle.properties）
- ✅ AndroidManifest.xml（权限声明、Activity 注册）
- ✅ 基础资源文件（strings.xml, themes.xml）
- ✅ 依赖声明（Compose, CameraX, ML Kit, Room, Hilt, TFLite 等）

### 缺失（核心问题）
- ❌ **无任何 Kotlin/Java 源代码** — 整个 `app/src/main/java/com/majiang/` 目录不存在
- ❌ 无 Application 类（Manifest 中引用了 `.MaijiangApplication`）
- ❌ 无 MainActivity（Manifest 中引用了 `.ui.MainActivity`）
- ❌ 无数据模型、分析引擎、视觉识别、UI 界面等任何业务代码
- ❌ 无测试代码

---

## 实施计划

按依赖关系从底层到上层逐步实现，共 7 个阶段：

### 阶段 1：基础框架搭建
**目标**：创建项目骨架，使应用能够编译和启动

1. **创建 Application 类** — `com.majiang.MaijiangApplication`
   - 继承 `Application`，添加 `@HiltAndroidApp` 注解
   - 初始化 Timber 日志

2. **创建 MainActivity** — `com.majiang.ui.MainActivity`
   - 继承 `ComponentActivity`，添加 `@AndroidEntryPoint` 注解
   - 设置 Compose 内容，引用主导航图

3. **创建导航定义** — `com.majiang.ui.navigation.MajiangNavigation`
   - 定义路由：主屏幕、游戏屏幕、摄像头识别屏幕、历史记录屏幕、统计屏幕、设置屏幕
   - 使用 Compose Navigation

4. **创建主屏幕** — `com.majiang.ui.MainScreen`
   - 底部导航栏 + NavHost 布局
   - 5 个 Tab：游戏、识别、历史、统计、设置

### 阶段 2：数据模型层
**目标**：定义核心数据结构

5. **牌张模型** — `com.majiang.model.Tile`
   - 枚举类定义所有麻将牌（万、条、筒、风、箭）
   - 牌的类别、编号、显示名称

6. **牌组模型** — `com.majiang.model.TileSet`
   - 一副完整麻将牌（136 张）
   - 洗牌、发牌逻辑

7. **玩家模型** — `com.majiang.model.Player`
   - 手牌列表、出牌历史、碰/杠/吃记录
   - 玩家位置（东南西北）

8. **游戏状态模型** — `com.majiang.model.GameState`
   - 当前阶段（摸牌、出牌、碰杠、胡牌）
   - 当前玩家、剩余牌数、牌墙

9. **游戏记录模型** — `com.majiang.model.GameRecord`
   - 用于持久化的游戏记录
   - 包含时间、玩家、结果、分数

10. **规则接口** — `com.majiang.model.rule.MahjongRule`
    - 定义规则接口：胡牌判断、计分、合法操作
    - 实现广东麻将规则 — `GuangdongRule`
    - 实现四川麻将规则 — `SichuanRule`

### 阶段 3：分析引擎
**目标**：实现核心麻将分析算法

11. **胡牌判断器** — `com.majiang.analyzer.WinChecker`
    - 标准胡牌判断（3N+2 结构）
    - 七对子判断
    - 十三幺判断

12. **听牌分析器** — `com.majiang.analyzer.ReadyAnalyzer`
    - 计算当前听哪些牌
    - 计算进张数（有效进张张数）

13. **出牌推荐器** — `com.majiang.analyzer.RecommendationEngine`
    - 基于向听数计算最优出牌
    - 考虑已出牌信息（危险牌判断）
    - 出牌推荐结果模型 — `TileRecommendation`

14. **危险牌分析器** — `com.majiang.analyzer.DangerAnalyzer`
    - 基于其他玩家出牌推测手牌
    - 标记危险牌等级（安全/注意/危险）

15. **蒙特卡洛模拟器** — `com.majiang.analyzer.MonteCarloSimulator`
    - 模拟随机抽牌计算胜率
    - 支持可配置的模拟次数

### 阶段 4：数据持久化层
**目标**：实现 Room 数据库和 Repository

16. **Room Entity 定义**
    - `GameRecordEntity` — 游戏记录
    - `PlayerStatsEntity` — 玩家统计
    - `DiscardRecordEntity` — 出牌记录

17. **Room DAO**
    - `GameRecordDao` — 游戏记录的 CRUD
    - `PlayerStatsDao` — 玩家统计查询
    - `DiscardRecordDao` — 出牌记录查询

18. **Room Database** — `com.majiang.repository.MajiangDatabase`
    - 数据库定义，版本管理

19. **Repository 实现**
    - `GameRepository` — 游戏数据仓储
    - `StatsRepository` — 统计数据仓储

### 阶段 5：视觉识别模块
**目标**：实现摄像头和牌张识别

20. **摄像头管理器** — `com.majiang.vision.CameraManager`
    - CameraX 集成
    - 预览、拍照、图像分析

21. **牌张识别器** — `com.majiang.vision.TileRecognizer`
    - ML Kit 对象检测集成
    - TensorFlow Lite 模型推理
    - 识别结果回调

22. **识别结果处理** — `com.majiang.vision.RecognitionResult`
    - 识别结果数据类
    - 置信度过滤

### 阶段 6：ViewModel 层
**目标**：实现各屏幕的 ViewModel

23. **GameViewModel** — `com.majiang.viewmodel.GameViewModel`
    - 游戏状态管理
    - 出牌记录操作
    - 获取推荐出牌

24. **CameraViewModel** — `com.majiang.viewmodel.CameraViewModel`
    - 摄像头控制
    - 识别结果处理

25. **HistoryViewModel** — `com.majiang.viewmodel.HistoryViewModel`
    - 游戏历史列表
    - 详情查看

26. **StatsViewModel** — `com.majiang.viewmodel.StatsViewModel`
    - 统计数据聚合
    - 胜率分析

27. **SettingsViewModel** — `com.majiang.viewmodel.SettingsViewModel`
    - 规则选择
    - 偏好设置

### 阶段 7：UI 界面层
**目标**：实现所有 Compose 界面

28. **游戏主界面** — `com.majiang.ui.game.GameScreen`
    - 四方手牌展示
    - 出牌区域
    - 推荐出牌高亮

29. **出牌记录界面** — `com.majiang.ui.game.DiscardPanel`
    - 出牌历史列表
    - 撤销操作

30. **摄像头识别界面** — `com.majiang.ui.camera.CameraScreen`
    - 摄像头预览
    - 识别结果叠加层
    - 手动修正识别结果

31. **历史记录界面** — `com.majiang.ui.history.HistoryScreen`
    - 游戏列表
    - 筛选和排序

32. **统计界面** — `com.majiang.ui.stats.StatsScreen`
    - 胜率图表
    - 玩家对比

33. **设置界面** — `com.majiang.ui.settings.SettingsScreen`
    - 规则选择
    - 识别灵敏度调节

34. **Hilt DI 模块** — `com.majiang.di.AppModule`
    - 提供 Repository、Analyzer、Database 等依赖

35. **通用 UI 组件** — `com.majiang.ui.components`
    - `TileComposable` — 单张牌的 Compose 组件
    - `TileHandComposable` — 手牌展示组件
    - `RecommendationBadge` — 推荐标记

---

## 文件结构总览

```
app/src/main/java/com/majiang/
├── MaijiangApplication.kt
├── model/
│   ├── Tile.kt
│   ├── TileSet.kt
│   ├── Player.kt
│   ├── GameState.kt
│   ├── GameRecord.kt
│   └── rule/
│       ├── MahjongRule.kt
│       ├── GuangdongRule.kt
│       └── SichuanRule.kt
├── analyzer/
│   ├── WinChecker.kt
│   ├── ReadyAnalyzer.kt
│   ├── RecommendationEngine.kt
│   ├── DangerAnalyzer.kt
│   ├── MonteCarloSimulator.kt
│   └── TileRecommendation.kt
├── repository/
│   ├── MajiangDatabase.kt
│   ├── entity/
│   │   ├── GameRecordEntity.kt
│   │   ├── PlayerStatsEntity.kt
│   │   └── DiscardRecordEntity.kt
│   ├── dao/
│   │   ├── GameRecordDao.kt
│   │   ├── PlayerStatsDao.kt
│   │   └── DiscardRecordDao.kt
│   ├── GameRepository.kt
│   └── StatsRepository.kt
├── vision/
│   ├── CameraManager.kt
│   ├── TileRecognizer.kt
│   └── RecognitionResult.kt
├── viewmodel/
│   ├── GameViewModel.kt
│   ├── CameraViewModel.kt
│   ├── HistoryViewModel.kt
│   ├── StatsViewModel.kt
│   └── SettingsViewModel.kt
├── di/
│   └── AppModule.kt
└── ui/
    ├── MainActivity.kt
    ├── navigation/
    │   └── MajiangNavigation.kt
    ├── MainScreen.kt
    ├── components/
    │   ├── TileComposable.kt
    │   ├── TileHandComposable.kt
    │   └── RecommendationBadge.kt
    ├── game/
    │   ├── GameScreen.kt
    │   └── DiscardPanel.kt
    ├── camera/
    │   └── CameraScreen.kt
    ├── history/
    │   └── HistoryScreen.kt
    ├── stats/
    │   └── StatsScreen.kt
    └── settings/
        └── SettingsScreen.kt
```

## 实施优先级

1. **阶段 1**（基础框架）— 最高优先级，没有它应用无法运行
2. **阶段 2**（数据模型）— 高优先级，所有模块依赖模型
3. **阶段 3**（分析引擎）— 高优先级，核心业务逻辑
4. **阶段 6**（ViewModel）— 中优先级，连接模型和 UI
5. **阶段 7**（UI 界面）— 中优先级，用户可见
6. **阶段 4**（数据持久化）— 中低优先级，可先用内存数据
7. **阶段 5**（视觉识别）— 低优先级，需要模型文件和硬件测试

## 技术要点

- 使用 Kotlin 编写所有代码
- UI 全部使用 Jetpack Compose + Material 3
- 依赖注入使用 Hilt
- 异步操作使用 Coroutines + Flow
- 数据库使用 Room
- 摄像头使用 CameraX
- 识别使用 ML Kit + TFLite
