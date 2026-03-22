// 原代码来自：https://github.com/MakutaZeml/Rotp-Green-Day (GPLv3)
// 修改日期：2026-03-19
// Original code from: https://github.com/MakutaZeml/Rotp-Green-Day (GPLv3)
// on 2026-03-19
/**
 * 侵蚀渲染层 / Erosion Render Layer
 * 处理黄色节制侵蚀效果的视觉渲染，包括不同等级的侵蚀纹理
 * Handles visual rendering of Yellow Temperance erosion effects, including erosion textures of different levels
 */
package com.TheChaYe.rotp_yellowtemperance.client.render;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.github.standobyte.jojo.client.render.entity.layerrenderer.HamonBurnLayer;
import com.github.standobyte.jojo.client.render.entity.layerrenderer.IFirstPersonHandLayer;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class ErosionLayer<T extends LivingEntity, M extends EntityModel<T>> extends LayerRenderer<T, M> implements IFirstPersonHandLayer {
    /**
     * 双足生物侵蚀纹理路径 / Biped erosion texture path
     */
    public static final ResourceLocation BIPED_PATH = new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/entity/layer/erosion");

    /**
     * 非双足生物侵蚀纹理路径 / Non-biped erosion texture path
     */
    public static final ResourceLocation NON_BIPED_PATH = new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/entity/layer/erosion");

    /**
     * 图层可见性状态 / Layer visibility state
     */
    private boolean visible = true;

    /**
     * 侵蚀纹理映射 / Erosion texture mapping
     */
    private Map<HamonBurnLayer.TextureSize, ResourceLocation[]> LAYER_TEXTURES_MOLD;


    /**
     * 构造函数 / Constructor
     *
     * @param renderer     实体渲染器 / Entity renderer
     * @param texturesPath 纹理路径 / Texture path
     */
    public ErosionLayer(IEntityRenderer<T, M> renderer, ResourceLocation texturesPath) {
        super(renderer);
        initTextures(texturesPath);
    }

    /**
     * 设置图层可见性 / Set layer visibility
     *
     * @param visible 是否可见 / Whether visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * 渲染侵蚀效果 / Render erosion effect
     *
     * @param matrixStack   矩阵栈 / Matrix stack
     * @param buffer        渲染缓冲区 / Render buffer
     * @param packedLight   光照等级 / Light level
     * @param entity        实体 / Entity
     * @param walkAnimPos   行走动画位置 / Walk animation position
     * @param walkAnimSpeed 行走动画速度 / Walk animation speed
     * @param partialTick   部分刻度 / Partial tick
     * @param ticks         刻度 / Ticks
     * @param headYRotation 头部Y轴旋转 / Head Y rotation
     * @param headXRotation 头部X轴旋转 / Head X rotation
     */
    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight,
                       T entity, float walkAnimPos, float walkAnimSpeed, float partialTick,
                       float ticks, float headYRotation, float headXRotation) {
        if (!entity.isInvisible()) {
            M model = getParentModel();
            ResourceLocation texture = getTexture(model, entity);
            if (texture == null) return;

            IVertexBuilder vertexBuilder = buffer.getBuffer(RenderType.entityTranslucent(texture));
            model.renderToBuffer(matrixStack, vertexBuilder, packedLight, LivingRenderer.getOverlayCoords(entity, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /**
     * 获取侵蚀纹理 / Get erosion texture
     *
     * @param model  实体模型 / Entity model
     * @param entity 实体 / Entity
     * @return 纹理位置，如果无侵蚀效果则返回null / Texture location, returns null if no erosion effect
     */
    @Nullable
    private ResourceLocation getTexture(EntityModel<?> model, LivingEntity entity) {
        EffectInstance erosion = entity.getEffect(InitEffects.YELLOW_TEMPERANCE_EROSION.get());
        if (erosion != null) {
            int erosionLVL = Math.min(erosion.getAmplifier() / 2, 4);
            HamonBurnLayer.TextureSize erosionSize = HamonBurnLayer.TextureSize.getClosestTexSize(model);
            return LAYER_TEXTURES_MOLD.get(erosionSize)[erosionLVL];
        }
        return null;
    }

    /**
     * 渲染第一人称手部 / Render first person hand
     *
     * @param side           手部侧边 / Hand side
     * @param matrixStack    矩阵栈 / Matrix stack
     * @param buffer         渲染缓冲区 / Render buffer
     * @param light          光照等级 / Light level
     * @param player         玩家实体 / Player entity
     * @param playerRenderer 玩家渲染器 / Player renderer
     */
    @Override
    public void renderHandFirstPerson(HandSide side, MatrixStack matrixStack,
                                      IRenderTypeBuffer buffer, int light, AbstractClientPlayerEntity player,
                                      PlayerRenderer playerRenderer) {
        PlayerModel<AbstractClientPlayerEntity> model = playerRenderer.getModel();
        IFirstPersonHandLayer.defaultRender(side, matrixStack, buffer, light, player, playerRenderer,
                model, getTexture(model, player));
    }

    /**
     * 初始化纹理映射 / Initialize texture mapping
     *
     * @param texturesPath 基础纹理路径 / Base texture path
     */
    private void initTextures(ResourceLocation texturesPath) {
        LAYER_TEXTURES_MOLD = Util.make(new EnumMap<>(HamonBurnLayer.TextureSize.class), map -> {
            String id = texturesPath.getNamespace();
            String path = texturesPath.getPath();
            map.put(HamonBurnLayer.TextureSize._64x32, new ResourceLocation[]{
                    new ResourceLocation(id, path + "/t64x32/1.png"),
                    new ResourceLocation(id, path + "/t64x32/2.png"),
                    new ResourceLocation(id, path + "/t64x32/3.png"),
                    new ResourceLocation(id, path + "/t64x32/4.png"),
                    new ResourceLocation(id, path + "/t64x32/5.png")
            });
            map.put(HamonBurnLayer.TextureSize._64x64, new ResourceLocation[]{
                    new ResourceLocation(id, path + "/t64x64/1.png"),
                    new ResourceLocation(id, path + "/t64x64/2.png"),
                    new ResourceLocation(id, path + "/t64x64/3.png"),
                    new ResourceLocation(id, path + "/t64x64/4.png"),
                    new ResourceLocation(id, path + "/t64x64/5.png")
            });
            map.put(HamonBurnLayer.TextureSize._128x64, new ResourceLocation[]{
                    new ResourceLocation(id, path + "/t128x64/1.png"),
                    new ResourceLocation(id, path + "/t128x64/2.png"),
                    new ResourceLocation(id, path + "/t128x64/3.png"),
                    new ResourceLocation(id, path + "/t128x64/4.png"),
                    new ResourceLocation(id, path + "/t128x64/5.png")

            });
            map.put(HamonBurnLayer.TextureSize._128x128, new ResourceLocation[]{
                    new ResourceLocation(id, path + "/t128x128/1.png"),
                    new ResourceLocation(id, path + "/t128x128/2.png"),
                    new ResourceLocation(id, path + "/t128x128/3.png"),
                    new ResourceLocation(id, path + "/t128x128/4.png"),
                    new ResourceLocation(id, path + "/t128x128/5.png")

            });
            map.put(HamonBurnLayer.TextureSize._256x128, new ResourceLocation[]{
                    new ResourceLocation(id, path + "/t256x128/1.png"),
                    new ResourceLocation(id, path + "/t256x128/2.png"),
                    new ResourceLocation(id, path + "/t256x128/3.png"),
                    new ResourceLocation(id, path + "/t256x128/4.png"),
                    new ResourceLocation(id, path + "/t256x128/5.png")

            });
            map.put(HamonBurnLayer.TextureSize._256x256, new ResourceLocation[]{
                    new ResourceLocation(id, path + "/t256x256/1.png"),
                    new ResourceLocation(id, path + "/t256x256/2.png"),
                    new ResourceLocation(id, path + "/t256x256/3.png"),
                    new ResourceLocation(id, path + "/t256x256/4.png"),
                    new ResourceLocation(id, path + "/t256x256/5.png")

            });
        });
    }


}
