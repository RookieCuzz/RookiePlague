package com.cuzz.rookiePlague.service;

import com.cuzz.rookiePlague.model.AnimalConfig;
import org.bukkit.entity.Animals;
import org.jetbrains.annotations.NotNull;

/**
 * 动物繁殖业务服务
 * 
 * <p>Service层：封装繁殖相关的业务逻辑</p>
 * <p>职责：</p>
 * <ul>
 *   <li>检查动物是否可以繁殖</li>
 *   <li>验证繁殖条件（染疫状态、次数限制等）</li>
 *   <li>处理繁殖成功后的数据更新</li>
 *   <li>提供繁殖相关的业务判断方法</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalBreedService {
    
    private final AnimalDataService animalDataService;
    private final AnimalNameService animalNameService;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param animalDataService 动物数据服务
     * @param animalNameService 动物名称服务
     * @param loggerService 日志服务
     */
    public AnimalBreedService(@NotNull AnimalDataService animalDataService,
                             @NotNull AnimalNameService animalNameService,
                             @NotNull LoggerService loggerService) {
        this.animalDataService = animalDataService;
        this.animalNameService = animalNameService;
        this.loggerService = loggerService;
    }
    
    /**
     * 检查动物是否染疫
     * 
     * <p>检查父母双方的染疫状态，任何一方染疫都不允许繁殖</p>
     * 
     * @param mother 母体
     * @param father 父体
     * @return 检查结果
     */
    @NotNull
    public BreedCheckResult checkInfectionStatus(@NotNull Animals mother, @NotNull Animals father) {
        // 检查母体染疫状态
        if (animalDataService.isInfected(mother)) {
            loggerService.debug("BREED_SERVICE", "母体 %s 已染疫", mother.getUniqueId());
            return BreedCheckResult.infected("母体已染疫", InfectionTarget.MOTHER);
        }
        
        // 检查父体染疫状态
        if (animalDataService.isInfected(father)) {
            loggerService.debug("BREED_SERVICE", "父体 %s 已染疫", father.getUniqueId());
            return BreedCheckResult.infected("父体已染疫", InfectionTarget.FATHER);
        }
        
        loggerService.debug("BREED_SERVICE", "父母双方均未染疫，通过检查");
        return BreedCheckResult.success();
    }
    
    /**
     * 检查繁殖次数限制
     * 
     * <p>检查父母双方的繁殖次数，任何一方达到上限都不允许繁殖</p>
     * 
     * @param mother 母体
     * @param father 父体
     * @param maxBreedTimes 最大繁殖次数
     * @return 检查结果
     */
    @NotNull
    public BreedCheckResult checkBreedLimit(@NotNull Animals mother, 
                                           @NotNull Animals father,
                                           int maxBreedTimes) {
        // 检查母体繁殖次数
        int motherCount = animalDataService.getBreedCount(mother);
        if (motherCount >= maxBreedTimes) {
            loggerService.debug("BREED_SERVICE", "母体 %s 已达到繁殖上限 (%d/%d)", 
                mother.getUniqueId(), motherCount, maxBreedTimes);
            return BreedCheckResult.limitReached("母体已达到繁殖上限", 
                BreedTarget.MOTHER, motherCount, maxBreedTimes);
        }
        
        // 检查父体繁殖次数
        int fatherCount = animalDataService.getBreedCount(father);
        if (fatherCount >= maxBreedTimes) {
            loggerService.debug("BREED_SERVICE", "父体 %s 已达到繁殖上限 (%d/%d)", 
                father.getUniqueId(), fatherCount, maxBreedTimes);
            return BreedCheckResult.limitReached("父体已达到繁殖上限", 
                BreedTarget.FATHER, fatherCount, maxBreedTimes);
        }
        
        loggerService.debug("BREED_SERVICE", "繁殖次数检查通过: 母体 %d/%d, 父体 %d/%d",
            motherCount, maxBreedTimes, fatherCount, maxBreedTimes);
        return BreedCheckResult.success();
    }
    
    /**
     * 处理繁殖成功
     * 
     * <p>增加父母双方的繁殖次数并返回结果</p>
     * 
     * @param mother 母体
     * @param father 父体
     * @param config 动物配置
     * @return 繁殖结果
     */
    @NotNull
    public BreedResult handleSuccessfulBreed(@NotNull Animals mother,
                                            @NotNull Animals father,
                                            @NotNull AnimalConfig config) {
        // 记录旧次数
        int motherOldCount = animalDataService.getBreedCount(mother);
        int fatherOldCount = animalDataService.getBreedCount(father);
        
        // 增加繁殖次数
        int motherNewCount = animalDataService.incrementBreedCount(mother);
        int fatherNewCount = animalDataService.incrementBreedCount(father);
        
        // 更新名称显示（检查是否达到上限）
        animalNameService.updateAnimalName(mother);
        animalNameService.updateAnimalName(father);
        
        loggerService.debug("BREED_SERVICE", "繁殖成功处理: 母体 %d->%d, 父体 %d->%d",
            motherOldCount, motherNewCount, fatherOldCount, fatherNewCount);
        
        return new BreedResult(
            motherOldCount, motherNewCount,
            fatherOldCount, fatherNewCount,
            config.getMaxBreedTimes()
        );
    }
    
    /**
     * 繁殖检查结果
     */
    public static class BreedCheckResult {
        private final boolean success;
        private final String reason;
        private final CheckFailureType failureType;
        private final InfectionTarget infectionTarget;
        private final BreedTarget breedTarget;
        private final int currentCount;
        private final int maxCount;
        
        private BreedCheckResult(boolean success, String reason, CheckFailureType failureType,
                                InfectionTarget infectionTarget, BreedTarget breedTarget,
                                int currentCount, int maxCount) {
            this.success = success;
            this.reason = reason;
            this.failureType = failureType;
            this.infectionTarget = infectionTarget;
            this.breedTarget = breedTarget;
            this.currentCount = currentCount;
            this.maxCount = maxCount;
        }
        
        public static BreedCheckResult success() {
            return new BreedCheckResult(true, null, null, null, null, 0, 0);
        }
        
        public static BreedCheckResult infected(String reason, InfectionTarget target) {
            return new BreedCheckResult(false, reason, CheckFailureType.INFECTED, 
                target, null, 0, 0);
        }
        
        public static BreedCheckResult limitReached(String reason, BreedTarget target,
                                                    int currentCount, int maxCount) {
            return new BreedCheckResult(false, reason, CheckFailureType.LIMIT_REACHED,
                null, target, currentCount, maxCount);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getReason() {
            return reason;
        }
        
        public CheckFailureType getFailureType() {
            return failureType;
        }
        
        public InfectionTarget getInfectionTarget() {
            return infectionTarget;
        }
        
        public BreedTarget getBreedTarget() {
            return breedTarget;
        }
        
        public int getCurrentCount() {
            return currentCount;
        }
        
        public int getMaxCount() {
            return maxCount;
        }
    }
    
    /**
     * 繁殖结果
     */
    public static class BreedResult {
        private final int motherOldCount;
        private final int motherNewCount;
        private final int fatherOldCount;
        private final int fatherNewCount;
        private final int maxBreedTimes;
        
        public BreedResult(int motherOldCount, int motherNewCount,
                          int fatherOldCount, int fatherNewCount,
                          int maxBreedTimes) {
            this.motherOldCount = motherOldCount;
            this.motherNewCount = motherNewCount;
            this.fatherOldCount = fatherOldCount;
            this.fatherNewCount = fatherNewCount;
            this.maxBreedTimes = maxBreedTimes;
        }
        
        public int getMotherOldCount() {
            return motherOldCount;
        }
        
        public int getMotherNewCount() {
            return motherNewCount;
        }
        
        public int getFatherOldCount() {
            return fatherOldCount;
        }
        
        public int getFatherNewCount() {
            return fatherNewCount;
        }
        
        public int getMaxBreedTimes() {
            return maxBreedTimes;
        }
    }
    
    /**
     * 检查失败类型
     */
    public enum CheckFailureType {
        /** 染疫 */
        INFECTED,
        /** 达到繁殖次数上限 */
        LIMIT_REACHED
    }
    
    /**
     * 染疫目标
     */
    public enum InfectionTarget {
        /** 母体 */
        MOTHER,
        /** 父体 */
        FATHER
    }
    
    /**
     * 繁殖目标
     */
    public enum BreedTarget {
        /** 母体 */
        MOTHER,
        /** 父体 */
        FATHER
    }
}
