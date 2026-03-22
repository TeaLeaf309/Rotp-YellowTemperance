package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.network.ModNetwork;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.RemoveDisguisePacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家退出事件处理器 / Player Logout Event Handler
 * 在玩家退出游戏时清除伪装NBT数据
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID)
public class PlayerLogoutEventHandler {

    /**
     * 玩家退出事件处理 / Player logout event handler
     * 清除玩家的所有伪装相关NBT数据，防止数据残留
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEntity player = event.getPlayer();

        // 只在服务端处理 / Only process on server side
        if (player.level.isClientSide() || !(player instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        RotPYellowTemperanceAddon.LOGGER.info("Player {} is logging out, clearing disguise data", player.getName().getString());

        // 方法1: 通过能力系统清除伪装数据 / Method 1: Clear disguise data through capability system
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(disguiseCap -> {
            // 重置伪装状态 / Reset disguise state
            disguiseCap.setHasDisguise(false);
            disguiseCap.setDisguiseName("");
            disguiseCap.setDisguiseProfile(null);
            disguiseCap.setDisguiseUUID(null);

            RotPYellowTemperanceAddon.LOGGER.debug("Cleared disguise capability data for player: {}", player.getName().getString());
        });

        // 方法2: 清除持久化NBT数据 / Method 2: Clear persistent NBT data
        player.getPersistentData().remove("YT_HAS_DISGUISE");
        player.getPersistentData().remove("YT_DISGUISE_NAME");
        player.getPersistentData().remove("disguiseProfile");
        player.getPersistentData().remove("YT_DISGUISE_UUID");

        RotPYellowTemperanceAddon.LOGGER.debug("Cleared persistent NBT data for player: {}", player.getName().getString());

        // 方法3: 发送移除伪装数据包到客户端 / Method 3: Send remove disguise packet to client
        ModNetwork.sendToClient(new RemoveDisguisePacket(), serverPlayer);

        RotPYellowTemperanceAddon.LOGGER.info("Successfully cleared all disguise data for player: {}", player.getName().getString());
    }
}