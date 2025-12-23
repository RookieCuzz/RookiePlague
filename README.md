# RookiePlague - 动物瘟疫系统插件

## 📋 项目概述

RookiePlague 是一个基于 Paper/Spigot 的 Minecraft 服务器插件，实现了完整的动物瘟疫生态系统。通过模拟真实的疫病传播机制，为服务器增加动物养殖的挑战性和策略性。

### 核心功能

- 🦠 **疫病传播系统**：基于自定义公式的染疫概率计算，支持环境因子（天气、玩家数量、区块密度等）
- 🐑 **繁殖限制机制**：限制动物繁殖次数，防止无限繁殖
- ⚰️ **死亡与尸体系统**：染疫动物死亡后生成尸体（支持 CustomEntity 模型）
- 🌍 **区块环境监控**：实时监控区块内动物密度和环境数据
- 🔄 **热重载配置**：支持不重启服务器动态更新配置
- 🌐 **国际化支持**：多语言消息系统

## ✨ 技术特性

- 🏗️ **分层架构**：Wrapper → Controller → Service → DAO/Cache，职责清晰
- 🔌 **SPI 扩展**：支持 MythicMobs 集成，可扩展自定义生物
- ⚡ **异步优化**：Scheduler + Task 模式，区块扫描不阻塞主线程
- 🛡️ **线程安全**：Cache 层使用 volatile 和 ConcurrentHashMap
- 🔄 **热重载**：配置、公式、语言文件支持实时重载
- 🧮 **表达式引擎**：使用 exp4j 支持自定义染疫公式
- 📊 **PDC 持久化**：动物数据保存到实体持久化容器

## 📦 项目结构

```
RookiePlague/
├── src/main/java/com/cuzz/rookiePlague/
│   ├── RookiePlague.java                      # 插件主类
│   ├── cache/                                  # 缓存层
│   │   ├── EnvironmentCache.java             # 环境数据缓存
│   │   └── InfectedAnimalCache.java          # 染疫动物缓存
│   ├── config/                                 # 配置管理
│   │   ├── ConfigurationManager.java         # 配置管理器（Facade）
│   │   ├── PluginConfig.java                 # 主配置模型
│   │   ├── spi/
│   │   │   └── ConfigurationLoader.java      # 配置加载器接口（SPI）
│   │   └── impl/
│   │       └── FileConfigurationLoader.java  # 文件加载器实现
│   ├── controller/                             # 控制器层
│   │   ├── AnimalBreedController.java        # 繁殖控制器
│   │   ├── BreedCountModifyController.java   # 繁殖次数修改控制器
│   │   ├── ConfigReloadController.java       # 配置重载控制器
│   │   └── PlagueSimulateController.java     # 瘟疫模拟控制器
│   ├── dao/
│   │   └── AnimalDataDao.java                # 动物数据 DAO
│   ├── event/                                  # 自定义事件
│   │   ├── AnimalBreedRequestEvent.java      # 繁殖请求事件
│   │   ├── BreedCountModifyRequestEvent.java # 繁殖次数修改事件
│   │   ├── ConfigReloadRequestEvent.java     # 配置重载事件
│   │   └── PlagueSimulateRequestEvent.java   # 瘟疫模拟事件
│   ├── model/
│   │   └── AnimalConfig.java                 # 动物配置模型
│   ├── scheduler/                              # 调度器层
│   │   ├── EnvironmentUpdateScheduler.java   # 环境更新调度器
│   │   ├── PlagueCheckScheduler.java         # 瘟疫检查调度器
│   │   └── PlagueDamageScheduler.java        # 瘟疫伤害调度器
│   ├── service/                                # 服务层
│   │   ├── AnimalBreedService.java           # 繁殖服务
│   │   ├── AnimalConfigService.java          # 配置服务
│   │   ├── AnimalDataService.java            # 数据服务
│   │   ├── AnimalNameService.java            # 名称服务
│   │   ├── CommandService.java               # 命令服务
│   │   ├── LanguageService.java              # 语言服务
│   │   ├── LoggerService.java                # 日志服务
│   │   ├── MythicMobsSpawnerService.java     # MythicMobs 生成器（SPI 实现）
│   │   ├── PlagueFormulaService.java         # 瘟疫公式服务
│   │   ├── PlagueInfectionService.java       # 瘟疫感染服务
│   │   └── spi/
│   │       └── MythicMobSpawner.java         # MythicMobs 生成器接口
│   ├── task/                                   # 异步任务层
│   │   ├── EnvironmentUpdateTask.java        # 环境更新任务
│   │   ├── PlagueCheckTask.java              # 瘟疫检查任务
│   │   └── PlagueDamageTask.java             # 瘟疫伤害任务
│   └── wrapper/                                # Wrapper 层
│       ├── AnimalBreedWrapper.java           # 繁殖事件包装器
│       ├── CommandTabCompleter.java          # 命令补全
│       └── CommandWrapper.java               # 命令包装器
├── src/main/resources/
│   ├── language/
│   │   └── zh_CN.yml                         # 中文语言文件
│   ├── animal.yml                             # 动物配置文件
│   ├── config.yml                             # 主配置文件
│   └── plugin.yml                             # 插件描述文件
├── docs/                                       # 文档目录
│   └── DESIGN_PATTERNS.md                    # 设计模式文档
└── pom.xml                                     # Maven 配置
```

