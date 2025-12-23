# RookiePlague 插件通用设计模式

## 🎯 概述

本文档总结了 RookiePlague 插件的通用设计模式，这些模式可以直接迁移到其他 Bukkit/Spigot 插件项目中。
RookiePlague 是一个动物瘟疫系统插件，实现了动物繁殖限制、疫病传播模拟、区块环境监控等功能。

---

## 一、核心架构模式

### 分层架构 (Layered Architecture)

| 层级 | 职责 | 示例组件 |
|------|------|----------|
| **主类层** | 插件生命周期管理、依赖注入、启动初始化 | `RookiePlague` |
| **Wrapper层** | 原生事件包装、快速过滤、事件转发 | `AnimalBreedWrapper` |
| **Controller层** | 自定义事件监听、业务调度 | `AnimalBreedController` |
| **Service层** | 业务逻辑实现、外部API调用 | `AnimalBreedService`、`PlagueInfectionService` |
| **Model层** | 数据模型、配置映射 | `AnimalConfig` |
| **DAO层** | 数据持久化访问 | `AnimalDataDao` |
| **Cache层** | 线程安全的数据缓存 | `EnvironmentCache`、`InfectedAnimalCache` |
| **Scheduler层** | 定时任务调度管理 | `PlagueCheckScheduler`、`PlagueDamageScheduler` |
| **Task层** | 异步任务执行 | `PlagueCheckTask`、`PlagueDamageTask` |
| **SPI层** | 接口定义、扩展点 | `ConfigurationLoader`、`MythicMobSpawner` |

**迁移价值**: 清晰的职责分离，每层独立测试和替换

---

## 二、设计模式应用

### 1. SPI (Service Provider Interface) 模式

**定义接口**：

```java
public interface ConfigurationLoader {
    YamlConfiguration loadConfig(String name);
    YamlConfiguration reloadConfig(String name);
    boolean exists(String name);
}
```

**具体实现**：

```java
public class FileConfigurationLoader implements ConfigurationLoader {
    private final Plugin plugin;
    
    public FileConfigurationLoader(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public YamlConfiguration loadConfig(String name) {
        // 文件系统实现
    }
}
```

**适用场景**: 
- 配置加载器（文件/数据库/网络）
- 怪物生成器（MythicMobs/原生/自定义）
- 可插拔的功能模块

---

### 2. Facade (外观) 模式

```java
public class ConfigurationManager {
    private ConfigurationLoader loader;
    private Map<String, YamlConfiguration> cache;
    
    public ConfigurationManager(ConfigurationLoader loader) {
        this.loader = loader;
        this.cache = new HashMap<>();
    }
    
    public YamlConfiguration getConfig(String name) {
        if (!cache.containsKey(name)) {
            cache.put(name, loader.loadConfig(name));
        }
        return cache.get(name);
    }
    
    public YamlConfiguration reloadConfig(String name) {
        YamlConfiguration config = loader.reloadConfig(name);
        cache.put(name, config);
        return config;
    }
}
```

**特点**:
- 隐藏配置加载细节
- 内置缓存机制
- 统一的访问接口

---

### 3. 依赖注入 (Dependency Injection)

```java
// 构造函数注入示例
public class PlagueInfectionService {
    private final PlagueFormulaService plagueFormulaService;
    private final AnimalConfigService animalConfigService;
    private final AnimalDataService animalDataService;
    private final EnvironmentCache environmentCache;
    private final InfectedAnimalCache infectedAnimalCache;
    private final LoggerService loggerService;
    
    public PlagueInfectionService(PlagueFormulaService plagueFormulaService,
                                 AnimalConfigService animalConfigService,
                                 AnimalDataService animalDataService,
                                 AnimalNameService animalNameService,
                                 PluginConfig pluginConfig,
                                 Plugin plugin,
                                 EnvironmentCache environmentCache,
                                 InfectedAnimalCache infectedAnimalCache,
                                 LoggerService loggerService) {
        this.plagueFormulaService = plagueFormulaService;
        this.animalConfigService = animalConfigService;
        this.animalDataService = animalDataService;
        this.environmentCache = environmentCache;
        this.infectedAnimalCache = infectedAnimalCache;
        this.loggerService = loggerService;
    }
}
```

**优势**:
- 解耦组件依赖
- 便于单元测试（Mock注入）
- 提高代码可维护性

---

## 三、通用服务层设计

### 1. LoggerService - 日志服务封装

