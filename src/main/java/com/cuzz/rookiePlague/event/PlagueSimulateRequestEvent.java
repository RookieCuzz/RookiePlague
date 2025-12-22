package com.cuzz.rookiePlague.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 染疫公式模拟计算请求事件
 * 
 * <p>当执行 /rp simulate 命令时触发此事件</p>
 * <p>该事件是同步事件，在主线程中触发</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class PlagueSimulateRequestEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final CommandSender sender;
    private final String animalType;
    private final Map<String, Double> customVariables;
    
    /**
     * 构造染疫公式模拟请求事件
     * 
     * @param sender 命令发送者
     * @param animalType 动物类型
     * @param customVariables 自定义变量（可选）
     */
    public PlagueSimulateRequestEvent(@NotNull CommandSender sender,
                                     @NotNull String animalType,
                                     @Nullable Map<String, Double> customVariables) {
        super(false);  // false = 同步事件
        this.sender = sender;
        this.animalType = animalType;
        this.customVariables = customVariables;
    }
    
    /**
     * 获取命令发送者
     * 
     * @return 命令发送者
     */
    @NotNull
    public CommandSender getSender() {
        return sender;
    }
    
    /**
     * 获取动物类型
     * 
     * @return 动物类型（如 COW, PIG 等）
     */
    @NotNull
    public String getAnimalType() {
        return animalType;
    }
    
    /**
     * 获取自定义变量
     * 
     * @return 自定义变量映射，如果没有则返回null
     */
    @Nullable
    public Map<String, Double> getCustomVariables() {
        return customVariables;
    }
    
    /**
     * 检查是否有自定义变量
     * 
     * @return 存在返回true
     */
    public boolean hasCustomVariables() {
        return customVariables != null && !customVariables.isEmpty();
    }
    
    // Bukkit 事件系统必需方法
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