## 🚀 快速开始

### 1. 环境要求

- Java 21+
- Paper/Spigot 1.21+
- Maven 3.6+

### 2. 编译插件

```bash
mvn clean package
```

### 3. 安装使用

1. 将生成的 jar 文件复制到服务器 `plugins` 目录
2. 启动服务器，插件会自动生成配置文件
3. 编辑 `plugins/RookiePlague/animal.yml` 配置动物属性
4. 使用 `/animal reload` 重载配置

### 4. 基本命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/rp reload` | 重载所有配置文件 | OP |
| `/rp breed <动物UUID> set <次数>` | 设置动物繁殖次数 | OP |
| `/rp breed <动物UUID> add <次数>` | 增加动物繁殖次数 | OP |
| `/rp plague simulate` | 模拟一次瘟疫检查 | OP |

## 📖 架构说明

### 核心组件

| 层级 | 组件 | 职责 |
|------|------|------|
| **Wrapper** | AnimalBreedWrapper | 监听原生繁殖事件，快速过滤并转发 |
| **Controller** | AnimalBreedController | 监听自定义事件，调度业务处理 |
| **Service** | PlagueInfectionService | 染疫感染计算和处理 |
| **Service** | PlagueFormulaService | 公式编译和计算（exp4j） |
| **Service** | AnimalBreedService | 繁殖次数检查和更新 |
| **DAO** | AnimalDataDao | PDC 数据持久化访问 |
| **Cache** | EnvironmentCache | 环境数据缓存（线程安全） |
| **Cache** | InfectedAnimalCache | 染疫动物缓存 |
| **Scheduler** | PlagueCheckScheduler | 定时调度染疫检查任务 |
| **Task** | PlagueCheckTask | 异步执行区块扫描和染疫判定 |

### 数据流程

#### 1. 插件启动流程
```
初始化日志服务 → 初始化配置系统 → 初始化语言服务 → 初始化服务层 
→ 注册监听器 → 加载配置数据 → 注册命令 → 启动瘟疫系统
```

#### 2. 繁殖事件流程
```
EntityBreedEvent → AnimalBreedWrapper（快速过滤）
→ AnimalBreedRequestEvent（自定义事件）
→ AnimalBreedController（检查染疫状态、繁殖次数）
→ AnimalBreedService（更新 PDC 数据）
```

#### 3. 瘟疫检查流程
```
PlagueCheckScheduler（每N秒）→ PlagueCheckTask（异步）
→ 扫描区块内动物 → PlagueInfectionService（计算染疫概率）
→ 更新 PDC 和 InfectedAnimalCache
```

## 🔧 配置文件

### animal.yml 示例

```yaml
- type: 'SHEEP'
  desc: '羊'
  speciesFactor: 0.9        # 物种因子（影响染疫概率）
  chunkLimit: 18            # 区块上限
  corpseDropRate: 70        # 尸体掉落率（%）
  corpseMobid: 'animal_corpse_large'
  maxBreedTimes: 5          # 最大繁殖次数
  plagueDeathTime: 320      # 瘟疫致死时间（秒）

- type: 'CHICKEN'
  desc: '鸡'
  speciesFactor: 1.3
  chunkLimit: 20
  corpseDropRate: 60
  corpseMobid: 'animal_corpse_small'
  maxBreedTimes: 8
  plagueDeathTime: 240
```

