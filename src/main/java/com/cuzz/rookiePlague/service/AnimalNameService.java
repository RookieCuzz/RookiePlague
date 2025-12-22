package com.cuzz.rookiePlague.service;

import com.cuzz.rookiePlague.config.PluginConfig;
import com.cuzz.rookiePlague.model.AnimalConfig;
import org.bukkit.entity.Animals;
import org.jetbrains.annotations.NotNull;

/**
 * 动物名称管理服务
 * 
 * <p>Service层：管理动物的自定义名称显示</p>
 * <p>职责：</p>
 * <ul>
 *   <li>根据动物状态组合自定义名称</li>
 *   <li>处理瘟疫标记和繁殖上限标记</li>
 *   <li>更新动物的 CustomName</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalNameService {
    
    private final AnimalDataService animalDataService;
    private final AnimalConfigService animalConfigService;
    private final PluginConfig pluginConfig;
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param animalDataService 动物数据服务
     * @param animalConfigService 动物配置服务
     * @param pluginConfig 插件配置
     * @param loggerService 日志服务
     */
    public AnimalNameService(@NotNull AnimalDataService animalDataService,
                            @NotNull AnimalConfigService animalConfigService,
                            @NotNull PluginConfig pluginConfig,
                            @NotNull LoggerService loggerService) {
        this.animalDataService = animalDataService;
        this.animalConfigService = animalConfigService;
        this.pluginConfig = pluginConfig;
        this.loggerService = loggerService;
    }
    
    /**
     * 更新动物的自定义名称
     * 
     * <p>根据动物的状态（染疫、繁殖次数）组合显示名称</p>
     * <p>规则：</p>
     * <ul>
     *   <li>仅染疫：显示瘟疫符号（如 "瘟"）</li>
     *   <li>仅繁殖上限：显示繁殖符号（如 "阉"）</li>
     *   <li>同时存在：显示"瘟 阉"（中间有空格）</li>
     *   <li>都不存在：清除 CustomName</li>
     * </ul>
     * 
     * @param animal 动物实体
     */
    public void updateAnimalName(@NotNull Animals animal) {
        try {
            // 检查动物状态
            boolean isInfected = animalDataService.isInfected(animal);
            boolean isBreedMaxed = isBreedMaxed(animal);
            
            // 组合名称
            String customName = buildCustomName(isInfected, isBreedMaxed);
            
            // 更新显示
            if (customName != null && !customName.isEmpty()) {
                animal.setCustomName(customName);
                animal.setCustomNameVisible(true);
            } else {
                animal.setCustomName(null);
                animal.setCustomNameVisible(false);
            }
            
            loggerService.debug("ANIMAL_NAME", 
                "更新动物名称: %s -> %s (染疫=%b, 繁殖上限=%b)", 
                animal.getType(), customName, isInfected, isBreedMaxed);
                
        } catch (Exception e) {
            loggerService.error("更新动物名称时发生异常", e);
        }
    }
    
    /**
     * 构建自定义名称
     * 
     * @param isInfected 是否染疫
     * @param isBreedMaxed 是否达到繁殖上限
     * @return 组合后的名称，无状态时返回 null
     */
    private String buildCustomName(boolean isInfected, boolean isBreedMaxed) {
        if (!isInfected && !isBreedMaxed) {
            return null; // 无状态
        }
        
        String plagueSymbol = pluginConfig.getPlagueSymbol();
        String breedSymbol = pluginConfig.getBreedSymbol();
        
        if (isInfected && isBreedMaxed) {
            // 两者都有：瘟 阉
            return "§f" + plagueSymbol + " §f" + breedSymbol;
        } else if (isInfected) {
            // 仅染疫：瘟
            return "§f" + plagueSymbol;
        } else {
            // 仅繁殖上限：阉
            return "§f" + breedSymbol;
        }
    }
    
    /**
     * 检查动物是否达到繁殖上限
     * 
     * @param animal 动物实体
     * @return 是否达到繁殖上限
     */
    private boolean isBreedMaxed(@NotNull Animals animal) {
        // 获取该物种的配置
        AnimalConfig config = animalConfigService.getAnimalConfig(animal.getType().name());
        if (config == null) {
            return false;
        }
        
        // 检查是否达到上限
        return animalDataService.hasReachedMaxBreedTimes(animal, config.getMaxBreedTimes());
    }
}
