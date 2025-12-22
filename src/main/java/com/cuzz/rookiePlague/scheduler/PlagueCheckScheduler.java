package com.cuzz.rookiePlague.scheduler;

import com.cuzz.rookiePlague.cache.InfectedAnimalCache;
import com.cuzz.rookiePlague.service.AnimalDataService;
import com.cuzz.rookiePlague.service.LoggerService;
import com.cuzz.rookiePlague.service.PlagueInfectionService;
import com.cuzz.rookiePlague.task.PlagueCheckTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * 染疫检查定时调度器
 * 
 * <p>Scheduler：管理染疫检查任务的定时执行</p>
 * <p>职责：</p>
 * <ul>
 *   <li>定时启动异步染疫检查任务</li>
 *   <li>管理任务的生命周期（启动/停止）</li>
 *   <li>提供可配置的检查间隔</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class PlagueCheckScheduler {
    
    private final Plugin plugin;
    private final PlagueInfectionService plagueInfectionService;
    private final AnimalDataService animalDataService;
    private final InfectedAnimalCache infectedAnimalCache;
    private final LoggerService loggerService;
    
    private BukkitTask scheduledTask;
    private boolean running;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param plugin 插件实例
     * @param plagueInfectionService 染疫感染服务
     * @param animalDataService 动物数据服务
     * @param infectedAnimalCache 染疫动物缓存
     * @param loggerService 日志服务
     */
    public PlagueCheckScheduler(@NotNull Plugin plugin,
                                @NotNull PlagueInfectionService plagueInfectionService,
                                @NotNull AnimalDataService animalDataService,
                                @NotNull InfectedAnimalCache infectedAnimalCache,
                                @NotNull LoggerService loggerService) {
        this.plugin = plugin;
        this.plagueInfectionService = plagueInfectionService;
        this.animalDataService = animalDataService;
        this.infectedAnimalCache = infectedAnimalCache;
        this.loggerService = loggerService;
        this.running = false;
    }
    
    /**
     * 启动定时任务
     * 
     * @param intervalSeconds 检查间隔（秒）
     * @param delaySeconds 首次延迟（秒）
     */
    public void start(long intervalSeconds, long delaySeconds) {
        if (running) {
            loggerService.warning("染疫检查调度器已在运行，无需重复启动");
            return;
        }
        
        long intervalTicks = intervalSeconds * 20L;  // 转换为 tick（1秒 = 20 tick）
        long delayTicks = delaySeconds * 20L;
        
        scheduledTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            new PlagueCheckTask(plugin, plagueInfectionService, animalDataService, infectedAnimalCache, loggerService),
            delayTicks,
            intervalTicks
        );
        
        running = true;
        
        loggerService.info("染疫检查调度器已启动: 间隔 %d 秒，延迟 %d 秒", intervalSeconds, delaySeconds);
    }
    
    /**
     * 启动定时任务（使用默认延迟）
     * 
     * @param intervalSeconds 检查间隔（秒）
     */
    public void start(long intervalSeconds) {
        start(intervalSeconds, 60L);  // 默认延迟 60 秒
    }
    
    /**
     * 停止定时任务
     */
    public void stop() {
        if (!running) {
            loggerService.warning("染疫检查调度器未运行，无需停止");
            return;
        }
        
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
        
        running = false;
        
        loggerService.info("染疫检查调度器已停止");
    }
    
    /**
     * 立即执行一次检查（异步）
     */
    public void checkNow() {
        loggerService.info("手动触发染疫检查");
        
        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            new PlagueCheckTask(plugin, plagueInfectionService, animalDataService, infectedAnimalCache, loggerService)
        );
    }
    
    /**
     * 检查调度器是否正在运行
     * 
     * @return 是否运行中
     */
    public boolean isRunning() {
        return running;
    }
}
