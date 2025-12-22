package com.cuzz.rookiePlague.service;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 日志服务
 * 
 * <p>统一管理插件日志输出，解耦各组件与 Plugin 的直接依赖</p>
 * 
 * @author cuzz
 * @since 1.0
 */
public class LoggerService {
    
    private final Logger logger;
    private final String pluginName;
    
    public LoggerService(Plugin plugin) {
        this.logger = plugin.getLogger();
        this.pluginName = plugin.getName();
    }
    
    /**
     * 记录 INFO 级别日志
     * 
     * @param message 日志消息
     */
    public void info(String message) {
        logger.info(message);
    }
    
    /**
     * 记录格式化的 INFO 日志
     * 
     * @param format 格式化字符串
     * @param args 格式化参数
     */
    public void info(String format, Object... args) {
        logger.info(String.format(format, args));
    }
    
    /**
     * 记录 WARNING 级别日志
     * 
     * @param message 日志消息
     */
    public void warning(String message) {
        logger.warning(message);
    }
    
    /**
     * 记录格式化的 WARNING 日志
     * 
     * @param format 格式化字符串
     * @param args 格式化参数
     */
    public void warning(String format, Object... args) {
        logger.warning(String.format(format, args));
    }
    
    /**
     * 记录 SEVERE 级别日志（严重错误）
     * 
     * @param message 日志消息
     */
    public void severe(String message) {
        logger.severe(message);
    }
    
    /**
     * 记录格式化的 SEVERE 日志
     * 
     * @param format 格式化字符串
     * @param args 格式化参数
     */
    public void severe(String format, Object... args) {
        logger.severe(String.format(format, args));
    }
    
    /**
     * 记录异常日志
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
    
    /**
     * 记录调试日志（带标签）
     * 
     * @param tag 标签
     * @param message 日志消息
     */
    public void debug(String tag, String message) {
        logger.info(String.format("[%s] %s", tag, message));
    }
    
    /**
     * 记录格式化的调试日志
     * 
     * @param tag 标签
     * @param format 格式化字符串
     * @param args 格式化参数
     */
    public void debug(String tag, String format, Object... args) {
        logger.info(String.format("[%s] %s", tag, String.format(format, args)));
    }
    
    /**
     * 记录成功消息（绿色前缀）
     * 
     * @param message 日志消息
     */
    public void success(String message) {
        logger.info("§a" + message);
    }
    
    /**
     * 记录格式化的成功消息
     * 
     * @param format 格式化字符串
     * @param args 格式化参数
     */
    public void success(String format, Object... args) {
        logger.info("§a" + String.format(format, args));
    }
    
    /**
     * 获取插件名称
     * 
     * @return 插件名称
     */
    public String getPluginName() {
        return pluginName;
    }
}
