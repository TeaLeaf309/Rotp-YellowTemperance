// 原代码来自：https://github.com/MakutaZeml/Rotp-CreamStarter (GPLv3)
// 修改日期：2026-03-19
// Original code from: https://github.com/MakutaZeml/Rotp-CreamStarter (GPLv3)
// on 2026-03-19
/**
 * 伪装渲染层 / Disguise Render Layer
 * 处理玩家伪装时的模型渲染逻辑，包括原始模型隐藏和伪装模型显示
 * Handles player disguise model rendering logic, including original model hiding and disguise model display
 */
package com.TheChaYe.rotp_yellowtemperance.client.render;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
public class DisguiseLayer<T extends LivingEntity, M extends BipedModel<T>> extends LayerRenderer<T, M> {

    /**
     * 构造函数 / Constructor
     *
     * @param entityRenderer 实体渲染器 / Entity renderer
     */
    public DisguiseLayer(IEntityRenderer<T, M> entityRenderer) {
        super(entityRenderer);
    }

    /**
     * 检查是否应该隐藏手臂 / Check if arms should be hidden
     *
     * @param player 玩家实体 / Player entity
     * @param model  玩家模型 / Player model
     * @return 是否隐藏手臂 / Whether to hide arms
     */
    public static boolean shouldHideArms(PlayerEntity player, PlayerModel<?> model) {
        return shouldHideArmsInternal(player, model);
    }

    /**
     * 检查客户端玩家是否应该隐藏手臂 / Check if client player should hide arms
     *
     * @param player 客户端玩家实体 / Client player entity
     * @param model  玩家模型 / Player model
     * @return 是否隐藏手臂 / Whether to hide arms
     */
    public static boolean shouldHideArms(AbstractClientPlayerEntity player, PlayerModel<?> model) {
        return shouldHideArmsInternal(player, model);
    }

