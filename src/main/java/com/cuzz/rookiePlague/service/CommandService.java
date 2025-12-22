package com.cuzz.rookiePlague.service;

import com.cuzz.rookiePlague.config.ConfigurationManager;
import com.cuzz.rookiePlague.config.PluginConfig;
import com.cuzz.rookiePlague.event.ConfigReloadRequestEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 命令服务层
 * 
 * <p>处理命令相关的业务逻辑，遵循 Controller → Service 架构</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class CommandService {
    
    private final Plugin plugin;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param plugin 插件实例
     * @param loggerService 日志服务
     */
    public CommandService(@NotNull Plugin plugin, 
                         @NotNull LoggerService loggerService) {
        this.plugin = plugin;
        this.loggerService = loggerService;
    }
    
    /**
     * 处理配置重载请求
     * 
     * <p>通过事件系统触发配置重载，支持不同来源的重载请求</p>
     * 
     * @param sender 命令发送者（可能为 null）
     * @param source 重载来源
     */
    public void handleReloadConfig(@Nullable CommandSender sender,
                                   @NotNull ConfigReloadRequestEvent.ReloadSource source) {
        loggerService.debug("COMMAND", "处理重载请求，来源: %s", source);
        
        // 创建重载请求事件
        ConfigReloadRequestEvent event = new ConfigReloadRequestEvent(sender, source);
        
        // 同步触发事件
        Bukkit.getPluginManager().callEvent(event);
        
        // 检查事件是否被取消
        if (event.isCancelled()) {
            String reason = event.getCancelReason();
            loggerService.warning("配置重载被取消: %s", reason != null ? reason : "未知原因");
        }
    }
    
    /**
     * 处理配置重载请求（从命令触发）
     * 
     * @param sender 命令发送者
     */
    public void handleReloadConfig(@NotNull CommandSender sender) {
        ConfigReloadRequestEvent.ReloadSource source = 
            sender instanceof org.bukkit.command.ConsoleCommandSender 
                ? ConfigReloadRequestEvent.ReloadSource.CONSOLE 
                : ConfigReloadRequestEvent.ReloadSource.COMMAND;
        
        handleReloadConfig(sender, source);
    }
    
    /**
     * 处理自动配置重载（无发送者）
     */
    public void handleAutoReload() {
        handleReloadConfig(null, ConfigReloadRequestEvent.ReloadSource.AUTO);
    }
}
