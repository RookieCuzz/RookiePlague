package com.cuzz.rookiePlague.service;

import com.cuzz.rookiePlague.cache.EnvironmentCache;
import com.cuzz.rookiePlague.cache.EnvironmentCache.WeatherType;
import com.cuzz.rookiePlague.cache.InfectedAnimalCache;
import com.cuzz.rookiePlague.config.PluginConfig;
import com.cuzz.rookiePlague.model.AnimalConfig;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 染疫感染服务
 * 
 * <p>Service层：处理染疫相关的业务逻辑</p>
 * <p>职责：</p>
 * <ul>
 *   <li>计算动物的染疫概率</li>
 *   <li>构建公式计算所需的变量</li>
 *   <li>判断动物是否应该染疫</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class PlagueInfectionService {
    
    private final PlagueFormulaService plagueFormulaService;
    private final AnimalConfigService animalConfigService;
    private final AnimalDataService animalDataService;
    private final AnimalNameService animalNameService;
    private final PluginConfig pluginConfig;
    private final Plugin plugin;
    private final EnvironmentCache environmentCache;
    private final InfectedAnimalCache infectedAnimalCache;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param plagueFormulaService 公式计算服务
     * @param animalConfigService 动物配置服务
     * @param animalDataService 动物数据服务
     * @param animalNameService 动物名称服务
     * @param pluginConfig 插件配置
     * @param plugin 插件实例
     * @param environmentCache 环境数据缓存
     * @param infectedAnimalCache 染疫动物缓存
     * @param loggerService 日志服务
     */
    public PlagueInfectionService(@NotNull PlagueFormulaService plagueFormulaService,
                                  @NotNull AnimalConfigService animalConfigService,
                                  @NotNull AnimalDataService animalDataService,
                                  @NotNull AnimalNameService animalNameService,
                                  @NotNull PluginConfig pluginConfig,
                                  @NotNull Plugin plugin,
                                  @NotNull EnvironmentCache environmentCache,
                                  @NotNull InfectedAnimalCache infectedAnimalCache,
                                  @NotNull LoggerService loggerService) {
        this.plagueFormulaService = plagueFormulaService;
        this.animalConfigService = animalConfigService;
        this.animalDataService = animalDataService;
        this.animalNameService = animalNameService;
        this.pluginConfig = pluginConfig;
        this.plugin = plugin;
        this.environmentCache = environmentCache;
        this.infectedAnimalCache = infectedAnimalCache;
        this.loggerService = loggerService;
    }
    
    /**
     * 处理单个区块的染疫检查（优化版：一次遍历）
     * 
     * @param chunk 区块
     */
    public void processChunk(@NotNull Chunk chunk) {
        // 一次遍历完成：统计 + 收集 + 按物种分组
        Map<EntityType, AnimalGroup> groups = new HashMap<>();
        
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Animals animal)) continue;
            
            EntityType type = entity.getType();
            AnimalGroup group = groups.computeIfAbsent(type, AnimalGroup::new);
            group.add(animal);
        }
        
        loggerService.debug("INFECTION", "区块 [%d,%d] 发现 %d 个物种组", 
            chunk.getX(), chunk.getZ(), groups.size());
        
        // 对每个物种组进行处理
        for (AnimalGroup group : groups.values()) {
            processAnimalGroup(group);
        }
    }
    
    /**
     * 处理单个物种组的染疫逻辑
     * 
     * @param group 动物分组
     */
    private void processAnimalGroup(@NotNull AnimalGroup group) {
        // 获取动物配置
        AnimalConfig config = animalConfigService.getAnimalConfig(group.getType().name());
        if (config == null) {
            loggerService.debug("INFECTION", "物种 %s 无配置，跳过", group.getType());
            return;
        }
        
        int count = group.size();
        int limit = config.getChunkLimit();
        
        // 只有超限时才计算染疫
        if (count <= limit) {
            loggerService.debug("INFECTION", "物种 %s 未超限 (%d/%d)，跳过", 
                group.getType(), count, limit);
            return;
        }
        
        loggerService.debug("INFECTION", "物种 %s 超限 (%d/%d)，开始计算染疫", 
            group.getType(), count, limit);
        
        // 构建计算变量（所有动物共用）
        Map<String, Double> variables = buildGroupVariables(group, config, count, limit);
        
        // 计算染疫概率（只计算一次）
        double probability = plagueFormulaService.calculate(variables);
        
        loggerService.debug("INFECTION", "物种 %s 染疫概率: %.4f", group.getType(), probability);
        
        // 收集需要染疫的动物
        List<UUID> toInfect = new ArrayList<>();
        
        for (Animals animal : group.getAnimals()) {
            // 跳过已染疫的
            if (animalDataService.isInfected(animal)) continue;
            
            // 根据概率随机判断
            if (Math.random() < probability) {
                toInfect.add(animal.getUniqueId());
            }
        }
        
        if (!toInfect.isEmpty()) {
            loggerService.debug("INFECTION", "物种 %s 将染疫 %d 只动物", 
                group.getType(), toInfect.size());
            applyInfectionOnMainThread(toInfect);
        }
    }
    
    /**
     * 构建物种组的计算变量
     * 
     * <p>⚠️ 异步安全：只读取缓存数据，不调用World API</p>
     * 
     * @param group 动物分组
     * @param config 动物配置
     * @param count 数量
     * @param limit 上限
     * @return 变量映射
     */
    private Map<String, Double> buildGroupVariables(@NotNull AnimalGroup group,
                                                    @NotNull AnimalConfig config,
                                                    int count,
                                                    int limit) {
        Map<String, Double> variables = new HashMap<>();
        
        // 基础变量
        variables.put("baseChance", pluginConfig.getBasePlagueChance());
        variables.put("speciesFactor", config.getSpeciesFactor());
        variables.put("count", (double) count);
        variables.put("limit", (double) limit);
        
        // 从缓存读取环境因子（异步安全）
        int onlinePlayers = environmentCache.getOnlinePlayerCount();
        variables.put("players", (double) onlinePlayers);
        
        // 天气因子（从缓存读取）
        String worldName = group.getWorldName();
        if (worldName != null) {
            WeatherType weather = environmentCache.getWorldWeather(worldName);
            variables.put("weatherFactor", getWeatherFactor(weather));
        } else {
            variables.put("weatherFactor", 1.0);
        }
        
        // 生物群系因子（暂时使用默认值）
        variables.put("biomeFactor", 1.0);
        
        return variables;
    }
    
    /**
     * 根据天气类型获取天气因子
     * 
     * @param weatherType 天气类型
     * @return 天气因子
     */
    private double getWeatherFactor(@NotNull WeatherType weatherType) {
        return switch (weatherType) {
            case THUNDER -> pluginConfig.getWeatherFactorThunder();
            case RAIN -> pluginConfig.getWeatherFactorRain();
            case CLEAR -> pluginConfig.getWeatherFactorClear();
        };
    }
    
    /**
     * 在主线程应用染疫状态
     * 
     * @param uuids 需要染疫的动物UUID列表
     */
    private void applyInfectionOnMainThread(@NotNull List<UUID> uuids) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            int successCount = 0;
            
            for (UUID uuid : uuids) {
                // 验证实体仍然存在
                Entity entity = Bukkit.getEntity(uuid);
                if (entity == null || !entity.isValid()) {
                    loggerService.debug("INFECTION", "实体 %s 不存在或无效，跳过", uuid);
                    continue;
                }
                
                if (!(entity instanceof Animals animal)) {
                    loggerService.debug("INFECTION", "实体 %s 不是动物，跳过", uuid);
                    continue;
                }
                
                // 再次检查是否已染疫（防止并发问题）
                if (animalDataService.isInfected(animal)) {
                    loggerService.debug("INFECTION", "动物 %s 已染疫，跳过", uuid);
                    continue;
                }
                
                // 应用染疫状态（会自动记录染疫时间）
                animalDataService.setInfected(animal, true);
                
                // 更新动物名称（会自动组合状态）
                animalNameService.updateAnimalName(animal);
                
                // 添加到染疫动物缓存
                infectedAnimalCache.addInfected(uuid);
                
                successCount++;
                
                loggerService.info("动物染疫: %s (%s)", animal.getType(), uuid);
            }
            
            if (successCount > 0) {
                loggerService.info("本次染疫成功: %d 只动物", successCount);
            }
        });
    }
    
    /**
     * 动物分组辅助类
     * 
     * <p>按物种对动物进行分组，优化染疫计算</p>
     */
    public static class AnimalGroup {
        private final EntityType type;
        private final List<Animals> animals;
        private String worldName;  // 缓存世界名称
        
        public AnimalGroup(@NotNull EntityType type) {
            this.type = type;
            this.animals = new ArrayList<>();
        }
        
        public void add(@NotNull Animals animal) {
            animals.add(animal);
            // 缓存第一个动物的世界名称
            if (worldName == null && animal.getWorld() != null) {
                worldName = animal.getWorld().getName();
            }
        }
        
        public EntityType getType() {
            return type;
        }
        
        public List<Animals> getAnimals() {
            return animals;
        }
        
        public int size() {
            return animals.size();
        }
        
        @Nullable
        public String getWorldName() {
            return worldName;
        }
    }
}