### 动物配置字段说明

| 字段 | 类型 | 说明 | 示例值 |
|------|------|------|----------|
| type | String | 动物类型（Bukkit EntityType） | SHEEP, COW, CHICKEN |
| desc | String | 动物中文名称 | 羊, 牛, 鸡 |
| speciesFactor | double | 物种因子（影响染疫概率） | 0.9, 1.2, 1.5 |
| chunkLimit | int | 单个区块内的数量上限 | 18, 15, 20 |
| corpseDropRate | int | 死亡时尸体掉落概率（%） | 70, 80, 60 |
| corpseMobid | String | 尸体的 CustomEntity 模型 ID | animal_corpse_large |
| maxBreedTimes | int | 单只动物最大繁殖次数 | 5, 8, 10 |
| plagueDeathTime | int | 染疫后致死时间（秒） | 320, 240, 180 |

### 主配置字段说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `language` | 语言文件名称 | zh_CN |
| `plague.enabled` | 是否启用瘟疫系统 | true |
| `plague.formula` | 染疫概率计算公式 | 见下方 |
| `plague.infection-check-interval` | 染疫检查间隔（秒） | 300 |
| `plague.damage.enabled` | 是否启用瘟疫伤害 | true |
| `plague.damage.interval` | 伤害间隔（秒） | 60 |
| `environment.update-interval` | 环境数据更新间隔（秒） | 30 |

### 瘟疫公式说明

默认公式：`speciesFactor * (1 + playerCount * 0.01) * weatherFactor * (density / 10)`

- `speciesFactor`：动物配置中的物种因子
- `playerCount`：在线玩家数量
- `weatherFactor`：天气因子（晴天=1.0, 雨天=1.2, 雷暴=1.5）
- `density`：区块内动物密度

可使用 exp4j 支持的数学函数，如 `min()`, `max()`, `sqrt()` 等

## 💻 代码示例

### 基础使用

```java
// 获取插件实例
RookiePlague plugin = (RookiePlague) Bukkit.getPluginManager().getPlugin("RookiePlague");

// 获取服务
AnimalConfigService configService = plugin.getAnimalConfigService();
AnimalDataService dataService = plugin.getAnimalDataService();

// 查询动物配置
AnimalConfig sheep = configService.getAnimalConfig("SHEEP");
if (sheep != null) {
    double factor = sheep.getSpeciesFactor();
    int maxBreeds = sheep.getMaxBreedTimes();
    int deathTime = sheep.getPlagueDeathTime();
}

// 检查动物是否染疫
Animals animal = (Animals) entity;
boolean infected = dataService.isInfected(animal);

// 获取/设置繁殖次数
int breedCount = dataService.getBreedCount(animal);
dataService.setBreedCount(animal, 3);
```

### 瘟疫系统集成

```java
// 手动触发一次瘟疫检查
PlagueCheckScheduler scheduler = plugin.getPlagueCheckScheduler();
scheduler.checkNow();

// 获取环境缓存数据
EnvironmentCache envCache = plugin.getEnvironmentCache();
int playerCount = envCache.getOnlinePlayerCount();
WeatherType weather = envCache.getWorldWeather("world");

// 使用瘟疫公式服务计算概率
PlagueFormulaService formulaService = plugin.getPlagueFormulaService();
Map<String, Double> variables = new HashMap<>();
variables.put("speciesFactor", 1.2);
variables.put("playerCount", (double) playerCount);
variables.put("weatherFactor", 1.0);
variables.put("density", 8.0);

double probability = formulaService.calculate(variables);
```

### 自定义事件监听

```java
// 监听繁殖请求事件
@EventHandler
public void onAnimalBreedRequest(AnimalBreedRequestEvent event) {
    Animals mother = event.getMother();
    Animals father = event.getFather();
    String animalType = event.getAnimalType();
    
    // 获取配置
    AnimalConfig config = configService.getAnimalConfig(animalType);
    if (config == null) return;
    
    // 自定义逻辑
    if (someCondition) {
        event.setCancelled(true);
        event.setCancelReason("自定义取消原因");
    }
}

// 监听配置重载事件
@EventHandler
public void onConfigReload(ConfigReloadRequestEvent event) {
    CommandSender sender = event.getSender();
    sender.sendMessage("配置重载完成！");
}
```

