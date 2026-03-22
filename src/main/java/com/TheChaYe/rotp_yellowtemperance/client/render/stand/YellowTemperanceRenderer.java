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
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * 黄色节制替身渲染器 / Yellow Temperance Stand Renderer
 * 处理黄色节制替身的渲染逻辑，包括伪装效果和模型变换
 */
public class YellowTemperanceRenderer extends StandEntityRenderer<YellowTemperanceEntity, StandEntityModel<YellowTemperanceEntity>> {
    /**
     * 伪装实体缓存Map，避免重复创建实体对象 / Disguise entity cache map to avoid recreating entities
     */
    private final Map<EntityType<?>, WeakReference<LivingEntity>> disguiseEntityCache = new HashMap<>();
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
     * 处理替身的主要渲染逻辑，包括伪装效果和变形动画
     */
    @Override
    public void render(YellowTemperanceEntity yellowtemperance, float yRotation, float partialTick, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        final Minecraft mc = Minecraft.getInstance();
        float ticks = yellowtemperance.tickCount + partialTick;
        float tf_progress = MathHelper.clamp(yellowtemperance.shapeshiftTickDifference(ticks) / (float) needTicks, 0, 1);

        if (yellowtemperance.getEntityForDisguise().isPresent()) {
            //testing
            EntityType<? extends LivingEntity> entityType = (EntityType<? extends LivingEntity>) yellowtemperance.getEntityForDisguise().get();
            LivingRenderer<LivingEntity, EntityModel<LivingEntity>> renderer = (LivingRenderer<LivingEntity, EntityModel<LivingEntity>>) mc.getEntityRenderDispatcher().renderers.get(entityType);
            //BEWARE
            final LivingEntity living = (LivingEntity) EntityTypeToInstance.getEntityInstance(EntitySubtype.base(entityType), mc.level);
            final EntityModel<LivingEntity> model = renderer.getModel();
            final ResourceLocation texture = renderer.getTextureLocation(living);

            float yHeadRotation = MathHelper.rotLerp(partialTick, yellowtemperance.yHeadRotO, yellowtemperance.yHeadRot);
            float yBodyRotation = MathHelper.rotLerp(partialTick, yellowtemperance.yBodyRotO, yellowtemperance.yBodyRot);
            float f2 = yHeadRotation - yBodyRotation;

            float xRotation = MathHelper.lerp(partialTick, yellowtemperance.xRotO, yellowtemperance.xRot);
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - yBodyRotation));

            float animSpeed = 0;
            float animPos = 0;
            float attackTime = 0;

            matrixStack.pushPose();
            matrixStack.scale(-1, -1, 1);
            matrixStack.scale(tf_progress, tf_progress, tf_progress);
            matrixStack.translate(0d, -1.501d, 0d);
            model.young = living.isBaby();
            model.riding = false;
            model.attackTime = attackTime;
            //animations
            model.prepareMobModel(living, animSpeed, animPos, partialTick);
            model.setupAnim(living, animSpeed, animPos, ticks, f2, xRotation);
            //texture
            if (texture != null) {
                final RenderType rendertype = model.renderType(texture);
                if (rendertype != null) {
                    final IVertexBuilder ivertexbuilder = buffer.getBuffer(rendertype);
                    model.renderToBuffer(matrixStack, ivertexbuilder, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, this.calcAlpha(yellowtemperance, partialTick));
                }
            }
            //finishing point
            matrixStack.popPose();
            MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>>(yellowtemperance, renderer, partialTick, matrixStack, buffer, packedLight));
        } else {
            super.render(yellowtemperance, yRotation, partialTick, matrixStack, buffer, packedLight);
        }
    }

    /**
     * 获取缓存的伪装实体 / Get cached disguise entity
     * 从缓存中获取伪装实体，如果不存在或已失效则创建新实体
     */
    private LivingEntity getCachedEntity(EntityType<? extends LivingEntity> entityType, Minecraft mc) {
        WeakReference<LivingEntity> ref = disguiseEntityCache.get(entityType);
        LivingEntity cached = ref != null ? ref.get() : null;

        if (cached == null || !cached.isAlive()) {
            cached = entityType.create(mc.level);
            if (cached != null) {
                disguiseEntityCache.put(entityType, new WeakReference<>(cached));
            }
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