package com.TheChaYe.rotp_yellowtemperance.network;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.*;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import javax.annotation.Nullable;

/**
 * 数据包处理器 / Packet Handler
 * 管理网络数据包的注册和分发
 */
public class PacketHandler {
    /**
     * 协议版本号 / Protocol version
     */
    private static final String PROTOCOL_VERSION = "1";
    /**
     * 主网络通道 / Main network channel
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    /**
     * 数据包 ID 计数器 / Packet ID counter
     */
    private static int id = 0;

    /**
     * 注册所有网络数据包 / Register all network packets
     * 注册客户端到服务端和服务端到客户端的所有数据包类型
     */
    public static void registerPackets() {
        // 客户端到服务端包 / Client to server packets
        CHANNEL.registerMessage(id++, ApplyDisguisePacket.class, ApplyDisguisePacket::encode, ApplyDisguisePacket::decode, ApplyDisguisePacket::handle);
        CHANNEL.registerMessage(id++, RemoveDisguisePacket.class, RemoveDisguisePacket::encode, RemoveDisguisePacket::decode, RemoveDisguisePacket::handle);
        CHANNEL.registerMessage(id++, QuitNBTRenderInfoPacket.class, QuitNBTRenderInfoPacket::encode, QuitNBTRenderInfoPacket::decode, QuitNBTRenderInfoPacket::handle);
        CHANNEL.registerMessage(id++, SetNBTRenderInfoPacket.class, SetNBTRenderInfoPacket::encode, SetNBTRenderInfoPacket::decode, SetNBTRenderInfoPacket::handle);
        CHANNEL.registerMessage(id++, DisguisePlayerPacket.class, DisguisePlayerPacket::encode, DisguisePlayerPacket::decode, DisguisePlayerPacket::handle);
        CHANNEL.registerMessage(id++, ClientSyncNbtPacket.class, ClientSyncNbtPacket::encode, ClientSyncNbtPacket::decode, ClientSyncNbtPacket::handle);
        CHANNEL.registerMessage(id++, OnPlayerDisguisedPacket.class, OnPlayerDisguisedPacket::encode, OnPlayerDisguisedPacket::decode, OnPlayerDisguisedPacket::handle);
        CHANNEL.registerMessage(id++, SyncSearchHelperPacket.class, SyncSearchHelperPacket::encode, SyncSearchHelperPacket::decode, SyncSearchHelperPacket::handle);

        // 服务端到客户端包 / Server to client packets
        CHANNEL.registerMessage(id++, SyncDisguisePacket.class, SyncDisguisePacket::encode, SyncDisguisePacket::decode, SyncDisguisePacket::handle);
        CHANNEL.registerMessage(id++, SyncNbtPacket.class, SyncNbtPacket::encode, SyncNbtPacket::decode, SyncNbtPacket::handle);
        CHANNEL.registerMessage(id++, SyncNbtToClientPacket.class, SyncNbtToClientPacket::encode, SyncNbtToClientPacket::decode, SyncNbtToClientPacket::handle);
        CHANNEL.registerMessage(id++, SyncErosionLevelPacket.class, SyncErosionLevelPacket::encode, SyncErosionLevelPacket::decode, SyncErosionLevelPacket::handle);
        CHANNEL.registerMessage(id++, SyncErosionActivePacket.class, SyncErosionActivePacket::encode, SyncErosionActivePacket::decode, SyncErosionActivePacket::handle);
        CHANNEL.registerMessage(id++, SyncDollRenderingPacket.class, SyncDollRenderingPacket::encode, SyncDollRenderingPacket::decode, SyncDollRenderingPacket::handle);
        CHANNEL.registerMessage(id++, SyncPlayerDisguisePacket.class, SyncPlayerDisguisePacket::encode, SyncPlayerDisguisePacket::decode, SyncPlayerDisguisePacket::handle);
        CHANNEL.registerMessage(id++, SyncSearchHelperToClientPacket.class, SyncSearchHelperToClientPacket::encode, SyncSearchHelperToClientPacket::decode, SyncSearchHelperToClientPacket::handle);

        // YellowTemperanceRenderPacket (服务端 -> 客户端) / YellowTemperanceRenderPacket (Server -> Client)
        CHANNEL.registerMessage(
                id++,
                YellowTemperanceRenderPacket.class,
                (packet, buffer) -> new YellowTemperanceRenderPacket.Handler().encode(packet, buffer),
                buffer -> new YellowTemperanceRenderPacket.Handler().decode(buffer),
                (packet, ctx) -> new YellowTemperanceRenderPacket.Handler().handle(packet, () -> ctx.get())
        );

        // StandDisguiseSetPacket (客户端 -> 服务端) / StandDisguiseSetPacket (Client -> Server)
        CHANNEL.registerMessage(
                id++,
                StandDisguiseSetPacket.class,
                (packet, buffer) -> new StandDisguiseSetPacket.Handler().encode(packet, buffer),
                buffer -> new StandDisguiseSetPacket.Handler().decode(buffer),
                (packet, ctx) -> new StandDisguiseSetPacket.Handler().handle(packet, () -> ctx.get())
        );

        // ResetSyncedCommonConfigPacket (客户端 -> 服务端) / ResetSyncedCommonConfigPacket (Client -> Server)
        CHANNEL.registerMessage(
                id++,
                ResetSyncedCommonConfigPacket.class,
                (packet, buffer) -> new ResetSyncedCommonConfigPacket.Handler().encode(packet, buffer),
                buffer -> new ResetSyncedCommonConfigPacket.Handler().decode(buffer),
                (packet, ctx) -> new ResetSyncedCommonConfigPacket.Handler().handle(packet, () -> ctx.get())
        );
    }

    /**
     * 发送数据包到指定客户端 / Send packet to specified client
     * 通过玩家连接发送数据包到客户端
     *
     * @param packet 要发送的数据包 / Packet to send
     * @param player 目标玩家 / Target player
     */
    public static void sendToClient(Object packet, ServerPlayerEntity player) {
        CHANNEL.sendTo(packet, player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
    }

    /**
     * 发送数据包到服务端 / Send packet to server
     * 通过客户端通道发送数据包到服务端
     *
     * @param msg 要发送的消息 / Message to send
     */
    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    /**
     * 发送数据包给追踪实体的所有玩家 / Send packet to all players tracking entity
     * 包括追踪该实体的玩家和实体自身
     *
     * @param packet 要发送的数据包 / Packet to send
     * @param player 目标玩家 / Target player
     */
    public static void sendToAllTracking(Object packet, ServerPlayerEntity player) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    /**
     * 发送数据包给追踪实体及自身的所有客户端 / Send packet to all clients tracking entity and self
     * 向追踪指定实体的所有客户端以及实体自身发送数据包
     *
     * @param packet 要发送的数据包 / Packet to send
     * @param entity 目标实体 / Target entity
     */
    public static void sendToClientsTrackingAndSelf(Object packet, Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    /**
     * 广播数据包给所有客户端 / Broadcast packet to all clients
     * 向服务器上的所有客户端广播数据包
     *
     * @param packet 要广播的数据包 / Packet to broadcast
     */
    public static void sendToAllClients(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    /**
     * 全局发送数据包 / Send packet globally
     * 根据维度向所有客户端或指定维度发送数据包
     *
     * @param msg       要发送的消息 / Message to send
     * @param dimension 目标维度，null 表示所有维度 / Target dimension, null for all dimensions
     */
    public static void sendGlobally(Object msg, @Nullable RegistryKey<World> dimension) {
        if (dimension != null) {
            CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), msg);
        } else {
            CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
        }
    }
}