```java
public class LoggerService {
    private final Logger logger;
    private final String pluginName;
    
    public LoggerService(Plugin plugin) {
        this.logger = plugin.getLogger();
        this.pluginName = plugin.getName();
    }
    
    // 基础日志方法
    public void info(String format, Object... args) {
        logger.info(String.format(format, args));
    }
    
    public void warning(String format, Object... args) {
        logger.warning(String.format(format, args));
    }
    
    // 带标签的调试日志
    public void debug(String tag, String format, Object... args) {
        logger.info(String.format("[%s] %s", tag, String.format(format, args)));
    }
    
    // 带颜色的成功日志
    public void success(String format, Object... args) {
        logger.info("§a" + String.format(format, args));
    }
    
    // 异常日志
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
```

**设计亮点**:
- 统一日志格式
- 支持格式化字符串
- 解耦各组件与 Plugin 的直接依赖
- 可扩展到文件日志、远程日志

---

### 2. Service层业务抽象

| Service | 职责 |
|---------|------|
| `AnimalConfigService` | 动物配置管理、数据查询 |
| `AnimalDataService` | PDC持久化、动物数据读写 |
| `AnimalBreedService` | 动物繁殖业务逻辑 |
| `PlagueInfectionService` | 染疫感染计算和处理 |
| `PlagueFormulaService` | 染疫公式编译和计算 |
| `LanguageService` | 多语言消息管理 |
| `CommandService` | 命令处理业务 |
| `MythicMobsSpawnerService` | MythicMobs集成（SPI实现） |

**设计原则**:
- 单一职责原则
- 业务逻辑集中
- 可复用、可测试

---

## 四、事件驱动架构

### 1. Wrapper → Event → Controller 模式

```
Bukkit原生事件 (EntityBreedEvent)
   ↓
AnimalBreedWrapper (快速过滤 + 类型检查)
   ↓
AnimalBreedRequestEvent (自定义同步事件)
   ↓
AnimalBreedController (监听处理)
   ↓
AnimalBreedService (业务实现)
```

**优势**:
- 原生事件不阻塞（快速返回）
- Wrapper 层只做快速过滤，不处理业务
- 自定义事件可被其他插件监听
- Controller 层专注业务调度
- Service 层实现具体业务逻辑

**实现示例**：

```java
// Step 1: Wrapper 层 - 快速过滤
@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
public void onEntityBreed(@NotNull EntityBreedEvent event) {
    // 快速类型检查 - 只处理动物
    Entity entity = event.getEntity();
    if (!(entity instanceof Animals)) {
        return;
    }
    
    // 获取父母实体
    Entity mother = event.getMother();
    Entity father = event.getFather();
    
    // 快速验证父母都是动物类型
    if (!(mother instanceof Animals) || !(father instanceof Animals)) {
        return;
    }
    
    // 包装为自定义事件
    AnimalBreedRequestEvent customEvent = new AnimalBreedRequestEvent(
        (Animals) mother,
        (Animals) father
    );
    
    // 同步触发自定义事件
    Bukkit.getPluginManager().callEvent(customEvent);
    
    // 如果自定义事件被取消，则取消原生事件
    if (customEvent.isCancelled()) {
        event.setCancelled(true);
    }
}

// Step 2: Controller 层 - 监听自定义事件
@EventHandler(priority = EventPriority.NORMAL)
public void onAnimalBreedRequest(@NotNull AnimalBreedRequestEvent event) {
    Animals mother = event.getMother();
    Animals father = event.getFather();
    
    // 获取动物配置
    AnimalConfig config = animalConfigService.getAnimalConfig(event.getAnimalType());
    if (config == null) return;
    
    // 检查染疫状态
    BreedCheckResult infectionCheck = animalBreedService.checkInfectionStatus(mother, father);
    if (!infectionCheck.isSuccess()) {
        event.setCancelled(true);
        return;
    }
    
    // 检查繁殖次数限制
    BreedCheckResult limitCheck = animalBreedService.checkBreedLimit(mother, father, config.getMaxBreedTimes());
    if (!limitCheck.isSuccess()) {
        event.setCancelled(true);
        return;
    }
    
    // 繁殖成功，处理结果
    animalBreedService.handleSuccessfulBreed(mother, father, config);
}
```

---

### 2. 自定义异步事件设计

