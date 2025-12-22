package com.cuzz.rookiePlague.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * 插件主配置类
 * 
 * <p>提供便捷的配置访问方法</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class PluginConfig {
    
    private final YamlConfiguration config;
    
    public PluginConfig(YamlConfiguration config) {
        this.config = config;
    }
    
    // ==================== 基础设置 ====================
    
    /**
     * 是否启用调试模式
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }
    
    /**
     * 获取配置版本
     */
    public String getConfigVersion() {
        return config.getString("settings.config-version", "1.0");
    }
    
    /**
     * 获取默认语言
     */
    public String getLanguage() {
        return config.getString("settings.language", "zh_CN");
    }
    
    /**
     * 是否启用详细日志
     */
    public boolean isVerboseLogging() {
        return config.getBoolean("settings.verbose-logging", true);
    }
    
    // ==================== 符号配置 ====================
    
    /**
     * 获取瘟疫符号
     */
    @NotNull
    public String getPlagueSymbol() {
        return config.getString("symbol.plague", "瘟");
    }
    
    /**
     * 获取繁殖上限符号
     */
    @NotNull
    public String getBreedSymbol() {
        return config.getString("symbol.breed", "阉");
    }
    
    // ==================== 瘟疫系统设置 ====================
    
    /**
     * 瘟疫系统是否启用
     */
    public boolean isPlagueEnabled() {
        return config.getBoolean("plague.enabled", true);
    }
    
    // ==================== 定时任务配置 ====================
    
    /**
     * 获取染疫检查间隔（秒）
     */
    public int getInfectionCheckInterval() {
        return config.getInt("plague.schedule.infection-check", 300);
    }
    
    /**
     * 获取染疫检查首次延迟（秒）
     */
    public int getInfectionCheckDelay() {
        return config.getInt("plague.schedule.infection-check-delay", 60);
    }
    
    /**
     * 获取环境数据更新间隔（秒）
     */
    public int getEnvironmentUpdateInterval() {
        return config.getInt("plague.schedule.environment-update", 30);
    }
    
    /**
     * 获取染疫概率公式
     * 
     * <p>支持 exp4j 语法的数学表达式</p>
     * <p>可用变量：</p>
     * <ul>
     *   <li>baseChance - 基础概率</li>
     *   <li>speciesFactor - 物种因子</li>
     *   <li>count - 区块内同类动物数量</li>
     *   <li>limit - 区块上限</li>
     *   <li>weatherFactor - 天气系数</li>
     *   <li>biomeFactor - 生物群系系数</li>
     *   <li>players - 在线人数</li>
     * </ul>
     * 
     * @return 公式字符串
     */
    public String getPlagueFormula() {
        return config.getString("plague.formula", 
            "baseChance * speciesFactor * ((count / limit) ^ 2) * weatherFactor * biomeFactor * (1 + (players / 100) * 0.1)");
    }
    
    /**
     * 获取基础染疫概率
     * 
     * @return 基础概率（0.0 - 1.0）
     */
    public double getBasePlagueChance() {
        return config.getDouble("plague.base-chance", 0.1);
    }
    
    /**
     * 获取晴天天气系数
     */
    public double getWeatherFactorClear() {
        return config.getDouble("plague.weather-factor.clear", 1.0);
    }
    
    /**
     * 获取雨天天气系数
     */
    public double getWeatherFactorRain() {
        return config.getDouble("plague.weather-factor.rain", 1.2);
    }
    
    /**
     * 获取雷暴天气系数
     */
    public double getWeatherFactorThunder() {
        return config.getDouble("plague.weather-factor.thunder", 1.5);
    }
    
    /**
     * 获取默认生物群系系数
     */
    public double getDefaultBiomeFactor() {
        return config.getDouble("plague.biome-factor.default", 1.0);
    }
    
    /**
     * 获取指定生物群系的系数
     * 
     * @param biome 生物群系名称
     * @return 系数值
     */
    public double getBiomeFactor(String biome) {
        return config.getDouble("plague.biome-factor." + biome.toLowerCase(), getDefaultBiomeFactor());
    }
    
    // ==================== 瘟疫伤害配置 ====================
    
    /**
     * 是否启用瘟疫伤害
     */
    public boolean isPlagueDamageEnabled() {
        return config.getBoolean("plague.damage.enabled", true);
    }
    
    /**
     * 获取瘟疫伤害间隔（秒）
     * 
     * <p>注：从 plague.schedule.damage-interval 读取</p>
     * <p>为了兼容性，如果新配置不存在，则回退到 plague.damage.interval</p>
     */
    public int getPlagueDamageInterval() {
        // 优先从 schedule 读取
        int scheduleInterval = config.getInt("plague.schedule.damage-interval", -1);
        if (scheduleInterval != -1) {
            return scheduleInterval;
        }
        // 回退到旧配置
        return config.getInt("plague.damage.interval", 10);
    }
    
    /**
     * 获取每次伤害值（心）
     */
    public double getPlagueDamageAmount() {
        return config.getDouble("plague.damage.amount", 1.0);
    }
    
    /**
     * 获取最低血量（心）
     */
    public double getPlagueDamageMinHealth() {
        return config.getDouble("plague.damage.min-health", 0.5);
    }
    
    /**
     * 获取原始配置对象
     */
    public YamlConfiguration getConfig() {
        return config;
    }
}
