package com.cuzz.rookiePlague.service;

import com.cuzz.rookiePlague.config.ConfigurationManager;
import com.cuzz.rookiePlague.model.AnimalConfig;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * 动物配置服务
 * 
 * <p>管理动物配置的业务逻辑</p>
 * 
 * <p>功能：</p>
 * <ul>
 *   <li>加载动物配置</li>
 *   <li>重载配置</li>
 *   <li>根据类型查询配置</li>
 *   <li>获取所有配置</li>
 *   <li>配置存在性检查</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalConfigService {
    
    private static final String CONFIG_FILE = "animal.yml";
    
    private final ConfigurationManager configManager;
    private Map<String, AnimalConfig> animalConfigMap;
    
    /**
     * 构造动物配置服务
     * 
     * @param configManager 配置管理器
     */
    public AnimalConfigService(ConfigurationManager configManager) {
        this.configManager = configManager;
        this.animalConfigMap = new LinkedHashMap<>();
    }
    
    /**
     * 加载动物配置
     * 
     * @return 是否加载成功
     */
    public boolean loadConfig() {
        try {
            YamlConfiguration config = configManager.getConfig(CONFIG_FILE);
            if (config == null) {
                return false;
            }
            
            this.animalConfigMap = AnimalConfig.parseToMap(config);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 重载动物配置
     * 
     * @return 是否重载成功
     */
    public boolean reloadConfig() {
        try {
            YamlConfiguration config = configManager.reloadConfig(CONFIG_FILE);
            if (config == null) {
                return false;
            }
            
            this.animalConfigMap = AnimalConfig.parseToMap(config);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 根据动物类型获取配置
     * 
     * @param type 动物类型（如 "SHEEP", "CHICKEN"）
     * @return 动物配置对象，不存在则返回null
     */
    public AnimalConfig getAnimalConfig(String type) {
        return animalConfigMap.get(type);
    }
    
    /**
     * 获取所有动物配置
     * 
     * @return 动物配置集合
     */
    public Collection<AnimalConfig> getAllAnimals() {
        return Collections.unmodifiableCollection(animalConfigMap.values());
    }
    
    /**
     * 获取所有动物类型
     * 
     * @return 动物类型集合
     */
    public Set<String> getAllAnimalTypes() {
        return Collections.unmodifiableSet(animalConfigMap.keySet());
    }
    
    /**
     * 检查指定类型的动物配置是否存在
     * 
     * @param type 动物类型
     * @return 存在返回true，否则返回false
     */
    public boolean exists(String type) {
        return animalConfigMap.containsKey(type);
    }
    
    /**
     * 获取已加载的动物配置数量
     * 
     * @return 配置数量
     */
    public int getAnimalCount() {
        return animalConfigMap.size();
    }
    
    /**
     * 获取配置文件路径
     * 
     * @return 配置文件名
     */
    public String getConfigFile() {
        return CONFIG_FILE;
    }
    
    /**
     * 根据物种因子范围查询动物
     * 
     * @param minFactor 最小物种因子
     * @param maxFactor 最大物种因子
     * @return 符合条件的动物配置列表
     */
    public List<AnimalConfig> getAnimalsBySpeciesFactor(double minFactor, double maxFactor) {
        List<AnimalConfig> result = new ArrayList<>();
        for (AnimalConfig config : animalConfigMap.values()) {
            if (config.getSpeciesFactor() >= minFactor && config.getSpeciesFactor() <= maxFactor) {
                result.add(config);
            }
        }
        return result;
    }
    
    /**
     * 根据区块上限查询动物
     * 
     * @param minLimit 最小区块上限
     * @param maxLimit 最大区块上限
     * @return 符合条件的动物配置列表
     */
    public List<AnimalConfig> getAnimalsByChunkLimit(int minLimit, int maxLimit) {
        List<AnimalConfig> result = new ArrayList<>();
        for (AnimalConfig config : animalConfigMap.values()) {
            if (config.getChunkLimit() >= minLimit && config.getChunkLimit() <= maxLimit) {
                result.add(config);
            }
        }
        return result;
    }
    
    /**
     * 根据尸体模型ID查询动物
     * 
     * @param corpseMobid 尸体模型ID
     * @return 使用该尸体模型的动物配置列表
     */
    public List<AnimalConfig> getAnimalsByCorpseMobid(String corpseMobid) {
        List<AnimalConfig> result = new ArrayList<>();
        for (AnimalConfig config : animalConfigMap.values()) {
            if (config.getCorpseMobid().equals(corpseMobid)) {
                result.add(config);
            }
        }
        return result;
    }
}