```java
public class AnimalBreedRequestEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Animals mother;
    private final Animals father;
    private final String animalType;
    private boolean cancelled;
    private String cancelReason;
    
    public AnimalBreedRequestEvent(@NotNull Animals mother, @NotNull Animals father) {
        super(false);  // false = 同步事件
        this.mother = mother;
        this.father = father;
        this.animalType = mother.getType().name();
        this.cancelled = false;
    }
    
    // Getters
    @NotNull
    public Animals getMother() { return mother; }
    
    @NotNull
    public Animals getFather() { return father; }
    
    @NotNull
    public String getAnimalType() { return animalType; }
    
    // Cancellable 接口实现
    @Override
    public boolean isCancelled() { return cancelled; }
    
    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    
    public String getCancelReason() { return cancelReason; }
    
    public void setCancelReason(String reason) { this.cancelReason = reason; }
    
    // Bukkit 事件系统必需方法
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

**关键点**:
- `super(false)` 标记为同步事件（在主线程触发）
- 实现 `Cancellable` 接口允许事件被取消
- 必须实现 `getHandlerList()` 静态方法
- 核心字段使用 `final` 保证不可变性
- 使用 `@NotNull` 注解明确空值约束
- 提供 `cancelReason` 字段记录取消原因

---

## 五、配置系统设计

### 1. Model驱动的配置解析

```java
public class AnimalConfig {
    /** 动物类型（如 COW, PIG, CHICKEN 等，作为唯一标识） */
    private String type;
    
    /** 动物描述（中文名称） */
    private String desc;
    
    /** 物种因子（影响染疫概率计算的系数） */
    private double speciesFactor;
    
    /** 区块上限（同类动物在单个区块内的数量上限） */
    private int chunkLimit;
    
    /** 死亡生成尸体概率（百分比） */
    private int corpseDropRate;
    
    /** 尸体的CE模型id */
    private String corpseMobid;
    
    /** 最大繁殖次数（每只动物的繁殖次数上限） */
    private int maxBreedTimes;
    
    /** 瘟疫致死时间（未治疗情况下从感染到死亡的时间，单位：秒） */
    private int plagueDeathTime;
    
    // 从Map解析
    private static AnimalConfig parseFromMap(Map<String, Object> map) {
        AnimalConfig config = new AnimalConfig();
        config.setType(getStringValue(map, "type", ""));
        config.setDesc(getStringValue(map, "desc", ""));
        config.setSpeciesFactor(getDoubleValue(map, "speciesFactor", 1.0));
        config.setChunkLimit(getIntValue(map, "chunkLimit", 10));
        config.setCorpseDropRate(getIntValue(map, "corpseDropRate", 50));
        config.setCorpseMobid(getStringValue(map, "corpseMobid", ""));
        config.setMaxBreedTimes(getIntValue(map, "maxBreedTimes", 5));
        config.setPlagueDeathTime(getIntValue(map, "plagueDeathTime", 300));
        return config;
    }
    
