package com.cuzz.rookiePlague.task;

import com.cuzz.rookiePlague.cache.InfectedAnimalCache;
import com.cuzz.rookiePlague.service.AnimalDataService;
import com.cuzz.rookiePlague.service.LoggerService;
import com.cuzz.rookiePlague.service.PlagueInfectionService;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 染疫检查异步任务
 * 
 * <p>AsyncTask：在异步线程中执行区块扫描和染疫计算</p>
 * <p>职责：</p>
 * <ul>
 *   <li>扫描已加载的区块</li>
 *   <li>过滤出正在 tick 的区块</li>
 *   <li>调用 PlagueInfectionService 处理每个区块</li>
 *   <li>重建染疫动物缓存（服务器重启后恢复）</li>
 *   <li>统计处理结果</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class PlagueCheckTask implements Runnable {
    
    private final Plugin plugin;
    private final PlagueInfectionService plagueInfectionService;
    private final AnimalDataService animalDataService;
    private final InfectedAnimalCache infectedAnimalCache;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param plugin 插件实例
     * @param plagueInfectionService 染疫感染服务
     * @param animalDataService 动物数据服务
     * @param infectedAnimalCache 染疫动物缓存
     * @param loggerService 日志服务
     */
    public PlagueCheckTask(@NotNull Plugin plugin,
                          @NotNull PlagueInfectionService plagueInfectionService,
                          @NotNull AnimalDataService animalDataService,
                          @NotNull InfectedAnimalCache infectedAnimalCache,
                          @NotNull LoggerService loggerService) {
        this.plugin = plugin;
        this.plagueInfectionService = plagueInfectionService;
        this.animalDataService = animalDataService;
        this.infectedAnimalCache = infectedAnimalCache;
        this.loggerService = loggerService;
    }
    
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        
        try {
            loggerService.debug("PLAGUE_CHECK", "开始染疫检查任务（异步）");
            
            // 执行染疫检查
            CheckResult result = performPlagueCheck();
            
            // 输出统计信息
            printCheckResult(result, startTime);
                
        } catch (Exception e) {
            loggerService.error("染疫检查任务执行异常", e);
        }
    }
    
    /**
     * 执行染疫检查
     * 
     * @return 检查结果
     */
    private CheckResult performPlagueCheck() {
        CheckResult result = new CheckResult();
        
        // 遍历所有世界
        for (World world : plugin.getServer().getWorlds()) {
            // 过滤世界类型
            if (!shouldProcessWorld(world)) {
                continue;
            }
            
            loggerService.debug("PLAGUE_CHECK", "扫描世界: %s", world.getName());
            
            // 处理世界中的所有区块
            processWorldChunks(world, result);
        }
        
        return result;
    }
    
    /**
     * 检查是否应该处理该世界
     * 
     * @param world 世界
     * @return 是否处理
     */
    private boolean shouldProcessWorld(@NotNull World world) {
        // 跳过非主世界（可配置）
        return world.getEnvironment() == World.Environment.NORMAL;
    }
    
    /**
     * 处理世界中的所有区块
     * 
     * @param world 世界
     * @param result 检查结果
     */
    private void processWorldChunks(@NotNull World world, @NotNull CheckResult result) {
        // 遍历已加载的区块
        for (Chunk chunk : world.getLoadedChunks()) {
            result.totalChunks++;
            
            // 检查区块是否应该被处理
            if (!shouldProcessChunk(chunk)) {
                continue;
            }
            
            // 重建缓存：扫描已染疫的动物
            result.rebuiltCache += rebuildCacheForChunk(chunk);
            
            // 处理该区块（染疫判定）
            plagueInfectionService.processChunk(chunk);
            result.processedChunks++;
        }
    }
    
    /**
     * 检查区块是否应该被处理
     * 
     * @param chunk 区块
     * @return 是否处理
     */
    private boolean shouldProcessChunk(@NotNull Chunk chunk) {
        // 只处理正在 tick 的区块
        if (chunk.getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING) {
            return false;
        }
        
        // 检查实体是否已加载
        return chunk.isEntitiesLoaded();
    }
    
    /**
     * 输出检查结果
     * 
     * @param result 检查结果
     * @param startTime 开始时间
     */
    private void printCheckResult(@NotNull CheckResult result, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        
        loggerService.info("染疫检查完成: 扫描 %d 个区块，处理 %d 个区块，重建缓存 %d 只，耗时 %d ms",
            result.totalChunks, result.processedChunks, result.rebuiltCache, duration);
    }
    
    /**
     * 重建区块内的染疫动物缓存
     * 
     * <p>扫描区块内所有动物，将已染疫的添加到缓存</p>
     * <p>用于服务器重启后恢复缓存</p>
     * 
     * @param chunk 区块
     * @return 重建的动物数量
     */
    private int rebuildCacheForChunk(@NotNull Chunk chunk) {
        int count = 0;
        
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Animals animal)) continue;
            
            // 检查是否已染疫（从PDC读取）
            if (animalDataService.isInfected(animal)) {
                // 添加到缓存（如果已存在会被忽略）
                infectedAnimalCache.addInfected(animal.getUniqueId());
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 检查结果
     */
    private static class CheckResult {
        int totalChunks = 0;
        int processedChunks = 0;
        int rebuiltCache = 0;
    }
}
