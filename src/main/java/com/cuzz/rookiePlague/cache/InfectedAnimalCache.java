package com.cuzz.rookiePlague.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 染疫动物缓存
 * 
 * <p>线程安全的染疫动物UUID缓存，优化瘟疫伤害扫描性能</p>
 * <p>功能：</p>
 * <ul>
 *   <li>记录所有染疫动物的UUID</li>
 *   <li>提供高效的增删查操作</li>
 *   <li>避免全服扫描，只处理染疫动物</li>
 * </ul>
 * 
 * @author cuzz
 * @since 1.0
 */
public class InfectedAnimalCache {
    
    // 使用 ConcurrentHashMap.newKeySet() 保证线程安全
    private final Set<UUID> infectedAnimals;
    
    public InfectedAnimalCache() {
        this.infectedAnimals = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * 添加染疫动物
     * 
     * @param uuid 动物UUID
     */
    public void addInfected(@NotNull UUID uuid) {
        infectedAnimals.add(uuid);
    }
    
    /**
     * 移除染疫动物
     * 
     * @param uuid 动物UUID
     */
    public void removeInfected(@NotNull UUID uuid) {
        infectedAnimals.remove(uuid);
    }
    
    /**
     * 检查是否在缓存中
     * 
     * @param uuid 动物UUID
     * @return 是否染疫
     */
    public boolean contains(@NotNull UUID uuid) {
        return infectedAnimals.contains(uuid);
    }
    
    /**
     * 获取所有染疫动物UUID（快照）
     * 
     * @return UUID集合
     */
    @NotNull
    public Set<UUID> getAllInfected() {
        return Set.copyOf(infectedAnimals);
    }
    
    /**
     * 获取染疫动物数量
     * 
     * @return 数量
     */
    public int size() {
        return infectedAnimals.size();
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        infectedAnimals.clear();
    }
}
