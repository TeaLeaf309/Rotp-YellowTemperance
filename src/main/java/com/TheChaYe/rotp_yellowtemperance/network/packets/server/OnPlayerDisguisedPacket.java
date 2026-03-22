package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.action.Disguise.Disguise;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 玩家伪装事件网络包 / Player Disguised Event Network Packet
 * 处理玩家完成伪装时的服务端通知
 */
public class OnPlayerDisguisedPacket {
    /**
     * 玩家UUID / Player UUID
     */
    private final UUID playerUUID;
    /**
     * 伪装类型资源定位符 / Disguise type resource location
     */
    private final ResourceLocation disguiseType;

    /**
     * 构造函数 / Constructor
     *
     * @param playerUUID   玩家UUID / Player UUID
     * @param disguiseType 伪装类型资源定位符 / Disguise type resource location
     */
    public OnPlayerDisguisedPacket(UUID playerUUID, ResourceLocation disguiseType) {
        this.playerUUID = playerUUID;
        this.disguiseType = disguiseType;
    }

    /**
     * 构造函数重载 / Constructor overload
     *
     * @param playerUUID 玩家UUID / Player UUID
     */
    public OnPlayerDisguisedPacket(UUID playerUUID) {
        this(playerUUID, null);
    }

    /**
     * 编码网络包 / Encode network packet
     * 将玩家伪装事件包数据写入网络缓冲区
     *
     * @param msg 要编码的消息 / Message to encode
     * @param buf 网络缓冲区 / Network buffer
     */
    public static void encode(OnPlayerDisguisedPacket msg, PacketBuffer buf) {
        buf.writeUUID(msg.playerUUID);
        buf.writeBoolean(msg.disguiseType != null);
        if (msg.disguiseType != null) {
            buf.writeResourceLocation(msg.disguiseType);
        }
    }

    /**
     * 解码网络包 / Decode network packet
     * 从网络缓冲区读取玩家伪装事件包数据
     *
     * @param buf 网络缓冲区 / Network buffer
     * @return 解码后的玩家伪装事件包 / Decoded player disguised event packet
     */
    public static OnPlayerDisguisedPacket decode(PacketBuffer buf) {
        UUID uuid = buf.readUUID();
        ResourceLocation disguiseType = null;
        boolean hasDisguiseType = buf.readBoolean();
        if (hasDisguiseType) {
            disguiseType = buf.readResourceLocation();
        }
        return new OnPlayerDisguisedPacket(uuid, disguiseType);
    }

    /**
     * 处理网络包 / Handle network packet
     * 服务端处理玩家伪装完成事件
     *
     * @param msg 接收到的消息 / Received message
     * @param ctx 网络上下文 / Network context
     */
    public static void handle(OnPlayerDisguisedPacket msg, Supplier<NetworkEvent.Context> ctx) {
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
    private static void handleClient(OnPlayerDisguisedPacket msg) {
        // 客户端逻辑为空，因为此包仅在服务端处理
    }

    private static void handleServer(OnPlayerDisguisedPacket msg, ServerPlayerEntity sender) {
        if (sender != null && sender.getUUID().equals(msg.playerUUID)) {
            if (msg.disguiseType != null) {
                EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(msg.disguiseType);
                if (entityType != null) {
                    // 这里需要获取到对应的Stand实体 / Need to get corresponding Stand entity here
                    // 由于我们没有直接引用，暂时留空或者需要通过其他方式获取 / Since we don't have direct reference, leave empty or need to get through other means
                }
            }
            // 获取玩家的替身实体 / Get player's stand entity
            IStandPower standPower = IStandPower.getStandPowerOptional(sender).orElse(null);
            StandEntity stand = null;
            if (standPower != null) {
                if (standPower.getStandManifestation() instanceof StandEntity) {
                    stand = (StandEntity) standPower.getStandManifestation();
                }
            }
            // 直接调用服务端处理方法 / Directly call server-side processing method
            Disguise.onPlayerDisguisedServer(sender, stand);
        }
    }
}