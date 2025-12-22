package com.cuzz.rookiePlague.wrapper;

import com.cuzz.rookiePlague.event.BreedCountModifyRequestEvent;
import com.cuzz.rookiePlague.event.BreedCountModifyRequestEvent.ModifyAction;
import com.cuzz.rookiePlague.event.ConfigReloadRequestEvent;
import com.cuzz.rookiePlague.event.PlagueSimulateRequestEvent;
import com.cuzz.rookiePlague.service.LoggerService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 命令包装器
 * 
 * <p>Wrapper层：快速过滤命令并转发为自定义事件</p>
 * <p>职责：</p>
 * <ul>
 *   <li>快速参数验证</li>
 *   <li>权限检查</li>
 *   <li>将命令请求包装为事件</li>
 *   <li>不处理具体业务逻辑</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class CommandWrapper implements CommandExecutor {
    
    private final LoggerService loggerService;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param loggerService 日志服务
     */
    public CommandWrapper(@NotNull LoggerService loggerService) {
        this.loggerService = loggerService;
    }
    
    /**
     * 命令处理入口
     * 
     * <p>快速过滤并转发为ConfigReloadRequestEvent事件</p>
     * 
     * @param sender 命令发送者
     * @param command 命令对象
     * @param label 命令标签
     * @param args 命令参数
     * @return 是否成功处理
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, 
                            @NotNull Command command, 
                            @NotNull String label, 
                            @NotNull String[] args) {
        // 快速参数验证
        if (args.length == 0) {
            sender.sendMessage("§c用法: /" + label + " <reload|simulate|breedcount|help>");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload" -> handleReloadCommand(sender);
            case "simulate" -> handleSimulateCommand(sender, args);
            case "breedcount", "bc" -> handleBreedCountCommand(sender, args);
            case "help" -> handleHelpCommand(sender);
            default -> {
                sender.sendMessage("§c未知的子命令: " + subCommand);
                sender.sendMessage("§7使用 /" + label + " help 查看帮助");
            }
        }
        
        return true;
    }
    
    /**
     * 处理reload命令
     * 
     * <p>快速权限检查后触发ConfigReloadRequestEvent事件</p>
     * 
     * @param sender 命令发送者
     */
    private void handleReloadCommand(@NotNull CommandSender sender) {
        // 快速权限检查
        if (!sender.hasPermission("rookieplague.admin.reload")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            loggerService.debug("WRAPPER", "%s 尝试执行reload命令但权限不足", sender.getName());
            return;
        }
        
        loggerService.debug("WRAPPER", "收到reload命令，来源: %s", sender.getName());
        
        // 确定事件来源
        ConfigReloadRequestEvent.ReloadSource source = 
            sender instanceof ConsoleCommandSender 
                ? ConfigReloadRequestEvent.ReloadSource.CONSOLE 
                : ConfigReloadRequestEvent.ReloadSource.COMMAND;
        
        // 包装为自定义事件并触发
        ConfigReloadRequestEvent event = new ConfigReloadRequestEvent(sender, source);
        Bukkit.getPluginManager().callEvent(event);
        
        loggerService.debug("WRAPPER", "已触发ConfigReloadRequestEvent事件");
    }
    
    /**
     * 处理simulate命令
     * 
     * <p>快速权限检查后触发PlagueSimulateRequestEvent事件</p>
     * <p>命令格式：/rp simulate <动物类型> [baseChance=0.1] [speciesFactor=0.9] ...</p>
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleSimulateCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // 快速权限检查
        if (!sender.hasPermission("rookieplague.admin.simulate")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            loggerService.debug("WRAPPER", "%s 尝试执行simulate命令但权限不足", sender.getName());
            return;
        }
        
        // 检查参数
        if (args.length < 2) {
            sender.sendMessage("§c用法: /rp simulate <动物类型> [baseChance=0.1] [count=10] ...");
            sender.sendMessage("§7示例: /rp simulate COW baseChance=0.15 count=8");
            return;
        }
        
        String animalType = args[1].toUpperCase();
        loggerService.debug("WRAPPER", "收到simulate命令，来源: %s, 动物: %s", sender.getName(), animalType);
        
        // 解析自定义变量（如果提供）
        Map<String, Double> customVariables = null;
        if (args.length > 2) {
            customVariables = parseVariables(args, sender);
            if (customVariables == null) {
                // 解析失败，已在parseVariables中发送错误消息
                return;
            }
        }
        
        // 包装为自定义事件并触发
        PlagueSimulateRequestEvent event = new PlagueSimulateRequestEvent(
            sender, 
            animalType, 
            customVariables
        );
        Bukkit.getPluginManager().callEvent(event);
        
        loggerService.debug("WRAPPER", "已触发PlagueSimulateRequestEvent事件");
    }
    
    /**
     * 处理breedcount命令
     * 
     * <p>快速权限检查后触发BreedCountModifyRequestEvent事件</p>
     * <p>命令格式：/rp breedcount <set|add|reset> [值]</p>
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleBreedCountCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        // 快速权限检查
        if (!sender.hasPermission("rookieplague.admin.breedcount")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            loggerService.debug("WRAPPER", "%s 尝试执行breedcount命令但权限不足", sender.getName());
            return;
        }
        
        // 必须是玩家
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return;
        }
        
        // 检查参数
        if (args.length < 2) {
            sender.sendMessage("§c用法: /rp breedcount <set|add|reset> [值]");
            sender.sendMessage("§7示例: /rp bc set 5  §8- 设置为5次");
            sender.sendMessage("§7示例: /rp bc add 2  §8- 增加2次");
            sender.sendMessage("§7示例: /rp bc reset  §8- 重置为0次");
            return;
        }
        
        String actionStr = args[1].toLowerCase();
        loggerService.debug("WRAPPER", "收到breedcount命令，来源: %s, 动作: %s", sender.getName(), actionStr);
        
        // 解析动作
        ModifyAction action;
        int value = 0;
        
        switch (actionStr) {
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /rp breedcount set <值>");
                    return;
                }
                action = ModifyAction.SET;
                value = parseValue(args[2], sender);
                if (value < 0) return;  // 解析失败
            }
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /rp breedcount add <值>");
                    return;
                }
                action = ModifyAction.ADD;
                value = parseValue(args[2], sender);
                if (value == Integer.MIN_VALUE) return;  // 解析失败
            }
            case "reset" -> {
                action = ModifyAction.RESET;
                value = 0;
            }
            default -> {
                sender.sendMessage("§c未知的动作: " + actionStr);
                sender.sendMessage("§7支持的动作: set, add, reset");
                return;
            }
        }
        
        // 获取玩家面朝的动物
        Animals targetAnimal = getTargetAnimal(player);
        
        if (targetAnimal == null) {
            loggerService.debug("WRAPPER", "未找到目标动物");
        }
        
        // 包装为自定义事件并触发
        BreedCountModifyRequestEvent event = new BreedCountModifyRequestEvent(
            sender,
            player,
            targetAnimal,
            action,
            value
        );
        Bukkit.getPluginManager().callEvent(event);
        
        loggerService.debug("WRAPPER", "已触发BreedCountModifyRequestEvent事件");
    }
    
    /**
     * 解析数值参数
     * 
     * @param valueStr 数值字符串
     * @param sender 命令发送者
     * @return 解析的数值，失败返回负数（set）或 Integer.MIN_VALUE（add）
     */
    private int parseValue(@NotNull String valueStr, @NotNull CommandSender sender) {
        try {
            int value = Integer.parseInt(valueStr);
            if (value < 0) {
                sender.sendMessage("§c数值不能为负数: " + value);
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的数值: " + valueStr);
            sender.sendMessage("§7请输入有效的整数（如 5 或 10）");
            return Integer.MIN_VALUE;
        }
    }
    
    /**
     * 获取玩家面朝的动物
     * 
     * @param player 玩家
     * @return 目标动物，未找到返回null
     */
    @Nullable
    private Animals getTargetAnimal(@NotNull Player player) {
        // 使用射线追踪获取玩家面朝的实体
        Location eyeLocation = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTraceEntities(
            eyeLocation,
            eyeLocation.getDirection(),
            5.0,  // 最大距离5格
            entity -> entity instanceof Animals && !entity.equals(player)
        );
        
        if (result != null) {
            Entity hitEntity = result.getHitEntity();
            if (hitEntity instanceof Animals animal) {
                loggerService.debug("WRAPPER", "找到目标动物: %s (%s)", 
                    animal.getType(), animal.getUniqueId());
                return animal;
            }
        }
        
        loggerService.debug("WRAPPER", "未找到目标动物（5格范围内）");
        return null;
    }
    
    /**
     * 解析命令参数为变量映射
     * 
     * <p>支持格式：key=value</p>
     * <p>支持的变量：baseChance, speciesFactor, count, limit, weatherFactor, biomeFactor, players</p>
     * 
     * @param args 命令参数
     * @param sender 命令发送者（用于发送错误消息）
     * @return 变量映射，解析失败返回null
     */
    private Map<String, Double> parseVariables(@NotNull String[] args, @NotNull CommandSender sender) {
        Map<String, Double> variables = new HashMap<>();
        
        // 支持的变量名
        String[] validVars = {"baseChance", "speciesFactor", "count", "limit", 
                             "weatherFactor", "biomeFactor", "players"};
        
        // 从第3个参数开始解析（args[0]=simulate, args[1]=动物类型）
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            
            // 检查是否为key=value格式
            if (!arg.contains("=")) {
                sender.sendMessage("§c错误的参数格式: " + arg);
                sender.sendMessage("§7正确格式: key=value （例如 baseChance=0.15）");
                return null;
            }
            
            String[] parts = arg.split("=", 2);
            String key = parts[0];
            String valueStr = parts[1];
            
            // 验证变量名
            boolean isValid = false;
            for (String validVar : validVars) {
                if (validVar.equalsIgnoreCase(key)) {
                    key = validVar;  // 使用标准大小写
                    isValid = true;
                    break;
                }
            }
            
            if (!isValid) {
                sender.sendMessage("§c未知的变量: " + key);
                sender.sendMessage("§7支持的变量: baseChance, speciesFactor, count, limit, weatherFactor, biomeFactor, players");
                return null;
            }
            
            // 解析数值
            try {
                double value = Double.parseDouble(valueStr);
                variables.put(key, value);
                loggerService.debug("WRAPPER", "解析变量: %s = %.4f", key, value);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c无效的数值: " + valueStr);
                sender.sendMessage("§7请输入有效的数字（如 0.15 或 10）");
                return null;
            }
        }
        
        return variables;
    }
    
    /**
     * 处理help命令
     * 
     * @param sender 命令发送者
     */
    private void handleHelpCommand(@NotNull CommandSender sender) {
        sender.sendMessage("§6=== RookiePlague 命令帮助 ===");
        sender.sendMessage("§e/rp reload §7- 重载插件配置（需要权限）");
        sender.sendMessage("§e/rp simulate <动物类型> [key=value...] §7- 模拟计算染疫公式");
        sender.sendMessage("§e/rp breedcount <set|add|reset> [值] §7- 修改动物繁殖次数");
        sender.sendMessage("§e/rp help §7- 显示此帮助信息");
        sender.sendMessage("");
        sender.sendMessage("§7simulate 命令示例:");
        sender.sendMessage("§7  /rp simulate COW");
        sender.sendMessage("§7  /rp simulate SHEEP baseChance=0.15 count=8");
        sender.sendMessage("§7  /rp simulate PIG weatherFactor=1.5 players=20");
        sender.sendMessage("");
        sender.sendMessage("§7breedcount 命令示例:");
        sender.sendMessage("§7  /rp bc set 5 §8- 设置为5次");
        sender.sendMessage("§7  /rp bc add 2 §8- 增加2次");
        sender.sendMessage("§7  /rp bc reset §8- 重置为0次");
        sender.sendMessage("");
        sender.sendMessage("§7权限节点:");
        sender.sendMessage("§7  - rookieplague.admin.reload §8- 重载配置权限");
        sender.sendMessage("§7  - rookieplague.admin.simulate §8- 模拟计算权限");
        sender.sendMessage("§7  - rookieplague.admin.breedcount §8- 修改繁歖次数权限");
    }
}
