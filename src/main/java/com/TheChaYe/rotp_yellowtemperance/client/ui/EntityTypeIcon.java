package com.TheChaYe.rotp_yellowtemperance.client.ui;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.client.ui.BlitFloat;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体类型图标管理器 / Entity Type Icon Manager
 * 处理实体类型的图标渲染，支持纹理图标和文字首字母显示
 */
public class EntityTypeIcon {
    /** 未知实体类型的默认图标 / Default icon for unknown entity types */
    public static final ResourceLocation UNKNOWN = new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/entity_icon/unknown.png");
    /** 实体类型图标缓存 / Entity type icons cache */
    private static final Map<EntityType<?>, ResourceLocation> ICONS_CACHE = new HashMap<>();

    /**
     * 构造函数 / Constructor
     * 实体类型图标管理器构造函数
     */
    public EntityTypeIcon() {
    }

    /**
     * 渲染实体类型图标 / Render entity type icon
     * 在指定位置渲染实体类型的图标
     */
    public static void renderIcon(EntityType<?> entityType, MatrixStack matrixStack, float x, float y) {
        renderIcon(entityType, matrixStack, x, y, true);
    }

    /**
     * 渲染实体类型图标（完整版本） / Render entity type icon (full version)
     * 在指定位置渲染实体类型的图标，可控制是否显示缺失图标的文字
     */
    public static void renderIcon(EntityType<?> entityType, MatrixStack matrixStack, float x, float y, boolean missingIconLetters) {
        ResourceLocation icon = getIcon(entityType);
        if (icon != UNKNOWN) {
            Minecraft.getInstance().getTextureManager().bind(icon);
            BlitFloat.blitFloat(matrixStack, x, y, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F, 16.0F);
        } else if (missingIconLetters) {
            String name = entityType.getDescription().getString();
            if (!name.isEmpty()) {
                FontRenderer font = Minecraft.getInstance().font;
                ITextComponent firstLetter = StringTextComponent.EMPTY;
                int widthNext = 0;

                for (int i = 1; i <= name.length() && widthNext < 12; ++i) {
                    firstLetter = new StringTextComponent(name.substring(0, i));
                    widthNext = font.width(firstLetter);
                }

                RenderSystem.disableDepthTest();
                float var10003 = x + (float) ((16 - widthNext) / 2);
                font.getClass();
                font.draw(matrixStack, firstLetter, var10003, y + (float) ((16 - 9 + 1) / 2), 16777215);
                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
            }
        }
    }

    /**
     * 获取实体类型的图标路径 / Get entity type icon path
     * 从缓存中获取或创建实体类型的图标资源路径
     */
    public static ResourceLocation getIcon(EntityType<?> entityType) {
        return ICONS_CACHE.computeIfAbsent(entityType, EntityTypeIcon::createIconPath);
    }

    /**
     * 创建图标路径 / Create icon path
     * 根据实体纹理路径生成对应的图标路径
     */
    private static ResourceLocation createIconPath(EntityType<?> entityType) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation entityTex = getEntityTexture(entityType);
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

                entityTex = new ResourceLocation(entityTex.getNamespace(), path);
                if (mc.getResourceManager().hasResource(entityTex)) {
                    return entityTex;
                }
            }

            return UNKNOWN;
        }
    }

    /**
     * 获取实体纹理 / Get entity texture
     * 通过创建实体实例来获取其纹理路径
     */
    @Nullable
    private static <T extends Entity> ResourceLocation getEntityTexture(EntityType<T> entityType) {
        Minecraft mc = Minecraft.getInstance();

        // 检查世界是否可用 / Check if world is available
        World world = mc.level;
        if (world == null) {
            return null;
        }

        T entity;
        try {
            // 尝试创建实体实例 / Try to create entity instance
            entity = entityType.create(world);
            if (entity == null) {
                return null;
            }
        } catch (Exception | ExceptionInInitializerError | OutOfMemoryError e) {
            // 如果创建实体失败，则记录警告并返回null / Log warning and return null if entity creation fails
            JojoMod.getLogger().warn("Failed to create entity for icon rendering: " + entityType.getRegistryName(), e);
            return null;
        } catch (Throwable t) {
            // 捕获更严重的错误 / Catch more serious errors
            JojoMod.getLogger().error("Critical error when creating entity for icon rendering: " + entityType.getRegistryName(), t);
            return null;
        }

        // 通过实体实例获取渲染器 / Get renderer through entity instance
        try {
            EntityRenderer<? super T> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
            if (renderer == null) {
                // 清理创建的实体 / Clean up created entity
                try {
                    entity.remove();
                } catch (Exception ignored) {
                }
                return null;
            }

            ResourceLocation textureLocation = renderer.getTextureLocation(entity);
            // 清理创建的实体以减少内存占用 / Clean up created entity to reduce memory usage
            try {
                entity.remove();
            } catch (Exception ignored) {
            }
            return textureLocation;
        } catch (Exception | ExceptionInInitializerError e) {
            JojoMod.getLogger().error("Error getting texture for entity type: " + entityType.getRegistryName(), e);
            try {
                entity.remove();
            } catch (Exception ignored) {
            }
            return null;
        } catch (Throwable t) {
            JojoMod.getLogger().error("Critical error when getting texture for entity type: " + entityType.getRegistryName(), t);
            try {
                entity.remove();
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    /**
     * 资源重载时清理缓存 / Clear cache on resource reload
     * 当游戏资源重载时清空图标缓存
     */
    public static void onResourceReload() {
        ICONS_CACHE.clear();
    }
}