    // 类型安全转换
    private int getObjectAsInt(Object obj, int defaultValue) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private double getObjectAsDouble(Object obj, double defaultValue) {
        if (obj instanceof Double) {
            return (Double) obj;
        } else if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
```

**设计特点**:
- 类型安全转换（处理 YAML 的类型不确定性）
- 默认值保护
- 支持 List 格式的 YAML 配置
- 提供静态工厂方法 `parseFromConfig()` 和 `parseToMap()`
- `parseToMap()` 使用 Stream API 转换为 Map，以 type 作为 key
- 提供 `getStringValue()`, `getIntValue()`, `getDoubleValue()` 等类型安全提取方法

---

### 2. 配置热重载支持

```java
public class ConfigurationManager {
    public YamlConfiguration reloadConfig(String name) {
        YamlConfiguration config = loader.reloadConfig(name);
        cache.put(name, config);  // 更新缓存
        return config;
    }
}
```

**配合命令系统**：

```java
@EventHandler(priority = EventPriority.NORMAL)
public void onReloadConfig(ReloadConfigEvent event) {
    commandService.handleReloadConfig(event.getSender());
}
```

---

## 六、初始化流程设计

```java
@Override
public void onEnable() {
    initializeLogger();           // 1. 日志优先
    initializeConfigSystem();     // 2. 配置系统
    initializeLanguage();         // 3. 语言服务
    initializeServices();         // 4. 服务初始化
    registerListeners();          // 5. 监听器注册
    loadConfigurations();         // 6. 加载配置数据
    registerCommands();           // 7. 命令注册
    startPlagueSystem();          // 8. 启动瘟疫系统
}

private void initializeServices() {
    // DAO 层
    animalDataDao = new AnimalDataDao(this);
    
    // Service 层（按依赖顺序）
    commandService = new CommandService(this, loggerService);
    animalDataService = new AnimalDataService(animalDataDao, loggerService);
    animalNameService = new AnimalNameService(animalDataService, animalConfigService, pluginConfig, loggerService);
    animalBreedService = new AnimalBreedService(animalDataService, animalNameService, loggerService);
    plagueFormulaService = new PlagueFormulaService(loggerService);
    
    // Cache 层
    environmentCache = new EnvironmentCache();
    infectedAnimalCache = new InfectedAnimalCache();
    
    // SPI 实现
    mythicMobSpawner = new MythicMobsSpawnerService(loggerService);
    
    // 高层服务
    plagueInfectionService = new PlagueInfectionService(
        plagueFormulaService, animalConfigService, animalDataService,
        animalNameService, pluginConfig, this,
        environmentCache, infectedAnimalCache, loggerService
    );
}
```

**设计原则**:
- **顺序依赖**: 日志 → 配置 → 服务 → 监听器
- **模块化初始化**: 每个步骤独立方法
- **失败日志**: 每步都有日志记录
- **启动反馈**: 最后输出完整状态

**每个初始化方法的模板**：

```java
private void initializeConfigSystem() {
    loggerService.info("正在初始化配置系统...");
    
    // 创建配置加载器（SPI）
    ConfigurationLoader loader = new FileConfigurationLoader(this);
    
    // 创建配置管理器（Facade）
    configManager = new ConfigurationManager(loader);
    
    // 加载主配置
    pluginConfig = new PluginConfig(configManager.getConfig("config.yml"));
    
    // 初始化配置服务
    animalConfigService = new AnimalConfigService(configManager);
    
    loggerService.success("配置系统初始化完成");
}
```

---

## 七、异步处理模式

### 1. 线程安全的随机数

```java
// ❌ 不推荐 - Random 多线程竞争
private Random random = new Random();
int value = random.nextInt(100);

// ✅ 推荐 - 线程安全
int random = ThreadLocalRandom.current().nextInt(1, 101);
```

---

### 2. Scheduler + Task 模式（定时任务）

**Scheduler（调度器）：管理任务的生命周期**

```java
public class PlagueCheckScheduler {
    private final Plugin plugin;
    private final PlagueInfectionService plagueInfectionService;
    private final LoggerService loggerService;
    private BukkitTask scheduledTask;
    private boolean running;
    
    public PlagueCheckScheduler(@NotNull Plugin plugin,
                                @NotNull PlagueInfectionService plagueInfectionService,
                                @NotNull AnimalDataService animalDataService,
                                @NotNull InfectedAnimalCache infectedAnimalCache,
                                @NotNull LoggerService loggerService) {
        this.plugin = plugin;
        this.plagueInfectionService = plagueInfectionService;
        this.loggerService = loggerService;
        this.running = false;
    }
    
    // 启动定时任务
    public void start(long intervalSeconds, long delaySeconds) {
        if (running) return;
        
        long intervalTicks = intervalSeconds * 20L;  // 转换为 tick
        long delayTicks = delaySeconds * 20L;
        
        scheduledTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            new PlagueCheckTask(plugin, plagueInfectionService, animalDataService, infectedAnimalCache, loggerService),
            delayTicks,
            intervalTicks
        );
        
        running = true;
        loggerService.info("染疫检查调度器已启动: 间隔 %d 秒", intervalSeconds);
    }
    
    // 停止定时任务
    public void stop() {
        if (!running) return;
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
        running = false;
        loggerService.info("染疫检查调度器已停止");
    }
    
    // 手动触发一次检查
    public void checkNow() {
        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            new PlagueCheckTask(plugin, plagueInfectionService, animalDataService, infectedAnimalCache, loggerService)
        );
    }
}
```

**Task（异步任务）：执行具体业务逻辑**

```java
public class PlagueCheckTask implements Runnable {
    private final Plugin plugin;
    private final PlagueInfectionService plagueInfectionService;
    private final AnimalDataService animalDataService;
    private final InfectedAnimalCache infectedAnimalCache;
    private final LoggerService loggerService;
    
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        
        try {
            loggerService.debug("PLAGUE_CHECK", "开始染疫检查任务（异步）");
            
            // 遍历所有世界和区块
            int processedChunks = 0;
            for (World world : plugin.getServer().getWorlds()) {
                if (!shouldProcessWorld(world)) continue;
                
                for (Chunk chunk : world.getLoadedChunks()) {
                    // 只处理正在 tick 的区块
                    if (chunk.getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING) continue;
                    if (!chunk.isEntitiesLoaded()) continue;
                    
                    // 重建缓存：扫描已染疫的动物
                    rebuildCacheForChunk(chunk);
                    
                    // 处理该区块（染疫判定）
                    plagueInfectionService.processChunk(chunk);
                    processedChunks++;
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            loggerService.info("染疫检查完成: 处理 %d 个区块，耗时 %d ms", processedChunks, duration);
                
        } catch (Exception e) {
            loggerService.error("染疫检查任务执行异常", e);
        }
    }
    
    // 重建区块内的染疫动物缓存（服务器重启后恢复）
    private void rebuildCacheForChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Animals animal)) continue;
            if (animalDataService.isInfected(animal)) {
                infectedAnimalCache.addInfected(animal.getUniqueId());
            }
        }
    }
}
```

**设计优势**：
- Scheduler 负责任务调度，Task 负责业务执行，职责分离
- 支持可配置的执行间隔和延迟
- 支持手动触发（用于调试或命令）
- Task 异步执行，不阻塞主线程
- 任务内包含异常处理和性能统计

---

### 3. Cache 模式（线程安全缓存）

**为什么需要 Cache？**
- 主线程定期更新环境数据（玩家数、天气等）
- 异步 Task 需要读取这些数据
- 避免在异步线程中调用 Bukkit API（不线程安全）

**实现示例：EnvironmentCache**

```java
public class EnvironmentCache {
    // 使用 volatile 保证可见性
    private volatile int onlinePlayerCount;
    
