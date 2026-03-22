package com.TheChaYe.rotp_yellowtemperance.action;

import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * 黄金体验镇魂曲切换纸娃娃渲染能力 / Yellow Temperance Toggle Doll Rendering Ability
 * 切换玩家的纸娃娃渲染状态
 * Toggles the player's doll rendering state
 */
public class YellowTemperanceToggleDoll extends StandEntityAction {

    /**
     * 构造函数 / Constructor
     *
     * @param builder 动作构建器 / Action builder
     */
    public YellowTemperanceToggleDoll(Builder builder) {
        super(builder);
    }

    /**
     * 执行切换纸娃娃渲染动作 / Execute toggle doll rendering action
     *
     * @param world       当前世界 / Current world
     * @param standEntity 替身实体 / Stand entity
     * @param userPower   用户替身能力 / User stand power
     * @param task        替身任务 / Stand task
     */
    @Override
    public void standPerform(World world, StandEntity standEntity, IStandPower userPower, StandEntityTask task) {
        if (!world.isClientSide) {
            LivingEntity user = userPower.getUser();
            if (user instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) user;

                // 使用Capability管理系统 / Use Capability management system
                player.getCapability(CapabilityHandler.LIVING_DATA_CAPABILITY).ifPresent(cap -> {
                    // 切换纸娃娃渲染状态 / Toggle doll rendering state
                    boolean isDollRendering = cap.isDollRendering();
                    cap.setDollRendering(!isDollRendering);

                    // 向玩家发送状态消息 / Send status message to player
                    IFormattableTextComponent message = new TranslationTextComponent(
                            !isDollRendering ?
                                    "action.rotp_yellowtemperance.toggle_doll.on" :
                                    "action.rotp_yellowtemperance.toggle_doll.off"
                    );
                    player.displayClientMessage(message, true);
                });
            }
        }
    }
}