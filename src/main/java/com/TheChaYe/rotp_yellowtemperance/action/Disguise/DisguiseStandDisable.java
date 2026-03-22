package com.TheChaYe.rotp_yellowtemperance.action.Disguise;

import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * 黄色节制取消伪装能力 / Yellow Temperance Disable Disguise Ability
 * 取消玩家当前的伪装状态
 * Cancels the player's current disguise state
 */
public class DisguiseStandDisable extends StandEntityAction {

    /**
     * 构造函数 / Constructor
     *
     * @param builder 动作构建器 / Action builder
     */
    public DisguiseStandDisable(Builder builder) {
        super(builder);
    }

    /**
     * 执行取消伪装动作 / Execute disable disguise action
     *
     * @param world     当前世界 / Current world
     * @param stand     替身实体 / Stand entity
     * @param userPower 用户替身能力 / User stand power
     * @param task      替身任务 / Stand task
     */
    @Override
    public void standPerform(World world, StandEntity stand, IStandPower userPower, StandEntityTask task) {
        if (!(stand instanceof AbstractDisguiseStandEntity)) return;
        AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity)
                stand;
        PlayerEntity player = disguiseStand.getUser() instanceof PlayerEntity ?
                (PlayerEntity) disguiseStand.getUser() : null;
        if (player == null) return;
        disguiseStand.clearDisguisesStand();
    }
}