    // 使用 ConcurrentHashMap 保证线程安全
    private final Map<String, WeatherType> worldWeatherCache;
    
    private volatile long lastUpdateTime;
    
    public EnvironmentCache() {
        this.onlinePlayerCount = 0;
        this.worldWeatherCache = new ConcurrentHashMap<>();
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    // 主线程调用：更新在线玩家数
    public void updateOnlinePlayerCount(int count) {
        this.onlinePlayerCount = count;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    // 主线程调用：更新世界天气
    public void updateWorldWeather(@NotNull World world, @NotNull WeatherType weatherType) {
        worldWeatherCache.put(world.getName(), weatherType);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    // 异步线程安全读取
    public int getOnlinePlayerCount() {
        return onlinePlayerCount;
    }
    
    // 异步线程安全读取
    @NotNull
    public WeatherType getWorldWeather(@NotNull String worldName) {
        return worldWeatherCache.getOrDefault(worldName, WeatherType.CLEAR);
    }
    
    public enum WeatherType {
        CLEAR, RAIN, THUNDER
    }
}
```

**配合定时更新任务**：

```java
public class EnvironmentUpdateScheduler {
    public void start(int intervalSeconds) {
        Bukkit.getScheduler().runTaskTimer(
            plugin,
            new EnvironmentUpdateTask(environmentCache, loggerService),
            0L,  // 立即执行
            intervalSeconds * 20L
        );
    }
}

public class EnvironmentUpdateTask implements Runnable {
    @Override
    public void run() {
        // 主线程执行：更新缓存
        int playerCount = Bukkit.getOnlinePlayers().size();
        environmentCache.updateOnlinePlayerCount(playerCount);
        
        for (World world : Bukkit.getWorlds()) {
            WeatherType weather = world.hasStorm() 
                ? (world.isThundering() ? WeatherType.THUNDER : WeatherType.RAIN)
                : WeatherType.CLEAR;
            environmentCache.updateWorldWeather(world, weather);
        }
        
        loggerService.debug("ENV_UPDATE", "环境数据已更新: 玩家数=%d", playerCount);
    }
}
```

**线程安全要点**：
- 基本类型字段使用 `volatile` 关键字
- 复杂数据结构使用 `ConcurrentHashMap`
- 主线程写入（update），异步线程读取（get）
- 避免在异步线程中直接调用 Bukkit API

---

### 4. 异步→同步切换

```java
// 在 Wrapper 中异步处理
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    // 异步执行：复杂计算、配置读取、PDC检查
    boolean shouldSpawn = shouldSpawnShiny(location, config);
    
    if (shouldSpawn) {
        // 回到主线程操作实体
        Bukkit.getScheduler().runTask(plugin, () -> {
            // 同步执行：生成实体等主线程操作
            spawnShinyMonster(entityType, location, config);
        });
    }
});
```

**线程使用规则**:

| 操作类型 | 线程 | 示例 |
|---------|------|------|
| 配置读取 | 异步 | `config.getString()` |
| 概率计算 | 异步 | `Math.random()` |
| PDC 检查 | 异步 | `chunk.getPersistentDataContainer()` |
| 实体生成 | 同步 | `world.spawnEntity()` |
| 世界修改 | 同步 | `block.setType()` |
| 粒子效果 | 同步 | `world.spawnParticle()` |

---

### 5. 延迟执行

```java
// 延迟 N ticks 后执行（主线程）
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    // 执行需要延迟的操作（如延迟生成实体）
    world.spawnEntity(location, EntityType.COW);
}, delayTicks);

