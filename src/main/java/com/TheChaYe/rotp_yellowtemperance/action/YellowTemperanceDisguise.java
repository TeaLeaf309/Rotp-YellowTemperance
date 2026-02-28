/**
 * 黄色节制伪装能力 / Yellow Temperance Disguise Ability
 * 处理替身的伪装功能，允许玩家伪装成其他实体
 * Handles the stand's disguise functionality, allowing players to disguise as other entities
 */
package com.TheChaYe.rotp_yellowtemperance.action;

import com.TheChaYe.rotp_yellowtemperance.client.ui.FormChoiceUI;
import com.TheChaYe.rotp_yellowtemperance.entity.YellowTemperanceEntity;
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
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class YellowTemperanceDisguise extends StandEntityAction {
    /**
     * 构造函数 / Constructor
     *
     * @param builder 动作构建器 / Action builder
     */
    public YellowTemperanceDisguise(Builder builder) {
        super(builder);
    }

    /**
     * 当玩家完成伪装时调用的事件 / Event called when player completes disguise
     *
     * @param player 伪装的玩家 / Disguised player
     * @param stand  玩家的替身 / Player's stand
     */
    public static void onPlayerDisguised(PlayerEntity player, StandEntity stand) {
        // 处理伪装完成后的逻辑 / Handle logic after disguise completion

        // 根据客户端/服务端环境处理 / Handle based on client/server environment
        if (player.level.isClientSide) {
            // 客户端发送包到服务端 / Client sends packet to server
            PacketHandler.CHANNEL.sendToServer(new OnPlayerDisguisedPacket(player.getUUID()));
        } else {
            // 服务端直接执行 / Server executes directly
            onPlayerDisguisedServer((ServerPlayerEntity) player, stand);
        }
    }

    /**
     * 在服务端执行的伪装完成事件 / Disguise completion event executed on server
     *
     * @param player 伪装的玩家 / Disguised player
     * @param stand  玩家的替身 / Player's stand
     */
    public static void onPlayerDisguisedServer(ServerPlayerEntity player, StandEntity stand) {
        // 更新玩家物理属性 / Update player physics properties
        // 延迟执行确保状态同步 / Delay execution to ensure state synchronization
        player.getServer().execute(() -> {
            com.TheChaYe.rotp_yellowtemperance.server.ServerDisguiseHandler.updatePlayerPhysicsOnServer(player);
        });

        // 广播给跟踪玩家的客户端 / Broadcast to clients tracking the player
        PacketHandler.sendToAllTracking(new OnPlayerDisguisedPacket(player.getUUID()), player);
    }

    /**
     * 执行替身动作的主要逻辑 / Main logic for executing stand action
     * 如果已经处于伪装状态，则取消伪装；否则打开伪装选择界面
     * If already disguised, cancel disguise; otherwise open disguise selection UI
     *
     * @param world     当前世界对象 / Current world object
     * @param stand     执行动作的替身实体 / Stand entity executing the action
     * @param userPower 使用替身的能力 / Power using the stand
     * @param task      替身任务对象 / Stand task object
     */
    @Override
    public void standPerform(World world, StandEntity stand, IStandPower userPower, StandEntityTask task) {
        // 获取YellowTemperance实体 / Get YellowTemperance entity
        final YellowTemperanceEntity yellowTemperance = (YellowTemperanceEntity) stand;
        LivingEntity livingEntity = stand.getUser();
        if (livingEntity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) livingEntity;

            if (world.isClientSide()) {
                FormChoiceUI.openUI(Minecraft.getInstance());
            }
        }
    }
}
    /**
     * 客户端和服务端共同执行的逻辑 / Logic executed by both client and server
     * @param player 伪装的玩家 / Disguised player
     * @param stand 玩家的替身实体 / Player's stand entity
     */