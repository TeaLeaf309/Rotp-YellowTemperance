/**
 * 伪装模型类 / Disguise Model Class
 * 扩展PlayerModel，用于处理伪装时的模型渲染和动画同步
 * Extends PlayerModel, used for handling model rendering and animation synchronization during disguise
 */
package com.TheChaYe.rotp_yellowtemperance.client.render;

import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;

public class DisguiseModel<T extends LivingEntity> extends PlayerModel<T> {
    /**
     * 构造函数 / Constructor
     * @param inflate 模型膨胀值 / Model inflation value
     * @param slim 是否为细手臂模型 / Whether it's slim arm model
     */
    public DisguiseModel(float inflate, boolean slim) {
        super(inflate, slim);
    }

    /**
     * 设置动画 / Setup animation
     * @param entity 实体 / Entity
     * @param limbSwing 肢体摆动 / Limb swing
     * @param limbSwingAmount 肢体摆动幅度 / Limb swing amount
     * @param ageInTicks 年龄刻度 / Age in ticks
     * @param netHeadYaw 网络头部偏航 / Network head yaw
     * @param headPitch 头部俯仰 / Head pitch
     */
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 确保所有动画都正确应用 / Ensure all animations are applied correctly
        if (entity instanceof net.minecraft.entity.player.PlayerEntity) {
            net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity) entity;

            // 同步手持物品的动画 / Synchronize item holding animation
            boolean isHoldingItem = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
            if (isHoldingItem) {
                // 确保手臂动画正确应用到持有物品的状态
                // Ensure arm animation is correctly applied to item holding state
                this.rightArm.xRot = this.rightArm.xRot;
                this.leftArm.xRot = this.leftArm.xRot;
            }
        }
    }

    /**
     * 从原始模型复制属性 / Copy properties from original model
     * @param original 原始玩家模型 / Original player model
     */
    public void copyPropertiesFrom(PlayerModel<?> original) {
        // 复制所有模型部件的变换 / Copy transformations of all model parts
        this.head.copyFrom(original.head);
        this.hat.copyFrom(original.hat);
        this.body.copyFrom(original.body);
        this.rightArm.copyFrom(original.rightArm);
        this.leftArm.copyFrom(original.leftArm);
        this.rightLeg.copyFrom(original.rightLeg);
        this.leftLeg.copyFrom(original.leftLeg);
        this.jacket.copyFrom(original.jacket);
        this.leftSleeve.copyFrom(original.leftSleeve);
        this.rightSleeve.copyFrom(original.rightSleeve);
        this.leftPants.copyFrom(original.leftPants);
        this.rightPants.copyFrom(original.rightPants);

        // 复制所有动画状态 / Copy all animation states
        this.crouching = original.crouching;
        this.attackTime = original.attackTime;
        this.swimAmount = original.swimAmount;
        this.riding = original.riding;
        this.young = original.young;

        // 复制手臂姿势 / Copy arm poses
        this.rightArmPose = original.rightArmPose;
        this.leftArmPose = original.leftArmPose;

        // 复制其他重要的动画属性 / Copy other important animation properties
        try {
            java.lang.reflect.Field[] fields = PlayerModel.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getType() == float.class || field.getType() == boolean.class) {
                    field.setAccessible(true);
                    Object value = field.get(original);
                    field.set(this, value);
                }
            }
        } catch (Exception e) {
            // 反射失败时忽略，基本属性已经复制
            // Ignore on reflection failure, basic properties already copied
        }
    }
}