## 🔌 扩展指南

### 1. 实现自定义 MythicMobs 生成器

```java
public class CustomMythicMobSpawner implements MythicMobSpawner {
    @Override
    public boolean isEnabled() {
        // 检查是否启用
        return Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }
    
    @Override
    public boolean spawn(String mobId, Location location) {
        // 实现生成逻辑
        return MythicBukkit.inst().getMobManager().spawnMob(mobId, location) != null;
    }
}
```

### 2. 添加新的调度任务

```java
// 1. 创建 Task
public class CustomTask implements Runnable {
    @Override
    public void run() {
        // 异步执行的逻辑
    }
}

// 2. 创建 Scheduler
public class CustomScheduler {
    private BukkitTask scheduledTask;
    
    public void start(int intervalSeconds) {
        scheduledTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            new CustomTask(),
            0L,
            intervalSeconds * 20L
        );
    }
    
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
    }
}

// 3. 在主类中启动
customScheduler = new CustomScheduler();
customScheduler.start(60);
```

### 3. 自定义染疫公式

在 `config.yml` 中修改 `plague.formula`：

```yaml
plague:
  formula: "speciesFactor * min(density / 5, 2.0) * (1 + playerCount * 0.02)"
```

支持的变量：
- `speciesFactor`: 物种因子
- `playerCount`: 在线玩家数
- `weatherFactor`: 天气因子（1.0/1.2/1.5）
- `density`: 区块动物密度

支持的函数（exp4j）：
- 基础运算：`+`, `-`, `*`, `/`, `^`
- 函数：`min()`, `max()`, `sqrt()`, `abs()`, `log()`, `sin()`, `cos()` 等

## 📊 性能特性

| 特性 | 实现方式 | 优势 |
|------|----------|------|
| **配置查询** | ConcurrentHashMap 缓存 | O(1) 查询，线程安全 |
| **区块扫描** | 异步 Task，只处理 ENTITY_TICKING 区块 | 不阻塞主线程 |
| **环境数据** | Cache 层缓存，主线程定期更新 | 异步线程安全读取 |
| **染疫检查** | PlagueCheckScheduler 可配置间隔 | 可根据服务器性能调整 |
| **公式计算** | exp4j 预编译表达式 | 避免重复解析 |
| **PDC 访问** | 异步读取，同步写入 | 符合 Bukkit API 规范 |

### 推荐配置

- **小型服务器**（<20人）：
  - `infection-check-interval: 180`（3分钟）
  - `environment.update-interval: 60`（1分钟）

- **中型服务器**（20-50人）：
  - `infection-check-interval: 300`（5分钟）
  - `environment.update-interval: 30`（30秒）

- **大型服务器**（>50人）：
  - `infection-check-interval: 600`（10分钟）
  - `environment.update-interval: 60`（1分钟）

## 📚 文档

- [DESIGN_PATTERNS.md](docs/DESIGN_PATTERNS.md) - 插件设计模式和架构文档

### 配置文件模板

- `animal.yml` - 动物配置模板（插件首次启动自动生成）
- `config.yml` - 主配置文件（包含瘟疫系统、调度器等配置）
- `language/zh_CN.yml` - 中文语言文件

## 🔧 依赖项

### 必需依赖

- **Paper API** (1.21-R0.1-SNAPSHOT)
- **SnakeYAML** (2.0)
- **exp4j** (0.4.8)

### 可选依赖

- **MythicMobs** (5.6.1) - 用于生成自定义尸体模型

## 📄 许可证

本项目采用 MIT 许可证。

## 👥 作者

- **cuzz** - 初始开发和架构设计

## 🙏 致谢

- Paper API 团队
- MythicMobs 插件
- exp4j 数学表达式引擎

---

**注意**：本插件为动物瘟疫系统的完整实现，采用分层架构设计，可作为 Bukkit/Spigot 插件开发的参考示例。
