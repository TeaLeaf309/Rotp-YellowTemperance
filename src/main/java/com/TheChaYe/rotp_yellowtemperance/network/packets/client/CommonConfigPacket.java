package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.github.standobyte.jojo.network.packets.IModPacketHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 通用配置网络包 / Common Config Network Packet
 * 处理客户端和服务端之间的配置同步
 */
public class CommonConfigPacket {
    /** 同步配置值 / Synced configuration values */
    private final YellowTemperanceConfig.Common.SyncedValues values;

    /**
     * 构造函数 / Constructor
     * @param values 要同步的配置值 / Configuration values to sync
     */
    public CommonConfigPacket(YellowTemperanceConfig.Common.SyncedValues values) {
        this.values = values;
    }

    /**
     * 数据包处理器 / Packet handler
     * 实现IModPacketHandler接口处理配置包
     */
    public static class Handler implements IModPacketHandler<CommonConfigPacket> {
        public Handler() {
        }

        /**
         * 编码配置包 / Encode config packet
         * @param msg 要编码的消息 / Message to encode
         * @param buf 网络缓冲区 / Network buffer
         */
        public void encode(CommonConfigPacket msg, PacketBuffer buf) {
            msg.values.writeToBuf(buf);
        }

        /**
         * 解码配置包 / Decode config packet
         * @param buf 网络缓冲区 / Network buffer
         * @return 解码后的配置包 / Decoded config packet
         */
        public CommonConfigPacket decode(PacketBuffer buf) {
            return new CommonConfigPacket(new YellowTemperanceConfig.Common.SyncedValues(buf));
        }

        /**
         * 处理配置包 / Handle config packet
         * @param msg 接收到的消息 / Received message
         * @param ctx 网络上下文 / Network context
         */
        public void handle(CommonConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
            msg.values.changeConfigValues();
        }

        /**
         * 获取数据包类 / Get packet class
         * @return 配置包类 / Config packet class
         */
        public Class<CommonConfigPacket> getPacketClass() {
            return CommonConfigPacket.class;
        }
    }
}