    /**
     * 内部手臂隐藏检查逻辑 / Internal arm hiding check logic
     *
     * @param player 玩家实体 / Player entity
     * @param model  玩家模型 / Player model
     * @return 是否应该隐藏手臂 / Whether arms should be hidden
     */
    private static boolean shouldHideArmsInternal(PlayerEntity player, PlayerModel<?> model) {
        CompoundNBT nbt = player.getPersistentData();
        return nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG);
    }

    /**
     * 隐藏原始模型部件 / Hide original model parts
     * 在主渲染之前隐藏原始模型部分并调整手臂类型
     * Hides original model parts and adjusts arm types before main rendering
     *
     * @param player 玩家实体 / Player entity
     * @param model  玩家模型 / Player model
     */
    public static void hideOriginalModelParts(PlayerEntity player, PlayerModel<?> model) {
        CompoundNBT nbt = player.getPersistentData();
        if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {

            // 记录隐藏前的状态 / Record state before hiding
            boolean wasHeadVisible = model.head.visible;
            boolean wasBodyVisible = model.body.visible;

            model.head.visible = false;
            model.hat.visible = false;
            model.body.visible = false;
            model.rightArm.visible = false;
            model.leftArm.visible = false;
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
            model.leftPants.visible = false;
            model.rightPants.visible = false;

            // 注意：不再在这里调整手臂类型，因为已经在PlayerModelMixin中处理
            // 调试信息（仅在开发环境中启用）
            // Note: No longer adjusting arm types here as it's handled in PlayerModelMixin
            // Debug info (enabled only in development environment)
        }
    }

    /**
     * 恢复原始模型部件可见性 / Restore original model parts visibility
     * 恢复原始模型部分的可见性
     * Restores visibility of original model parts
     *
     * @param player 玩家实体 / Player entity
     * @param model  玩家模型 / Player model
     */
    public static void restoreOriginalModelParts(PlayerEntity player, PlayerModel<?> model) {

        // 记录恢复前的状态 / Record state before restoration
        boolean wasHeadVisible = model.head.visible;
        boolean wasBodyVisible = model.body.visible;

        model.head.visible = true;
        model.hat.visible = true;
        model.body.visible = true;
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        model.jacket.visible = true;
        model.leftSleeve.visible = true;
        model.rightSleeve.visible = true;
        model.leftPants.visible = true;
        model.rightPants.visible = true;

        // 注意：不再在这里恢复手臂类型，因为应该保持玩家的原始手臂类型
        // Note: No longer restoring arm types here as player's original arm type should be maintained
    }

    /**
     * 检查模型是否为细手臂模型 / Check if model is slim arm model
     *
     * @param model 玩家模型 / Player model
     * @return 是否为细手臂模型 / Whether it's a slim arm model
     */
    private static boolean isSlimModel(PlayerModel<?> model) {
        try {
            // 尝试多个可能的字段名 / Try multiple possible field names
            String[] possibleFieldNames = {"slim", "smallArms", "thinArms"};

            for (String fieldName : possibleFieldNames) {
                try {
                    Field field = PlayerModel.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    if (field.getType() == boolean.class) {
                        boolean result = field.getBoolean(model);
                        return result;
                    }
                } catch (NoSuchFieldException ignored) {
                    // 继续尝试下一个字段名 / Continue trying next field name
                }
            }

            // 如果找不到字段，通过手臂宽度判断
            // 细手臂模型的右臂X偏移通常是-2.5F，普通模型是-3.0F
            // If field not found, judge by arm width
            // Slim arm model's right arm X offset is typically -2.5F, regular model is -3.0F
            boolean isSlimByPosition = Math.abs(model.rightArm.x + 2.5F) < 0.1F;
//            RotPYellowTemperanceAddon.LOGGER.debug("[YT ARM DEBUG] No slim field found, using position detection. Right arm X: {}, isSlim: {}",
//                    model.rightArm.x, isSlimByPosition);
            return isSlimByPosition;

        } catch (Exception e) {
//            RotPYellowTemperanceAddon.LOGGER.error("Failed to detect slim model: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 手动调整手臂位置 / Manually adjust arm positions
     *
     * @param model 玩家模型 / Player model
     * @param slim  是否为细手臂模型 / Whether it's slim arm model
     */
    private static void adjustArmPositions(PlayerModel<?> model, boolean slim) {
        try {
            float armOffset = slim ? 2.5F : 3.0F;

            // 调整手臂位置 / Adjust arm positions
            model.rightArm.x = -armOffset;
            model.leftArm.x = armOffset;
            model.rightSleeve.x = -armOffset;
            model.leftSleeve.x = armOffset;

        } catch (Exception e) {
//            RotPYellowTemperanceAddon.LOGGER.error("Failed to adjust arm positions: " + e.getMessage(), e);
        }
    }

    /**
     * 渲染伪装层 / Render disguise layer
     *
     * @param matrixStack     矩阵栈 / Matrix stack
     * @param buffer          渲染缓冲区 / Render buffer
     * @param packedLight     光照等级 / Light level
     * @param entity          实体 / Entity
     * @param limbSwing       肢体摆动 / Limb swing
     * @param limbSwingAmount 肢体摆动幅度 / Limb swing amount
     * @param partialTicks    部分刻度 / Partial ticks
     * @param ageInTicks      年龄刻度 / Age in ticks
     * @param netHeadYaw      网络头部偏航 / Network head yaw
     * @param headPitch       头部俯仰 / Head pitch
     */
    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof PlayerEntity)) return;

        PlayerEntity player = (PlayerEntity) entity;
        CompoundNBT nbt = player.getPersistentData();

        if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
            // 检查是否仍在加载伪装数据 / Check if disguise data is still loading
            if (nbt.contains(InitTags.YT_DISGUISE_LOADING_TAG) && nbt.getBoolean(InitTags.YT_DISGUISE_LOADING_TAG)) {
                // 伪装数据仍在加载中，暂时不渲染任何内容以避免双重显示
                // 恢复原始模型可见性
                // Disguise data still loading, temporarily don't render anything to avoid double display
                // Restore original model visibility
                if (this.getParentModel() instanceof PlayerModel) {
                    restoreOriginalModelParts(player, (PlayerModel<?>) this.getParentModel());
                }
                return;
            }

