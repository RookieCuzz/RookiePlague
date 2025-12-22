package com.cuzz.rookiePlague.controller;

import com.cuzz.rookiePlague.event.BreedCountModifyRequestEvent;
import com.cuzz.rookiePlague.event.BreedCountModifyRequestEvent.ModifyAction;
import com.cuzz.rookiePlague.model.AnimalConfig;
import com.cuzz.rookiePlague.service.AnimalBreedService;
import com.cuzz.rookiePlague.service.AnimalConfigService;
import com.cuzz.rookiePlague.service.AnimalDataService;
import com.cuzz.rookiePlague.service.LoggerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * 繁殖次数修改控制器
 * 
 * <p>Controller层：处理繁殖次数修改请求</p>
 * <p>职责：</p>
 * <ul>
 *   <li>监听 BreedCountModifyRequestEvent 事件</li>
 *   <li>验证目标动物</li>
 *   <li>调用 Service 层执行修改</li>
 *   <li>向玩家发送反馈信息</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class BreedCountModifyController implements Listener {
    
    private final AnimalConfigService animalConfigService;
    private final AnimalDataService animalDataService;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param animalConfigService 动物配置服务
     * @param animalDataService 动物数据服务
     * @param loggerService 日志服务
     */
    public BreedCountModifyController(@NotNull AnimalConfigService animalConfigService,
                                     @NotNull AnimalDataService animalDataService,
                                     @NotNull LoggerService loggerService) {
        this.animalConfigService = animalConfigService;
        this.animalDataService = animalDataService;
        this.loggerService = loggerService;
    }
    
    /**
     * 监听繁殖次数修改请求事件
     * 
     * @param event 修改请求事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBreedCountModifyRequest(@NotNull BreedCountModifyRequestEvent event) {
        CommandSender sender = event.getSender();
        Animals targetAnimal = event.getTargetAnimal();
        ModifyAction action = event.getAction();
        int value = event.getValue();
        
        loggerService.debug("CONTROLLER", "处理繁殖次数修改请求: 动作=%s, 值=%d", action, value);
        
        // 1. 验证目标动物
        if (targetAnimal == null) {
            sender.sendMessage("§c未找到目标动物！请面朝一只动物执行此命令。");
            loggerService.debug("CONTROLLER", "修改失败: 未找到目标动物");
            return;
        }
        
        // 2. 获取动物类型和配置
        EntityType entityType = targetAnimal.getType();
        String animalType = entityType.name();
        AnimalConfig config = animalConfigService.getAnimalConfig(animalType);
        
        if (config == null) {
            sender.sendMessage("§c该动物类型不受插件管理: " + animalType);
            loggerService.debug("CONTROLLER", "修改失败: 动物类型 %s 无配置", animalType);
            return;
        }
        
        // 3. 获取当前繁殖次数
        int oldCount = animalDataService.getBreedCount(targetAnimal);
        int maxCount = config.getMaxBreedTimes();
        
        // 4. 执行修改动作
        int newCount = executeAction(targetAnimal, action, value, oldCount);
        
        // 5. 发送反馈信息
        sendFeedback(sender, config, action, oldCount, newCount, maxCount, targetAnimal);
        
        // 6. 记录日志
        loggerService.info("繁殖次数已修改: %s(%s), %s, %d -> %d (上限: %d)",
            config.getDesc(), targetAnimal.getUniqueId(), action, oldCount, newCount, maxCount);
    }
    
    /**
     * 执行修改动作
     * 
     * @param animal 目标动物
     * @param action 修改动作
     * @param value 修改值
     * @param oldCount 原始次数
     * @return 新的繁殖次数
     */
    private int executeAction(@NotNull Animals animal, 
                             @NotNull ModifyAction action, 
                             int value,
                             int oldCount) {
        return switch (action) {
            case SET -> {
                animalDataService.setBreedCount(animal, value);
                yield value;
            }
            case ADD -> {
                int newCount = oldCount + value;
                if (newCount < 0) newCount = 0;  // 防止负数
                animalDataService.setBreedCount(animal, newCount);
                yield newCount;
            }
            case RESET -> {
                animalDataService.setBreedCount(animal, 0);
                yield 0;
            }
        };
    }
    
    /**
     * 发送反馈信息
     * 
     * @param sender 命令发送者
     * @param config 动物配置
     * @param action 修改动作
     * @param oldCount 旧次数
     * @param newCount 新次数
     * @param maxCount 最大次数
     * @param animal 动物实体
     */
    private void sendFeedback(@NotNull CommandSender sender,
                             @NotNull AnimalConfig config,
                             @NotNull ModifyAction action,
                             int oldCount,
                             int newCount,
                             int maxCount,
                             @NotNull Animals animal) {
        String animalDesc = config.getDesc();
        String uuid = animal.getUniqueId().toString().substring(0, 8);
        
        sender.sendMessage("§a=== 繁殖次数修改成功 ===");
        sender.sendMessage("§7动物类型: §f" + animalDesc + " §8(" + config.getType() + ")");
        sender.sendMessage("§7动物UUID: §f" + uuid + "...");
        sender.sendMessage("§7修改动作: §f" + getActionDescription(action));
        sender.sendMessage("§7繁殖次数: §e" + oldCount + " §7→ §a" + newCount + " §8(上限: " + maxCount + ")");
        
        // 显示状态提示
        if (newCount >= maxCount) {
            sender.sendMessage("§c⚠ 该动物已达到繁殖上限，无法继续繁殖！");
        } else {
            int remaining = maxCount - newCount;
            sender.sendMessage("§7剩余繁殖次数: §a" + remaining);
        }
    }
    
    /**
     * 获取动作描述
     * 
     * @param action 修改动作
     * @return 动作描述
     */
    private String getActionDescription(@NotNull ModifyAction action) {
        return switch (action) {
            case SET -> "设置";
            case ADD -> "增加";
            case RESET -> "重置";
        };
    }
}
