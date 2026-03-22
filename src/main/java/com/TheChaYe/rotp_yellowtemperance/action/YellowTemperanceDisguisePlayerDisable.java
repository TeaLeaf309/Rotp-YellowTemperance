package com.TheChaYe.rotp_yellowtemperance.action;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.RemoveDisguisePacket;
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
public class YellowTemperanceDisguisePlayerDisable extends StandEntityAction {
    
    /**
     * 构造函数 / Constructor
     * @param builder 动作构建器 / Action builder
     */
    public YellowTemperanceDisguisePlayerDisable(Builder builder) {
        super(builder);
    }

    /**
     * 执行取消玩家伪装动作 / Execute disable player disguise action
     * @param world 当前世界 / Current world
     * @param stand 替身实体 / Stand entity
     * @param userPower 用户替身能力 / User stand power
     * @param task 替身任务 / Stand task
     */
    @Override
    public void standPerform(World world, StandEntity stand, IStandPower userPower, StandEntityTask task) {
        LivingEntity user = userPower.getUser();
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            CompoundNBT nbt = player.getPersistentData();

            // 取消玩家伪装状态 / Cancel player disguise state
            if (nbt.contains(InitTags.YT_HAS_DISGUISE_TAG)) {
//                RotPYellowTemperanceAddon.LOGGER.debug("Removing disguise for player: {}", player.getName().getString());
                PacketHandler.CHANNEL.sendToServer(new RemoveDisguisePacket());
                nbt.remove(InitTags.YT_HAS_DISGUISE_TAG);
                nbt.remove(InitTags.YT_DISGUISE_NAME_TAG);
            }
        }
    }
}