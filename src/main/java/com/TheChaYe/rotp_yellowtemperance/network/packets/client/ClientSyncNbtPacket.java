package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 客户端NBT同步网络包 / Client NBT Sync Network Packet
 * 处理客户端和服务端之间的NBT数据同步
 */
public class ClientSyncNbtPacket {
    /**
     * 玩家UUID / Player UUID
     */
    private final UUID playerId;
    /**
     * NBT数据 / NBT data
     */
    private final CompoundNBT nbtData;

    /**
     * 构造函数 / Constructor
     *
     * @param playerId 玩家UUID / Player UUID
     * @param nbtData  要同步的NBT数据 / NBT data to sync
     */
    public ClientSyncNbtPacket(UUID playerId, CompoundNBT nbtData) {
        this.playerId = playerId;
        this.nbtData = nbtData;
    }

    /**
     * 编码网络包 / Encode network packet
     * 将NBT同步包数据写入网络缓冲区
     *
     * @param msg    要编码的消息 / Message to encode
     * @param buffer 网络缓冲区 / Network buffer
     */
    public static void encode(ClientSyncNbtPacket msg, PacketBuffer buffer) {
        buffer.writeUUID(msg.playerId);
        buffer.writeNbt(msg.nbtData);
    }

    /**
     * 解码网络包 / Decode network packet
     * 从网络缓冲区读取NBT同步包数据
     *
     * @param buffer 网络缓冲区 / Network buffer
     * @return 解码后的NBT同步包 / Decoded NBT sync packet
     */
    public static ClientSyncNbtPacket decode(PacketBuffer buffer) {
        return new ClientSyncNbtPacket(buffer.readUUID(), buffer.readNbt());
    }

    /**
     * 处理网络包 / Handle network packet
     * 根据接收方向处理NBT数据同步逻辑
     *
     * @param msg 接收到的消息 / Received message
     * @param ctx 网络上下文 / Network context
     */
    public static void handle(ClientSyncNbtPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            } else {
                handleServer(msg, ctx.get().getSender());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ClientSyncNbtPacket msg) {
        PlayerEntity clientPlayer = Minecraft.getInstance().level.getPlayerByUUID(msg.playerId);
        if (clientPlayer != null) {
            // 检查是否需要清理伪装 / Check if disguise cleanup is needed
            if (msg.nbtData.contains("clearDisguise") && msg.nbtData.getBoolean("clearDisguise")) {
                // 清理客户端缓存 / Clear client cache
                com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache.clearPlayerDisguise(msg.playerId);

                // 清理NBT数据 / Clear NBT data
                clientPlayer.getPersistentData().remove(InitTags.YT_HAS_DISGUISE_TAG);
                clientPlayer.getPersistentData().remove(InitTags.YT_DISGUISE_NAME_TAG);

                RotPYellowTemperanceAddon.LOGGER.debug("Cleared player disguise for {}", msg.playerId);
                return;
            }

            // 正常的伪装同步 / Normal disguise synchronization
            if (msg.nbtData.contains(InitTags.YT_DISGUISE_NAME_TAG)) {
                // 更新NBT数据 / Update NBT data
                clientPlayer.getPersistentData().putString(
                        InitTags.YT_DISGUISE_NAME_TAG,
                        msg.nbtData.getString(InitTags.YT_DISGUISE_NAME_TAG)
                );
                clientPlayer.getPersistentData().putBoolean(
                        InitTags.YT_HAS_DISGUISE_TAG,
                        msg.nbtData.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)
                );

                // 同步profile信息到客户端缓存 / Sync profile information to client cache
                if (msg.nbtData.contains("disguiseProfile")) {
                    com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache.cachePlayerDisguiseProfile(
                            msg.playerId,
                            msg.nbtData.getCompound("disguiseProfile")
                    );
                }

                RotPYellowTemperanceAddon.LOGGER.debug("Synced player disguise with profile data");
            }
        }
    }

    private static void handleServer(ClientSyncNbtPacket msg, ServerPlayerEntity sender) {
        if (sender != null && sender.getUUID().equals(msg.playerId)) {
            // 处理客户端发送的伪装加载完成通知 / Handle disguise loading completion notification from client
            if (msg.nbtData.contains(InitTags.YT_DISGUISE_LOADING_TAG)) {
                boolean isLoading = msg.nbtData.getBoolean(InitTags.YT_DISGUISE_LOADING_TAG);
                sender.getPersistentData().putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, isLoading);

                // 广播给其他玩家该玩家的伪装加载状态已更新 / Broadcast to other players that this player's disguise loading status has been updated
                net.minecraft.nbt.CompoundNBT syncData = new net.minecraft.nbt.CompoundNBT();
                syncData.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, isLoading);
                syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, sender.getPersistentData().getBoolean(InitTags.YT_HAS_DISGUISE_TAG));

                com.TheChaYe.rotp_yellowtemperance.network.ModNetwork.sendToAllTracking(
                        new com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncNbtToClientPacket(sender.getUUID(), syncData),
                        sender
                );

                RotPYellowTemperanceAddon.LOGGER.debug("Updated disguise loading status for player {}: {}",
                        sender.getName().getString(), isLoading ? "loading" : "complete");
            }
        }
    }
}