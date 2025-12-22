package com.cuzz.rookiePlague.scheduler;

import com.cuzz.rookiePlague.cache.EnvironmentCache;
import com.cuzz.rookiePlague.service.LoggerService;
import com.cuzz.rookiePlague.task.EnvironmentUpdateTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * 环境数据更新调度器
 * 
 * <p>Scheduler：管理环境数据更新任务的定时执行</p>
 * <p>职责：</p>
 * <ul>
 *   <li>定时启动环境数据更新任务</li>
 *   <li>管理任务的生命周期（启动/停止/重启）</li>
 *   <li>提供可配置的更新间隔</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class EnvironmentUpdateScheduler {
    
    private final Plugin plugin;
    private final EnvironmentCache environmentCache;
    private final LoggerService loggerService;
    
    private BukkitTask scheduledTask;
    private boolean running;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param plugin 插件实例
     * @param environmentCache 环境数据缓存
     * @param loggerService 日志服务
     */
    public EnvironmentUpdateScheduler(@NotNull Plugin plugin,
                                      @NotNull EnvironmentCache environmentCache,
                                      @NotNull LoggerService loggerService) {
        this.plugin = plugin;
        this.environmentCache = environmentCache;
        this.loggerService = loggerService;
        this.running = false;
    }
    
    /**
     * 启动定时任务
     * 
     * @param intervalSeconds 更新间隔（秒）
     */
    public void start(int intervalSeconds) {
        if (running) {
            loggerService.warning("环境更新调度器已在运行，无需重复启动");
            return;
        }
        
        long intervalTicks = intervalSeconds * 20L;  // 转换为 tick（1秒 = 20 tick）
        
        scheduledTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            new EnvironmentUpdateTask(plugin, environmentCache, loggerService),
            0L,  // 立即执行
            intervalTicks
        );
        
        running = true;
        
        loggerService.info("环境更新调度器已启动: 间隔 %d 秒", intervalSeconds);
    }
    
    /**
     * 停止定时任务
     */
    public void stop() {
        if (!running) {
            loggerService.warning("环境更新调度器未运行，无需停止");
            return;
        }
        
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
        
        running = false;
        
        loggerService.info("环境更新调度器已停止");
    }
    
    /**
     * 重启调度器（调整执行频率）
     * 
     * @param newIntervalSeconds 新的更新间隔（秒）
     */
    public void restart(int newIntervalSeconds) {
        loggerService.info("重启环境更新调度器，新间隔: %d 秒", newIntervalSeconds);
        stop();
        start(newIntervalSeconds);
    }
    
    /**
     * 立即执行一次更新
     */
    public void updateNow() {
        loggerService.info("手动触发环境数据更新");
        
        Bukkit.getScheduler().runTask(
            plugin,
            new EnvironmentUpdateTask(plugin, environmentCache, loggerService)
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
