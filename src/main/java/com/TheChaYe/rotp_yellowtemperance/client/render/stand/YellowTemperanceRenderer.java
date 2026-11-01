// 原代码来自：https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// 修改日期：2026-03-19
// Original code from: https://github.com/silentevermore/RotP-Whitesnake-Rework (GPLv3)
// on 2026-03-19
package com.TheChaYe.rotp_yellowtemperance.client.render.stand;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.YellowTemperanceEntity;
import com.github.standobyte.jojo.client.render.entity.model.stand.StandEntityModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
import com.github.standobyte.jojo.util.mc.entitysubtype.EntitySubtype;
import com.github.standobyte.jojo.util.mc.entitysubtype.EntityTypeToInstance;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 黄色节制替身渲染器 / Yellow Temperance Stand Renderer
 * 处理黄色节制替身的渲染逻辑，包括伪装效果和模型变换
 */
public class YellowTemperanceRenderer extends StandEntityRenderer<YellowTemperanceEntity, StandEntityModel<YellowTemperanceEntity>> {
    /**
     * 伪装实体缓存Map，避免重复创建实体对象 / Disguise entity cache map to avoid recreating entities
     */
    private final Map<EntityType<?>, WeakReference<LivingEntity>> disguiseEntityCache = new HashMap<>();
    /**
     * 渲染失败的伪装类型（负缓存）：同一类型只记一次日志，之后不再尝试渲染，
     * 避免每帧重试刷屏、也避免同一个类型反复崩。
     */
    private final Set<EntityType<?>> unsupportedDisguiseTypes =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    // 获取配置值 / Get config values
    private double needTicks = YellowTemperanceConfig.DisguiseStandNeedTicks.get();

    /**
     * 构造函数 / Constructor
     * 初始化黄色节制替身渲染器，注册模型和纹理
     */
    public YellowTemperanceRenderer(EntityRendererManager renderManager) {
        super(renderManager,
                StandModelRegistry.registerModel(new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "yellow_temperance"), YellowTemperanceModel::new),
                new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/entity/stand/yellow_temperance.png"), 0);
    }

    /**
     * 渲染黄色节制替身 / Render Yellow Temperance stand
     *
     * <p>伪装分支整体做异常隔离：伪装目标是"别的替身的渲染器 + 一个没有主人的临时实体"，
     * 任何一个第三方替身模型在这条路上抛异常都会把渲染线程带崩。这里失败只影响这一帧的显示，
     * 并把该类型记进负缓存，之后直接跳过渲染。</p>
     */
    @Override
    public void render(YellowTemperanceEntity yellowtemperance, float yRotation, float partialTick, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        EntityType<?> disguiseType = yellowtemperance.getEntityForDisguise().orElse(null);
        if (disguiseType == null) {
            super.render(yellowtemperance, yRotation, partialTick, matrixStack, buffer, packedLight);
            return;
        }
        // 已知渲染会崩的类型：什么都不画（替身自身带全额隐身，不会暴露本体），也不再重试
        if (unsupportedDisguiseTypes.contains(disguiseType)) {
            return;
        }
        try {
            renderDisguisedAs(yellowtemperance, disguiseType, yRotation, partialTick, matrixStack, buffer, packedLight);
        } catch (Throwable t) {
            markDisguiseUnsupported(disguiseType, t);
        }
    }

    /**
     * 以伪装目标的渲染器绘制替身。
     * 与原逻辑一致：目标渲染器不是 LivingRenderer 时什么都不画（避免暴露替身本体）。
     * 所有可能抛异常的操作由调用方统一兜住。
     */
    private void renderDisguisedAs(YellowTemperanceEntity yellowtemperance, EntityType<?> disguiseType,
                                   float yRotation, float partialTick, MatrixStack matrixStack,
                                   IRenderTypeBuffer buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return; // 退世界/切维度的过渡帧：不画，也不要去造实体
        }
        float ticks = yellowtemperance.tickCount + partialTick;
        float tf_progress = MathHelper.clamp(yellowtemperance.shapeshiftTickDifference(ticks) / (float) needTicks, 0, 1);

        EntityRenderer<?> entityRenderer = mc.getEntityRenderDispatcher().renderers.get(disguiseType);
        if (!(entityRenderer instanceof LivingRenderer)) {
            // 非 LivingRenderer（例如 GeckoLib 渲染的替身）无法安全复现，宁可不画
            return;
        }
        @SuppressWarnings("unchecked")
        LivingRenderer<LivingEntity, EntityModel<LivingEntity>> renderer =
                (LivingRenderer<LivingEntity, EntityModel<LivingEntity>>) entityRenderer;
        LivingEntity living = getCachedEntity(disguiseType, mc);
        if (living == null) {
            // 造不出用于取模型/贴图的实例：退回本体模型（原行为）
            super.render(yellowtemperance, yRotation, partialTick, matrixStack, buffer, packedLight);
            return;
        }
        syncEntityState(yellowtemperance, living, partialTick);

