package com.TheChaYe.rotp_yellowtemperance.action.Disguise;

import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

/**
 * 黄色节制取消玩家伪装能力 / Yellow Temperance Disable Player Disguise Ability
 * 取消玩家的玩家伪装状态
 * Cancels the player's player disguise state
 */
public class DisguisePlayerDisable extends StandEntityAction {

    /**
     * 构造函数 / Constructor
     *
     * @param builder 动作构建器 / Action builder
     */
    public DisguisePlayerDisable(Builder builder) {
        super(builder);
    }

    /**
     * 执行取消玩家伪装动作 / Execute disable player disguise action
     *
     * @param world     当前世界 / Current world
     * @param stand     替身实体 / Stand entity
     * @param userPower 用户替身能力 / User stand power
     * @param task      替身任务 / Stand task
     */
    @Override
    public void standPerform(World world, StandEntity stand, IStandPower userPower, StandEntityTask task) {
        LivingEntity user = userPower.getUser();
        if (!(stand instanceof AbstractDisguiseStandEntity)) return;
        AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity) stand;
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            CompoundNBT nbt = player.getPersistentData();
            // 取消玩家伪装状态 / Cancel player disguise state
            if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG)) {
                disguiseStand.clearDisguisePlayer();
            }
        }
    }
}