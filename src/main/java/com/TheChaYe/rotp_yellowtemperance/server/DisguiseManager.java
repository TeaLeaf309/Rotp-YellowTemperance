package com.TheChaYe.rotp_yellowtemperance.server;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.network.ModNetwork;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.QuitNBTRenderInfoPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncNbtToClientPacket;
import com.TheChaYe.rotp_yellowtemperance.util.GameProfileSerializer;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

/**
 * 服务端伪装管理器 / Server Disguise Manager
 * 处理服务端的伪装逻辑和数据同步
 */
public class DisguiseManager {

    /**
     * 应用伪装 / Apply disguise
     * 为玩家应用指定的伪装配置
     *
     * @param player          玩家实体 / Player entity
     * @param disguiseProfile 伪装的游戏档案 / Disguise game profile
     */
    public static void applyDisguise(PlayerEntity player, GameProfile disguiseProfile) {
        CompoundNBT persistentData = player.getPersistentData();

        // 生成唯一ID作为缓存键 (1.16.5兼容版本) / Generate unique ID as cache key (1.16.5 compatible version)
        UUID disguiseId = UUID.randomUUID();

        // 序列化GameProfile / Serialize GameProfile
        CompoundNBT profileTag = GameProfileSerializer.serialize(disguiseProfile);

        // 保存数据到玩家NBT（使用与util.DisguiseManager一致的键名） / Save data to player NBT (using consistent key names with util.DisguiseManager)
        persistentData.put("disguiseProfile", profileTag);
        persistentData.putString(InitTags.YT_DISGUISE_UUID_TAG, disguiseId.toString());
        persistentData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);

