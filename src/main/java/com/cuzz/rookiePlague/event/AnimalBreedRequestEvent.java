package com.cuzz.rookiePlague.event;

import org.bukkit.entity.Animals;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 动物繁殖请求事件
 * 
 * <p>当动物尝试繁殖时触发此事件，用于检查繁殖次数限制</p>
 * <p>该事件是同步事件，在主线程中触发</p>
 * <p>可以被取消以阻止繁殖</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalBreedRequestEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Animals mother;
    private final Animals father;
    private final String animalType;
    private boolean cancelled;
    private String cancelReason;
    
    /**
     * 构造动物繁殖请求事件
     * 
     * @param mother 母体动物
     * @param father 父体动物
     */
    public AnimalBreedRequestEvent(@NotNull Animals mother, @NotNull Animals father) {
        super(false);  // false = 同步事件
        this.mother = mother;
        this.father = father;
        this.animalType = mother.getType().name();
        this.cancelled = false;
    }
    
    /**
     * 获取母体动物
     * 
     * @return 母体动物实体
     */
    @NotNull
    public Animals getMother() {
        return mother;
    }
    
    /**
     * 获取父体动物
     * 
     * @return 父体动物实体
     */
    @NotNull
    public Animals getFather() {
        return father;
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
     * 获取取消原因
     * 
     * @return 取消原因，未取消则返回null
     */
    public String getCancelReason() {
        return cancelReason;
    }
    
    /**
     * 设置取消原因
     * 
     * @param reason 取消原因
     */
    public void setCancelReason(String reason) {
        this.cancelReason = reason;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