//            RotPYellowTemperanceAddon.LOGGER.debug("Rendering disguise layer for player: {}", player.getName().getString());
            try {
                ResourceLocation skinTexture = ClientDisguiseCache.getCachedSkinTexture(player.getUUID());
                // 只有当皮肤纹理可用时才渲染，否则跳过渲染以避免显示原始皮肤
                // Only render when skin texture is available, otherwise skip to avoid showing original skin
                if (skinTexture == null) {
                    // 皮肤纹理尚未加载完成，跳过渲染以避免双重显示
                    // Skin texture not loaded yet, skip rendering to avoid double display
                    return;
                }

                boolean isSlimModel = ClientDisguiseCache.isSlimSkinModel(player.getUUID());
                M normalModel = this.getParentModel();

                // 在创建伪装模型之前就隐藏原始模型
                // Hide original model before creating disguise model
                if (normalModel instanceof PlayerModel) {
                    hideOriginalModelParts(player, (PlayerModel<?>) normalModel);
//                    RotPYellowTemperanceAddon.LOGGER.debug("[YT RENDER] Hidden original model BEFORE creating disguise model");
                }

                // 创建伪装模型并完全同步所有状态
                // Create disguise model and fully synchronize all states
                DisguiseModel<PlayerEntity> skinModel = new DisguiseModel<>(0.0F, isSlimModel);

/*                RotPYellowTemperanceAddon.LOGGER.debug("[YT RENDER DEBUG] Creating disguise model for {} with isSlim: {}",
                        player.getName().getString(), isSlimModel);*/

                if (normalModel instanceof PlayerModel) {
                    PlayerModel<?> playerModel = (PlayerModel<?>) normalModel;

                    // 使用新的渲染控制器预处理模型可见性
                    // Use new render controller to preprocess model visibility
                    com.TheChaYe.rotp_yellowtemperance.client.PlayerRenderController.preprocessPlayerModel(player, playerModel);

                    // 根据伪装目标调整玩家模型的手臂类型（如果这是玩家本人）
                    // Adjust player model's arm type based on disguise target (if this is the player themselves)
                    if (player == net.minecraft.client.Minecraft.getInstance().player) {
                        try {
                            java.lang.reflect.Field slimField = PlayerModel.class.getDeclaredField("slim");
                            slimField.setAccessible(true);
                            slimField.setBoolean(playerModel, isSlimModel);
                        } catch (Exception e) {
                            // 如果无法设置字段，静默处理
                            // If unable to set field, handle silently
                        }
                    }

                    skinModel.copyPropertiesFrom(playerModel);

                    // 确保手臂类型正确设置，使粗手臂玩家伪装细手臂角色时能正确显示
                    // Ensure arm type is correctly set so thick-arm players can properly display when disguising as slim-arm characters
                    try {
                        // 直接通过手臂位置检测并调整，避免反射字段
                        boolean currentIsSlim = isSlimModel(playerModel);
                        if (currentIsSlim != isSlimModel) {
                            adjustArmPositions(skinModel, isSlimModel);
                        }
                    } catch (Exception e) {
                        RotPYellowTemperanceAddon.LOGGER.warn("Failed to adjust arm positions for disguise model", e);
                    }
                }

                // 确保完全同步动画状态 / Ensure complete animation state synchronization
                skinModel.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

                // 同步额外的动画属性 / Synchronize additional animation properties
                if (normalModel instanceof BipedModel) {
                    BipedModel<?> bipedModel = normalModel;
                    skinModel.rightArmPose = bipedModel.rightArmPose;
                    skinModel.leftArmPose = bipedModel.leftArmPose;
                }

                // 渲染伪装模型（包含影子） / Render disguise model (including shadow)
                skinModel.renderToBuffer(
                        matrixStack,
                        buffer.getBuffer(RenderType.entityCutoutNoCull(skinTexture)),
                        packedLight,
                        LivingRenderer.getOverlayCoords(player, 0),
                        1.0F, 1.0F, 1.0F, 1.0F
                );

//                RotPYellowTemperanceAddon.LOGGER.debug("[YT RENDER] Rendered disguise model for player: {}", player.getName().getString());
            } catch (Exception e) {
//                RotPYellowTemperanceAddon.LOGGER.error("Error rendering disguise layer for player {}: {}",
//                        player.getName().getString(), e.getMessage(), e);
            }
        } else {
            // 没有伪装时恢复原始模型可见性 / Restore original model visibility when no disguise
            if (this.getParentModel() instanceof PlayerModel) {
                restoreOriginalModelParts(player, (PlayerModel<?>) this.getParentModel());
            }
        }
    }
}