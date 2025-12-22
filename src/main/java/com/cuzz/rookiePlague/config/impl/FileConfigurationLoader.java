package com.cuzz.rookiePlague.config.impl;

import com.cuzz.rookiePlague.config.spi.ConfigurationLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;

/**
 * 文件配置加载器实现
 * 
 * <p>从本地文件系统加载 YAML 配置文件</p>
 * 
 * <p>功能特点：</p>
 * <ul>
 *   <li>自动从插件资源复制默认配置</li>
 *   <li>支持列表格式的 YAML 文件</li>
 *   <li>处理配置文件不存在的情况</li>
 *   <li>加载默认配置作为 fallback</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class FileConfigurationLoader implements ConfigurationLoader {
    
    private final JavaPlugin plugin;
    
    /**
     * 构造文件配置加载器
     * 
     * @param plugin 插件实例
     */
    public FileConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public YamlConfiguration loadConfig(String name) {
        // 确保配置文件存在
        File configFile = new File(plugin.getDataFolder(), name);
        if (!configFile.exists()) {
            plugin.saveResource(name, false);
        }
        
        try {
            return loadYamlConfiguration(configFile, name);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "加载配置文件失败: " + name, e);
            return loadDefaultConfig(name);
        }
    }
    
    @Override
    public YamlConfiguration reloadConfig(String name) {
        File configFile = new File(plugin.getDataFolder(), name);
        if (!configFile.exists()) {
            plugin.getLogger().warning("配置文件不存在，无法重载: " + name);
            return loadDefaultConfig(name);
        }
        
        try {
            return loadYamlConfiguration(configFile, name);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "重载配置文件失败: " + name, e);
            return null;
        }
    }
    
    @Override
    public boolean exists(String name) {
        File configFile = new File(plugin.getDataFolder(), name);
        return configFile.exists();
    }
    
    /**
     * 加载 YAML 配置文件
     * 
     * <p>自动检测配置文件格式：</p>
     * <ul>
     *   <li>列表格式：包装为 {rootKey: [list]}</li>
     *   <li>Map格式：正常加载</li>
     * </ul>
     * 
     * @param configFile 配置文件
     * @param name 配置文件名称
     * @return YAML配置对象
     * @throws IOException 读取文件失败时抛出
     */
    private YamlConfiguration loadYamlConfiguration(File configFile, String name) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        
        // 使用 SnakeYAML 检测数据类型
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(fis);
            
            // 如果是列表格式，需要包装一下
            if (data instanceof List) {
                String rootKey = getRootKeyFromFileName(name);
                config.set(rootKey, data);
                plugin.getLogger().info("检测到列表格式配置文件，已包装为: " + rootKey);
            } else {
                // 普通 Map 格式，正常加载
                config = YamlConfiguration.loadConfiguration(configFile);
            }
        }
        
        return config;
    }
    
    /**
     * 从配置文件名称提取根键名
     * 
     * @param fileName 文件名（如 "animal.yml"）
     * @return 根键名（如 "animals"）
     */
    private String getRootKeyFromFileName(String fileName) {
        String name = fileName.replace(".yml", "").replace(".yaml", "");
        // 转换为复数形式
        if (!name.endsWith("s")) {
            name += "s";
        }
        return name;
    }
    
    /**
     * 加载默认配置文件（从资源中）
     * 
     * @param name 配置文件名称
     * @return YAML配置对象，如果加载失败则返回空配置
     */
    private YamlConfiguration loadDefaultConfig(String name) {
        try (InputStream is = plugin.getResource(name)) {
            if (is != null) {
                plugin.getLogger().info("使用默认配置: " + name);
                return YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(is));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "加载默认配置失败: " + name, e);
        }
        return new YamlConfiguration();
    }
}
