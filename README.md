# 麻将分析系统 (Majiang Analysis System)

一个智能麻将分析 Android 应用，通过摄像头识别麻将牌，记录游戏过程，并推荐最优出牌策略。

## 功能特性

- 📱 **摄像头识别**: 通过 ML Kit 识别麻将牌
- 🎯 **出牌记录**: 实时记录所有玩家的出牌历史
- 🤖 **智能推荐**: 基于胜率算法推荐最优出牌
- 🎲 **多规则支持**: 支持广东麻将、四川麻将、杭州麻将等多种规则
- 📊 **数据统计**: 记录游戏统计和胜率分析

## 项目架构

```
├── app/                          # Android 应用模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/majiang/
│   │   │   │   ├── ui/           # UI 层
│   │   │   │   ├── viewmodel/    # ViewModel 层
│   │   │   │   ├── repository/   # 数据仓储层
│   │   │   │   ├── model/        # 数据模型
│   │   │   │   ├── analyzer/     # 麻将分析引擎
│   │   │   │   └── vision/       # 摄像头和识别
│   │   │   └── res/              # 资源文件
│   │   └── test/                 # 单元测试
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 技术栈

- **开发语言**: Kotlin/Java
- **UI 框架**: Jetpack Compose / Material 3
- **视觉识别**: ML Kit Vision、TensorFlow Lite
- **数据存储**: Room Database
- **架构模式**: MVVM + Repository Pattern
- **异步处理**: Coroutines
- **依赖注入**: Hilt

## 快速开始

### 前置要求
- Android Studio 2023.1+
- JDK 11+
- 目标 API 级别 28+
- 最低 API 级别 21

### 安装步骤
```bash
git clone https://github.com/zhanxl/majiang.git
cd majiang
./gradlew build
```

### 运行应用
```bash
./gradlew installDebug
./gradlew connectedAndroidTest
```

## 核心功能模块

### 1. 牌张识别模块 (Vision Module)
- 实时摄像头预览
- 使用 ML Kit 和自训练的 TensorFlow Lite 模型识别麻将牌
- 支持单张识别和批量识别

### 2. 游戏管理模块
- 管理四个玩家的牌堆
- 追踪出牌历史
- 支持撤销操作

### 3. 分析推荐模块
- 基于当前牌面和历史出牌计算胜率
- 蒙特卡洛模拟
- 危险牌识别

### 4. 规则引擎
- 标准胡牌判断
- 多种地方规则支持
- 点数计算

### 5. 数据统计模块
- 游戏历史记录
- 玩家统计
- 胜率分析

## 文件说明

详见各模块的 README.md 文件。

## 使用示例

```kotlin
// 初始化摄像头识别
val visionManager = TileVisionManager(context)
visionManager.startCamera()

// 识别牌张
val tiles = visionManager.recognizeTiles(bitmap)

// 创建游戏
val game = MaijiangGame(rule = GuangdongRule())
game.addPlayer("玩家1")

// 添加出牌记录
game.recordDiscard("玩家1", Tile.WAN_1)

// 获取推荐出牌
val recommendations = game.getRecommendedTiles()
```

## 贡献指南

欢迎提交 Pull Request 和 Issue！

## 许可证

MIT License

## 联系方式

- 作者: zhanxl
- 邮箱: your-email@example.com
