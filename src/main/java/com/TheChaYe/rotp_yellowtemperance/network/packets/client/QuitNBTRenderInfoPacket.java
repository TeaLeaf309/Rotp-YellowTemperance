package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 退出NBT渲染信息网络包 / Quit NBT Render Info Network Packet
 * 处理玩家退出时需要移除的NBT标签信息
 */
public class QuitNBTRenderInfoPacket {
    /**
     * 玩家UUID / Player UUID
     */
    private final UUID playerId;
    /**
     * 需要移除的标签集合 / Set of tags to remove
     */
    private final Set<String> tagsToRemove;

    /**
     * 构造函数 / Constructor
     *
     * @param playerId 玩家UUID / Player UUID
     * @param tags     要移除的标签数组 / Array of tags to remove
     */
    public QuitNBTRenderInfoPacket(UUID playerId, String... tags) {
        this.playerId = playerId;
        this.tagsToRemove = new HashSet<>();
        Collections.addAll(this.tagsToRemove, tags);
    }

    /**
     * 编码网络包 / Encode network packet
     * 将退出NBT渲染信息包数据写入网络缓冲区
     *
     * @param msg    要编码的消息 / Message to encode
     * @param buffer 网络缓冲区 / Network buffer
     */
    public static void encode(QuitNBTRenderInfoPacket msg, PacketBuffer buffer) {
        buffer.writeUUID(msg.playerId);
        buffer.writeInt(msg.tagsToRemove.size());
        for (String tag : msg.tagsToRemove) {
            buffer.writeUtf(tag);
        }
    }

    /**
     * 解码网络包 / Decode network packet
     * 从网络缓冲区读取退出NBT渲染信息包数据
     *
     * @param buffer 网络缓冲区 / Network buffer
     * @return 解码后的退出NBT渲染信息包 / Decoded quit NBT render info packet
     */
    public static QuitNBTRenderInfoPacket decode(PacketBuffer buffer) {
        UUID playerId = buffer.readUUID();
        int count = buffer.readInt();
        Set<String> tags = new HashSet<>();
        for (int i = 0; i < count; i++) {
            tags.add(buffer.readUtf());
        }
        return new QuitNBTRenderInfoPacket(playerId, tags.toArray(new String[0]));
    }

    /**
     * 处理网络包 / Handle network packet
     * 处理玩家退出时的NBT标签清理逻辑
     *
     * @param msg 接收到的消息 / Received message
     * @param ctx 网络上下文 / Network context
     */
    public static void handle(QuitNBTRenderInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Context context = ctx.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                handleServerSide(msg, ctx);
            }
        });
        context.setPacketHandled(true);
    }

    /**
     * 服务端处理逻辑 / Server side handling logic
     * 移除指定玩家的NBT标签并清理相关缓存
     *
     * @param msg 网络包消息 / Network packet message
     * @param ctx 网络上下文 / Network context
     */
    private static void handleServerSide(QuitNBTRenderInfoPacket msg, Supplier<Context> ctx) {
        ServerPlayerEntity sender = ctx.get().getSender();
        if (sender == null || sender.level == null) return;

        PlayerEntity player = sender.level.getPlayerByUUID(msg.playerId);
        if (player != null) {
            CompoundNBT persistentData = player.getPersistentData();

            // 移除指定标签 / Remove specified tags
            for (String tag : msg.tagsToRemove) {
                persistentData.remove(tag);

                // 特殊处理伪装标签（支持新旧键名） / Special handling for disguise tags (supporting old and new key names)
                if (tag.equals(InitTags.YT_DISGUISE_UUID_TAG) ||
                        tag.equals("disguiseProfile") ||
                        tag.equals(InitTags.YT_DISGUISE_PROFILE_TAG) ||
                        tag.equals(InitTags.YT_HAS_DISGUISE_TAG)) {

                    // 清理对应缓存 / Clear corresponding cache
                    if (persistentData.contains(InitTags.YT_DISGUISE_UUID_TAG)) {
                        UUID disguiseId = persistentData.getUUID(InitTags.YT_DISGUISE_UUID_TAG);

                    }
                }
            }
        }
    }
}