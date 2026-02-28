// 文档2: SyncDisguisePacket.java
package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 伪装同步网络包 / Disguise Sync Network Packet
 * 服务端向客户端同步单个玩家的伪装数据
 */
public class SyncDisguisePacket {
    /** 玩家UUID / Player UUID */
    private final UUID playerId;
    /** 伪装名称 / Disguise name */
    private final String disguiseName;
    /** 伪装配置文件标签 / Disguise profile tag */
    private final CompoundNBT profileTag;

    /**
     * 构造函数 / Constructor
     * @param playerId 玩家UUID / Player UUID
     * @param disguiseName 伪装名称 / Disguise name
     * @param profileTag 伪装配置文件标签 / Disguise profile tag
     */
    public SyncDisguisePacket(UUID playerId, String disguiseName, CompoundNBT profileTag) {
        this.playerId = playerId;
        this.disguiseName = disguiseName;
        this.profileTag = profileTag;
    }

    /**
     * 编码网络包 / Encode network packet
     * 将伪装同步包数据写入网络缓冲区
     * @param msg 要编码的消息 / Message to encode
     * @param buffer 网络缓冲区 / Network buffer
     */
    public static void encode(SyncDisguisePacket msg, PacketBuffer buffer) {
        buffer.writeUUID(msg.playerId);
        buffer.writeUtf(msg.disguiseName);
        buffer.writeNbt(msg.profileTag);
    }

    /**
     * 解码网络包 / Decode network packet
     * 从网络缓冲区读取伪装同步包数据
     * @param buffer 网络缓冲区 / Network buffer
     * @return 解码后的伪装同步包 / Decoded disguise sync packet
     */
    public static SyncDisguisePacket decode(PacketBuffer buffer) {
        return new SyncDisguisePacket(
                buffer.readUUID(),
                buffer.readUtf(32767),
                buffer.readNbt()
        );
    }

    /**
     * 处理网络包 / Handle network packet
     * 客户端处理服务端发送的伪装同步数据
     * @param msg 接收到的消息 / Received message
     * @param ctx 网络上下文 / Network context
     */
    public static void handle(SyncDisguisePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 缓存完整的伪装数据 / Cache complete disguise data
            ClientDisguiseCache.cachePlayerDisguiseProfile(msg.playerId, msg.profileTag);
        });
        ctx.get().setPacketHandled(true);
    }
}