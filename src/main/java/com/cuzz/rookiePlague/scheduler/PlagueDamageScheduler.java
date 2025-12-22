package com.cuzz.rookiePlague.scheduler;

import com.cuzz.rookiePlague.cache.InfectedAnimalCache;
import com.cuzz.rookiePlague.config.PluginConfig;
import com.cuzz.rookiePlague.service.AnimalConfigService;
import com.cuzz.rookiePlague.service.AnimalDataService;
import com.cuzz.rookiePlague.service.LoggerService;
import com.cuzz.rookiePlague.service.spi.MythicMobSpawner;
import com.cuzz.rookiePlague.task.PlagueDamageTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * 瘟疫伤害调度器
 * 
 * <p>Scheduler：管理瘟疫伤害任务的定时执行</p>
 * <p>职责：</p>
 * <ul>
 *   <li>定时启动瘟疫伤害任务</li>
 *   <li>管理任务的生命周期（启动/停止/重启）</li>
 *   <li>提供可配置的伤害间隔</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class PlagueDamageScheduler {
    
    private final Plugin plugin;
    private final AnimalDataService animalDataService;
    private final AnimalConfigService animalConfigService;
    private final InfectedAnimalCache infectedAnimalCache;
    private final MythicMobSpawner mythicMobSpawner;
    private final PluginConfig pluginConfig;
    private final LoggerService loggerService;
    
    private BukkitTask scheduledTask;
    private boolean running;
    
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
    public PlagueDamageScheduler(@NotNull Plugin plugin,
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
        this.running = false;
    }
    
    /**
     * 启动定时任务
     * 
     * @param intervalSeconds 伤害间隔（秒）
     * @param delaySeconds 首次延迟（秒）
     */
    public void start(int intervalSeconds, int delaySeconds) {
        if (running) {
            loggerService.warning("瘟疫伤害调度器已在运行，无需重复启动");
            return;
        }
        
        long intervalTicks = intervalSeconds * 20L;  // 转换为 tick（1秒 = 20 tick）
        long delayTicks = delaySeconds * 20L;
        
        scheduledTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            new PlagueDamageTask(
                plugin,
                animalDataService,
                animalConfigService,
                infectedAnimalCache,
                mythicMobSpawner,
                pluginConfig,
                loggerService
            ),
            delayTicks,
            intervalTicks
        );
        
        running = true;
        
        loggerService.info("瘟疫伤害调度器已启动: 间隔 %d 秒，延迟 %d 秒", intervalSeconds, delaySeconds);
    }
    
    /**
     * 启动定时任务（使用默认延迟）
     * 
     * @param intervalSeconds 伤害间隔（秒）
     */
    public void start(int intervalSeconds) {
        start(intervalSeconds, intervalSeconds);  // 默认延迟 = 间隔时间
    }
    
    /**
     * 停止定时任务
     */
    public void stop() {
        if (!running) {
            loggerService.warning("瘟疫伤害调度器未运行，无需停止");
            return;
        }
        
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
        
        running = false;
        
        loggerService.info("瘟疫伤害调度器已停止");
    }
    
    /**
     * 重启调度器（调整执行频率）
     * 
     * @param newIntervalSeconds 新的伤害间隔（秒）
     */
    public void restart(int newIntervalSeconds) {
        loggerService.info("重启瘟疫伤害调度器，新间隔: %d 秒", newIntervalSeconds);
        stop();
        start(newIntervalSeconds);
    }
    
    /**
     * 立即执行一次伤害处理
     */
    public void damageNow() {
        loggerService.info("手动触发瘟疫伤害处理");
        
        Bukkit.getScheduler().runTask(
            plugin,
            new PlagueDamageTask(
                plugin,
                animalDataService,
                animalConfigService,
                infectedAnimalCache,
                mythicMobSpawner,
                pluginConfig,
                loggerService
            )
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