// 延迟执行（异步线程）
Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
    // 执行需要延迟的异步操作
    performAsyncCalculation();
}, delayTicks);
```

---

## 八、可迁移的通用组件清单

| 组件 | 文件 | 迁移难度 | 依赖 | 迁移价值 |
|------|------|----------|------|----------|
| **LoggerService** | `LoggerService.java` | ⭐ 简单 | 仅依赖 Plugin | ⭐⭐⭐⭐⭐ 强烈推荐 |
| **ConfigurationManager** | `ConfigurationManager.java` | ⭐ 简单 | ConfigurationLoader 接口 | ⭐⭐⭐⭐⭐ 强烈推荐 |
| **SPI接口** | `ConfigurationLoader.java` | ⭐ 简单 | 无 | ⭐⭐⭐⭐ 推荐 |
| **Model基类** | `AnimalConfig.java` | ⭐⭐ 中等 | 需调整字段 | ⭐⭐⭐⭐ 推荐 |
| **同步事件模板** | `AnimalBreedRequestEvent.java` | ⭐⭐ 中等 | 需调整字段 | ⭐⭐⭐⭐ 推荐 |
| **Wrapper模式** | `AnimalBreedWrapper.java` | ⭐⭐ 中等 | 业务相关 | ⭐⭐⭐⭐ 推荐 |
| **Controller模式** | `AnimalBreedController.java` | ⭐⭐ 中等 | 业务相关 | ⭐⭐⭐⭐ 推荐 |
| **Scheduler模式** | `PlagueCheckScheduler.java` | ⭐⭐ 中等 | Task 类 | ⭐⭐⭐⭐ 推荐 |
| **AsyncTask模式** | `PlagueCheckTask.java` | ⭐⭐⭐ 复杂 | 业务相关 | ⭐⭐⭐ 参考思路 |
| **Cache模式** | `EnvironmentCache.java` | ⭐⭐ 中等 | 无 | ⭐⭐⭐⭐ 推荐 |
| **LanguageService** | `LanguageService.java` | ⭐⭐ 中等 | ConfigurationManager | ⭐⭐⭐⭐ 推荐 |

---

## 九、最佳实践建议

### 1. 命名规范

```
Wrapper:    原生事件监听 + 快速过滤
Controller: 自定义事件监听 + 调度
Service:    业务逻辑实现
Model:      数据模型
```

**示例**：
- `AnimalBreedWrapper` - 监听 `EntityBreedEvent`
- `AnimalBreedController` - 监听 `AnimalBreedRequestEvent`
- `AnimalBreedService` - 处理动物繁殖业务
- `AnimalConfig` - 动物配置数据模型
- `PlagueCheckScheduler` - 调度染疫检查任务
- `PlagueCheckTask` - 执行异步染疫检查

---

### 2. 日志规范

```java
// 普通信息
loggerService.info("普通信息");
loggerService.info("格式化: %s, 数量: %d", name, count);

// 成功操作（绿色）
loggerService.success("✅ 成功加载配置");

// 警告信息（黄色）
loggerService.warning("⚠️ 警告: %s", message);

// 调试日志（带标签）
loggerService.debug("生成器", "线程: %s", threadName);

// 异常日志
loggerService.error("操作失败: " + e.getMessage(), e);
```

---

### 3. 配置缓存策略

```java
// 启动时加载（自动缓存）
YamlConfiguration config = configManager.getConfig("config.yml");

