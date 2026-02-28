package com.TheChaYe.rotp_yellowtemperance.client.render;

import com.TheChaYe.rotp_yellowtemperance.entity.YellowTemperanceEntity;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.client.render.entity.model.stand.HumanoidStandModel;
import com.github.standobyte.jojo.client.render.entity.pose.IModelPose;
import com.github.standobyte.jojo.client.render.entity.pose.ModelPose;
import com.github.standobyte.jojo.client.render.entity.pose.RotationAngle;
import com.github.standobyte.jojo.client.render.entity.pose.anim.PosedActionAnimation;
import com.github.standobyte.jojo.entity.stand.StandPose;
import net.minecraft.client.renderer.model.ModelRenderer;

/**
 * 黄色节制替身模型 / Yellow Temperance Stand Model
 * 处理黄色节制替身的3D模型渲染和动画姿态
 */
public class YellowTemperanceModel extends HumanoidStandModel<YellowTemperanceEntity> {
    private ModelRenderer pickaxe;

    public YellowTemperanceModel() {
        super();
    }

    /**
     * 初始化召唤姿态旋转角度 / Initialize summon pose rotations
     * 定义替身召唤时的初始姿态动画
     */
    @Override
    protected RotationAngle[][] initSummonPoseRotations() {
        return new RotationAngle[][]{
                new RotationAngle[]{

                },
                new RotationAngle[]{

                }
        };
    }

    /**
     * 初始化动作姿态 / Initialize action poses
     * 设置替身各种动作的动画姿态，如远程攻击等
     */
    @Override
    protected void initActionPoses() {
        actionAnim.put(StandPose.RANGED_ATTACK, new PosedActionAnimation.Builder<YellowTemperanceEntity>()
                .addPose(StandEntityAction.Phase.BUTTON_HOLD, new ModelPose<>(new RotationAngle[]{
                        new RotationAngle(body, 0.0F, -0.48F, 0.0F),
                        new RotationAngle(leftArm, 0.0F, 0.0F, -0.7854F),
                        new RotationAngle(leftForeArm, 0.0F, 0.0F, 0.6109F),
                        new RotationAngle(rightArm, -1.0908F, 0.0F, 1.5708F),
                        new RotationAngle(rightForeArm, 0.0F, 0.0F, 0.0F)
                }))
                .build(idlePose));

        super.initActionPoses();
    }

    /**
     * 准备实体模型 / Prepare mob model
     * 在每帧渲染前更新模型状态和动画参数
     */
    @Override
    public void prepareMobModel(YellowTemperanceEntity entity, float walkAnimPos, float walkAnimSpeed, float partialTick) {
        super.prepareMobModel(entity, walkAnimPos, walkAnimSpeed, partialTick);

    }


    /**
     * 初始化待机姿态 / Initialize idle pose
     * 定义替身静止状态下的默认姿态
     */
    @Override
    protected ModelPose<YellowTemperanceEntity> initIdlePose() {
        return super.initIdlePose();
    }

    /**
     * 初始化循环待机姿态 / Initialize idle pose loop
     * 定义替身待机状态的循环动画姿态
     */
    @Override
    protected IModelPose<YellowTemperanceEntity> initIdlePose2Loop() {
        return super.initIdlePose2Loop();
    }
}