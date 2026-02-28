package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.TheChaYe.rotp_yellowtemperance.capability.IDisguiseCapability;
import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncDisguisePacket;
import com.TheChaYe.rotp_yellowtemperance.util.DisguiseManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 应用伪装网络包 / Apply Disguise Network Packet
 * 客户端发送伪装请求到服务端的数据包
 */
public class ApplyDisguisePacket {
    /** 伪装名称 / Disguise name */
    private final String disguiseName;

    /**
     * 构造函数 / Constructor
     * @param disguiseName 要应用的伪装名称 / Disguise name to apply
     */
    public ApplyDisguisePacket(String disguiseName) {
        this.disguiseName = disguiseName;
    }

    /**
     * 编码网络包 / Encode network packet
     * 将伪装包数据写入网络缓冲区
     * @param msg 要编码的消息 / Message to encode
     * @param buffer 网络缓冲区 / Network buffer
     */
    public static void encode(ApplyDisguisePacket msg, PacketBuffer buffer) {
        buffer.writeUtf(msg.disguiseName);
    }

    /**
     * 解码网络包 / Decode network packet
     * 从网络缓冲区读取伪装包数据
     * @param buffer 网络缓冲区 / Network buffer
     * @return 解码后的伪装包 / Decoded disguise packet
     */
    public static ApplyDisguisePacket decode(PacketBuffer buffer) {
        return new ApplyDisguisePacket(buffer.readUtf(32767));
    }

    /**
     * 处理网络包 / Handle network packet
     * 服务端处理客户端发送的伪装请求
     * @param msg 接收到的消息 / Received message
     * @param ctx 网络上下文 / Network context
     */
    public static void handle(ApplyDisguisePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() != null) {
                ServerPlayerEntity player = ctx.get().getSender();
                
                // 使用util包中的DisguiseManager处理玩家伪装 / Use DisguiseManager in util package to handle player disguise
                com.TheChaYe.rotp_yellowtemperance.util.DisguiseManager.applyDisguise(player, msg.disguiseName);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}