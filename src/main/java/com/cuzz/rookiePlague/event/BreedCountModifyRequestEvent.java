package com.cuzz.rookiePlague.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 繁殖次数修改请求事件
 * 
 * <p>自定义事件：表示玩家请求修改动物的繁殖次数</p>
 * <p>事件流程：Wrapper → Event → Controller → Service</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class BreedCountModifyRequestEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final CommandSender sender;
    private final Player player;
    private final Animals targetAnimal;
    private final ModifyAction action;
    private final int value;
    
    /**
     * 构造函数
     * 
     * @param sender 命令发送者
     * @param player 执行命令的玩家
     * @param targetAnimal 目标动物
     * @param action 修改动作
     * @param value 修改值
     */
    public BreedCountModifyRequestEvent(@NotNull CommandSender sender,
                                       @NotNull Player player,
                                       @Nullable Animals targetAnimal,
                                       @NotNull ModifyAction action,
                                       int value) {
        this.sender = sender;
        this.player = player;
        this.targetAnimal = targetAnimal;
        this.action = action;
        this.value = value;
    }
    
    /**
     * 获取命令发送者
     * 
     * @return 命令发送者
     */
    @NotNull
    public CommandSender getSender() {
        return sender;
    }
    
    /**
     * 获取执行命令的玩家
     * 
     * @return 玩家
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * 获取目标动物
     * 
     * @return 目标动物，未找到则返回null
     */
    @Nullable
    public Animals getTargetAnimal() {
        return targetAnimal;
    }
    
    /**
     * 获取修改动作
     * 
     * @return 修改动作
     */
    @NotNull
    public ModifyAction getAction() {
        return action;
    }
    
    /**
     * 获取修改值
     * 
     * @return 修改值
     */
    public int getValue() {
        return value;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    
    /**
     * 修改动作枚举
     */
    public enum ModifyAction {
        /** 设置为指定值 */
        SET,
        /** 增加指定值 */
        ADD,
        /** 重置为0 */
        RESET
    }
}
