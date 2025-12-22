package com.cuzz.rookiePlague.service;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 染疫公式计算服务
 * 
 * <p>使用 exp4j 库解析和计算染疫概率公式</p>
 * <p>支持的变量：</p>
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
 * @author cuzz
 * @since 1.0
 */
public class PlagueFormulaService {
    
    private final LoggerService loggerService;
    private String formulaString;
    private Expression compiledExpression;
    
    /**
     * 构造函数 - 依赖注入
     * 
     * @param loggerService 日志服务
     */
    public PlagueFormulaService(@NotNull LoggerService loggerService) {
        this.loggerService = loggerService;
    }
    
    /**
     * 设置并编译公式
     * 
     * @param formula 公式字符串
     * @return 是否编译成功
     */
    public boolean setFormula(@NotNull String formula) {
        try {
            this.formulaString = formula;
            
            // 预编译公式以提高性能
            this.compiledExpression = new ExpressionBuilder(formula)
                .variables("baseChance", "speciesFactor", "count", "limit", 
                          "weatherFactor", "biomeFactor", "players")
                .build();
            
            loggerService.debug("FORMULA", "成功编译染疫公式: %s", formula);
            return true;
        } catch (Exception e) {
            loggerService.error("编译染疫公式失败: " + formula, e);
            return false;
        }
    }
    
    /**
     * 计算染疫概率
     * 
     * @param variables 变量映射
     * @return 计算结果（0.0 - 1.0 之间）
     */
    public double calculate(@NotNull Map<String, Double> variables) {
        if (compiledExpression == null) {
            loggerService.warning("染疫公式未初始化，返回默认值 0.0");
            return 0.0;
        }
        
        try {
            // 设置变量值
            compiledExpression.setVariables(variables);
            
            // 计算结果
            double result = compiledExpression.evaluate();
            
            // 限制结果在 0-1 之间
            result = Math.max(0.0, Math.min(1.0, result));
            
            loggerService.debug("FORMULA", "计算结果: %.4f, 变量: %s", result, variables);
            
            return result;
        } catch (Exception e) {
            loggerService.error("计算染疫概率失败，变量: " + variables, e);
            return 0.0;
        }
    }
    
    /**
     * 使用构建器模式创建变量映射
     * 
     * @return 变量构建器
     */
    public VariableBuilder createVariables() {
        return new VariableBuilder();
    }
    
    /**
     * 获取当前公式字符串
     * 
     * @return 公式字符串
     */
    public String getFormulaString() {
        return formulaString;
    }
    
    /**
     * 变量构建器
     * 
     * <p>使用链式调用构建变量映射</p>
     */
    public static class VariableBuilder {
        private final Map<String, Double> variables = new HashMap<>();
        
        public VariableBuilder baseChance(double value) {
            variables.put("baseChance", value);
            return this;
        }
        
        public VariableBuilder speciesFactor(double value) {
            variables.put("speciesFactor", value);
            return this;
        }
        
        public VariableBuilder count(double value) {
            variables.put("count", value);
            return this;
        }
        
        public VariableBuilder limit(double value) {
            variables.put("limit", value);
            return this;
        }
        
        public VariableBuilder weatherFactor(double value) {
            variables.put("weatherFactor", value);
            return this;
        }
        
        public VariableBuilder biomeFactor(double value) {
            variables.put("biomeFactor", value);
            return this;
        }
        
        public VariableBuilder players(double value) {
            variables.put("players", value);
            return this;
        }
        
        /**
         * 添加自定义变量
         * 
         * @param name 变量名
         * @param value 变量值
         * @return 构建器本身
         */
        public VariableBuilder custom(String name, double value) {
            variables.put(name, value);
            return this;
        }
        
        /**
         * 构建变量映射
         * 
         * @return 变量映射
         */
        public Map<String, Double> build() {
            return new HashMap<>(variables);
        }
    }
}
