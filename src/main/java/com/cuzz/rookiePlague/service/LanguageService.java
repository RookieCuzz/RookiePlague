package com.cuzz.rookiePlague.service;

import com.cuzz.rookiePlague.config.ConfigurationManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 语言服务
 * 
 * <p>负责加载和管理多语言文件</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class LanguageService {
    
    private final ConfigurationManager configManager;
    private final LoggerService loggerService;
    private YamlConfiguration languageConfig;
    private String currentLanguage;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param configManager 配置管理器
     * @param loggerService 日志服务
     */
    public LanguageService(@NotNull ConfigurationManager configManager, 
                          @NotNull LoggerService loggerService) {
        this.configManager = configManager;
        this.loggerService = loggerService;
    }
    
    /**
     * 加载语言文件
     * 
     * @param language 语言代码 (如 zh_CN, en_US)
     * @return 是否加载成功
     */
    public boolean loadLanguage(@NotNull String language) {
        this.currentLanguage = language;
        
        // 构建语言文件名
        String languageFileName = "language/" + language + ".yml";
        
        try {
            // 通过配置管理器加载语言文件
            languageConfig = configManager.getConfig(languageFileName);
            
            if (languageConfig == null) {
                loggerService.warning("语言文件不存在: %s", languageFileName);
                
                // 尝试加载默认语言 zh_CN
                if (!language.equals("zh_CN")) {
                    loggerService.warning("尝试加载默认语言 zh_CN");
                    return loadLanguage("zh_CN");
                }
                
                return false;
            }
            
            loggerService.success("成功加载语言文件: %s", language);
            return true;
        } catch (Exception e) {
            loggerService.error("加载语言文件失败: " + language, e);
            
            // 尝试加载默认语言 zh_CN
            if (!language.equals("zh_CN")) {
                loggerService.warning("尝试加载默认语言 zh_CN");
                return loadLanguage("zh_CN");
            }
            
            return false;
        }
    }
    
    /**
     * 获取消息
     * 
     * @param key 消息键
     * @return 消息内容
     */
    @NotNull
    public String getMessage(@NotNull String key) {
        if (languageConfig == null) {
            return "§c[Language Error] " + key;
        }
        
        String message = languageConfig.getString("messages." + key);
        
        if (message == null) {
            loggerService.warning("语言文件中缺少消息键: %s", key);
            return "§c[Missing] " + key;
        }
        
        return message;
    }
    
    /**
     * 获取消息并替换占位符
     * 
     * @param key 消息键
     * @param replacements 替换映射 (占位符 -> 值)
     * @return 替换后的消息
     */
    @NotNull
    public String getMessage(@NotNull String key, @NotNull Map<String, String> replacements) {
        String message = getMessage(key);
        
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
    
    /**
     * 获取消息并替换占位符（简化方法）
     * 
     * @param key 消息键
     * @param placeholder 占位符名称
     * @param value 替换值
     * @return 替换后的消息
     */
    @NotNull
    public String getMessage(@NotNull String key, @NotNull String placeholder, @NotNull String value) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put(placeholder, value);
        return getMessage(key, replacements);
    }
    
    /**
     * 重载语言文件
     * 
     * @return 是否重载成功
     */
    public boolean reloadLanguage() {
        // 构建语言文件名
        String languageFileName = "language/" + currentLanguage + ".yml";
        
        try {
            // 通过配置管理器重载语言文件
            languageConfig = configManager.reloadConfig(languageFileName);
            
            if (languageConfig == null) {
                loggerService.warning("重载语言文件失败: %s", languageFileName);
                return false;
            }
            
            loggerService.success("成功重载语言文件: %s", currentLanguage);
            return true;
        } catch (Exception e) {
            loggerService.error("重载语言文件失败: " + currentLanguage, e);
            return false;
        }
    }
    
    /**
     * 获取当前语言
     * 
     * @return 当前语言代码
     */
    @NotNull
    public String getCurrentLanguage() {
        return currentLanguage != null ? currentLanguage : "zh_CN";
    }
}