        final EntityModel<LivingEntity> model = renderer.getModel();
        final ResourceLocation texture = renderer.getTextureLocation(living);

        float yHeadRotation = MathHelper.rotLerp(partialTick, yellowtemperance.yHeadRotO, yellowtemperance.yHeadRot);
        float yBodyRotation = MathHelper.rotLerp(partialTick, yellowtemperance.yBodyRotO, yellowtemperance.yBodyRot);
        float f2 = yHeadRotation - yBodyRotation;

        float xRotation = MathHelper.lerp(partialTick, yellowtemperance.xRotO, yellowtemperance.xRot);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - yBodyRotation));

        matrixStack.pushPose();
        try {
            matrixStack.scale(-1, -1, 1);
            matrixStack.scale(tf_progress, tf_progress, tf_progress);
            matrixStack.translate(0d, -1.501d, 0d);
            model.young = living.isBaby();
            model.riding = false;
            model.attackTime = 0;
            //animations
            model.prepareMobModel(living, 0, 0, partialTick);
            model.setupAnim(living, 0, 0, ticks, f2, xRotation);
            //texture
            if (texture != null) {
                final RenderType rendertype = model.renderType(texture);
                if (rendertype != null) {
                    final IVertexBuilder ivertexbuilder = buffer.getBuffer(rendertype);
                    model.renderToBuffer(matrixStack, ivertexbuilder, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, this.calcAlpha(yellowtemperance, partialTick));
                }
            }
        } finally {
            // 异常时也要把矩阵栈弹回去，否则整个世界的矩阵栈都会错位
            matrixStack.popPose();
        }
        //finishing point
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>>(yellowtemperance, renderer, partialTick, matrixStack, buffer, packedLight));
    }

    /**
     * 记录一个渲染失败的伪装类型（同一类型只报一次），后续该类型不再尝试渲染。
     */
    private void markDisguiseUnsupported(EntityType<?> disguiseType, Throwable cause) {
        if (unsupportedDisguiseTypes.add(disguiseType)) {
            RotPYellowTemperanceAddon.LOGGER.error("[YT] 替身伪装渲染失败，已禁用该类型：{}",
                    disguiseType.getRegistryName(), cause);
        }
    }

    /**
     * 获取缓存的伪装实体 / Get cached disguise entity
     * 从缓存中获取伪装实体，如果不存在或已失效则创建新实体。
     * 非生物类型（或造不出实体）返回 null，由调用方退回本体模型，避免 ClassCastException。
     */
    private LivingEntity getCachedEntity(EntityType<?> entityType, Minecraft mc) {
        if (mc.level == null) {
            return null;
        }
        WeakReference<LivingEntity> ref = disguiseEntityCache.get(entityType);
        LivingEntity cached = ref != null ? ref.get() : null;

        if (cached == null || !cached.isAlive() || cached.level != mc.level) {
            Entity created = entityType.create(mc.level);
            if (!(created instanceof LivingEntity)) {
                return null;
            }
            cached = (LivingEntity) created;
            disguiseEntityCache.put(entityType, new WeakReference<>(cached));
        }
        return cached;
    }

    /**
     * 同步实体状态 / Sync entity state
     * 使伪装实体的动作与黄色节制保持一致
     */
    private void syncEntityState(YellowTemperanceEntity source, LivingEntity target, float partialTick) {
        target.setPos(source.getX(), source.getY(), source.getZ());
        target.xo = source.xo;
        target.yo = source.yo;
        target.zo = source.zo;
        target.xOld = source.xOld;
        target.yOld = source.yOld;
        target.zOld = source.zOld;
        target.yRot = MathHelper.rotLerp(partialTick, source.yRotO, source.yRot);
        target.xRot = MathHelper.lerp(partialTick, source.xRotO, source.xRot);
        target.yHeadRot = source.yHeadRot;
        target.yHeadRotO = source.yHeadRotO;
        target.yBodyRot = source.yBodyRot;
        target.yBodyRotO = source.yBodyRotO;
        target.tickCount = source.tickCount;
    }

    /**
     * 计算透明度 / Calculate alpha
     * 返回实体的透明度值
     */
    @Override
    protected float calcAlpha(YellowTemperanceEntity entity, float partialTick) {
        return 1.0f;
    }


}
