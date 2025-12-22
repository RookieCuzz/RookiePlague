package com.cuzz.rookiePlague.task;

import com.cuzz.rookiePlague.cache.InfectedAnimalCache;
import com.cuzz.rookiePlague.config.PluginConfig;
import com.cuzz.rookiePlague.model.AnimalConfig;
import com.cuzz.rookiePlague.service.AnimalConfigService;
import com.cuzz.rookiePlague.service.AnimalDataService;
import com.cuzz.rookiePlague.service.LoggerService;
import com.cuzz.rookiePlague.service.spi.MythicMobSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 瘟疫伤害任务
 * 
 * <p>主线程定期任务：对染疫动物造成伤害，处理死亡</p>
 * <p>职责：</p>
 * <ul>
 *   <li>从缓存获取染疫动物UUID</li>
 *   <li>验证动物存活后造成伤害</li>
 *   <li>检查并处理超时死亡</li>
 *   <li>动物死亡时根据配置概率召唤尸体</li>
 *   <li>清理无效缓存</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class PlagueDamageTask implements Runnable {
    
    private final Plugin plugin;
    private final AnimalDataService animalDataService;
    private final AnimalConfigService animalConfigService;
    private final InfectedAnimalCache infectedAnimalCache;
    private final MythicMobSpawner mythicMobSpawner;
    private final PluginConfig pluginConfig;
    private final LoggerService loggerService;
    private final Random random;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param plugin 插件实例
     * @param animalDataService 动物数据服务
     * @param animalConfigService 动物配置服务
     * @param infectedAnimalCache 染疫动物缓存
     * @param mythicMobSpawner MythicMobs 怪物生成器
     * @param pluginConfig 插件配置
     * @param loggerService 日志服务
     */
    public PlagueDamageTask(@NotNull Plugin plugin,
                           @NotNull AnimalDataService animalDataService,
                           @NotNull AnimalConfigService animalConfigService,
                           @NotNull InfectedAnimalCache infectedAnimalCache,
                           @NotNull MythicMobSpawner mythicMobSpawner,
                           @NotNull PluginConfig pluginConfig,
                           @NotNull LoggerService loggerService) {
        this.plugin = plugin;
        this.animalDataService = animalDataService;
        this.animalConfigService = animalConfigService;
        this.infectedAnimalCache = infectedAnimalCache;
        this.mythicMobSpawner = mythicMobSpawner;
        this.pluginConfig = pluginConfig;
        this.loggerService = loggerService;
        this.random = new Random();
    }
    
    @Override
    public void run() {
        try {
            processPlagueEffects();
        } catch (Exception e) {
            loggerService.error("瘟疫伤害任务执行异常", e);
        }
    }
    
    /**
     * 处理瘟疫效果
     * 
     * <p>遍历所有染疫动物，造成伤害或处理死亡</p>
     */
    private void processPlagueEffects() {
        // 从缓存获取所有染疫动物UUID（快照）
        Set<UUID> infectedUUIDs = infectedAnimalCache.getAllInfected();
        
        if (infectedUUIDs.isEmpty()) {
            return; // 无染疫动物，直接返回
        }
        
        loggerService.debug("PLAGUE_DAMAGE", "开始处理 %d 只染疫动物", infectedUUIDs.size());
        
        // 初始化统计数据
        ProcessingStats stats = new ProcessingStats();
        
        // 遍历并处理每只动物
        for (UUID uuid : infectedUUIDs) {
            processInfectedAnimal(uuid, stats);
        }
        
        // 输出统计信息
        printProcessingStats(stats);
    }
    
    /**
     * 处理单只染疫动物
     * 
     * @param uuid 动物UUID
     * @param stats 统计数据
     */
    private void processInfectedAnimal(@NotNull UUID uuid, @NotNull ProcessingStats stats) {
        // 验证并获取动物实体
        Animals animal = validateAndGetAnimal(uuid);
        if (animal == null) {
            stats.invalidCount++;
            return;
        }
        
        // 获取动物配置
        AnimalConfig config = getAnimalConfig(animal);
        if (config == null) {
            return;
        }
        
        // 检查是否应该死亡
        if (shouldDie(animal, config)) {
            handleAnimalDeath(animal, uuid, config, stats);
        } else {
            // 造成伤害
            applyPlagueDamage(animal, uuid, stats);
        }
    }
    
    /**
     * 验证并获取动物实体
     * 
     * @param uuid 动物UUID
     * @return 动物实体，验证失败返回 null
     */
    private Animals validateAndGetAnimal(@NotNull UUID uuid) {
        // 获取实体（验证存活）
        Entity entity = Bukkit.getEntity(uuid);
        
        // 实体不存在或无效
        if (entity == null || !entity.isValid()) {
            infectedAnimalCache.removeInfected(uuid);
            loggerService.debug("PLAGUE_DAMAGE", "移除无效动物: %s", uuid);
            return null;
        }
        
        // 验证是动物
        if (!(entity instanceof Animals animal)) {
            infectedAnimalCache.removeInfected(uuid);
            return null;
        }
        
        // 再次检查染疫状态（防止数据不同步）
        if (!animalDataService.isInfected(animal)) {
            infectedAnimalCache.removeInfected(uuid);
            return null;
        }
        
        return animal;
    }
    
    /**
     * 获取动物配置
     * 
     * @param animal 动物实体
     * @return 动物配置，未找到返回 null
     */
    private AnimalConfig getAnimalConfig(@NotNull Animals animal) {
        AnimalConfig config = animalConfigService.getAnimalConfig(animal.getType().name());
        if (config == null) {
            loggerService.debug("PLAGUE_DAMAGE", "物种 %s 无配置，跳过", animal.getType());
        }
        return config;
    }
    
    /**
     * 检查动物是否应该死亡
     * 
     * @param animal 动物实体
     * @param config 动物配置
     * @return 是否应该死亡
     */
    private boolean shouldDie(@NotNull Animals animal, @NotNull AnimalConfig config) {
        int deathTime = config.getPlagueDeathTime();
        return animalDataService.shouldDieFromPlague(animal, deathTime);
    }
    
    /**
     * 处理动物死亡
     * 
     * @param animal 动物实体
     * @param uuid 动物UUID
     * @param config 动物配置
     * @param stats 统计数据
     */
    private void handleAnimalDeath(@NotNull Animals animal, 
                                   @NotNull UUID uuid,
                                   @NotNull AnimalConfig config, 
                                   @NotNull ProcessingStats stats) {
        Location deathLocation = animal.getLocation();
        int deathTime = config.getPlagueDeathTime();
        
        // 执行死亡
        animal.setHealth(0);
        infectedAnimalCache.removeInfected(uuid);
        stats.deathCount++;
        
        loggerService.debug("PLAGUE_DAMAGE", "动物 %s (%s) 因瘟疫死亡（%d秒）", 
            animal.getType(), uuid, deathTime);
        
        // 尝试生成尸体
        if (trySpawnCorpse(config, deathLocation)) {
            stats.corpseSpawnedCount++;
        }
    }
    
    /**
     * 对动物造成瘟疫伤害
     * 
     * @param animal 动物实体
     * @param uuid 动物UUID
     * @param stats 统计数据
     */
    private void applyPlagueDamage(@NotNull Animals animal, 
                                   @NotNull UUID uuid, 
                                   @NotNull ProcessingStats stats) {
        double damageAmount = pluginConfig.getPlagueDamageAmount();
        double minHealth = pluginConfig.getPlagueDamageMinHealth();
        
        double currentHealth = animal.getHealth();
        double newHealth = Math.max(minHealth, currentHealth - damageAmount);
        
        if (newHealth < currentHealth) {
            animal.setHealth(newHealth);
            stats.damagedCount++;
            
            loggerService.debug("PLAGUE_DAMAGE", 
                "动物 %s 受到瘟疫伤害: %.1f -> %.1f", 
                uuid, currentHealth, newHealth);
        }
    }
    
    /**
     * 输出处理统计信息
     * 
     * @param stats 统计数据
     */
    private void printProcessingStats(@NotNull ProcessingStats stats) {
        if (stats.damagedCount > 0 || stats.deathCount > 0 || stats.invalidCount > 0) {
            loggerService.debug("PLAGUE_DAMAGE", 
                "瘟疫伤害: %d 只受伤, %d 只死亡, %d 个尸体, %d 只无效", 
                stats.damagedCount, stats.deathCount, stats.corpseSpawnedCount, stats.invalidCount);
        }
    }
    
    /**
     * 处理统计数据
     */
    private static class ProcessingStats {
        int damagedCount = 0;
        int deathCount = 0;
        int corpseSpawnedCount = 0;
        int invalidCount = 0;
    }
    
    /**
     * 尝试生成尸体
     * 
     * <p>根据配置的概率和 MythicMobs ID 生成尸体</p>
     * 
     * @param config 动物配置
     * @param location 生成位置
     * @return 是否成功生成尸体
     */
    private boolean trySpawnCorpse(@NotNull AnimalConfig config, @NotNull Location location) {
        // 1. 检查配置是否有效
        String corpseMobId = config.getCorpseMobid();
        if (corpseMobId == null || corpseMobId.trim().isEmpty()) {
            loggerService.debug("PLAGUE_CORPSE", "动物 %s 未配置尸体ID，跳过", config.getType());
            return false;
        }
        
        int dropRate = config.getCorpseDropRate();
        if (dropRate <= 0) {
            loggerService.debug("PLAGUE_CORPSE", "动物 %s 尸体掉落率为0，跳过", config.getType());
            return false;
        }
        
        // 2. 概率判定（corpseDropRate 是百分比，如0-100）
        int roll = random.nextInt(100) + 1; // 1-100
        if (roll > dropRate) {
            loggerService.debug("PLAGUE_CORPSE", 
                "动物 %s 尸体概率判定失败: %d > %d", 
                config.getType(), roll, dropRate);
            return false;
        }
        
        // 3. 检查 MythicMobs 是否可用
        if (!mythicMobSpawner.isAvailable()) {
            loggerService.warning("无法生成尸体：MythicMobs 插件未加载");
            return false;
        }
        
        // 4. 生成尸体
        boolean success = mythicMobSpawner.spawnMob(location, corpseMobId);
        
        if (success) {
            loggerService.debug("PLAGUE_CORPSE", 
                "成功生成尸体: %s (%s) 概率 %d%% (投点 %d)",
                config.getDesc(), corpseMobId, dropRate, roll);
        } else {
            loggerService.warning(
                "生成尸体失败: %s (%s)",
                config.getDesc(), corpseMobId);
        }
        
        return success;
    }
}
