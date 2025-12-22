package com.cuzz.rookiePlague.service.spi;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * MythicMobs 怪物生成器接口
 * 
 * <p>定义生成 MythicMobs 怪物的统一接口</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public interface MythicMobSpawner {
    
    /**
     * 在指定位置生成 MythicMobs 怪物
     * 
     * @param location 生成位置
     * @param mobId 怪物ID（MythicMobs 配置中的 internal name）
     * @return 是否生成成功
     */
    boolean spawnMob(@NotNull Location location, @NotNull String mobId);
    
    /**
     * 在指定位置生成指定等级的 MythicMobs 怪物
     * 
     * @param location 生成位置
     * @param mobId 怪物ID
     * @param level 怪物等级
     * @return 是否生成成功
     */
    boolean spawnMob(@NotNull Location location, @NotNull String mobId, int level);
    
    /**
     * 检查 MythicMobs 插件是否可用
     * 
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取服务名称
     * 
     * @return 服务名称
     */
    @NotNull
    String getServiceName();
}
