package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.network.ModNetwork;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.QuitNBTRenderInfoPacket;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;

/**
 * 玩家伪装工具类 / Player Disguise Utility
 * 提供玩家间伪装功能的实用方法和数据同步
 * Provides utility methods and data synchronization for player-to-player disguise functionality
 */
public class PlayerDisguiseUtil {

    /**
     * 设置玩家间伪装 / Set Player-to-Player Disguise
     * 将目标玩家伪装为另一个玩家的外观
     * @param player 需要伪装的玩家 / Player to be disguised
     * @param targetPlayer 伪装目标玩家 / Target player for disguise
     */
    public static void setPlayerDisguise(PlayerEntity player, PlayerEntity targetPlayer) {
        CompoundNBT nbt = player.getPersistentData();

        // 创建包含完整皮肤数据的GameProfile / Create GameProfile with complete skin data
        GameProfile profile = targetPlayer.getGameProfile();
        CompoundNBT profileTag = new CompoundNBT();
        NBTUtil.writeGameProfile(profileTag, profile);

        // 写入玩家NBT（使用统一的键名）/ Write to player NBT (using unified key names)
        nbt.putString(InitTags.YT_DISGUISE_NAME_TAG, targetPlayer.getName().getString());
        nbt.put("disguiseProfile", profileTag);
        nbt.putUUID(InitTags.YT_DISGUISE_UUID_TAG, targetPlayer.getUUID());

        // 同步到客户端 / Synchronize to client
        CompoundNBT syncData = new CompoundNBT();
        syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, targetPlayer.getName().getString());
        syncData.put("disguiseProfile", profileTag);
        syncData.putUUID(InitTags.YT_DISGUISE_UUID_TAG, targetPlayer.getUUID());

        ModNetwork.sendNbtSyncToClient(player, syncData);
    }

    /**
     * 清除玩家伪装 / Clear Player Disguise
     * 清除指定玩家的伪装状态并同步到服务端
     * @param player 目标玩家 / Target player
     */
    public static void clearPlayerDisguise(PlayerEntity player) {
        CompoundNBT nbt = player.getPersistentData();
        nbt.remove(InitTags.YT_DISGUISE_NAME_TAG);
        nbt.remove("disguiseProfile");
        nbt.remove(InitTags.YT_DISGUISE_UUID_TAG);

        // 发送清除指令到服务端 / Send clear command to server
        ModNetwork.sendToServer(new QuitNBTRenderInfoPacket(
                player.getUUID(),
                InitTags.YT_DISGUISE_NAME_TAG,
                "disguiseProfile",
                InitTags.YT_DISGUISE_UUID_TAG
        ));
    }
}