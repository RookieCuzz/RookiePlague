package com.cuzz.rookiePlague.controller;

import com.cuzz.rookiePlague.RookiePlague;
import com.cuzz.rookiePlague.event.ConfigReloadRequestEvent;
import com.cuzz.rookiePlague.service.AnimalConfigService;
import com.cuzz.rookiePlague.service.LanguageService;
import com.cuzz.rookiePlague.service.LoggerService;
import com.cuzz.rookiePlague.config.ConfigurationManager;
import com.cuzz.rookiePlague.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 配置重载控制器
 * 
 * <p>监听配置重载请求事件并调度业务逻辑</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class ConfigReloadController implements Listener {
    
    private final ConfigurationManager configManager;
    private final AnimalConfigService animalConfigService;
    private final LoggerService loggerService;
    private final LanguageService languageService;
    private PluginConfig pluginConfig;
    private RookiePlague plugin;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param configManager 配置管理器
     * @param animalConfigService 动物配置服务
     * @param loggerService 日志服务
     * @param languageService 语言服务
     */
    public ConfigReloadController(ConfigurationManager configManager,
                                 AnimalConfigService animalConfigService,
                                 LoggerService loggerService,
                                 LanguageService languageService) {
        this.configManager = configManager;
        this.animalConfigService = animalConfigService;
        this.loggerService = loggerService;
        this.languageService = languageService;
    }
    
    /**
     * 设置插件配置引用（用于重载后更新）
     * 
     * @param pluginConfig 插件配置
     */
    public void setPluginConfig(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }
    
    /**
     * 设置插件实例（用于调用主类的重载方法）
     * 
     * @param plugin 插件实例
     */
    public void setPlugin(RookiePlague plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 监听配置重载请求事件
     * 
     * @param event 配置重载请求事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onConfigReloadRequest(ConfigReloadRequestEvent event) {
        CommandSender sender = event.getSender();
        long startTime = System.currentTimeMillis();
        
        loggerService.debug("RELOAD", "收到配置重载请求，来源: %s", event.getSource());
        
        try {
            if (sender != null) {
                sender.sendMessage("§e[RookiePlague] 开始重载配置...");
            }
            
            // 1. 重载主配置文件
            reloadMainConfig(sender);
            
            // 2. 重载动物配置
            reloadAnimalConfig(sender);
            
            // 3. 重载语言文件
            reloadLanguage(sender);
            
            // 4. 计算耗时
            long duration = System.currentTimeMillis() - startTime;
            
            // 5. 反馈结果
            String successMsg = languageService.getMessage("reload-success");
            
            if (sender != null) {
                sender.sendMessage(successMsg);
                sender.sendMessage(String.format("§7耗时: %dms", duration));
            }
            
            loggerService.success("配置重载完成，耗时 %dms", duration);
            
        } catch (Exception e) {
            // 重载失败
            event.setCancelReason("配置重载失败: " + e.getMessage());
            
            String failMsg = languageService.getMessage("reload-failed");
            
            if (sender != null) {
                sender.sendMessage(failMsg);
                sender.sendMessage("§c错误: " + e.getMessage());
            }
            
            loggerService.error("配置重载失败", e);
        }
    }
    
    /**
     * 重载主配置文件
     * 
     * @param sender 命令发送者
     */
    private void reloadMainConfig(CommandSender sender) {
        loggerService.debug("RELOAD", "正在重载主配置文件...");
        
        // 获取插件实例并调用重载方法
        if (plugin == null) {
            plugin = (RookiePlague) Bukkit.getPluginManager().getPlugin("RookiePlague");
        }
        
        if (plugin != null) {
            boolean success = plugin.reloadPluginConfig();
            
            if (success) {
                // 更新本地引用
                this.pluginConfig = plugin.getPluginConfig();
                
                loggerService.debug("RELOAD", "主配置文件重载完成");
                if (sender != null) {
                    sender.sendMessage("§7  - config.yml §a✓");
                }
            } else {
                throw new RuntimeException("主配置文件重载失败");
            }
        } else {
            throw new RuntimeException("无法获取插件实例");
        }
    }
    
    /**
     * 重载动物配置
     * 
     * @param sender 命令发送者
     */
    private void reloadAnimalConfig(CommandSender sender) {
        loggerService.debug("RELOAD", "正在重载动物配置...");
        
        boolean success = animalConfigService.reloadConfig();
        
        if (success) {
            int count = animalConfigService.getAnimalCount();
            loggerService.debug("RELOAD", "动物配置重载完成，共 %d 个配置项", count);
            
            if (sender != null) {
                sender.sendMessage(String.format("§7  - animal.yml §a✓ §7(共 %d 个配置)", count));
            }
        } else {
            throw new RuntimeException("动物配置重载失败");
        }
    }
    
    /**
     * 重载语言文件
     * 
     * @param sender 命令发送者
     */
    private void reloadLanguage(CommandSender sender) {
        loggerService.debug("RELOAD", "正在重载语言文件...");
        
        boolean success = languageService.reloadLanguage();
        
        if (success) {
            String language = languageService.getCurrentLanguage();
            loggerService.debug("RELOAD", "语言文件重载完成: %s", language);
            
            if (sender != null) {
                sender.sendMessage(String.format("§7  - language/%s.yml §a✓", language));
            }
        } else {
            throw new RuntimeException("语言文件重载失败");
        }
    }
}
