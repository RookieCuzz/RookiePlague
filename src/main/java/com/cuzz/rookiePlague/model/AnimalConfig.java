package com.cuzz.rookiePlague.model;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动物配置数据模型
 * 
 * <p>对应 animal.yml 中的配置项</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalConfig {
    
    /** 动物类型（如 COW, PIG, CHICKEN 等，作为唯一标识） */
    private String type;
    
    /** 动物描述（中文名称） */
    private String desc;
    
    /** 物种因子（影响染疫概率计算的系数） */
    private double speciesFactor;
    
    /** 区块上限（同类动物在单个区块内的数量上限） */
    private int chunkLimit;
    
    /** 死亡生成尸体概率（百分比，如80表示80%） */
    private int corpseDropRate;
    
    /** 尸体的CE模型id（对应CustomEntity插件的模型ID） */
    private String corpseMobid;
    
    /** 最大繁殖次数（每只动物的繁殖次数上限） */
    private int maxBreedTimes;
    
    /** 瘟疫致死时间（未治疗情况下从感染到死亡的时间，单位：秒） */
    private int plagueDeathTime;
    
    // 构造方法
    public AnimalConfig() {
    }
    
    public AnimalConfig(String type, String desc, double speciesFactor, int chunkLimit,
                       int corpseDropRate, String corpseMobid, int maxBreedTimes, int plagueDeathTime) {
        this.type = type;
        this.desc = desc;
        this.speciesFactor = speciesFactor;
        this.chunkLimit = chunkLimit;
        this.corpseDropRate = corpseDropRate;
        this.corpseMobid = corpseMobid;
        this.maxBreedTimes = maxBreedTimes;
        this.plagueDeathTime = plagueDeathTime;
    }
    
    // Getter 和 Setter
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDesc() {
        return desc;
    }
    
    public void setDesc(String desc) {
        this.desc = desc;
    }
    
    public double getSpeciesFactor() {
        return speciesFactor;
    }
    
    public void setSpeciesFactor(double speciesFactor) {
        this.speciesFactor = speciesFactor;
    }
    
    public int getChunkLimit() {
        return chunkLimit;
    }
    
    public void setChunkLimit(int chunkLimit) {
        this.chunkLimit = chunkLimit;
    }
    
    public int getCorpseDropRate() {
        return corpseDropRate;
    }
    
    public void setCorpseDropRate(int corpseDropRate) {
        this.corpseDropRate = corpseDropRate;
    }
    
    public String getCorpseMobid() {
        return corpseMobid;
    }
    
    public void setCorpseMobid(String corpseMobid) {
        this.corpseMobid = corpseMobid;
    }
    
    public int getMaxBreedTimes() {
        return maxBreedTimes;
    }
    
    public void setMaxBreedTimes(int maxBreedTimes) {
        this.maxBreedTimes = maxBreedTimes;
    }
    
    public int getPlagueDeathTime() {
        return plagueDeathTime;
    }
    
    public void setPlagueDeathTime(int plagueDeathTime) {
        this.plagueDeathTime = plagueDeathTime;
    }
    
    // 工具方法
    
    /**
     * 从配置文件解析动物配置列表
     * 
     * @param config YAML配置对象
     * @return 动物配置列表
     */
    public static List<AnimalConfig> parseFromConfig(YamlConfiguration config) {
        List<AnimalConfig> animals = new ArrayList<>();
        
        // 尝试多个可能的根键名
        List<?> dataList = config.getList("animals");
        if (dataList == null) {
            dataList = config.getList("animal");
        }
        
        if (dataList == null || dataList.isEmpty()) {
            return animals;
        }
        
        for (Object obj : dataList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                AnimalConfig animal = parseFromMap(map);
                if (animal != null) {
                    animals.add(animal);
                }
            }
        }
        
        return animals;
    }
    
    /**
     * 从配置文件解析动物配置并转换为Map
     * 
     * <p>以 type 字段作为 key，提供 O(1) 查找效率</p>
     * 
     * @param config YAML配置对象
     * @return 以动物类型为键的Map
     */
    public static Map<String, AnimalConfig> parseToMap(YamlConfiguration config) {
        List<AnimalConfig> animals = parseFromConfig(config);
        return animals.stream()
                .collect(Collectors.toMap(
                        AnimalConfig::getType,
                        animal -> animal,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));
    }
    
    /**
     * 从Map解析单个动物配置
     * 
     * @param map 配置数据Map
     * @return 动物配置对象，解析失败返回null
     */
    private static AnimalConfig parseFromMap(Map<String, Object> map) {
        try {
            AnimalConfig config = new AnimalConfig();
            
            config.setType(getStringValue(map, "type", ""));
            config.setDesc(getStringValue(map, "desc", ""));
            config.setSpeciesFactor(getDoubleValue(map, "speciesFactor", 1.0));
            config.setChunkLimit(getIntValue(map, "chunkLimit", 10));
            config.setCorpseDropRate(getIntValue(map, "corpseDropRate", 50));
            config.setCorpseMobid(getStringValue(map, "corpseMobid", ""));
            config.setMaxBreedTimes(getIntValue(map, "maxBreedTimes", 5));
            config.setPlagueDeathTime(getIntValue(map, "plagueDeathTime", 300));
            
            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 类型安全的值提取方法
    
    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private static double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(value.toString()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    @Override
    public String toString() {
        return "AnimalConfig{" +
                "type='" + type + '\'' +
                ", desc='" + desc + '\'' +
                ", speciesFactor=" + speciesFactor +
                ", chunkLimit=" + chunkLimit +
                ", corpseDropRate=" + corpseDropRate +
                ", corpseMobid='" + corpseMobid + '\'' +
                ", maxBreedTimes=" + maxBreedTimes +
                ", plagueDeathTime=" + plagueDeathTime +
                '}';
    }
}
