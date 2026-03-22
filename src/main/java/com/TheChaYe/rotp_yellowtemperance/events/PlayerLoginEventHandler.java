package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.server.DisguiseManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/**
 * 玩家登录事件处理器 / Player Login Event Handler
 * 处理新玩家加入世界时的伪装状态同步
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID)
public class PlayerLoginEventHandler {

    /**
     * 玩家登录事件处理 / Player login event handler
     * 向新登录的玩家同步在线玩家的伪装数据
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();

        // 只在服务端处理 / Only process on server side
        if (player.level.isClientSide() || !(player instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity newPlayer = (ServerPlayerEntity) player;
        MinecraftServer server = newPlayer.getServer();

        if (server == null) {
            RotPYellowTemperanceAddon.LOGGER.warn("Server is null when handling player login for {}", player.getName().getString());
            return;
        }

        RotPYellowTemperanceAddon.LOGGER.info("Player {} logged in, sending disguise sync data", player.getName().getString());

        // 遍历所有在线玩家（不包括新玩家自己） / Iterate through all online players (excluding new player themselves)
        for (ServerPlayerEntity onlinePlayer : server.getPlayerList().getPlayers()) {
            if (onlinePlayer == newPlayer) continue;

            // 使用 server.DisguiseManager 的 hasDisguise 检查 / Use server.DisguiseManager's hasDisguise check
            if (!DisguiseManager.hasDisguise(onlinePlayer)) continue;

            // 发送单点同步包 / Send point-to-point sync packet
            DisguiseManager.sendDisguiseDataToPlayer(onlinePlayer, newPlayer);
            RotPYellowTemperanceAddon.LOGGER.debug("Sent disguise data from {} to new player {}",
                    onlinePlayer.getName().getString(), newPlayer.getName().getString());
        }
    }

    /**
     * 开始追踪事件处理 / Start tracking event handler
     * 当玩家开始追踪另一个实体时触发，确保伪装数据正确同步
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        // 只在服务端处理 / Only process on server side
        if (event.getPlayer().level.isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        if (!(event.getTarget() instanceof PlayerEntity)) return;

        ServerPlayerEntity trackingPlayer = (ServerPlayerEntity) event.getPlayer();
        PlayerEntity trackedPlayer = (PlayerEntity) event.getTarget();

        // 检查被追踪的玩家是否有伪装 / Check if tracked player has disguise
        if (DisguiseManager.hasDisguise(trackedPlayer)) {
            DisguiseManager.sendDisguiseDataToPlayer(trackedPlayer, trackingPlayer);
            RotPYellowTemperanceAddon.LOGGER.debug("Sent disguise data from {} to tracking player {} via StartTracking",
                    trackedPlayer.getName().getString(), trackingPlayer.getName().getString());
        }
    }


}