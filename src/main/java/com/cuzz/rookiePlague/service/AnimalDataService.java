package com.cuzz.rookiePlague.service;

import com.cuzz.rookiePlague.dao.AnimalDataDao;
import org.bukkit.entity.Animals;
import org.jetbrains.annotations.NotNull;

/**
 * 动物数据服务
 * 
 * <p>Service层：提供动物数据的业务逻辑</p>
 * <p>主要功能：</p>
 * <ul>
 *   <li>管理动物的繁殖次数（业务逻辑）</li>
 *   <li>管理动物的染疫状态（业务逻辑）</li>
 *   <li>提供高级查询和判断方法</li>
 *   <li>调用 DAO 层进行数据持久化</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalDataService {
    
    private final AnimalDataDao animalDataDao;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param animalDataDao 动物数据DAO
     * @param loggerService 日志服务
     */
    public AnimalDataService(@NotNull AnimalDataDao animalDataDao,
                            @NotNull LoggerService loggerService) {
        this.animalDataDao = animalDataDao;
        this.loggerService = loggerService;
        
        loggerService.debug("SERVICE", "动物数据服务初始化完成");
    }
    
    // ==================== 繁殖次数业务逻辑 ====================
    
    /**
     * 获取动物的繁殖次数
     * 
     * @param animal 动物实体
     * @return 繁殖次数，未繁殖过返回 0
     */
    public int getBreedCount(@NotNull Animals animal) {
        Integer count = animalDataDao.readBreedCount(animal);
        
        if (count == null) {
            loggerService.debug("SERVICE", "动物 %s 未找到繁殖次数数据，返回 0", animal.getUniqueId());
            return 0;
        }
        
        loggerService.debug("SERVICE", "动物 %s 繁殖次数: %d", animal.getUniqueId(), count);
        return count;
    }
    
    /**
     * 增加动物的繁殖次数
     * 
     * @param animal 动物实体
     * @return 增加后的繁殖次数
     */
    public int incrementBreedCount(@NotNull Animals animal) {
        int currentCount = getBreedCount(animal);
        int newCount = currentCount + 1;
        
        animalDataDao.writeBreedCount(animal, newCount);
        
        loggerService.debug("SERVICE", "动物 %s 繁殖次数增加: %d -> %d", 
            animal.getUniqueId(), currentCount, newCount);
        
        return newCount;
    }
    
    /**
     * 设置动物的繁殖次数
     * 
     * @param animal 动物实体
     * @param count 繁殖次数
     */
    public void setBreedCount(@NotNull Animals animal, int count) {
        animalDataDao.writeBreedCount(animal, count);
        loggerService.debug("SERVICE", "设置动物 %s 繁殖次数: %d", animal.getUniqueId(), count);
    }
    
    /**
     * 重置动物的繁殖次数
     * 
     * @param animal 动物实体
     */
    public void resetBreedCount(@NotNull Animals animal) {
        setBreedCount(animal, 0);
        loggerService.debug("SERVICE", "重置动物 %s 繁殖次数", animal.getUniqueId());
    }
    
    /**
     * 检查动物是否达到最大繁殖次数
     * 
     * @param animal 动物实体
     * @param maxBreedTimes 最大繁殖次数
     * @return 是否达到上限
     */
    public boolean hasReachedMaxBreedTimes(@NotNull Animals animal, int maxBreedTimes) {
        int currentCount = getBreedCount(animal);
        boolean reached = currentCount >= maxBreedTimes;
        
        if (reached) {
            loggerService.debug("SERVICE", "动物 %s 已达到繁殖上限: %d/%d", 
                animal.getUniqueId(), currentCount, maxBreedTimes);
        }
        
        return reached;
    }
    
    // ==================== 染疫状态业务逻辑 ====================
    
    /**
     * 检查动物是否染疫
     * 
     * @param animal 动物实体
     * @return 是否染疫
     */
    public boolean isInfected(@NotNull Animals animal) {
        Byte infected = animalDataDao.readInfectedStatus(animal);
        return infected != null && infected == 1;
    }
    
    /**
     * 设置动物染疫状态
     * 
     * @param animal 动物实体
     * @param infected 是否染疫
     */
    public void setInfected(@NotNull Animals animal, boolean infected) {
        animalDataDao.writeInfectedStatus(animal, infected);
        
        if (infected) {
            // 记录染疫时间
            long currentTime = System.currentTimeMillis();
            animalDataDao.writeInfectedTime(animal, currentTime);
            loggerService.debug("SERVICE", "标记动物 %s 为染疫状态，时间: %d", 
                animal.getUniqueId(), currentTime);
        } else {
            // 清除染疫时间
            animalDataDao.deleteInfectedTime(animal);
            loggerService.debug("SERVICE", "清除动物 %s 的染疫状态", animal.getUniqueId());
        }
    }
    
    /**
     * 获取动物的染疫时间
     * 
     * @param animal 动物实体
     * @return 染疫时间戳（毫秒），未染疫返回 null
     */
    public Long getInfectedTime(@NotNull Animals animal) {
        return animalDataDao.readInfectedTime(animal);
    }
    
    /**
     * 计算动物染疫持续时间
     * 
     * @param animal 动物实体
     * @return 染疫持续时间（秒），未染疫返回 0
     */
    public long getInfectedDuration(@NotNull Animals animal) {
        if (!isInfected(animal)) {
            return 0;
        }
        
        Long infectedTime = getInfectedTime(animal);
        if (infectedTime == null) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        return (currentTime - infectedTime) / 1000;  // 转换为秒
    }
    
    /**
     * 检查动物是否应该死于瘟疫
     * 
     * @param animal 动物实体
     * @param plagueDeathTime 瘟疫致死时间（秒）
     * @return 是否应该死亡
     */
    public boolean shouldDieFromPlague(@NotNull Animals animal, int plagueDeathTime) {
        if (!isInfected(animal)) {
            return false;
        }
        
        long duration = getInfectedDuration(animal);
        return duration >= plagueDeathTime;
    }
    
    // ==================== 其他业务逻辑 ====================
    
    /**
     * 清除动物的所有数据
     * 
     * @param animal 动物实体
     */
    public void clearAllData(@NotNull Animals animal) {
        animalDataDao.clearAll(animal);
        loggerService.debug("SERVICE", "清除动物 %s 的所有数据", animal.getUniqueId());
    }
}
