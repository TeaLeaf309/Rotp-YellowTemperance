package com.TheChaYe.rotp_yellowtemperance.action.Disguise;

import com.TheChaYe.rotp_yellowtemperance.client.ui.DisguiseConfigGUI;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.YellowTemperanceEntity;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.OnPlayerDisguisedPacket;
import com.github.standobyte.jojo.action.stand.StandEntityAction;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 黄色节制玩家伪装能力 / Yellow Temperance Player Disguise Ability
 * 允许玩家伪装成其他玩家
 * Allows players to disguise as other players
 */
public class DisguisePlayer extends StandEntityAction {

    /**
     * 构造函数 / Constructor
     *
     * @param builder 动作构建器 / Action builder
     */
    public DisguisePlayer(Builder builder) {
        super(builder);
    }

    /**
     * 执行玩家伪装动作 / Execute player disguise action
     *
     * @param world     当前世界 / Current world
     * @param stand     替身实体 / Stand entity
     * @param userPower 用户替身能力 / User stand power
     * @param task      替身任务 / Stand task
     */
    @Override
    public void standPerform(World world, StandEntity stand, IStandPower userPower, StandEntityTask task) {
        LivingEntity user = userPower.getUser();
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            //黄色节制在饱食度为零时无法使用此技能
            int foodLevel = player.getFoodData().getFoodLevel();
            if (stand instanceof YellowTemperanceEntity && foodLevel <= 0) {
                // 向玩家发送状态消息 / Send status message to player
                IFormattableTextComponent message = new TranslationTextComponent(
                        "action.rotp_yellowtemperance.no_food"
                );
                player.displayClientMessage(message, true);
                return;
            }
            if (world.isClientSide()) {
                // 只在本地玩家上打开配置界面 / Open config GUI only for local player
                if (player == Minecraft.getInstance().player) {
                    openConfigGui(player);
                }
            } else {
                // 服务端广播伪装效果 / Broadcast disguise effect on server
                if (player instanceof ServerPlayerEntity) {
                    PacketHandler.sendToAllTracking(new OnPlayerDisguisedPacket(player.getUUID()), (ServerPlayerEntity) player);
                }
            }
        }
    }

    /**
     * 在客户端打开伪装配置界面 / Open disguise config GUI on client
     *
     * @param player 目标玩家 / Target player
     */
    @OnlyIn(Dist.CLIENT)
    private void openConfigGui(PlayerEntity player) {
        Minecraft.getInstance().setScreen(new DisguiseConfigGUI(player));
    }
}