// 热重载时清除缓存并重新加载
YamlConfiguration newConfig = configManager.reloadConfig("config.yml");

// 服务层重新加载配置
animalConfigService.loadConfig();
```

---

### 4. 依赖注入顺序

```java
// 主类中的初始化顺序
public void onEnable() {
    // 1. 最底层服务（无依赖）
    loggerService = new LoggerService(this);
    
    // 2. 配置系统（依赖 logger）
    ConfigurationLoader loader = new FileConfigurationLoader(this);
    configManager = new ConfigurationManager(loader);
    pluginConfig = new PluginConfig(configManager.getConfig("config.yml"));
    animalConfigService = new AnimalConfigService(configManager);
    
    // 3. 语言服务（依赖 config）
    languageService = new LanguageService(configManager, loggerService);
    
    // 4. DAO 层（依赖 plugin）
    animalDataDao = new AnimalDataDao(this);
    
    // 5. 基础服务（依赖 DAO 和 config）
    animalDataService = new AnimalDataService(animalDataDao, loggerService);
    animalNameService = new AnimalNameService(animalDataService, animalConfigService, pluginConfig, loggerService);
    
    // 6. 业务服务（依赖其他服务）
    animalBreedService = new AnimalBreedService(animalDataService, animalNameService, loggerService);
    plagueFormulaService = new PlagueFormulaService(loggerService);
    
    // 7. Cache 层
    environmentCache = new EnvironmentCache();
    infectedAnimalCache = new InfectedAnimalCache();
    
    // 8. 高层服务（依赖多个组件）
    plagueInfectionService = new PlagueInfectionService(
        plagueFormulaService, animalConfigService, animalDataService,
        animalNameService, pluginConfig, this,
        environmentCache, infectedAnimalCache, loggerService
    );
    
    // 9. 控制器（依赖服务层）
    animalBreedController = new AnimalBreedController(
        animalConfigService, animalBreedService, 
        languageService, loggerService
    );
    
    // 10. 调度器（依赖服务和缓存）
    plagueCheckScheduler = new PlagueCheckScheduler(
        this, plagueInfectionService, animalDataService,
        infectedAnimalCache, loggerService
    );
}
```

---

### 5. 事件优先级选择

| 优先级 | 使用场景 |
|--------|----------|
| `LOWEST` | 最先执行，适合预处理 |
| `LOW` | 早期处理 |
| `NORMAL` | 默认优先级，大部分业务逻辑 |
| `HIGH` | **快速过滤**（Wrapper 层推荐） |
| `HIGHEST` | 最后执行 |
| `MONITOR` | 只读监控，不应修改事件 |

```java
// Wrapper 层使用 HIGH 优先级快速返回
@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
public void onEntityBreed(@NotNull EntityBreedEvent event) {
    // 快速过滤逻辑：类型检查 + 包装自定义事件
}

// Controller 层使用 NORMAL 优先级
@EventHandler(priority = EventPriority.NORMAL)
public void onAnimalBreedRequest(@NotNull AnimalBreedRequestEvent event) {
    // 业务处理：检查配置、验证状态、调用Service
}
```

---

## 十、架构图总结

### 完整架构图

```
┌──────────────────────────────────────────────────┐
│           RookiePlague (主类)                     │
│  - 依赖注入所有Service/Controller/Scheduler       │
│  - 生命周期管理 (onEnable/onDisable)             │
│  - 提供Getter供外部访问                           │
└─────────────────┬────────────────────────────────┘
                  │
      ┌───────────┼───────────┬──────────────┐
      ▼           ▼           ▼              ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Wrapper  │ │Controller│ │Scheduler │ │ Service  │
│  层      │ │   层     │ │   层     │ │   层     │
├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤
│原生事件  │→│自定义事件│ │定时调度  │ │业务逻辑  │
│快速过滤  │ │  监听    │ │任务管理  │ │公式计算  │
│事件转发  │ │  调度    │ │生命周期  │ │数据持久化│
└──────────┘ └──────────┘ └────┬─────┘ └─────┬────┘
                                │             │
                    ┌───────────┼─────────────┼─────────┐
                    ▼           ▼             ▼         ▼
              ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
              │  Task   │ │  Model  │ │  Cache  │ │   DAO   │
              │   层    │ │   层    │ │   层    │ │   层    │
              ├─────────┤ ├─────────┤ ├─────────┤ ├─────────┤
              │异步任务 │ │数据模型 │ │线程安全 │ │数据访问 │
              │区块扫描 │ │配置映射 │ │环境缓存 │ │PDC操作  │
              │染疫检查 │ │         │ │染疫缓存 │ │         │
              └─────────┘ └─────────┘ └─────────┘ └─────────┘
                                 ▲
                                 │
                           ┌─────────┐
                           │   SPI   │
                           │  接口层 │
                           ├─────────┤
                           │可扩展点 │
                           │多实现   │
                           └─────────┘