        // 添加伪装状态标记，用于控制渲染时机 / Add disguise status flag to control rendering timing
        persistentData.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, true);

        // 同步到客户端 / Sync to client
        CompoundNBT syncData = new CompoundNBT();
        syncData.put("disguiseProfile", profileTag);
        syncData.putString(InitTags.YT_DISGUISE_UUID_TAG, disguiseId.toString());
        syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
        syncData.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, true);

        RotPYellowTemperanceAddon.LOGGER.debug("Applying disguise for player: {}", player.getName().getString());
        // 发送给所有玩家（包含自己） / Send to all players (including self)
        sendSyncPacket(player, syncData);
    }

    /**
     * 移除伪装 / Remove disguise
     * 移除玩家的伪装状态
     *
     * @param player 玩家实体 / Player entity
     */
    public static void removeDisguise(PlayerEntity player) {
        CompoundNBT persistentData = player.getPersistentData();

        // 清理缓存 / Clear cache
        if (persistentData.contains(InitTags.YT_DISGUISE_UUID_TAG)) {
            UUID disguiseId = UUID.fromString(persistentData.getString(InitTags.YT_DISGUISE_UUID_TAG));

        }

        // 移除伪装标记 / Remove disguise flags
        persistentData.remove(InitTags.YT_DISGUISE_UUID_TAG);
        persistentData.remove("disguiseProfile");
        persistentData.remove(InitTags.YT_HAS_DISGUISE_TAG);
        persistentData.remove(InitTags.YT_DISGUISE_LOADING_TAG);

        RotPYellowTemperanceAddon.LOGGER.debug("Removing disguise for player: {}", player.getName().getString());
        // 发送移除命令到客户端 / Send removal command to client
        CompoundNBT syncData = new CompoundNBT();
        syncData.putBoolean(InitTags.YT_REMOVE_DISGUISE, true);
        sendSyncPacket(player, syncData);
    }

    /**
     * 发送同步数据包 / Send sync packet
     * 向客户端发送伪装同步数据
     *
     * @param player   目标玩家 / Target player
     * @param syncData 同步数据 / Sync data
     */
    private static void sendSyncPacket(PlayerEntity player, CompoundNBT syncData) {
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            // 发送给自己 / Send to self
            ModNetwork.sendToClient(
                    new SyncNbtToClientPacket(player.getUUID(), syncData),
                    serverPlayer
            );

            // 发送给所有在线玩家（全局广播） / Send to all online players (global broadcast)
            if (serverPlayer.getServer() != null) {
                for (ServerPlayerEntity onlinePlayer : serverPlayer.getServer().getPlayerList().getPlayers()) {
                    // 不重复发送给自己 / Don't send to self again
                    if (onlinePlayer != serverPlayer) {
                        ModNetwork.sendToClient(
                                new SyncNbtToClientPacket(player.getUUID(), syncData),
                                onlinePlayer
                        );
                    }
                }
            }
        }
    }

    /**
     * 处理玩家登出 / Handle player logout
     * 当玩家登出时发送清理命令
     *
     * @param player 登出的玩家 / Logging out player
     */
    public static void handlePlayerLogout(ServerPlayerEntity player) {
        player.getPersistentData().remove(InitTags.YT_HAS_DISGUISE_TAG);
        player.getPersistentData().remove(InitTags.YT_DISGUISE_LOADING_TAG);
        QuitNBTRenderInfoPacket cleanupPacket = new QuitNBTRenderInfoPacket(
                player.getUUID(),
                "disguiseProfile",
                InitTags.YT_DISGUISE_UUID_TAG
        );
        ModNetwork.sendToClient(cleanupPacket, player);
    }

    /**
     * 检查伪装状态 / Check disguise status
     * 检查玩家是否有伪装
     *
     * @param player 目标玩家 / Target player
     * @return 是否有伪装 / Whether has disguise
     */
    public static boolean hasDisguise(PlayerEntity player) {
        // 检查NBT标记 / Check NBT flags
        CompoundNBT persistentData = player.getPersistentData();
        if (persistentData.contains(InitTags.YT_HAS_DISGUISE_TAG) &&
                persistentData.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
            return true;
        }

        // 检查是否包含伪装profile数据 / Check if contains disguise profile data
        if (persistentData.contains("disguiseProfile") ||
                persistentData.contains(InitTags.YT_DISGUISE_PROFILE_TAG)) {
            return true;
        }

        // 检查Capability / Check Capability
        return player.getCapability(InitCapabilities.DISGUISE_CAPABILITY)
                .map(cap -> cap.hasDisguise())
                .orElse(false);
    }

    /**
     * 发送伪装数据给玩家 / Send disguise data to player
     * 将指定玩家的伪装数据发送给目标玩家
     *
     * @param disguisedPlayer 有伪装的玩家 / Disguised player
     * @param targetPlayer    目标玩家（接收数据的玩家） / Target player (recipient)
     */
    public static void sendDisguiseDataToPlayer(PlayerEntity disguisedPlayer, ServerPlayerEntity targetPlayer) {
        CompoundNBT persistentData = disguisedPlayer.getPersistentData();
        if (!persistentData.contains(InitTags.YT_HAS_DISGUISE_TAG) ||
                !persistentData.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
            return;
        }

        CompoundNBT syncData = new CompoundNBT();
        syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);

        boolean hasProfile = persistentData.contains("disguiseProfile");
        if (hasProfile) {
            syncData.put("disguiseProfile", persistentData.getCompound("disguiseProfile"));
            syncData.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, false); // 已完成 / Completed
        } else {
            // 没有 profile，说明异步未完成，标记为加载中 / No profile means async not completed, mark as loading
            syncData.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, true);
        }

        if (persistentData.contains(InitTags.YT_DISGUISE_UUID_TAG)) {
            syncData.putString(InitTags.YT_DISGUISE_UUID_TAG, persistentData.getString(InitTags.YT_DISGUISE_UUID_TAG));
        }
        if (persistentData.contains(InitTags.YT_DISGUISE_NAME_TAG)) {
            syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, persistentData.getString(InitTags.YT_DISGUISE_NAME_TAG));
        }

        // 使用 PacketHandler 发送，确保包被客户端正确处理 / Use PacketHandler to send, ensuring packet is handled correctly by client
        PacketHandler.sendToClient(new SyncNbtToClientPacket(disguisedPlayer.getUUID(), syncData), targetPlayer);
        RotPYellowTemperanceAddon.LOGGER.debug("Sent disguise data from {} to {}",
                disguisedPlayer.getName().getString(), targetPlayer.getName().getString());
    }
}