package com.cuzz.rookiePlague.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 配置重载请求事件
 * 
 * <p>使用 Bukkit 内置事件系统，统一包装来自不同触发源的重载请求</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class ConfigReloadRequestEvent extends Event {
    
    // Bukkit 事件系统必需的 HandlerList
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final CommandSender sender;
    private final ReloadSource source;
    private boolean cancelled = false;
    private String cancelReason;
    
    /**
     * 重载来源枚举
     */
    public enum ReloadSource {
        COMMAND,        // 来自命令
        CONSOLE,        // 来自控制台
        AUTO,           // 自动重载
        PLUGIN_LOAD     // 插件加载时
    }
    
    /**
     * 构造函数
     * 
     * @param sender 命令发送者（可能为 null）
     * @param source 重载来源
     */
    public ConfigReloadRequestEvent(@Nullable CommandSender sender, 
                                   @NotNull ReloadSource source) {
        this.sender = sender;
        this.source = source;
    }
    
    // ==================== Getters ====================
    
    /**
     * 获取命令发送者
     * 
     * @return 命令发送者，可能为 null
     */
    @Nullable
    public CommandSender getSender() {
        return sender;
    }
    
    /**
     * 获取重载来源
     * 
     * @return 重载来源
     */
    @NotNull
    public ReloadSource getSource() {
        return source;
    }
    
    // ==================== 事件取消机制 ====================
    
    /**
     * 事件是否被取消
     * 
     * @return 是否被取消
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * 设置事件取消状态
     * 
     * @param cancelled 是否取消
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * 获取取消原因
     * 
     * @return 取消原因，可能为 null
     */
    @Nullable
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
        this.cancelled = true;
    }
    
    // ==================== Bukkit 事件系统必需方法 ====================
    
    /**
     * Bukkit 事件系统必需
     * 返回实例的 HandlerList
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    /**
     * Bukkit 事件系统必需
     * 返回静态的 HandlerList
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