```

---

### 数据流向图

```
1. 事件触发流程:
   Bukkit Event → Wrapper (过滤) → Custom Event → Controller → Service → DAO/Cache

2. 配置加载流程:
   YAML File → ConfigurationLoader (SPI) → ConfigurationManager (Facade + Cache) → Service → Model

3. 定时任务流程:
   Scheduler (定时调度) → Task (异步执行) → Service (业务处理) → Cache (数据更新)

4. 日志输出流程:
   Service/Controller/Task → LoggerService → Plugin Logger → Console

5. 染疫检查流程:
   PlagueCheckScheduler → PlagueCheckTask (异步) → 扫描区块 → PlagueInfectionService → 计算染疫 → 更新PDC和Cache
```

---

## 十一、迁移检查清单

### 基础组件迁移

- [ ] 复制 `LoggerService.java` 到新项目
- [ ] 复制 `ConfigurationManager.java`
- [ ] 复制 `ConfigurationLoader.java` (SPI接口)
- [ ] 复制 `FileConfigurationLoader.java` (实现类)
- [ ] 根据业务调整 `Model` 类字段

### 架构模式实施

- [ ] 设计自定义事件（参考同步事件模板 `AnimalBreedRequestEvent`）
- [ ] 实现 Wrapper → Controller → Service 三层架构
- [ ] 在主类中应用依赖注入模式
- [ ] 设计并实现 SPI 扩展点
- [ ] 遵循初始化流程设计（8步法：日志→配置→语言→服务→监听器→加载数据→命令→调度器）
- [ ] 实现 Scheduler + Task 异步任务模式
- [ ] 实现线程安全的 Cache 层

### 代码规范

- [ ] 使用 `ThreadLocalRandom` 处理并发随机数
- [ ] 正确处理异步/同步线程切换
- [ ] 统一命名规范（Wrapper/Controller/Service/Model/Scheduler/Task/Cache）
- [ ] 统一日志输出格式
- [ ] 实现配置热重载功能
- [ ] Cache 层使用 volatile 和 ConcurrentHashMap 保证线程安全

### 性能优化

- [ ] 在 Wrapper 层快速过滤（HIGH 优先级）
- [ ] 复杂逻辑异步处理（Scheduler + Task 模式）
- [ ] 配置缓存机制（ConfigurationManager）
- [ ] 环境数据缓存（EnvironmentCache）
- [ ] 避免主线程阻塞（异步 Task 处理区块扫描）

---

## 十二、扩展阅读

### 推荐的设计模式书籍
- 《设计模式：可复用面向对象软件的基础》
- 《Head First 设计模式》

### Bukkit/Spigot 开发资源
- [Spigot API 文档](https://hub.spigotmc.org/javadocs/spigot/)
- [Paper API 文档](https://jd.papermc.io/paper/1.21/)

### 相关技术栈
- Java 21
- Maven 构建工具
- Bukkit/Spigot/Paper API
- MythicMobs API (可选集成)

---

## 结语

这套设计模式在 **Bukkit/Spigot 插件开发**中具有高度通用性，核心思想可扩展到其他 Java 项目。关键在于：

1. **分层清晰**：各层职责明确，便于维护（Wrapper/Controller/Service/DAO/Cache/Scheduler/Task）
2. **解耦设计**：依赖注入 + SPI 模式 + Facade 模式
3. **异步优化**：Scheduler 调度异步 Task，不阻塞主线程
4. **线程安全**：Cache 层使用 volatile 和 ConcurrentHashMap
5. **可测试性**：每个组件独立可测，依赖注入便于 Mock
6. **可扩展性**：SPI 接口支持多种实现（如 MythicMobSpawner）
7. **可配置化**：所有参数从配置文件读取，支持热重载
8. **国际化支持**：LanguageService 实现多语言

建议根据项目实际需求，选择性地采用这些模式，避免过度设计。
