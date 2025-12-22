package com.cuzz.rookiePlague.service;

import com.cuzz.rookiePlague.service.spi.MythicMobSpawner;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * MythicMobs API 实现的怪物生成服务
 * 
 * <p>Service层：通过 MythicMobs API 生成怪物</p>
 * <p>职责：</p>
 * <ul>
 *   <li>调用 MythicMobs API 生成指定怪物</li>
 *   <li>支持指定怪物等级</li>
 *   <li>处理生成失败的情况</li>
 *   <li>记录生成日志</li>
 *   <li>检查 MythicMobs 插件可用性</li>
 * </ul>
 * 
 * <p>注意：本实现使用反射调用 MythicMobs API，兼容 MythicMobs 5.x 版本</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class MythicMobsSpawnerService implements MythicMobSpawner {
    
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param loggerService 日志服务
     */
    public MythicMobsSpawnerService(@NotNull LoggerService loggerService) {
        this.loggerService = loggerService;
    }
    
    @Override
    public boolean spawnMob(@NotNull Location location, @NotNull String mobId) {
        return spawnMob(location, mobId, 1);
    }
    
    @Override
    public boolean spawnMob(@NotNull Location location, @NotNull String mobId, int level) {
        if (!isAvailable()) {
            loggerService.warning("[MythicMobs] 插件未加载，无法生成怪物");
            return false;
        }
        
        try {
            MythicBukkit mythic = MythicBukkit.inst();
            
            // 1. 通过 mobId 获取 MythicMob（使用泛型通配符）
            var optMob = mythic.getMobManager().getMythicMob(mobId);
            if (optMob.isEmpty()) {
                loggerService.warning("[MythicMobs] 找不到怪物类型: %s", mobId);
                return false;
            }
            
            var mob = optMob.get();
            
            // 2. 调用 spawn 方法生成怪物（使用反射以兼容不同版本）
            var activeMob = mob.getClass()
                .getMethod("spawn", io.lumine.mythic.api.adapters.AbstractLocation.class, double.class)
                .invoke(mob, BukkitAdapter.adapt(location), (double) level);
            
            if (activeMob == null) {
                loggerService.warning("[MythicMobs] 生成失败: %s (等级 %d)，请检查配置或世界限制", mobId, level);
                return false;
            }
            
            // 3. 生成成功
            loggerService.info(
                "[MythicMobs] 成功生成怪物: %s (等级 %d) at (%s, %.1f, %.1f, %.1f)",
                mobId,
                level,
                location.getWorld() != null ? location.getWorld().getName() : "unknown",
                location.getX(),
                location.getY(),
                location.getZ()
            );
            
            return true;
            
        } catch (NoSuchMethodException e) {
            loggerService.error("[MythicMobs] API 方法不存在，可能是版本不兼容: " + mobId, e);
            return false;
        } catch (Exception e) {
            loggerService.error("[MythicMobs] 生成怪物时发生异常: " + mobId, e);
            return false;
        }
    }
    
    @Override
    public boolean isAvailable() {
        // 检查 MythicMobs 插件是否已加载
        return Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }
    
    @Override
    @NotNull
    public String getServiceName() {
        return "MythicMobs";
    }
}
