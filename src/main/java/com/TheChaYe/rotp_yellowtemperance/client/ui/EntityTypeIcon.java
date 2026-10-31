// 原代码来自：https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// 修改日期：2026-03-19
// Original code from: https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// on 2026-03-19
package com.TheChaYe.rotp_yellowtemperance.client.ui;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.github.standobyte.jojo.client.ui.BlitFloat;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.IHasHead;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 实体类型图标 / Entity Type Icon
 * <p>
 * 按钮上的"平面头部贴图"有三种来源，按优先级取第一个可用的：
 * <ol>
 *   <li>整图：替身头像 {@code textures/power/<id>.png}，或 RotP 自带的 {@code textures/entity_icon/**.png}（16×16 平面脸）；</li>
 *   <li>裁剪：取该生物皮肤上"头部各立方体正面"的 UV 区域
 *       （{@code (u+dz, v+dz, dx, dy)}，u/v 为盒子 texOffs，dx/dy/dz 为盒子尺寸），
 *       按模型里的相对位置拼成一张 16×16 的头像 —— 不生成任何新纹理；</li>
 *   <li>文字兜底：把生物名缩写画进 16×16（与 RotP 自己 UI 的做法一致），保证按钮不会空白。</li>
 * </ol>
 * 说明：RotP 的 entity_icon 实测就是"皮肤正面 UV 区域按最近邻放大 2 倍"，
 * 所以裁剪出来的图标与自带图标是同一套观感；村民/灾厄村民/劫掠兽这类
 * 鼻子（和嘴）是独立立方体的生物，会把额外部件按模型位置叠上去。
 */
public class EntityTypeIcon {

    /**
     * 图标缓存 / Icon cache
     */
    private static final Map<EntityType<?>, IconSpec> ICONS_CACHE = new HashMap<>();

    /**
     * 超过这个宽度就不再往 16×16 里塞字 / Max label width in pixels
     */
    private static final int MAX_LABEL_WIDTH = 12;

    /**
     * 头像规格表：key = 实体注册名 / Portrait table keyed by registry name
     */
    private static final Map<ResourceLocation, Portrait> PORTRAITS = new HashMap<>();

    static {
        // 全部按等比缩放（不拉伸）：村民的头是 8×10 的长方形，铺满 16×16 会变成正方形。
        // 村民家族：脸 + 鼻子（VillagerModel / ZombieVillagerModel，鼻子比下巴略低）。
        addPortrait("minecraft:villager", 8, 11, 64, 64,
                new Part(8, 8, 8, 10, 0, 0), new Part(26, 2, 2, 4, 3, 7));
        addPortrait("minecraft:zombie_villager", 8, 11, 64, 64,
                new Part(8, 8, 8, 10, 0, 0), new Part(26, 2, 2, 4, 3, 7));
        addPortrait("minecraft:wandering_trader", 8, 11, 64, 64,
                new Part(8, 8, 8, 10, 0, 0), new Part(26, 2, 2, 4, 3, 7));
        // 灾厄村民（IllagerModel）：鼻子的模型位置比村民低 2px，图标里上移到同一水平，
        // 否则在 16×16 里会变成"脸下面挂一条"。
        addPortrait("minecraft:evoker", 8, 11, 64, 64,
                new Part(8, 8, 8, 10, 0, 0), new Part(26, 2, 2, 4, 3, 7));
        addPortrait("minecraft:vindicator", 8, 11, 64, 64,
                new Part(8, 8, 8, 10, 0, 0), new Part(26, 2, 2, 4, 3, 7));
        addPortrait("minecraft:pillager", 8, 11, 64, 64,
                new Part(8, 8, 8, 10, 0, 0), new Part(26, 2, 2, 4, 3, 7));
        addPortrait("minecraft:illusioner", 8, 11, 64, 64,
                new Part(8, 8, 8, 10, 0, 0), new Part(26, 2, 2, 4, 3, 7));
        // 女巫：VillagerModel，贴图 64×128
        addPortrait("minecraft:witch", 8, 11, 64, 128,
                new Part(8, 8, 8, 10, 0, 0), new Part(26, 2, 2, 4, 3, 7));
        // 劫掠兽：头 + 鼻子 + 嘴是三个独立盒子
        addPortrait("minecraft:ravager", 16, 23, 128, 128,
                new Part(16, 16, 16, 20, 0, 0),
                new Part(4, 4, 4, 8, 6, 14),
                new Part(16, 52, 16, 3, 0, 20));
        // 羊驼 / 商队羊驼：LlamaModel 头(8×18×6, texOffs 0,14)，贴图 128×64
        addPortrait("minecraft:llama", 8, 18, 128, 64, new Part(6, 20, 8, 18, 0, 0));
        addPortrait("minecraft:trader_llama", 8, 18, 128, 64, new Part(6, 20, 8, 18, 0, 0));
        // 雪傀儡 / 恼鬼 / 巨人（后两个当前尺寸过滤下一般不会出现，留作放宽配置后使用）
        addPortrait("minecraft:snow_golem", 8, 8, 64, 64, new Part(8, 8, 8, 8, 0, 0));
        addPortrait("minecraft:vex", 8, 8, 64, 64, new Part(8, 8, 8, 8, 0, 0));
        addPortrait("minecraft:giant", 8, 8, 64, 64, new Part(8, 8, 8, 8, 0, 0));
    }

    /**
     * 构造函数 / Constructor
     */
    public EntityTypeIcon() {
    }

    // ------------------------------------------------------------------
    // 对外接口 / Public API
    // ------------------------------------------------------------------

    /**
     * 解析图标（带缓存） / Resolve icon (cached)
     *
     * @param entityType 实体类型 / Entity type
     * @return 图标规格（永不返回 null，最差是文字兜底） / Icon spec, never null
     */
    public static IconSpec resolve(EntityType<?> entityType) {
        if (entityType == null) {
            return IconSpec.label("?");
        }
        IconSpec cached = ICONS_CACHE.get(entityType);
        if (cached != null) {
            return cached;
        }
        IconSpec spec = createIconSpec(entityType);
        ICONS_CACHE.put(entityType, spec);
        return spec;
    }

    /**
     * 把图标画到按钮上 / Render icon onto a button
     *
     * @param matrixStack 矩阵 / Matrix stack
     * @param entityType  实体类型 / Entity type
     * @param x           图标左上角 X / icon top-left X
     * @param y           图标左上角 Y / icon top-left Y
     */
    public static void render(MatrixStack matrixStack, EntityType<?> entityType, float x, float y) {
        IconSpec icon = resolve(entityType);
        if (icon == null) {
            return;
        }
        if (icon.layers == null) {
            drawLabel(matrixStack, icon.label, x, y);
            return;
        }
        Minecraft.getInstance().getTextureManager().bind(icon.texture);
        for (IconSpec.Layer layer : icon.layers) {
            // 10 参数重载：目标矩形与 UV 区域解耦，一次绘制完成"裁剪 + 缩放"
            BlitFloat.blitFloat(matrixStack, x + layer.dx, y + layer.dy, layer.dw, layer.dh,
                    layer.u, layer.v, layer.w, layer.h, icon.texW, icon.texH);
        }
    }

    /**
     * 清理图标缓存 / Clear icon cache
     */
    public static void onResourceReload() {
        ICONS_CACHE.clear();
    }

    // ------------------------------------------------------------------
    // 解析 / Resolution
    // ------------------------------------------------------------------

    private static IconSpec createIconSpec(EntityType<?> entityType) {
        Minecraft mc = Minecraft.getInstance();
        try {
            // 1) 替身实体：优先替身头像（整图）
            if (isStandEntity(entityType)) {
                ResourceLocation portrait = getStandPortrait(entityType);
                if (portrait != null) {
                    return IconSpec.texture(portrait);
                }
                return labelFor(entityType);
            }

            ResourceLocation skin = getEntityTexture(entityType);
            if (skin == null) {
                return labelFor(entityType);
            }

            // 2) RotP 自带 entity_icon（整图）
            ResourceLocation icon = toEntityIconPath(mc, skin);
            if (icon != null) {
                return IconSpec.texture(icon);
            }

            // 3) 查表裁剪（村民/灾厄村民/劫掠兽……含鼻子等独立部件）
            Portrait portrait = PORTRAITS.get(entityType.getRegistryName());
            // 4) 模型实现 IHasHead 时自动推导头部矩形（覆盖多数模组人形生物）
            if (portrait == null) {
                portrait = inferPortrait(entityType);
            }
            if (portrait != null) {
                IconSpec crop = buildCrop(mc, skin, portrait);
                if (crop != null) {
                    return crop;
                }
            }

            // 5) 文字兜底
            return labelFor(entityType);
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to resolve entity icon for {}", entityType, e);
            return labelFor(entityType);
        }
    }

    /**
     * 按头像规格从皮肤贴图上裁出各部件并排到 16×16 画布 / Build the 16×16 icon from the skin
     *
     * @return 图标规格；资源/尺寸/透明度校验失败时返回 null（调用方降级） / spec or null
     */
    private static IconSpec buildCrop(Minecraft mc, ResourceLocation skin, Portrait portrait) {
        if (portrait.bboxW <= 0 || portrait.bboxH <= 0 || portrait.parts == null || portrait.parts.length == 0) {
            return null;
        }
        try (InputStream stream = mc.getResourceManager().getResource(skin).getInputStream();
             NativeImage image = NativeImage.read(stream)) {
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            if (imageWidth < 16 || imageHeight < 16) {
                return null;
            }

            // HD 皮肤资源包：按"实际尺寸 / 基准尺寸"换算 UV
            float skinScaleX = portrait.baseW > 0 ? (float) imageWidth / (float) portrait.baseW : 1.0F;
            float skinScaleY = portrait.baseH > 0 ? (float) imageHeight / (float) portrait.baseH : 1.0F;

            // 模型空间 -> 16×16 画布：等比缩放居中，保持头部原本的长宽比（绝不非等比拉伸）
            float uniform = Math.min(16.0F / (float) portrait.bboxW, 16.0F / (float) portrait.bboxH);
            float scaleX = uniform;
            float scaleY = uniform;
            float offsetX = (16.0F - (float) portrait.bboxW * uniform) / 2.0F;
            float offsetY = (16.0F - (float) portrait.bboxH * uniform) / 2.0F;

            IconSpec.Layer[] layers = new IconSpec.Layer[portrait.parts.length];
            for (int i = 0; i < portrait.parts.length; i++) {
                Part part = portrait.parts[i];
                int u = Math.round((float) part.u * skinScaleX);
                int v = Math.round((float) part.v * skinScaleY);
                int w = Math.max(1, Math.round((float) part.w * skinScaleX));
                int h = Math.max(1, Math.round((float) part.h * skinScaleY));
                if (u < 0 || v < 0 || u + w > imageWidth || v + h > imageHeight) {
                    return null;
                }

                // 透明度校验：至少一半像素不是全透明，避免裁出空白/无效区域
                int opaque = 0;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        if (NativeImage.getA(image.getPixelRGBA(u + x, v + y)) != 0) {
                            opaque++;
                        }
                    }
                }
                if (opaque * 2 < w * h) {
                    return null;
                }

                layers[i] = new IconSpec.Layer(
                        u, v, w, h,
                        offsetX + (float) part.x * scaleX,
                        offsetY + (float) part.y * scaleY,
                        (float) part.w * scaleX,
                        (float) part.h * scaleY);
            }
            return IconSpec.crop(skin, layers, imageWidth, imageHeight);
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to crop skin {} for icon", skin, e);
            return null;
        }
    }

    /**
     * 根据模型的头部件推导正面矩形 / Infer head front rect from the model's head part
     */
    private static Portrait inferPortrait(EntityType<?> entityType) {
        try {
            Minecraft mc = Minecraft.getInstance();
            EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().renderers.get(entityType);
            if (!(renderer instanceof LivingRenderer)) {
                return null;
            }
            Object model = ((LivingRenderer<?, ?>) renderer).getModel();
            if (!(model instanceof IHasHead)) {
                return null;
            }
            ModelRenderer head = ((IHasHead) model).getHead();
            if (head == null) {
                return null;
            }
            // getRandomCube 是 ModelRenderer 上唯一能公开取到方块的入口；头部只有一个方块时结果稳定
            ModelRenderer.ModelBox box = head.getRandomCube(new Random(0));
            if (box == null) {
                return null;
            }
            int dx = Math.round(box.maxX - box.minX);
            int dy = Math.round(box.maxY - box.minY);
            int dz = Math.round(box.maxZ - box.minZ);
            if (dx <= 0 || dy <= 0 || dz <= 0 || dx > 48 || dy > 48 || dz > 48) {
                return null;
            }
            // BipedModel / VillagerModel / IllagerModel 的名义贴图都是 64×64
            return new Portrait(new Part[]{new Part(dz, dz, dx, dy, 0, 0)}, dx, dy, 64, 64);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 实体贴图 -> RotP entity_icon 路径（存在才返回） / Map entity texture to RotP entity_icon path
     */
    private static ResourceLocation toEntityIconPath(Minecraft mc, ResourceLocation entityTexture) {
        String path = entityTexture.getPath();
        if (!path.contains("/entity/")) {
            return null;
        }
        String iconPath = path.contains("/model/entity/")
                ? path.replace("/model/entity/", "/entity_icon/")
                : path.replace("/entity/", "/entity_icon/");
        ResourceLocation icon = new ResourceLocation(entityTexture.getNamespace(), iconPath);
        return mc.getResourceManager().hasResource(icon) ? icon : null;
    }

    private static IconSpec labelFor(EntityType<?> entityType) {
        String name;
        try {
            name = entityType.getDescription().getString();
        } catch (Exception e) {
            name = "?";
        }
        if (name == null || name.isEmpty()) {
            name = "?";
        }
        return IconSpec.label(name);
    }

    /**
     * 画文字兜底（与 RotP 自己 UI 的 renderIcon 一致） / Draw text fallback
     */
    private static void drawLabel(MatrixStack matrixStack, String name, float x, float y) {
        if (name == null || name.isEmpty()) {
            return;
        }
        FontRenderer font = Minecraft.getInstance().font;
        ITextComponent shown = StringTextComponent.EMPTY;
        int width = 0;
        for (int i = 1; i <= name.length() && width <= MAX_LABEL_WIDTH; i++) {
            shown = new StringTextComponent(name.substring(0, i));
            width = font.width(shown);
        }
        RenderSystem.disableDepthTest();
        font.draw(matrixStack, shown,
                x + (16.0F - (float) width) / 2.0F,
                y + 4.0F,
                0xFFFFFF);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
    }

    // ------------------------------------------------------------------
    // 原有逻辑 / Existing helpers
    // ------------------------------------------------------------------

    /**
     * 判断是否为替身实体 / Check if entity is stand entity
     */
    private static boolean isStandEntity(EntityType<?> entityType) {
        return entityType instanceof com.github.standobyte.jojo.entity.stand.StandEntityType<?>;
    }

    /**
     * 获取指定实体类型对应的替身（Stand）头像纹理位置。
     * <p>
     * 该方法会尝试根据实体类型的注册名构造一个纹理路径，
     * 然后检查该纹理文件是否存在，如果存在则返回对应的 {@link ResourceLocation}，
     * 否则返回 {@code null}。
     *
     * @param entityType 实体类型，通常代表一个替身实体
     * @return 替身头像的 ResourceLocation，如果不存在则返回 null
     */
    private static ResourceLocation getStandPortrait(EntityType<?> entityType) {
        // 获取实体类型的注册名（例如 "my_mod:star_platinum"）
        ResourceLocation registryName = entityType.getRegistryName();
        // 如果注册名为空（理论上不应发生，但做防御性检查），直接返回 null
        if (registryName == null) {
            return null;
        }

        // 从注册名中提取路径部分（即 "star_platinum"），作为替身的 ID
        String standId = registryName.getPath();

        // 获取 Minecraft 客户端实例，用于访问资源管理器
        Minecraft mc = Minecraft.getInstance();

        // 首先尝试使用实体注册的命名空间
        ResourceLocation portraitPath = new ResourceLocation(
                registryName.getNamespace(),
                "textures/power/" + standId + ".png"
        );
        if (mc.getResourceManager().hasResource(portraitPath)) {
            return portraitPath;
        }

        // 如果没找到，尝试使用配置的命名空间列表（处理 Extra-Stands 等情况）
        @SuppressWarnings("unchecked")
        java.util.List<String> configuredNamespaces = (java.util.List<String>) com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig.standPortraitNamespaces.get();
        if (configuredNamespaces != null && !configuredNamespaces.isEmpty()) {
            for (String namespace : configuredNamespaces) {
                portraitPath = new ResourceLocation(namespace.trim(), "textures/power/" + standId + ".png");
                if (mc.getResourceManager().hasResource(portraitPath)) {
                    return portraitPath;
                }
            }
        }

        // 没有替身头像时不再刷 WARN（列表里可能有很多替身），只留 debug
        RotPYellowTemperanceAddon.LOGGER.debug("Stand portrait not found for entity: {}", registryName);
        return null;
    }

    /**
     * 获取实体贴图 / Get entity texture
     *
     * @param entityType 实体类型 / Entity type
     * @return 贴图资源路径 / Texture resource path
     */
    @SuppressWarnings("unchecked")
    private static <T extends Entity> ResourceLocation getEntityTexture(EntityType<T> entityType) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderer<? super T> renderer =
                (EntityRenderer<? super T>) mc.getEntityRenderDispatcher().renderers.get(entityType);

        // 如果连渲染器都不存在，直接返回 null
        if (renderer == null) {
            return null;
        }

        // 如果实体无法实例化，说明拿不到贴图，返回 null
        try {
            T entity = entityType.create(mc.level);
            if (entity == null) {
                return null;
            }
            return renderer.getTextureLocation(entity);
        } catch (Exception e) {
            // 某些实体（如 CustomNPCs 的 EntityCustomNpc）在创建时需要额外的初始化，
            // 直接创建会导致 NPE 或其他异常，捕获后返回 null，使用文字兜底。
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to create or get texture for entity: {}", entityType.getRegistryName(), e);
            return null;
        }
    }

    private static void addPortrait(String id, int bboxW, int bboxH, int baseW, int baseH, Part... parts) {
        PORTRAITS.put(new ResourceLocation(id), new Portrait(parts, bboxW, bboxH, baseW, baseH));
    }

    // ------------------------------------------------------------------
    // 数据结构 / Data structures
    // ------------------------------------------------------------------

    /**
     * 头像里的一个部件：皮肤上的矩形 + 相对脸部正面左上角的模型位置
     * <p>
     * 皮肤像素与模型单位 1:1，所以部件在模型空间的宽高就等于皮肤矩形的 w/h。
     */
    private static final class Part {
        final int u;
        final int v;
        final int w;
        final int h;
        final int x;
        final int y;

        Part(int u, int v, int w, int h, int x, int y) {
            this.u = u;
            this.v = v;
            this.w = w;
            this.h = h;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * 一个生物的头像规格 / Portrait layout of a mob head
     */
    private static final class Portrait {
        final Part[] parts;
        final int bboxW;
        final int bboxH;
        final int baseW;
        final int baseH;

        Portrait(Part[] parts, int bboxW, int bboxH, int baseW, int baseH) {
            this.parts = parts;
            this.bboxW = bboxW;
            this.bboxH = bboxH;
            this.baseW = baseW;
            this.baseH = baseH;
        }
    }

    /**
     * 图标规格：若干图层（整图也是单个图层），或文字兜底（layers 为 null）
     */
    public static final class IconSpec {
        public final ResourceLocation texture;
        public final float texW;
        public final float texH;
        public final Layer[] layers;
        public final String label;

        private IconSpec(ResourceLocation texture, float texW, float texH, Layer[] layers, String label) {
            this.texture = texture;
            this.texW = texW;
            this.texH = texH;
            this.layers = layers;
            this.label = label;
        }

        /**
         * 整图：UV 0..texW / 0..texH，texW/texH 传 16 即"整张贴图铺满 16×16"
         */
        static IconSpec texture(ResourceLocation texture) {
            return new IconSpec(texture, 16.0F, 16.0F,
                    new Layer[]{new Layer(0, 0, 16, 16, 0.0F, 0.0F, 16.0F, 16.0F)}, null);
        }

        /**
         * 裁剪：按图层列表拼出 16×16 图标
         */
        static IconSpec crop(ResourceLocation texture, Layer[] layers, float texW, float texH) {
            return new IconSpec(texture, texW, texH, layers, null);
        }

        static IconSpec label(String name) {
            return new IconSpec(null, 0.0F, 0.0F, null, name);
        }

        /**
         * 一个绘制图层：皮肤上的 UV 矩形 + 图标 16×16 画布内的目标矩形
         */
        public static final class Layer {
            public final float u;
            public final float v;
            public final float w;
            public final float h;
            public final float dx;
            public final float dy;
            public final float dw;
            public final float dh;

            Layer(float u, float v, float w, float h, float dx, float dy, float dw, float dh) {
                this.u = u;
                this.v = v;
                this.w = w;
                this.h = h;
                this.dx = dx;
                this.dy = dy;
                this.dw = dw;
                this.dh = dh;
            }
        }
    }
}
