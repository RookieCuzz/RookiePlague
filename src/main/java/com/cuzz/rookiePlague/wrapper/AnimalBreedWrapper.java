package com.cuzz.rookiePlague.wrapper;

import com.cuzz.rookiePlague.event.AnimalBreedRequestEvent;
import com.cuzz.rookiePlague.service.LoggerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 动物繁殖包装器
 * 
 * <p>Wrapper层：监听原生繁殖事件并快速转发</p>
 * <p>职责：</p>
 * <ul>
 *   <li>监听 Bukkit 原生的 EntityBreedEvent</li>
 *   <li>快速过滤：只处理动物类型实体</li>
 *   <li>包装为自定义事件 AnimalBreedRequestEvent</li>
 *   <li>不处理具体业务逻辑</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalBreedWrapper implements Listener {
    
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param loggerService 日志服务
     */
    public AnimalBreedWrapper(@NotNull LoggerService loggerService) {
        this.loggerService = loggerService;
    }
    
    /**
     * 监听动物繁殖事件
     * 
     * <p>快速过滤并转发为自定义事件</p>
     * <p>使用 HIGH 优先级以便尽早介入</p>
     * 
     * @param event 原生繁殖事件
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityBreed(@NotNull EntityBreedEvent event) {
        // 快速类型检查 - 只处理动物
        Entity entity = event.getEntity();
        if (!(entity instanceof Animals)) {
            return;
        }
        
        // 获取父母实体
        Entity mother = event.getMother();
        Entity father = event.getFather();
        
        // 快速验证父母都是动物类型
        if (!(mother instanceof Animals) || !(father instanceof Animals)) {
            loggerService.debug("WRAPPER", "繁殖事件父母类型异常，跳过处理");
            return;
        }
        
        Animals motherAnimal = (Animals) mother;
        Animals fatherAnimal = (Animals) father;
        
        loggerService.debug("WRAPPER", "检测到动物繁殖: %s, 母体: %s, 父体: %s",
            entity.getType().name(),
            motherAnimal.getUniqueId(),
            fatherAnimal.getUniqueId());
        
        // 包装为自定义事件
        AnimalBreedRequestEvent customEvent = new AnimalBreedRequestEvent(
            motherAnimal,
            fatherAnimal
        );
        
        // 同步触发自定义事件
        Bukkit.getPluginManager().callEvent(customEvent);
        
        // 如果自定义事件被取消，则取消原生事件
        if (customEvent.isCancelled()) {
            event.setCancelled(true);
            
            String reason = customEvent.getCancelReason();
            if (reason != null) {
                loggerService.debug("WRAPPER", "繁殖被取消: %s", reason);
            }
        }
    }
}
