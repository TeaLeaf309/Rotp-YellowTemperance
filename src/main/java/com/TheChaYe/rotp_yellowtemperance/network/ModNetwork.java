package com.TheChaYe.rotp_yellowtemperance.network;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.ClientSyncNbtPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.QuitNBTRenderInfoPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.SetNBTRenderInfoPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.SyncOtherPlayerPhysicsPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.BatchDisguiseSyncPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncPlayerPhysicsPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * 模组网络管理器 / Mod Network Manager
 * 管理黄色节制模组的网络通信和数据包注册
 */
public class ModNetwork {
    /** 日志记录器 / Logger */
    private static final Logger LOGGER = LogManager.getLogger();
    /** 协议版本号 / Protocol version */
    private static final String PROTOCOL_VERSION = "1";
    /** 网络通道实例 / Network channel instance */
    public static SimpleChannel INSTANCE;
    /** 数据包ID计数器 / Packet ID counter */
    private static final int packetId = 0;
    /** 网络是否已初始化 / Whether network is initialized */
    private static boolean isInitialized = false;
    /** 网络是否正在初始化 / Whether network is initializing */
    private static boolean isInitializing = false;

    /**
     * 注册网络数据包 / Register network packets
     * 注册所有网络通信所需的数据包类型
     */
    public static void registerPackets() {
        int id = 0;

        INSTANCE.registerMessage(id++, SetNBTRenderInfoPacket.class,
                SetNBTRenderInfoPacket::encode,
                SetNBTRenderInfoPacket::decode,
                SetNBTRenderInfoPacket::handle);

        INSTANCE.registerMessage(id++, QuitNBTRenderInfoPacket.class,
                QuitNBTRenderInfoPacket::encode,
                QuitNBTRenderInfoPacket::decode,
                QuitNBTRenderInfoPacket::handle);

        // 添加物理属性同步包 / Add physics property sync packet
        INSTANCE.registerMessage(id++, SyncPlayerPhysicsPacket.class,
                SyncPlayerPhysicsPacket::encode,
                SyncPlayerPhysicsPacket::decode,
                SyncPlayerPhysicsPacket::handle);

        // 添加其他玩家物理属性同步包 / Add other player physics property sync packet
        INSTANCE.registerMessage(id++, SyncOtherPlayerPhysicsPacket.class,
                SyncOtherPlayerPhysicsPacket::encode,
                SyncOtherPlayerPhysicsPacket::decode,
                SyncOtherPlayerPhysicsPacket::handle);

        // SyncNbtToClientPacket 已在 PacketHandler 中注册，此处不重复注册 / SyncNbtToClientPacket already registered in PacketHandler, no duplicate registration here
        
        // 注册批量伪装同步包 / Register batch disguise sync packet
        INSTANCE.registerMessage(id++, BatchDisguiseSyncPacket.class,
                BatchDisguiseSyncPacket::encode,
                BatchDisguiseSyncPacket::decode,
                BatchDisguiseSyncPacket::handle);
    }

    /**
     * 初始化网络系统 / Initialize network system
     * 在模组设置阶段初始化网络通道和注册数据包
     * @param event FML通用设置事件 / FML common setup event
     */
    public static void init(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            isInitializing = true;
            INSTANCE = NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(RotPYellowTemperanceAddon.MOD_ID, "main"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );
            registerPackets();
            isInitialized = true;
            isInitializing = false;
            LOGGER.info("Yellow Temperance network initialized successfully");
        });
    }

    /**
     * 发送数据包给追踪实体的所有玩家 / Send packet to all players tracking entity
     * 包括追踪该实体的玩家和实体自身
     * @param packet 要发送的数据包 / Packet to send
     * @param player 目标玩家 / Target player
     */
    public static void sendToAllTracking(Object packet, ServerPlayerEntity player) {
        if (!isInitialized) {
//            LOGGER.error("Attempted to send packet before network initialization: {}", packet);
            return;
        }
        if (INSTANCE == null) {
//            LOGGER.error("Network channel is null while sending packet: {}", packet);
            return;
        }
        if (player == null) {
//            LOGGER.error("Attempted to send packet to null player: {}", packet);
            return;
        }

        INSTANCE.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                packet
        );
    }

    /**
     * 发送数据包到服务端 / Send packet to server
     * 客户端向服务端发送网络数据包
     * @param packet 要发送的数据包 / Packet to send
     */
    public static void sendToServer(Object packet) {
        if (isInitializing) {
            LOGGER.debug("Network is initializing, queuing packet: {}", packet.getClass().getSimpleName());
            // 网络正在初始化，暂时不发送 / Network is initializing, temporarily not sending
            return;
        }
        if (!isInitialized) {
//            LOGGER.error("Attempted to send packet before network initialization: {}", packet);
            return;
        }
        if (INSTANCE == null) {
//            LOGGER.error("Network channel is null while sending packet: {}", packet);
            return;
        }
        INSTANCE.sendToServer(packet);
    }

    /**
     * 发送数据包到指定客户端 / Send packet to specified client
     * 服务端向特定客户端玩家发送数据包
     * @param packet 要发送的数据包 / Packet to send
     * @param player 目标客户端玩家 / Target client player
     */
    public static void sendToClient(Object packet, ServerPlayerEntity player) {
        if (!isInitialized) {
//           LOGGER.error("Attempted to send client packet before network init: {} to {}", packet, player);
            return;
        }
        if (INSTANCE == null) {
            LOGGER.error("Network channel null for client packet: {} to {}", packet, player);
            return;
        }
        if (player == null) {
//            LOGGER.error("Attempted to send packet to null player: {}", packet);
            return;
        }
        if (player.connection == null) {
            LOGGER.warn("Player connection is null for {}", player.getScoreboardName());
            return;
        }
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * 发送NBT同步数据到客户端 / Send NBT sync data to client
     * 向客户端同步玩家的NBT数据
     * @param player 目标玩家 / Target player
     * @param nbtData 要同步的NBT数据 / NBT data to sync
     */
    public static void sendNbtSyncToClient(PlayerEntity player, CompoundNBT nbtData) {
        if (player instanceof ServerPlayerEntity) {
            sendToClient(new ClientSyncNbtPacket(player.getUUID(), nbtData), (ServerPlayerEntity) player);
        } else {
            LOGGER.warn("Attempted to sync NBT to non-server player: {}", player);
        }
    }

    /**
     * 发送玩家物理属性到服务端 / Send player physics properties to server
     * 客户端向服务端同步玩家的物理属性数据
     * @param playerUUID 玩家UUID / Player UUID
     * @param eyeHeightScale 眼高缩放因子 / Eye height scale factor
     * @param hitboxWidthScale 碰撞箱宽度缩放因子 / Hitbox width scale factor
     * @param hitboxHeightScale 碰撞箱高度缩放因子 / Hitbox height scale factor
     */
    public static void sendPlayerPhysicsToServer(UUID playerUUID, float eyeHeightScale, float hitboxWidthScale, float hitboxHeightScale) {
        sendToServer(new SyncPlayerPhysicsPacket(playerUUID, eyeHeightScale, hitboxWidthScale, hitboxHeightScale));
    }
}