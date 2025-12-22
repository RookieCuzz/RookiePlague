package com.cuzz.rookiePlague.controller;

import com.cuzz.rookiePlague.cache.EnvironmentCache;
import com.cuzz.rookiePlague.config.PluginConfig;
import com.cuzz.rookiePlague.event.PlagueSimulateRequestEvent;
import com.cuzz.rookiePlague.model.AnimalConfig;
import com.cuzz.rookiePlague.service.AnimalConfigService;
import com.cuzz.rookiePlague.service.LoggerService;
import com.cuzz.rookiePlague.service.PlagueFormulaService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 染疫公式模拟计算控制器
 * 
 * <p>Controller层：监听染疫模拟请求事件并调度业务处理</p>
 * <p>职责：</p>
 * <ul>
 *   <li>监听 PlagueSimulateRequestEvent 事件</li>
 *   <li>协调各Service进行模拟计算</li>
 *   <li>从游戏环境获取缺失的变量（玩家数、天气、生物群系）</li>
 *   <li>输出详细的计算过程到后台日志</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class PlagueSimulateController implements Listener {
    
    private final AnimalConfigService animalConfigService;
    private final PlagueFormulaService plagueFormulaService;
    private final EnvironmentCache environmentCache;
    private final LoggerService loggerService;
    private final PluginConfig pluginConfig;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param animalConfigService 动物配置服务
     * @param plagueFormulaService 公式服务
     * @param environmentCache 环境数据缓存
     * @param loggerService 日志服务
     * @param pluginConfig 插件配置
     */
    public PlagueSimulateController(@NotNull AnimalConfigService animalConfigService,
                                    @NotNull PlagueFormulaService plagueFormulaService,
                                    @NotNull EnvironmentCache environmentCache,
                                    @NotNull LoggerService loggerService,
                                    @NotNull PluginConfig pluginConfig) {
        this.animalConfigService = animalConfigService;
        this.plagueFormulaService = plagueFormulaService;
        this.environmentCache = environmentCache;
        this.loggerService = loggerService;
        this.pluginConfig = pluginConfig;
    }
    
    /**
     * 监听染疫公式模拟请求事件
     * 
     * @param event 模拟请求事件
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlagueSimulateRequest(@NotNull PlagueSimulateRequestEvent event) {
        CommandSender sender = event.getSender();
        String animalType = event.getAnimalType();
        
        loggerService.info("====== 染疫公式模拟计算开始 ======");
        loggerService.info("请求者: %s", sender.getName());
        loggerService.info("动物类型: %s", animalType);
        
        // 1. 获取动物配置
        AnimalConfig animalConfig = animalConfigService.getAnimalConfig(animalType.toUpperCase());
        if (animalConfig == null) {
            sender.sendMessage("§c错误: 未找到动物 " + animalType + " 的配置");
            loggerService.warning("未找到动物配置: %s", animalType);
            loggerService.info("====== 染疫公式模拟计算结束（失败） ======");
            return;
        }
        
        loggerService.info("动物配置: %s (%s)", animalConfig.getType(), animalConfig.getDesc());
        
        // 2. 构建变量
        Map<String, Double> variables = buildVariables(event, animalConfig);
        
        // 3. 输出计算过程
        sender.sendMessage("§e====== 染疫公式模拟计算 ======");
        sender.sendMessage("§7动物: §f" + animalConfig.getDesc() + " §8(" + animalConfig.getType() + ")");
        sender.sendMessage("");
        sender.sendMessage("§7公式: §f" + plagueFormulaService.getFormulaString());
        sender.sendMessage("");
        sender.sendMessage("§7变量值:");
        
        loggerService.info("计算公式: %s", plagueFormulaService.getFormulaString());
        loggerService.info("变量值:");
        
        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            String varName = entry.getKey();
            Double varValue = entry.getValue();
            sender.sendMessage(String.format("§7  %s = §f%.4f", varName, varValue));
            loggerService.info("  %s = %.4f", varName, varValue);
        }
        
        // 4. 执行计算
        double result = plagueFormulaService.calculate(variables);
        
        // 5. 输出结果
        sender.sendMessage("");
        sender.sendMessage(String.format("§7计算结果: §a%.4f §7(%.2f%%)", result, result * 100));
        sender.sendMessage("§e==============================");
        
        loggerService.info("计算结果: %.4f (%.2f%%)", result, result * 100);
        loggerService.info("====== 染疫公式模拟计算结束 ======");
    }
    
    /**
     * 构建计算变量
     * 
     * <p>优先使用用户提供的自定义变量，缺失的变量从游戏环境获取</p>
     * <p>环境变量获取优先级：</p>
     * <ul>
     *   <li>count: 如果请求者是玩家，实时统计所在区块的同类动物数量；否则使用模拟值（50%上限）</li>
     *   <li>players: 从 EnvironmentCache 缓存获取</li>
     *   <li>weatherFactor: 如果请求者是玩家，从所在世界实时读取；否则使用默认晴天系数</li>
     *   <li>biomeFactor: 如果请求者是玩家，从所在位置实时读取；否则使用默认系数</li>
     * </ul>
     * 
     * @param event 模拟请求事件
     * @param animalConfig 动物配置
     * @return 完整的变量映射
     */
    @NotNull
    private Map<String, Double> buildVariables(@NotNull PlagueSimulateRequestEvent event,
                                              @NotNull AnimalConfig animalConfig) {
        Map<String, Double> variables = new HashMap<>();
        Map<String, Double> customVars = event.getCustomVariables();
        
        // baseChance - 基础概率
        if (customVars != null && customVars.containsKey("baseChance")) {
            variables.put("baseChance", customVars.get("baseChance"));
            loggerService.debug("SIMULATE", "baseChance: 使用自定义值 %.4f", customVars.get("baseChance"));
        } else {
            double baseChance = pluginConfig.getBasePlagueChance();
            variables.put("baseChance", baseChance);
            loggerService.debug("SIMULATE", "baseChance: 从配置获取 %.4f", baseChance);
        }
        
        // speciesFactor - 物种因子
        if (customVars != null && customVars.containsKey("speciesFactor")) {
            variables.put("speciesFactor", customVars.get("speciesFactor"));
            loggerService.debug("SIMULATE", "speciesFactor: 使用自定义值 %.4f", customVars.get("speciesFactor"));
        } else {
            double speciesFactor = animalConfig.getSpeciesFactor();
            variables.put("speciesFactor", speciesFactor);
            loggerService.debug("SIMULATE", "speciesFactor: 从动物配置获取 %.4f", speciesFactor);
        }
        
        // count - 区块内同类动物数量
        if (customVars != null && customVars.containsKey("count")) {
            variables.put("count", customVars.get("count"));
            loggerService.debug("SIMULATE", "count: 使用自定义值 %.0f", customVars.get("count"));
        } else {
            // 从实际游戏环境获取
            double count = getAnimalCountFromEnvironment(event.getSender(), animalConfig);
            variables.put("count", count);
            loggerService.debug("SIMULATE", "count: 从环境获取 %.0f", count);
        }
        
        // limit - 区块上限
        if (customVars != null && customVars.containsKey("limit")) {
            variables.put("limit", customVars.get("limit"));
            loggerService.debug("SIMULATE", "limit: 使用自定义值 %.0f", customVars.get("limit"));
        } else {
            double limit = animalConfig.getChunkLimit();
            variables.put("limit", limit);
            loggerService.debug("SIMULATE", "limit: 从动物配置获取 %.0f", limit);
        }
        
        // weatherFactor - 天气系数
        if (customVars != null && customVars.containsKey("weatherFactor")) {
            variables.put("weatherFactor", customVars.get("weatherFactor"));
            loggerService.debug("SIMULATE", "weatherFactor: 使用自定义值 %.4f", customVars.get("weatherFactor"));
        } else {
            // 从实际游戏环境获取天气
            double weatherFactor = getWeatherFactorFromEnvironment(event.getSender());
            variables.put("weatherFactor", weatherFactor);
            loggerService.debug("SIMULATE", "weatherFactor: 从环境获取 %.4f", weatherFactor);
        }
        
        // biomeFactor - 生物群系系数
        if (customVars != null && customVars.containsKey("biomeFactor")) {
            variables.put("biomeFactor", customVars.get("biomeFactor"));
            loggerService.debug("SIMULATE", "biomeFactor: 使用自定义值 %.4f", customVars.get("biomeFactor"));
        } else {
            // 从实际游戏环境获取生物群系
            double biomeFactor = getBiomeFactorFromEnvironment(event.getSender());
            variables.put("biomeFactor", biomeFactor);
            loggerService.debug("SIMULATE", "biomeFactor: 从环境获取 %.4f", biomeFactor);
        }
        
        // players - 在线玩家数
        if (customVars != null && customVars.containsKey("players")) {
            variables.put("players", customVars.get("players"));
            loggerService.debug("SIMULATE", "players: 使用自定义值 %.0f", customVars.get("players"));
        } else {
            // 从环境缓存获取在线玩家数
            double players = environmentCache.getOnlinePlayerCount();
            variables.put("players", players);
            loggerService.debug("SIMULATE", "players: 从缓存获取 %.0f", players);
        }
        
        return variables;
    }
    
    /**
     * 从游戏环境获取天气系数
     * 
     * <p>如果请求者是玩家，从所在世界实时读取天气</p>
     * <p>否则使用默认晴天系数</p>
     * 
     * @param sender 命令发送者
     * @return 天气系数
     */
    private double getWeatherFactorFromEnvironment(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            // 非玩家请求，使用默认晴天系数
            return pluginConfig.getWeatherFactorClear();
        }
        
        // 获取玩家所在世界的天气
        String worldName = player.getWorld().getName();
        EnvironmentCache.WeatherType weather = environmentCache.getWorldWeather(worldName);
        
        // 根据天气类型返回系数
        return switch (weather) {
            case CLEAR -> pluginConfig.getWeatherFactorClear();
            case RAIN -> pluginConfig.getWeatherFactorRain();
            case THUNDER -> pluginConfig.getWeatherFactorThunder();
        };
    }
    
    /**
     * 从游戏环境获取生物群系系数
     * 
     * <p>如果请求者是玩家，从所在位置实时读取生物群系</p>
     * <p>否则使用默认系数</p>
     * 
     * @param sender 命令发送者
     * @return 生物群系系数
     */
    private double getBiomeFactorFromEnvironment(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            // 非玩家请求，使用默认系数
            return pluginConfig.getDefaultBiomeFactor();
        }
        
        // 获取玩家所在位置的生物群系
        Location location = player.getLocation();
        Biome biome = location.getBlock().getBiome();
        
        // 从配置获取该生物群系的系数
        return pluginConfig.getBiomeFactor(biome.name());
    }
    
    /**
     * 从游戏环境获取动物数量
     * 
     * <p>如果请求者是玩家，实时统计所在区块的同类动物数量</p>
     * <p>否则使用模拟值（上限的50%）</p>
     * 
     * @param sender 命令发送者
     * @param animalConfig 动物配置
     * @return 动物数量
     */
    private double getAnimalCountFromEnvironment(@NotNull CommandSender sender,
                                                 @NotNull AnimalConfig animalConfig) {
        if (!(sender instanceof Player player)) {
            // 非玩家请求，使用模拟值
            return animalConfig.getChunkLimit() * 0.5;
        }
        
        // 获取玩家所在区块
        Chunk chunk = player.getLocation().getChunk();
        
        // 获取目标动物类型
        EntityType targetType;
        try {
            targetType = EntityType.valueOf(animalConfig.getType());
        } catch (IllegalArgumentException e) {
            loggerService.warning("无效的动物类型: %s，使用模拟值", animalConfig.getType());
            return animalConfig.getChunkLimit() * 0.5;
        }
        
        // 统计区块内的同类动物数量
        int count = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == targetType && entity instanceof Animals) {
                count++;
            }
        }
        
        return count;
    }
}
