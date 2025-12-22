package com.cuzz.rookiePlague.task;

import com.cuzz.rookiePlague.cache.EnvironmentCache;
import com.cuzz.rookiePlague.cache.EnvironmentCache.WeatherType;
import com.cuzz.rookiePlague.service.LoggerService;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 环境数据更新任务
 * 
 * <p>主线程定期任务：更新环境数据缓存</p>
 * <p>职责：</p>
 * <ul>
 *   <li>更新在线玩家数</li>
 *   <li>更新各世界天气状态</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class EnvironmentUpdateTask implements Runnable {
    
    private final Plugin plugin;
    private final EnvironmentCache environmentCache;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param plugin 插件实例
     * @param environmentCache 环境数据缓存
     * @param loggerService 日志服务
     */
    public EnvironmentUpdateTask(@NotNull Plugin plugin,
                                 @NotNull EnvironmentCache environmentCache,
                                 @NotNull LoggerService loggerService) {
        this.plugin = plugin;
        this.environmentCache = environmentCache;
        this.loggerService = loggerService;
    }
    
    @Override
    public void run() {
        try {
            // 更新在线玩家数
            int playerCount = plugin.getServer().getOnlinePlayers().size();
            environmentCache.updateOnlinePlayerCount(playerCount);
            
            // 更新各世界天气
            for (World world : plugin.getServer().getWorlds()) {
                WeatherType weatherType = determineWeatherType(world);
                environmentCache.updateWorldWeather(world, weatherType);
            }
            
            loggerService.debug("ENV_CACHE", "环境缓存已更新: 玩家=%d, 时间=%d",
                playerCount, System.currentTimeMillis());
                
        } catch (Exception e) {
            loggerService.error("环境数据更新任务执行异常", e);
        }
    }
    
    /**
     * 判断世界天气类型
     * 
     * @param world 世界
     * @return 天气类型
     */
    private WeatherType determineWeatherType(@NotNull World world) {
        if (world.isThundering()) {
            return WeatherType.THUNDER;
        } else if (world.hasStorm()) {
            return WeatherType.RAIN;
        }
        return WeatherType.CLEAR;
    }
}
