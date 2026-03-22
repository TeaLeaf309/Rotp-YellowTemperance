package com.TheChaYe.rotp_yellowtemperance.client.render;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.entity.YellowTemperanceEntity;
import com.github.standobyte.jojo.client.render.entity.model.stand.StandEntityModel;
import com.github.standobyte.jojo.client.render.entity.model.stand.StandModelRegistry;
import com.github.standobyte.jojo.client.render.entity.renderer.stand.StandEntityRenderer;
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

    /** 伪装实体缓存Map，避免重复创建实体对象 / Disguise entity cache map to avoid recreating entities */
    private final Map<EntityType<?>, WeakReference<LivingEntity>> disguiseEntityCache = new HashMap<>();

    /**
     * 构造函数 / Constructor
     * 初始化黄色节制替身渲染器，注册模型和纹理
     */
    public YellowTemperanceRenderer(EntityRendererManager renderManager) {
        super(renderManager,
                StandModelRegistry.registerModel(new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "yellow_temperance"), YellowTemperanceModel::new),
                new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/entity/stand/yellow_temperance_stand.png"), 0);
    }

    /**
     * 渲染黄色节制替身 / Render Yellow Temperance stand
     * 处理替身的主要渲染逻辑，包括伪装效果和变形动画
     */
    @Override
    public void render(YellowTemperanceEntity yellowtemperance, float yRotation, float partialTick, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        final Minecraft mc = Minecraft.getInstance();
        float ticks = yellowtemperance.tickCount + partialTick;
        // 计算变形进度，限制在0-1之间 / Calculate transformation progress, clamp between 0-1
        float tf_progress = MathHelper.clamp(yellowtemperance.shapeshiftTickDifference(ticks) / 20f, 0, 1);

        // 检查是否有伪装目标 / Check if there's a disguise target
        if (yellowtemperance.getEntityForDisguise().isPresent()) {
            Object disguiseObject = yellowtemperance.getEntityForDisguise().get();

            // 如果伪装目标是实体类型 / If disguise target is an entity type
            if (disguiseObject instanceof EntityType) {
                EntityType<? extends LivingEntity> entityType = (EntityType<? extends LivingEntity>) disguiseObject;
                // 从缓存中获取或创建伪装实体 / Get or create disguise entity from cache
                LivingEntity living = getCachedEntity(entityType, mc);
                if (living == null) {
                    // 如果无法获取实体，则使用默认渲染 / If entity cannot be obtained, use default rendering
                    super.render(yellowtemperance, yRotation, partialTick, matrixStack, buffer, packedLight);
                    return;
                }

                // 同步实体状态，使伪装实体与黄温一致 / Sync entity state to make disguise entity consistent with Yellow Temperance
                syncEntityState(yellowtemperance, living, partialTick);
                // 获取实体渲染器 / Get entity renderer
                LivingRenderer<?, ?> rawRenderer = (LivingRenderer<?, ?>) mc.getEntityRenderDispatcher().renderers.get(entityType);

                // 如果渲染器有效且模型是EntityModel类型 / If renderer is valid and model is EntityModel type
                if (rawRenderer != null && rawRenderer.getModel() instanceof EntityModel) {
                    LivingRenderer<LivingEntity, EntityModel<LivingEntity>> renderer = (LivingRenderer<LivingEntity, EntityModel<LivingEntity>>) rawRenderer;
                    EntityModel<LivingEntity> model = renderer.getModel();
                    ResourceLocation texture = renderer.getTextureLocation(living);

                    matrixStack.pushPose();
                    if (texture != null) {
                        // 获取渲染类型并渲染模型 / Get render type and render model
                        RenderType rendertype = model.renderType(texture);
                        if (rendertype != null) {
                            IVertexBuilder vertexBuilder = buffer.getBuffer(rendertype);
                            model.renderToBuffer(matrixStack, vertexBuilder, packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
                        }
                    }
                    matrixStack.popPose();
                    // 发送渲染后事件 / Post render living event
                    MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(yellowtemperance, renderer, partialTick, matrixStack, buffer, packedLight));
                } else {
                    // 渲染器无效时使用默认渲染 / Use default rendering when renderer is invalid
                    super.render(yellowtemperance, yRotation, partialTick, matrixStack, buffer, packedLight);
                }
            } else {
                // 伪装对象不是EntityType时使用默认渲染 / Use default rendering when disguise object is not EntityType
                super.render(yellowtemperance, yRotation, partialTick, matrixStack, buffer, packedLight);
            }
        } else {
            // 没有伪装目标时使用默认渲染 / Use default rendering when no disguise target
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