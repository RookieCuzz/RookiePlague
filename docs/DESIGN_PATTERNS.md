# MonsterController 插件通用设计模式

## 🎯 概述

本文档总结了 MonsterController 插件的通用设计模式，这些模式可以直接迁移到其他 Bukkit/Spigot 插件项目中。

---

## 一、核心架构模式

### 分层架构 (Layered Architecture)

| 层级 | 职责 | 示例组件 |
|------|------|----------|
| **主类层** | 插件生命周期管理、依赖注入、启动初始化 | `MonsterController` |
| **Wrapper层** | 原生事件包装、快速过滤、事件转发 | `CreatureSpawnWrapper` |
| **Controller层** | 自定义事件监听、业务调度 | `ShinyMonsterController` |
| **Service层** | 业务逻辑实现、外部API调用 | `ShinyMonsterService` |
| **Model层** | 数据模型、配置映射 | `MonsterConfig` |
| **SPI层** | 接口定义、扩展点 | `ConfigurationLoader`、`ShinyMonsterSpawner` |

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
// 构造函数注入
public class ShinyMonsterService {
    private final Plugin plugin;
    private final ShinyMonsterSpawner spawner;
    private final LoggerService logger;
    private final ShinyMonsterSystemConfig systemConfig;
    
    public ShinyMonsterService(Plugin plugin, 
                               ShinyMonsterSpawner spawner,
                               LoggerService logger,
                               ShinyMonsterSystemConfig systemConfig) {
        this.plugin = plugin;
        this.spawner = spawner;
        this.logger = logger;
        this.systemConfig = systemConfig;
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
| `MonsterConfService` | 配置管理、数据查询 |
| `MonsterCoreService` | PDC持久化、区块检测 |
| `ShinyMonsterService` | 闪光怪物生成逻辑 |
| `CommandService` | 命令处理业务 |

**设计原则**:
- 单一职责原则
- 业务逻辑集中
- 可复用、可测试

---

## 四、事件驱动架构

### 1. Wrapper → Event → Controller 模式

```
Bukkit原生事件 
   ↓
CreatureSpawnWrapper (快速过滤 + 异步)
   ↓
ShinyMonsterSpawnRequestEvent (自定义异步事件)
   ↓
ShinyMonsterController (监听处理)
   ↓
ShinyMonsterService (业务实现)
```

**优势**:
- 原生事件不阻塞（快速返回）
- 异步处理复杂逻辑
- 自定义事件可被其他插件监听

**实现示例**：

```java
// Step 1: Wrapper 层 - 快速过滤
@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
public void onCreatureSpawn(CreatureSpawnEvent event) {
    MonsterConfig config = monsterService.getMonsterConfig(event.getEntityType().name());
    if (config == null) return;
    
    // 快速同步判断
    if (shouldRestrainSpawn(config)) {
        event.setCancelled(true);
        return;
    }
    
    // 异步处理复杂逻辑
    handleShinySpawnAsync(event.getEntityType(), event.getLocation(), config);
}

// Step 2: 发送自定义事件
private void handleShinySpawnAsync(...) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
        if (shouldSpawnShiny(location, config)) {
            ShinyMonsterSpawnRequestEvent customEvent = 
                new ShinyMonsterSpawnRequestEvent(entityType, location, config);
            Bukkit.getPluginManager().callEvent(customEvent);
        }
    });
}

// Step 3: Controller 层 - 监听自定义事件
@EventHandler(priority = EventPriority.NORMAL)
public void onShinyMonsterSpawnRequest(ShinyMonsterSpawnRequestEvent event) {
    shinyMonsterService.handleShinySpawnAsync(
        event.getEntityType(),
        event.getLocation(),
        event.getConfig()
    );
}
```

---

### 2. 自定义异步事件设计

```java
public class ShinyMonsterSpawnRequestEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final EntityType entityType;
    private final Location location;
    private final MonsterConfig config;
    
    public ShinyMonsterSpawnRequestEvent(@NotNull EntityType entityType,
                                        @NotNull Location location,
                                        @NotNull MonsterConfig config) {
        super(true);  // true = 异步事件
        this.entityType = entityType;
        this.location = location;
        this.config = config;
    }
    
    // Getters
    @NotNull
    public EntityType getEntityType() { return entityType; }
    
    @NotNull
    public Location getLocation() { return location; }
    
    @NotNull
    public MonsterConfig getConfig() { return config; }
    
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
- `super(true)` 标记为异步事件
- 必须实现 `getHandlerList()` 静态方法
- 所有字段使用 `final` 保证不可变性
- 使用 `@NotNull` 注解明确空值约束

---

## 五、配置系统设计

### 1. Model驱动的配置解析

```java
public class MonsterConfig {
    private int id;
    private String type;
    private String desc;
    private int rangeChunk;
    private int restrainRate;
    private int spawnRate;
    private double dropMulti;
    private String shinyMob;
    private String ceBlockId;
    
    public MonsterConfig(Map<String, Object> data) {
        this.id = getObjectAsInt(data.get("id"), 0);
        this.type = (String) data.getOrDefault("type", "");
        this.desc = (String) data.getOrDefault("desc", "");
        this.rangeChunk = getObjectAsInt(data.get("rangeChunk"), 0);
        this.restrainRate = getObjectAsInt(data.get("restrainRate"), 0);
        this.spawnRate = getObjectAsInt(data.get("spawnRate"), 0);
        this.dropMulti = getObjectAsDouble(data.get("dropMulti"), 1.0);
        this.shinyMob = (String) data.getOrDefault("shinyMob", "");
        this.ceBlockId = (String) data.getOrDefault("ceBlockId", "");
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
- 支持 List 和 Map 两种 YAML 格式
- 提供静态工厂方法 `parseFromConfig()` 和 `parseToMap()`

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
    initializeConfiguration();    // 2. 配置加载
    initializeServices();         // 3. 服务初始化
    registerListeners();          // 4. 监听器注册
    registerCommands();           // 5. 命令注册
    printStartupInfo();           // 6. 启动信息
}

private void initializeServices() {
    initializeMonsterService();
    initializeMonsterCoreService();
    initializeShinyMonsterSpawner();
    initializeShinyMonsterService();
    initializeCommandService();
}
```

**设计原则**:
- **顺序依赖**: 日志 → 配置 → 服务 → 监听器
- **模块化初始化**: 每个步骤独立方法
- **失败日志**: 每步都有日志记录
- **启动反馈**: 最后输出完整状态

**每个初始化方法的模板**：

```java
private void initializeMonsterService() {
    monsterService = new MonsterConfService(configManager);
    monsterService.loadConfig();
    loggerService.info("成功加载怪物配置，共 %d 个怪物配置项", 
                       monsterService.getMonsterCount());
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

### 2. 异步→同步切换

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

### 3. 延迟执行

```java
// 延迟 N ticks 后执行（配置化延迟）
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    spawnShinyMonster(entityType, location, config);
}, systemConfig.getSpawnDelayTicks());
```

---

## 八、可迁移的通用组件清单

| 组件 | 文件 | 迁移难度 | 依赖 | 迁移价值 |
|------|------|----------|------|----------|
| **LoggerService** | `LoggerService.java` | ⭐ 简单 | 仅依赖 Plugin | ⭐⭐⭐⭐⭐ 强烈推荐 |
| **ConfigurationManager** | `ConfigurationManager.java` | ⭐ 简单 | ConfigurationLoader 接口 | ⭐⭐⭐⭐⭐ 强烈推荐 |
| **SPI接口** | `ConfigurationLoader.java` | ⭐ 简单 | 无 | ⭐⭐⭐⭐ 推荐 |
| **Model基类** | `MonsterConfig.java` | ⭐⭐ 中等 | 需调整字段 | ⭐⭐⭐ 参考 |
| **异步事件模板** | `ShinyMonsterSpawnRequestEvent.java` | ⭐⭐ 中等 | 需调整字段 | ⭐⭐⭐⭐ 推荐 |
| **Wrapper模式** | `CreatureSpawnWrapper.java` | ⭐⭐⭐ 复杂 | 业务相关 | ⭐⭐⭐ 参考思路 |
| **Controller模式** | `ShinyMonsterController.java` | ⭐⭐ 中等 | 业务相关 | ⭐⭐⭐⭐ 推荐 |

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
- `CreatureSpawnWrapper` - 监听 `CreatureSpawnEvent`
- `ShinyMonsterController` - 监听 `ShinyMonsterSpawnRequestEvent`
- `ShinyMonsterService` - 处理闪光怪物业务
- `MonsterConfig` - 怪物配置数据模型

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
monsterService.loadConfig();
```

---

### 4. 依赖注入顺序

```java
// 主类中的初始化顺序
public void onEnable() {
    // 1. 最底层服务（无依赖）
    loggerService = new LoggerService(this);
    
    // 2. 配置服务（依赖 logger）
    configManager = new ConfigurationManager(new FileConfigurationLoader(this));
    
    // 3. 业务服务（依赖 config 和 logger）
    monsterService = new MonsterConfService(configManager);
    
    // 4. 高层服务（依赖其他服务）
    shinyMonsterService = new ShinyMonsterService(
        this, 
        shinySpawner, 
        loggerService, 
        systemConfig
    );
    
    // 5. 控制器（依赖所有服务）
    shinyMonsterController = new ShinyMonsterController(
        shinyMonsterService, 
        monsterCoreService, 
        loggerService
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
public void onCreatureSpawn(CreatureSpawnEvent event) {
    // 快速过滤逻辑
}

// Controller 层使用 NORMAL 优先级
@EventHandler(priority = EventPriority.NORMAL)
public void onShinyMonsterSpawnRequest(ShinyMonsterSpawnRequestEvent event) {
    // 业务处理
}
```

---

## 十、架构图总结

### 完整架构图

```
┌─────────────────────────────────────────┐
│        MonsterController (主类)         │
│  - 依赖注入所有Service                   │
│  - 生命周期管理                          │
│  - 提供Getter供外部访问                  │
└─────────────────┬───────────────────────┘
                  │
      ┌───────────┼───────────┐
      ▼           ▼           ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│ Wrapper  │ │Controller│ │ Service  │
│  层      │ │   层     │ │   层     │
├──────────┤ ├──────────┤ ├──────────┤
│原生事件  │→│自定义事件│→│业务逻辑  │
│快速过滤  │ │  监听    │ │API调用   │
│异步转发  │ │  调度    │ │外部集成  │
└──────────┘ └──────────┘ └─────┬────┘
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
              ┌─────────┐  ┌─────────┐  ┌─────────┐
              │  Model  │  │   SPI   │  │  Utils  │
              │   层    │  │  接口层 │  │  工具层 │
              ├─────────┤  ├─────────┤  ├─────────┤
              │数据模型 │  │可扩展点 │  │通用工具 │
              │配置映射 │  │多实现   │  │辅助方法 │
              └─────────┘  └─────────┘  └─────────┘
```

---

### 数据流向图

```
1. 事件触发流程:
   Bukkit Event → Wrapper (过滤) → Async Task → Custom Event → Controller → Service

2. 配置加载流程:
   YAML File → ConfigurationLoader → ConfigurationManager (缓存) → Service → Model

3. 日志输出流程:
   Service/Controller → LoggerService → Plugin Logger → Console
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

- [ ] 设计自定义事件（参考异步事件模板）
- [ ] 实现 Wrapper → Controller → Service 三层架构
- [ ] 在主类中应用依赖注入模式
- [ ] 设计并实现 SPI 扩展点
- [ ] 遵循初始化流程设计（6步法）

### 代码规范

- [ ] 使用 `ThreadLocalRandom` 处理并发随机数
- [ ] 正确处理异步/同步线程切换
- [ ] 统一命名规范（Wrapper/Controller/Service/Model）
- [ ] 统一日志输出格式
- [ ] 实现配置热重载功能

### 性能优化

- [ ] 在 Wrapper 层快速过滤（HIGH 优先级）
- [ ] 复杂逻辑异步处理
- [ ] 配置缓存机制
- [ ] 避免主线程阻塞

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

1. **分层清晰**：各层职责明确，便于维护
2. **解耦设计**：依赖注入 + SPI 模式
3. **异步优化**：不阻塞主线程
4. **可测试性**：每个组件独立可测
5. **可扩展性**：SPI 接口支持多种实现

建议根据项目实际需求，选择性地采用这些模式，避免过度设计。
