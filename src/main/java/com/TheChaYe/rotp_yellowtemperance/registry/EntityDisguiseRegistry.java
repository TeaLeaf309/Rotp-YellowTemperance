package com.TheChaYe.rotp_yellowtemperance.registry;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 实体伪装注册器 / Entity Disguise Registry
 * 管理可用于伪装的实体列表和尺寸验证
 */
public class EntityDisguiseRegistry {
    /**
     * 可用于伪装的实体列表（线程安全） / Entities available for disguise (thread-safe)
     */
    private static final CopyOnWriteArrayList<EntityType<?>> ENTITIES_FOR_DISGUISE = new CopyOnWriteArrayList<>();
    
    /**
     * 可用于替身伪装的实体列表（只包含替身实体） / Entities available for stand disguise (stand entities only)
     */
    private static final CopyOnWriteArrayList<EntityType<?>> ENTITIES_FOR_STAND_DISGUISE = new CopyOnWriteArrayList<>();
    
    /**
     * 实体尺寸缓存 / Entity size cache
     */
    private static final Map<EntityType<?>, EntitySize> ENTITY_SIZE_CACHE = new HashMap<>();

    /**
     * 初始化伪装实体列表 / Initialize disguise entity list
     * 遍历所有注册的实体并筛选出可用于伪装的实体
     */
    public static void init() {
        if (ENTITIES_FOR_DISGUISE.isEmpty()) {
            java.util.concurrent.atomic.AtomicInteger addedCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger skippedCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger standCount = new java.util.concurrent.atomic.AtomicInteger(0);

            ForgeRegistries.ENTITIES.getEntries().forEach(entry -> {
                EntityType<?> entityType = entry.getValue();
                ResourceLocation registryName = entityType.getRegistryName();

                // 排除末影龙 / Exclude ender dragon
                if (entityType.equals(EntityType.ENDER_DRAGON)) {
                    return;
                }

                // 检查自定义黑名单 / Check custom blacklist
                if (isBlacklisted(entityType, registryName)) {
                    return;
                }
                
                // 检查是否为替身实体 / Check if stand entity
                boolean isStand = isStandEntity(entityType);
                
                // 允许生物、怪物和部分 MISC 实体（但不包括替身实体） / Allow creatures, monsters and some MISC entities (but not stand entities)
                EntityClassification classification = entityType.getCategory();
                boolean isValidCategory = classification == EntityClassification.CREATURE ||
                        classification == EntityClassification.MONSTER ||
                        (classification == EntityClassification.MISC && isLivingEntityLike(entityType));

                // 检查实体类别和尺寸 / Check entity category and size
                if (isValidCategory) {
                    try {
                        if (isEntitySizeValid(entityType)) {
                            EntitySize size = ENTITY_SIZE_CACHE.get(entityType);
                            RotPYellowTemperanceAddon.getLogger().debug("Added valid entity: {} (H: {:.2f}, W: {:.2f})",
                                    entityType.toString(),
                                    size.height,
                                    size.width);

                            ENTITIES_FOR_DISGUISE.add(entityType);
                            addedCount.incrementAndGet();
                        } else {
                            skippedCount.incrementAndGet();
                        }
                    } catch (RuntimeException e) {
                        skippedCount.incrementAndGet();
                    }
                } else if (isStand && !isStandBlacklisted(entityType, registryName)) {
                    // 替身实体只添加到替身伪装列表 / Stand entities only added to stand disguise list
                    // 替身实体不检查尺寸限制，所有替身都可以加入伪装列表
                    try {
                        ENTITIES_FOR_STAND_DISGUISE.add(entityType);
                        standCount.incrementAndGet();
                    } catch (Exception ignored) {
                        RotPYellowTemperanceAddon.getLogger().error("Error adding stand entity: {}", registryName, ignored);
                    }
                }
            });

            RotPYellowTemperanceAddon.getLogger().info("Entity disguise initialization complete: {} added, {} skipped, {} stands",
                    addedCount.get(), skippedCount.get(), standCount.get());
        }
    }

    /**
     * 获取可用于伪装的实体列表 / Get entities for disguise
     *
     * @return 实体类型列表 / List of entity types
     */
    public static CopyOnWriteArrayList<EntityType<?>> getEntitiesForDisguise() {
        return ENTITIES_FOR_DISGUISE;
    }

    /**
     * 获取可用于替身伪装的实体列表 / Get entities for stand disguise
     *
     * @return 替身实体类型列表 / List of stand entity types
     */
    public static CopyOnWriteArrayList<EntityType<?>> getEntitiesForStandDisguise() {
        return ENTITIES_FOR_STAND_DISGUISE;
    }

