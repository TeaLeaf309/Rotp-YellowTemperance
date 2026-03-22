/**
 * 黄色节制渲染层 / Yellow Temperance Render Layer
 * 处理黄色节制替身的手套渲染和第一人称手部渲染
 * Handles glove rendering and first-person hand rendering for Yellow Temperance stand
 */
package com.TheChaYe.rotp_yellowtemperance.client.render;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitStands;
import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.playeranim.PlayerAnimationHandler;
import com.github.standobyte.jojo.client.render.entity.layerrenderer.IFirstPersonHandLayer;
import com.github.standobyte.jojo.client.standskin.StandSkinsManager;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModStatusEffects;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effects;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;

public class YellowTemperanceLayer<T extends LivingEntity, M extends BipedModel<T>> extends LayerRenderer<T, M> implements IFirstPersonHandLayer {
    /**
     * 标准手臂纹理路径 / Standard arm texture path
     */
    private static final ResourceLocation WHITE_LAYER = new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/entity/stand/yellow_temperance.png");

    /**
     * 细手臂纹理路径 / Slim arm texture path
     */
    private static final ResourceLocation WHITE_LAYER_SLIM = new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "textures/entity/stand/yellow_temperance_slim.png");

    /**
     * 手套模型 / Gloves model
     */
    private final M glovesModel;

    /**
     * 是否为细手臂模型 / Whether it's slim arm model
     */
    private final boolean slim;

    /**
     * 构造函数 / Constructor
     *
     * @param renderer     实体渲染器 / Entity renderer
     * @param hermitoModel 手套模型 / Gloves model
     * @param slim         是否为细手臂模型 / Whether it's slim arm model
     */
    public YellowTemperanceLayer(IEntityRenderer<T, M> renderer, M hermitoModel, boolean slim) {
        super(renderer);
        this.glovesModel = hermitoModel;
        this.slim = slim;
        PlayerAnimationHandler.getPlayerAnimator().onArmorLayerInit(this);
    }

    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ticks, float yRot, float xRot) {
        if (!ClientUtil.canSeeStands()) {
            return;
        }

        if (!(entity.hasEffect(Effects.INVISIBILITY) || entity.hasEffect(ModStatusEffects.FULL_INVISIBILITY.get()))) {
            IStandPower.getStandPowerOptional(entity).ifPresent((stand) -> {
                if (stand.getType() == InitStands.STAND_YELLOW_TEMPERANCE.getStandType() && stand.getStandManifestation() instanceof StandEntity) {
                    M playerModel = getParentModel();
                    glovesModel.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
                    playerModel.copyPropertiesTo(glovesModel);
                    glovesModel.setupAnim(entity, limbSwing, limbSwingAmount, ticks, yRot, xRot);

                    glovesModel.leftArm.visible = playerModel.leftArm.visible;
                    glovesModel.rightArm.visible = playerModel.rightArm.visible;
                    ResourceLocation texture = getTexture();
                    texture = StandSkinsManager.getInstance().getRemappedResPath(manager -> manager
                            .getStandSkin(stand.getStandInstance().get()), texture);
                    IVertexBuilder vertexBuilder = buffer.getBuffer(RenderType.entityTranslucent(texture));
                    glovesModel.renderToBuffer(matrixStack, vertexBuilder, packedLight, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                }
            });
        }
    }

    private ResourceLocation getTexture() {
        return slim ? WHITE_LAYER_SLIM : WHITE_LAYER;
    }

    @Override
    public void renderHandFirstPerson(HandSide side, MatrixStack matrixStack, IRenderTypeBuffer buffer, int light,
                                      AbstractClientPlayerEntity player, PlayerRenderer playerRenderer) {
        if (!ClientUtil.canSeeStands()) {
            return;
        }

        if (!(player.hasEffect(Effects.INVISIBILITY) || player.hasEffect(ModStatusEffects.FULL_INVISIBILITY.get()))) {
            IStandPower.getStandPowerOptional(player).ifPresent((stand) -> {
                StandType<?> hm = InitStands.STAND_YELLOW_TEMPERANCE.getStandType();
                if (stand.getType() == hm && stand.getStandManifestation() instanceof StandEntity && stand.getHeldAction() != InitStands.YELLOW_TEMPERANCE_BLOCK.get()) {
                    PlayerModel<AbstractClientPlayerEntity> model = (PlayerModel<AbstractClientPlayerEntity>) glovesModel;
                    ResourceLocation texture = getTexture();
                    texture = StandSkinsManager.getInstance().getRemappedResPath(manager -> manager
                            .getStandSkin(stand.getStandInstance().get()), texture);
                    ClientUtil.setupForFirstPersonRender(model, player);
                    IVertexBuilder vertexBuilder = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(texture), false, false);
                    ModelRenderer glove = ClientUtil.getArm(model, side);
                    ModelRenderer gloveOuter = ClientUtil.getArmOuter(model, side);
                    glove.xRot = 0.0F;
                    glove.render(matrixStack, vertexBuilder, light, OverlayTexture.NO_OVERLAY);
                    gloveOuter.xRot = 0.0F;
                    gloveOuter.render(matrixStack, vertexBuilder, light, OverlayTexture.NO_OVERLAY);
                }
            });
        }
    }

}
