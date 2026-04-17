// 原代码来自：https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// 修改日期：2026-03-19
// Original code from: https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// on 2026-03-19
package com.TheChaYe.rotp_yellowtemperance.client.ui;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import net.minecraft.entity.EntityType;

/**
 * 实体类型图标 / Entity Type Icon
 * 用于获取和渲染实体类型的图标
 */
public class EntityTypeIcon {
    /**
     * 未知图标默认路径 / Default unknown icon path
     */
    public static final net.minecraft.util.ResourceLocation UNKNOWN = new net.minecraft.util.ResourceLocation("textures/entity_icon/unknown.png");
    /**
     * 未知图标路径 / Unknown icon path
     */
    private static final java.util.Map<EntityType<?>, net.minecraft.util.ResourceLocation> ICONS_CACHE = new java.util.HashMap<>();

    /**
     * 构造函数 / Constructor
     */
    public EntityTypeIcon() {
    }

    /**
     * 获取实体图标 / Get entity icon
     *
     * @param entityType 实体类型 / Entity type
     * @return 图标资源路径 / Icon resource path
     */
    public static net.minecraft.util.ResourceLocation getIcon(EntityType<?> entityType) {
        return ICONS_CACHE.computeIfAbsent(entityType, EntityTypeIcon::createIconPath);
    }

    /**
     * 创建图标路径 / Create icon path
     *
     * @param entityType 实体类型 / Entity type
     * @return 图标资源路径 / Icon resource path
     */
    private static net.minecraft.util.ResourceLocation createIconPath(EntityType<?> entityType) {
        // 首先尝试替身头像路径 / Try stand portrait path first
        if (isStandEntity(entityType)) {
            net.minecraft.util.ResourceLocation standPortrait = getStandPortrait(entityType);
            if (standPortrait != null) {
                return standPortrait;
            }
        }

        // 然后尝试普通实体图标路径 / Then try normal entity icon path
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.util.ResourceLocation entityTex = getEntityTexture(entityType);
        if (entityTex == null) {
            return UNKNOWN;
        } else {
            String path = entityTex.getPath();
            if (path.contains("/entity/")) {
                if (path.contains("/model/entity/")) {
                    path = path.replace("/model/entity/", "/entity_icon/");
                } else {
                    path = path.replace("/entity/", "/entity_icon/");
                }

                entityTex = new net.minecraft.util.ResourceLocation(entityTex.getNamespace(), path);
                if (mc.getResourceManager().hasResource(entityTex)) {
                    return entityTex;
                }
            }

            return UNKNOWN;
        }
    }

    /**
     * 获取替身头像 / Get stand portrait
     * 从 assets\模组 id\textures\power\替身 id.png 路径获取替身头像
     *
     * @param entityType 替身实体类型 / Stand entity type
     * @return 替身头像资源路径 / Stand portrait resource path
     */
    /**
     * 获取指定实体类型对应的替身（Stand）头像纹理位置。
     * <p>
     * 该方法会尝试根据实体类型的注册名构造一个纹理路径，
     * 然后检查该纹理文件是否存在，如果存在则返回对应的 {@link net.minecraft.util.ResourceLocation}，
     * 否则返回 {@code null}。
     *
     * @param entityType 实体类型，通常代表一个替身实体
     * @return 替身头像的 ResourceLocation，如果不存在则返回 null
     */
    private static net.minecraft.util.ResourceLocation getStandPortrait(EntityType<?> entityType) {
        // 获取实体类型的注册名（例如 "my_mod:star_platinum"）
        net.minecraft.util.ResourceLocation registryName = entityType.getRegistryName();
        // 如果注册名为空（理论上不应发生，但做防御性检查），直接返回 null
        if (registryName == null) {
            return null;
        }

        // 从注册名中提取路径部分（即 "star_platinum"），作为替身的 ID
        String standId = registryName.getPath();

        // 获取 Minecraft 客户端实例，用于访问资源管理器
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        // 首先尝试使用实体注册的命名空间
        net.minecraft.util.ResourceLocation portraitPath = new net.minecraft.util.ResourceLocation(
                registryName.getNamespace(),
                "textures/power/" + standId + ".png"
        );
        com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.debug("Trying to get stand portrait: {}", portraitPath);
        if (mc.getResourceManager().hasResource(portraitPath)) {
            com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.debug("Found stand portrait: {}", portraitPath);
            return portraitPath;
        }

        // 如果没找到，尝试使用配置的命名空间列表（处理 Extra-Stands 等情况）
        java.util.List<String> configuredNamespaces = (java.util.List<String>) com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig.standPortraitNamespaces.get();
        if (configuredNamespaces != null && !configuredNamespaces.isEmpty()) {
            for (String namespace : configuredNamespaces) {
                portraitPath = new net.minecraft.util.ResourceLocation(namespace.trim(), "textures/power/" + standId + ".png");
                com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.debug("Trying alternate namespace: {}", portraitPath);
                if (mc.getResourceManager().hasResource(portraitPath)) {
                    com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.debug("Found stand portrait: {}", portraitPath);
                    return portraitPath;
                }
            }
        }

        // 如果文件不存在，返回 null，表示没有替身头像
        com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.warn("Stand portrait not found for entity: {}", registryName);
        return null;
    }

    /**
     * 检查是否为替身实体 / Check if entity is stand entity
     *
     * @param entityType 实体类型 / Entity type
     * @return 是否为替身实体 / Whether entity is stand entity
     */
    private static boolean isStandEntity(EntityType<?> entityType) {
        return entityType instanceof com.github.standobyte.jojo.entity.stand.StandEntityType<?>;
    }

    /**
     * 获取实体贴图 / Get entity texture
     *
     * @param entityType 实体类型 / Entity type
     * @return 贴图资源路径 / Texture resource path
     */
    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.entity.Entity> net.minecraft.util.ResourceLocation getEntityTexture(EntityType<T> entityType) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.renderer.entity.EntityRenderer<? super T> renderer =
                (net.minecraft.client.renderer.entity.EntityRenderer<? super T>) mc.getEntityRenderDispatcher().renderers.get(entityType);

        // 如果连渲染器都不存在，直接返回 null
        if (renderer == null) {
            return null;
        }

        T entity = null;
        try {
            entity = entityType.create(mc.level);
            if (entity == null) {
                return null;
            }
            return renderer.getTextureLocation(entity);
        } catch (Exception e) {
            // 某些实体（如 CustomNPCs 的 EntityCustomNpc）在创建时需要额外的初始化，
            // 直接创建会导致 NPE 或其他异常，捕获后返回 null，使用默认未知图标。
            RotPYellowTemperanceAddon.LOGGER.warn("Failed to create or get texture for entity: {}", entityType.getRegistryName(), e);
            return null;
        } finally {
            // 确保创建的实体从世界中移除（如果被添加了的话）
            // 注意：entityType.create 只是创建实例，不会自动添加到世界，无需额外清理。
        }
    }

    /**
     * 清理图标缓存 / Clear icon cache
     */
    public static void onResourceReload() {
        ICONS_CACHE.clear();
    }
}
