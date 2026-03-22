package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 玩家伪装网络包 / Player Disguise Network Packet
 * 处理玩家伪装成指定实体类型的网络通信
 */
public class DisguisePlayerPacket {
    /**
     * 实体类型资源定位符 / Entity type resource location
     */
    private final ResourceLocation entityType; // 保持为 ResourceLocation
    /**
     * 玩家ID / Player ID
     */
    private final int playerId;

    /**
     * 构造函数 / Constructor
     *
     * @param entityType 实体类型资源定位符 / Entity type resource location
     * @param playerId   玩家ID / Player ID
     */
    public DisguisePlayerPacket(ResourceLocation entityType, int playerId) {
        this.entityType = entityType;
        this.playerId = playerId;
    }

    /**
     * 编码网络包 / Encode network packet
     * 将伪装包数据写入网络缓冲区
     *
     * @param msg 要编码的消息 / Message to encode
     * @param buf 网络缓冲区 / Network buffer
     */
    public static void encode(DisguisePlayerPacket msg, PacketBuffer buf) {
        buf.writeResourceLocation(msg.entityType);
        buf.writeInt(msg.playerId);
    }

    /**
     * 解码网络包 / Decode network packet
     * 从网络缓冲区读取伪装包数据
     *
     * @param buf 网络缓冲区 / Network buffer
     * @return 解码后的伪装包 / Decoded disguise packet
     */
    public static DisguisePlayerPacket decode(PacketBuffer buf) {
        ResourceLocation entityType = buf.readResourceLocation();
        int playerId = buf.readInt();
        return new DisguisePlayerPacket(entityType, playerId);
    }

    /**
     * 处理网络包 / Handle network packet
     * 处理接收到的伪装包消息
     *
     * @param msg 接收到的消息 / Received message
     * @param ctx 网络上下文 / Network context
     */
    public static void handle(DisguisePlayerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
        });
        ctx.get().setPacketHandled(true);
    }
}