    /**
     * 检查实体尺寸是否有效 / Check if entity size is valid
     * 验证实体是否适合用于伪装
     *
     * @param entityType 实体类型 / Entity type
     * @return 尺寸是否有效 / Whether size is valid
     */
    public static boolean isEntitySizeValid(EntityType<?> entityType) {
        // 从缓存获取尺寸 / Get size from cache
        EntitySize size = ENTITY_SIZE_CACHE.computeIfAbsent(entityType, type -> {
            try {
                EntitySize dimensions = type.getDimensions();
                if (dimensions == null) {
                    RotPYellowTemperanceAddon.getLogger().warn("Entity {} returned null dimensions, using default size", type.getRegistryName());
                    return EntitySize.scalable(0.6f, 1.8f); // 默认玩家尺寸 / Default player size
                }
                return dimensions;
            } catch (NullPointerException e) {
                RotPYellowTemperanceAddon.getLogger().error("Entity {} threw NPE when getting dimensions, skipping", type.getRegistryName(), e);
                throw new RuntimeException("Failed to get dimensions for entity: " + type.getRegistryName(), e);
            } catch (Exception e) {
                RotPYellowTemperanceAddon.getLogger().error("Entity {} threw exception when getting dimensions, skipping", type.getRegistryName(), e);
                throw new RuntimeException("Failed to get dimensions for entity: " + type.getRegistryName(), e);
            }
        });

        // 如果获取尺寸失败，返回 false / If size retrieval failed, return false
        if (size == null) {
            return false;
        }

        // 获取配置的尺寸要求 / Get configured size requirements
        final double MIN_HEIGHT = YellowTemperanceConfig.minEntityHeight.get();
        final double MAX_HEIGHT = YellowTemperanceConfig.maxEntityHeight.get();
        final double MIN_WIDTH = YellowTemperanceConfig.minEntityWidth.get();
        final double MAX_WIDTH = YellowTemperanceConfig.maxEntityWidth.get();

        // 检查尺寸是否符合要求 / Check if size meets requirements
        boolean heightValid = size.height >= MIN_HEIGHT && size.height <= MAX_HEIGHT;
        boolean widthValid = size.width >= MIN_WIDTH && size.width <= MAX_WIDTH;

        return heightValid && widthValid;
    }

    /**
     * 检查实体是否类似生物实体 / Check if entity is living entity-like
     * 判断 MISC 分类的实体是否具有生物特征
     *
     * @param entityType 实体类型 / Entity type
     * @return 是否类似生物实体 / Whether entity is living entity-like
     */
    private static boolean isLivingEntityLike(EntityType<?> entityType) {
        // 检查实体是否类似于生物实体（如铁傀儡） / Check if entity is similar to living entities (like iron golem)
        // 创建一个实体实例来检查它是否是LivingEntity的子类 / Create entity instance to check if it's subclass of LivingEntity
        try {
            Entity entity = entityType.create(null); // 传入 null 世界，因为我们只关心类型 / Pass null world since we only care about type
            if (entity instanceof LivingEntity) {
                // 对于 LivingEntity 实例，进一步检查是否具有生物特征 / For LivingEntity instances, further check if they have living characteristics
                // 检查是否是特定的可接受MISC类型实体（如铁傀儡） / Check if it's specific acceptable MISC type entities (like iron golem)
                return entityType == EntityType.IRON_GOLEM ||
                        entityType == EntityType.SNOW_GOLEM ||
                        entityType == EntityType.VILLAGER ||
                        entityType == EntityType.WANDERING_TRADER;
            }
        } catch (Exception e) {
            // 如果创建实体时出错，回退到基于 ID 的检查 / If entity creation fails, fall back to ID-based check
            ResourceLocation registryName = entityType.getRegistryName();
            if (registryName != null) {
                String id = registryName.toString();
                // 检查是否包含典型的生物实体关键词 / Check if contains typical living entity keywords
                return id.contains("golem") ||
                        id.contains("villager") ||
                        id.contains("zombie") ||
                        id.contains("skeleton");
            }
        }
        return false;
    }

    /**
     * 检查是否为替身实体 / Check if entity is stand entity
     * 判断 MISC 分类的实体是否为 JoJo 替身实体
     *
     * @param entityType 实体类型 / Entity type
     * @return 是否为替身实体 / Whether entity is stand entity
     */
    private static boolean isStandEntity(EntityType<?> entityType) {
        // 检查实体是否是 StandEntityType 的实例
        return entityType instanceof StandEntityType<?>;
    }

    /**
     * 检查实体是否在黑名单中 / Check if entity is blacklisted
     * 判断指定实体是否被禁止用于伪装
     *
     * @param entityType   实体类型 / Entity type
     * @param registryName 注册名 / Registry name
     * @return 是否在黑名单中 / Whether in blacklist
     */
    private static boolean isBlacklisted(EntityType<?> entityType, ResourceLocation registryName) {
        if (registryName == null) {
            return false;
        }
        String id = registryName.toString();

        // 从配置文件中获取黑名单列表 / Get blacklist from config file
        List<String> configBlacklist = (List<String>) YellowTemperanceConfig.entityDisguiseBlacklist.get();

        // 检查是否在配置黑名单中 / Check if in config blacklist
        if (configBlacklist != null && configBlacklist.contains(id)) {
            return true;
        }

        // 排除所有弹射物 - 通过名称识别（硬编码后备逻辑）
        // Exclude all projectiles - identified by name (hardcoded fallback)
        return id.contains("fireball") ||
                id.contains("bullet") ||
                id.contains("arrow") ||
                id.contains("projectile");
    }

    /**
     * 检查替身实体是否在黑名单中 / Check if stand entity is blacklisted
     * 判断指定替身实体是否被禁止用于伪装
     *
     * @param entityType   实体类型 / Entity type
     * @param registryName 注册名 / Registry name
     * @return 是否在黑名单中 / Whether in blacklist
     */
    private static boolean isStandBlacklisted(EntityType<?> entityType, ResourceLocation registryName) {
        if (registryName == null) {
            return false;
        }
        String id = registryName.toString();

        // 从配置文件中获取黑名单列表 / Get blacklist from config file
        List<String> configBlacklist = (List<String>) YellowTemperanceConfig.standDisguiseBlacklist.get();

        // 检查是否在配置黑名单中 / Check if in config blacklist
        return configBlacklist != null && configBlacklist.contains(id);
    }

    /**
     * 获取实体尺寸缓存 / Get entity size cache
     *
     * @return 尺寸缓存映射表 / Size cache map
     */
    public static Map<EntityType<?>, EntitySize> getEntitySizeCache() {
        return ENTITY_SIZE_CACHE;
    }
}
