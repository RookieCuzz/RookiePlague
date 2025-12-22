package com.cuzz.rookiePlague.config.spi;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 配置加载器接口 (SPI)
 * 
 * <p>定义配置加载的标准行为，支持多种实现方式（文件、云端、数据库等）</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public interface ConfigurationLoader {
    
    /**
     * 加载指定名称的配置文件
     * 
     * @param name 配置文件名称（如 "animal.yml"）
     * @return YAML配置对象，如果加载失败则返回null
     */
    YamlConfiguration loadConfig(String name);
    
    /**
     * 重新加载指定名称的配置文件
     * 
     * @param name 配置文件名称
     * @return 重新加载的YAML配置对象，如果加载失败则返回null
     */
    YamlConfiguration reloadConfig(String name);
    
    /**
     * 检查指定配置文件是否存在
     * 
     * @param name 配置文件名称
     * @return 存在返回true，否则返回false
     */
    boolean exists(String name);
}
