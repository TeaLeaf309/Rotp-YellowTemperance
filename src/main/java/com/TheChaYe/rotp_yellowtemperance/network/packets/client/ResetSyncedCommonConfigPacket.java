package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.github.standobyte.jojo.network.packets.IModPacketHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 重置同步通用配置网络包 / Reset Synced Common Config Network Packet
 * 用于重置客户端和服务端同步的通用配置
 */
public class ResetSyncedCommonConfigPacket {
    /**
     * 构造函数 / Constructor
     * 创建重置同步配置包实例
     */
    public ResetSyncedCommonConfigPacket() {
    }

    /**
     * 数据包处理器 / Packet handler
     * 实现IModPacketHandler接口处理配置重置包
     */
    public static class Handler implements IModPacketHandler<ResetSyncedCommonConfigPacket> {
        public Handler() {
        }

        /**
         * 编码配置重置包 / Encode config reset packet
         * 重置包不需要编码数据
         *
         * @param msg 要编码的消息 / Message to encode
         * @param buf 网络缓冲区 / Network buffer
         */
        public void encode(ResetSyncedCommonConfigPacket msg, PacketBuffer buf) {
            // 重置包不需要编码数据 / Reset packet doesn't need to encode data
        }

        /**
         * 解码配置重置包 / Decode config reset packet
         *
         * @param buf 网络缓冲区 / Network buffer
         * @return 解码后的配置重置包 / Decoded config reset packet
         */
        public ResetSyncedCommonConfigPacket decode(PacketBuffer buf) {
            return new ResetSyncedCommonConfigPacket();
        }

        /**
         * 处理配置重置包 / Handle config reset packet
         * 重置同步配置值
         *
         * @param msg 接收到的消息 / Received message
         * @param ctx 网络上下文 / Network context
         */
        public void handle(ResetSyncedCommonConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        }

        /**
         * 获取数据包类 / Get packet class
         *
         * @return 配置重置包类 / Config reset packet class
         */
        public Class<ResetSyncedCommonConfigPacket> getPacketClass() {
            return ResetSyncedCommonConfigPacket.class;
        }
    }
}