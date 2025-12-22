package com.cuzz.rookiePlague.cache;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 环境数据缓存
 * 
 * <p>线程安全的环境数据缓存，主线程定期更新，异步线程安全读取</p>
 * <p>缓存内容：</p>
 * <ul>
 *   <li>全服在线玩家数</li>
 *   <li>各世界天气状态</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
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
    
    /**
     * 更新在线玩家数（主线程调用）
     * 
     * @param count 在线玩家数
     */
    public void updateOnlinePlayerCount(int count) {
        this.onlinePlayerCount = count;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 更新世界天气（主线程调用）
     * 
     * @param world 世界
     * @param weatherType 天气类型
     */
    public void updateWorldWeather(@NotNull World world, @NotNull WeatherType weatherType) {
        worldWeatherCache.put(world.getName(), weatherType);
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 获取在线玩家数（异步安全）
     * 
     * @return 在线玩家数
     */
    public int getOnlinePlayerCount() {
        return onlinePlayerCount;
    }
    
    /**
     * 获取世界天气（异步安全）
     * 
     * @param worldName 世界名称
     * @return 天气类型，未找到返回 CLEAR
     */
    @NotNull
    public WeatherType getWorldWeather(@NotNull String worldName) {
        return worldWeatherCache.getOrDefault(worldName, WeatherType.CLEAR);
    }
    
    /**
     * 获取最后更新时间
     * 
     * @return 时间戳（毫秒）
     */
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        onlinePlayerCount = 0;
        worldWeatherCache.clear();
        lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 天气类型枚举
     */
    public enum WeatherType {
        /** 晴天 */
        CLEAR,
        /** 下雨 */
        RAIN,
        /** 雷暴 */
        THUNDER
    }
}
