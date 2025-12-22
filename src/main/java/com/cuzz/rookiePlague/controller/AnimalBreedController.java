package com.cuzz.rookiePlague.controller;

import com.cuzz.rookiePlague.event.AnimalBreedRequestEvent;
import com.cuzz.rookiePlague.model.AnimalConfig;
import com.cuzz.rookiePlague.service.AnimalBreedService;
import com.cuzz.rookiePlague.service.AnimalBreedService.BreedCheckResult;
import com.cuzz.rookiePlague.service.AnimalBreedService.BreedResult;
import com.cuzz.rookiePlague.service.AnimalBreedService.CheckFailureType;
import com.cuzz.rookiePlague.service.AnimalConfigService;
import com.cuzz.rookiePlague.service.LanguageService;
import com.cuzz.rookiePlague.service.LoggerService;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 动物繁殖控制器
 * 
 * <p>Controller层：监听繁殖请求事件并调度业务处理</p>
 * <p>职责：</p>
 * <ul>
 *   <li>监听 AnimalBreedRequestEvent 事件</li>
 *   <li>调用 Service 层执行业务检查</li>
 *   <li>处理检查结果并通知玩家</li>
 *   <li>记录日志</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalBreedController implements Listener {
    
    private final AnimalConfigService animalConfigService;
    private final AnimalBreedService animalBreedService;
    private final LanguageService languageService;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param animalConfigService 动物配置服务
     * @param animalBreedService 动物繁殖服务
     * @param languageService 语言服务
     * @param loggerService 日志服务
     */
    public AnimalBreedController(@NotNull AnimalConfigService animalConfigService,
                                 @NotNull AnimalBreedService animalBreedService,
                                 @NotNull LanguageService languageService,
                                 @NotNull LoggerService loggerService) {
        this.animalConfigService = animalConfigService;
        this.animalBreedService = animalBreedService;
        this.languageService = languageService;
        this.loggerService = loggerService;
    }
    
    /**
     * 监听动物繁殖请求事件
     * 
     * @param event 繁殖请求事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onAnimalBreedRequest(@NotNull AnimalBreedRequestEvent event) {
        Animals mother = event.getMother();
        Animals father = event.getFather();
        String animalType = event.getAnimalType();
        
        loggerService.debug("CONTROLLER", "处理繁殖请求: %s", animalType);
        
        // 1. 获取并验证动物配置
        AnimalConfig config = getAnimalConfig(animalType);
        if (config == null) {
            return;  // 没有配置的动物不做限制
        }
        
        // 2. 检查染疫状态
        BreedCheckResult infectionCheck = animalBreedService.checkInfectionStatus(mother, father);
        if (!infectionCheck.isSuccess()) {
            handleCheckFailure(event, infectionCheck, mother, father, config);
            return;
        }
        
        // 3. 检查繁殖次数限制
        int maxBreedTimes = config.getMaxBreedTimes();
        BreedCheckResult limitCheck = animalBreedService.checkBreedLimit(mother, father, maxBreedTimes);
        if (!limitCheck.isSuccess()) {
            handleCheckFailure(event, limitCheck, mother, father, config);
            return;
        }
        
        // 4. 繁殖成功，处理结果
        BreedResult result = animalBreedService.handleSuccessfulBreed(mother, father, config);
        handleSuccessfulBreed(mother, father, animalType, config, result);
    }
    
    /**
     * 获取并验证动物配置
     * 
     * @param animalType 动物类型
     * @return 动物配置，不存在返回 null
     */
    private AnimalConfig getAnimalConfig(@NotNull String animalType) {
        AnimalConfig config = animalConfigService.getAnimalConfig(animalType);
        if (config == null) {
            loggerService.debug("CONTROLLER", "未找到动物配置: %s，允许繁殖", animalType);
        }
        return config;
    }
    
    /**
     * 处理检查失败
     * 
     * @param event 繁殖请求事件
     * @param checkResult 检查结果
     * @param mother 母体
     * @param father 父体
     * @param config 动物配置
     */
    private void handleCheckFailure(@NotNull AnimalBreedRequestEvent event,
                                   @NotNull BreedCheckResult checkResult,
                                   @NotNull Animals mother,
                                   @NotNull Animals father,
                                   @NotNull AnimalConfig config) {
        // 取消繁殖
        cancelBreedWithReason(event, checkResult.getReason());
        
        // 根据失败类型处理
        if (checkResult.getFailureType() == CheckFailureType.INFECTED) {
            // 染疫失败
            Animals target = checkResult.getInfectionTarget() == AnimalBreedService.InfectionTarget.MOTHER 
                ? mother : father;
            notifyNearbyPlayers(target, "infected-cannot-breed");
            loggerService.info("繁殖被阻止: %s", checkResult.getReason());
            
        } else if (checkResult.getFailureType() == CheckFailureType.LIMIT_REACHED) {
            // 次数限制失败
            Animals target = checkResult.getBreedTarget() == AnimalBreedService.BreedTarget.MOTHER 
                ? mother : father;
            notifyBreedLimitReached(target, config, checkResult.getMaxCount());
            loggerService.info("繁殖被阻止: %s (%d/%d)",
                checkResult.getReason(), checkResult.getCurrentCount(), checkResult.getMaxCount());
        }
    }
    
    /**
     * 处理繁殖成功的情况
     * 
     * @param mother 母体
     * @param father 父体
     * @param animalType 动物类型
     * @param config 动物配置
     * @param result 繁殖结果
     */
    private void handleSuccessfulBreed(@NotNull Animals mother,
                                       @NotNull Animals father,
                                       @NotNull String animalType,
                                       @NotNull AnimalConfig config,
                                       @NotNull BreedResult result) {
        // 记录成功日志
        loggerService.info("繁殖成功: %s, 母体繁殖次数: %d/%d, 父体繁殖次数: %d/%d",
            animalType, 
            result.getMotherNewCount(), result.getMaxBreedTimes(),
            result.getFatherNewCount(), result.getMaxBreedTimes());
        
        // 记录详细日志
        logBreedDetails(mother, father, config, result);
    }
    
    /**
     * 取消繁殖并设置原因
     * 
     * @param event 繁殖请求事件
     * @param reason 取消原因
     */
    private void cancelBreedWithReason(@NotNull AnimalBreedRequestEvent event, @NotNull String reason) {
        event.setCancelled(true);
        event.setCancelReason(reason);
    }
    
    /**
     * 通知繁殖次数达到上限
     * 
     * @param animal 动物实体
     * @param config 动物配置
     * @param maxBreedTimes 最大繁殖次数
     */
    private void notifyBreedLimitReached(@NotNull Animals animal,
                                        @NotNull AnimalConfig config,
                                        int maxBreedTimes) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("animal", config.getDesc());
        replacements.put("max", String.valueOf(maxBreedTimes));
        notifyNearbyPlayers(animal, "breed-limit-reached", replacements);
    }
    
    /**
     * 记录繁殖详细日志
     * 
     * @param mother 母体
     * @param father 父体
     * @param config 动物配置
     * @param result 繁殖结果
     */
    private void logBreedDetails(@NotNull Animals mother,
                                @NotNull Animals father,
                                @NotNull AnimalConfig config,
                                @NotNull BreedResult result) {
        loggerService.debug("BREED", "=== 繁殖详情 ===");
        loggerService.debug("BREED", "动物类型: %s (%s)", config.getType(), config.getDesc());
        loggerService.debug("BREED", "母体 UUID: %s", mother.getUniqueId());
        loggerService.debug("BREED", "母体繁殖次数: %d -> %d (上限: %d)", 
            result.getMotherOldCount(), result.getMotherNewCount(), result.getMaxBreedTimes());
        loggerService.debug("BREED", "父体 UUID: %s", father.getUniqueId());
        loggerService.debug("BREED", "父体繁殖次数: %d -> %d (上限: %d)", 
            result.getFatherOldCount(), result.getFatherNewCount(), result.getMaxBreedTimes());
        loggerService.debug("BREED", "===============");
    }
    
    /**
     * 通知附近玩家
     * 
     * @param animal 动物实体
     * @param messageKey 消息键
     */
    private void notifyNearbyPlayers(@NotNull Animals animal, @NotNull String messageKey) {
        notifyNearbyPlayers(animal, messageKey, null);
    }
    
    /**
     * 通知附近玩家（带变量替换）
     * 
     * @param animal 动物实体
     * @param messageKey 消息键
     * @param replacements 变量替换映射
     */
    private void notifyNearbyPlayers(@NotNull Animals animal, 
                                    @NotNull String messageKey,
                                    Map<String, String> replacements) {
        // 获取消息
        String message;
        if (replacements != null) {
            message = languageService.getMessage(messageKey, replacements);
        } else {
            message = languageService.getMessage(messageKey);
        }
        
        // 通知附近 16 格内的玩家
        double notifyRadius = 16.0;
        for (Player player : animal.getWorld().getPlayers()) {
            if (player.getLocation().distance(animal.getLocation()) <= notifyRadius) {
                player.sendMessage(message);
            }
        }
    }
}
