package com.cuzz.rookiePlague.dao;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Animals;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 动物数据访问对象
 * 
 * <p>DAO层：负责 PersistentDataContainer 的底层数据读写</p>
 * <p>职责：</p>
 * <ul>
 *   <li>提供统一的 PDC 数据访问接口</li>
 *   <li>管理 NamespacedKey 定义</li>
 *   <li>封装底层数据存储细节</li>
 *   <li>不包含业务逻辑，只做数据 CRUD</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class AnimalDataDao {
    
    // PDC 键定义
    private final NamespacedKey breedCountKey;
    private final NamespacedKey infectedKey;
    private final NamespacedKey infectedTimeKey;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public AnimalDataDao(@NotNull Plugin plugin) {
        // 初始化 NamespacedKey
        this.breedCountKey = new NamespacedKey(plugin, "breed_count");
        this.infectedKey = new NamespacedKey(plugin, "infected");
        this.infectedTimeKey = new NamespacedKey(plugin, "infected_time");
    }
    
    // ==================== 繁殖次数数据访问 ====================
    
    /**
     * 读取动物的繁殖次数
     * 
     * @param animal 动物实体
     * @return 繁殖次数，未设置返回 null
     */
    @Nullable
    public Integer readBreedCount(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        return pdc.get(breedCountKey, PersistentDataType.INTEGER);
    }
    
    /**
     * 写入动物的繁殖次数
     * 
     * @param animal 动物实体
     * @param count 繁殖次数
     */
    public void writeBreedCount(@NotNull Animals animal, int count) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        pdc.set(breedCountKey, PersistentDataType.INTEGER, count);
    }
    
    /**
     * 删除动物的繁殖次数数据
     * 
     * @param animal 动物实体
     */
    public void deleteBreedCount(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        pdc.remove(breedCountKey);
    }
    
    /**
     * 检查是否存在繁殖次数数据
     * 
     * @param animal 动物实体
     * @return 是否存在
     */
    public boolean hasBreedCount(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        return pdc.has(breedCountKey, PersistentDataType.INTEGER);
    }
    
    // ==================== 染疫状态数据访问 ====================
    
    /**
     * 读取动物的染疫状态
     * 
     * @param animal 动物实体
     * @return 染疫状态（1=已染疫，0=未染疫），未设置返回 null
     */
    @Nullable
    public Byte readInfectedStatus(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        return pdc.get(infectedKey, PersistentDataType.BYTE);
    }
    
    /**
     * 写入动物的染疫状态
     * 
     * @param animal 动物实体
     * @param infected 是否染疫
     */
    public void writeInfectedStatus(@NotNull Animals animal, boolean infected) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        pdc.set(infectedKey, PersistentDataType.BYTE, (byte) (infected ? 1 : 0));
    }
    
    /**
     * 删除动物的染疫状态数据
     * 
     * @param animal 动物实体
     */
    public void deleteInfectedStatus(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        pdc.remove(infectedKey);
    }
    
    /**
     * 检查是否存在染疫状态数据
     * 
     * @param animal 动物实体
     * @return 是否存在
     */
    public boolean hasInfectedStatus(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        return pdc.has(infectedKey, PersistentDataType.BYTE);
    }
    
    // ==================== 染疫时间数据访问 ====================
    
    /**
     * 读取动物的染疫时间
     * 
     * @param animal 动物实体
     * @return 染疫时间戳（毫秒），未设置返回 null
     */
    @Nullable
    public Long readInfectedTime(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        return pdc.get(infectedTimeKey, PersistentDataType.LONG);
    }
    
    /**
     * 写入动物的染疫时间
     * 
     * @param animal 动物实体
     * @param timestamp 时间戳（毫秒）
     */
    public void writeInfectedTime(@NotNull Animals animal, long timestamp) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        pdc.set(infectedTimeKey, PersistentDataType.LONG, timestamp);
    }
    
    /**
     * 删除动物的染疫时间数据
     * 
     * @param animal 动物实体
     */
    public void deleteInfectedTime(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        pdc.remove(infectedTimeKey);
    }
    
    /**
     * 检查是否存在染疫时间数据
     * 
     * @param animal 动物实体
     * @return 是否存在
     */
    public boolean hasInfectedTime(@NotNull Animals animal) {
        PersistentDataContainer pdc = animal.getPersistentDataContainer();
        return pdc.has(infectedTimeKey, PersistentDataType.LONG);
    }
    
    // ==================== 批量操作 ====================
    
    /**
     * 清除动物的所有数据
     * 
     * @param animal 动物实体
     */
    public void clearAll(@NotNull Animals animal) {
        deleteBreedCount(animal);
        deleteInfectedStatus(animal);
        deleteInfectedTime(animal);
    }
    
    // ==================== 键访问器 ====================
    
    /**
     * 获取繁殖次数键
     * 
     * @return NamespacedKey
     */
    @NotNull
    public NamespacedKey getBreedCountKey() {
        return breedCountKey;
    }
    
    /**
     * 获取染疫状态键
     * 
     * @return NamespacedKey
     */
    @NotNull
    public NamespacedKey getInfectedKey() {
        return infectedKey;
    }
    
    /**
     * 获取染疫时间键
     * 
     * @return NamespacedKey
     */
    @NotNull
    public NamespacedKey getInfectedTimeKey() {
        return infectedTimeKey;
    }
}
