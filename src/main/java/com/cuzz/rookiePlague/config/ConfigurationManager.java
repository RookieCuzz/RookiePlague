package com.cuzz.rookiePlague.config;

import com.cuzz.rookiePlague.config.spi.ConfigurationLoader;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 配置管理器
 * 
 * <p>提供统一的配置访问接口和缓存管理</p>
 * 
 * <p>功能特点：</p>
 * <ul>
 *   <li>配置缓存（避免重复加载）</li>
 *   <li>配置热重载</li>
 *   <li>统一访问入口</li>
 *   <li>线程安全</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class ConfigurationManager {
    
    private final ConfigurationLoader loader;
    private final Map<String, YamlConfiguration> configCache;
    
    /**
     * 构造配置管理器
     * 
     * @param loader 配置加载器实现
     */
    public ConfigurationManager(ConfigurationLoader loader) {
        this.loader = loader;
        this.configCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取指定名称的配置文件
     * 
     * <p>如果缓存中存在则直接返回，否则加载后缓存</p>
     * 
     * @param name 配置文件名称
     * @return YAML配置对象，如果加载失败则返回null
     */
    public YamlConfiguration getConfig(String name) {
        return configCache.computeIfAbsent(name, loader::loadConfig);
    }
    
    /**
     * 重新加载指定名称的配置文件
     * 
     * <p>清除缓存并重新加载</p>
     * 
     * @param name 配置文件名称
     * @return 重新加载的YAML配置对象
     */
    public YamlConfiguration reloadConfig(String name) {
        configCache.remove(name);
        YamlConfiguration config = loader.reloadConfig(name);
        if (config != null) {
            configCache.put(name, config);
        }
        return config;
    }
    
    /**
     * 检查指定配置文件是否存在
     * 
     * @param name 配置文件名称
     * @return 存在返回true，否则返回false
     */
    public boolean exists(String name) {
        return loader.exists(name);
    }
    
    /**
     * 清空所有配置缓存
     */
    public void clearCache() {
        configCache.clear();
    }
    
    /**
     * 重载所有已缓存的配置文件
     */
    public void reloadAll() {
        for (String name : configCache.keySet()) {
            reloadConfig(name);
        }
    }
}
