package com.cuzz.rookiePlague.wrapper;

import com.cuzz.rookiePlague.service.AnimalConfigService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 命令 Tab 自动补全器
 * 
 * <p>提供智能的命令参数提示</p>
 * <p>职责：</p>
 * <ul>
 *   <li>根据当前输入提供参数建议</li>
 *   <li>动态加载动物类型列表</li>
 *   <li>提供变量名和动作名提示</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class CommandTabCompleter implements TabCompleter {
    
    private final AnimalConfigService animalConfigService;
    
    // 子命令列表
    private static final List<String> SUB_COMMANDS = Arrays.asList(
        "reload", "simulate", "breedcount", "bc", "help"
    );
    
    // breedcount 动作列表
    private static final List<String> BREED_ACTIONS = Arrays.asList(
        "set", "add", "reset"
    );
    
    // simulate 变量名列表
    private static final List<String> SIMULATE_VARIABLES = Arrays.asList(
        "baseChance", "speciesFactor", "count", "limit", 
        "weatherFactor", "biomeFactor", "players"
    );
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param animalConfigService 动物配置服务（用于获取动物类型列表）
     */
    public CommandTabCompleter(@NotNull AnimalConfigService animalConfigService) {
        this.animalConfigService = animalConfigService;
    }
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                     @NotNull Command command,
                                     @NotNull String alias,
                                     @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 第一个参数：子命令
        if (args.length == 1) {
            return filterMatches(SUB_COMMANDS, args[0]);
        }
        
        // 根据子命令提供不同的补全
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "simulate" -> {
                return handleSimulateCompletion(args);
            }
            case "breedcount", "bc" -> {
                return handleBreedCountCompletion(args);
            }
            case "reload", "help" -> {
                // 这些命令不需要额外参数
                return completions;
            }
            default -> {
                return completions;
            }
        }
    }
    
    /**
     * 处理 simulate 命令的自动补全
     * 
     * @param args 当前参数列表
     * @return 补全建议
     */
    private List<String> handleSimulateCompletion(@NotNull String[] args) {
        // 第二个参数：动物类型
        if (args.length == 2) {
            return getAnimalTypes(args[1]);
        }
        
        // 第三个及之后的参数：变量名=值
        if (args.length >= 3) {
            String current = args[args.length - 1];
            
            // 如果已经包含 =，提供数值示例
            if (current.contains("=")) {
                String[] parts = current.split("=", 2);
                String varName = parts[0];
                
                // 根据变量名提供合适的示例值
                return getVariableValueSuggestions(varName, current);
            }
            
            // 否则提供变量名提示
            List<String> suggestions = new ArrayList<>();
            for (String var : SIMULATE_VARIABLES) {
                if (var.toLowerCase().startsWith(current.toLowerCase())) {
                    suggestions.add(var + "=");
                }
            }
            return suggestions;
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 处理 breedcount 命令的自动补全
     * 
     * @param args 当前参数列表
     * @return 补全建议
     */
    private List<String> handleBreedCountCompletion(@NotNull String[] args) {
        // 第二个参数：动作（set/add/reset）
        if (args.length == 2) {
            return filterMatches(BREED_ACTIONS, args[1]);
        }
        
        // 第三个参数：数值（仅 set 和 add 需要）
        if (args.length == 3) {
            String action = args[1].toLowerCase();
            if (action.equals("set") || action.equals("add")) {
                return getNumberSuggestions(args[2]);
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 获取动物类型列表
     * 
     * @param prefix 前缀过滤
     * @return 匹配的动物类型
     */
    private List<String> getAnimalTypes(@NotNull String prefix) {
        return animalConfigService.getAllAnimals()
            .stream()
            .map(config -> config.getType())
            .filter(type -> type.toLowerCase().startsWith(prefix.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * 根据变量名提供数值建议
     * 
     * @param varName 变量名
     * @param current 当前输入
     * @return 数值建议
     */
    private List<String> getVariableValueSuggestions(@NotNull String varName, @NotNull String current) {
        List<String> suggestions = new ArrayList<>();
        String prefix = varName + "=";
        
        switch (varName.toLowerCase()) {
            case "basechance" -> {
                // 概率值：0.0 - 1.0
                suggestions.add(prefix + "0.1");
                suggestions.add(prefix + "0.15");
                suggestions.add(prefix + "0.2");
                suggestions.add(prefix + "0.5");
            }
            case "speciesfactor" -> {
                // 物种因子：通常 0.5 - 1.5
                suggestions.add(prefix + "0.8");
                suggestions.add(prefix + "0.9");
                suggestions.add(prefix + "1.0");
                suggestions.add(prefix + "1.2");
            }
            case "count", "limit", "players" -> {
                // 整数值
                suggestions.add(prefix + "5");
                suggestions.add(prefix + "10");
                suggestions.add(prefix + "15");
                suggestions.add(prefix + "20");
            }
            case "weatherfactor", "biomefactor" -> {
                // 环境因子：通常 0.5 - 2.0
                suggestions.add(prefix + "1.0");
                suggestions.add(prefix + "1.2");
                suggestions.add(prefix + "1.5");
                suggestions.add(prefix + "2.0");
            }
        }
        
        return suggestions;
    }
    
    /**
     * 获取数值建议
     * 
     * @param prefix 前缀
     * @return 数值建议
     */
    private List<String> getNumberSuggestions(@NotNull String prefix) {
        List<String> suggestions = Arrays.asList("0", "1", "2", "3", "5", "10");
        
        // 如果已经输入了数字，返回空列表（让玩家自由输入）
        if (prefix.matches("\\d+")) {
            return new ArrayList<>();
        }
        
        return filterMatches(suggestions, prefix);
    }
    
    /**
     * 过滤匹配的选项
     * 
     * @param options 所有选项
     * @param prefix 前缀
     * @return 匹配的选项
     */
    private List<String> filterMatches(@NotNull List<String> options, @NotNull String prefix) {
        return options.stream()
            .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
}
