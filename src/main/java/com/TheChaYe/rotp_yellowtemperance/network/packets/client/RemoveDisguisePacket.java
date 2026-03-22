package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import com.TheChaYe.rotp_yellowtemperance.client.DisguiseRenderHandler;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.util.DisguiseManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 移除伪装网络包 / Remove Disguise Network Packet
 * 用于在服务端和客户端之间同步伪装状态的清除
 */
public class RemoveDisguisePacket {

    /**
     * 构造函数 / Constructor
     * 创建移除伪装包实例
     */
    public RemoveDisguisePacket() {
    }

    /**
     * 编码网络包 / Encode network packet
     * 移除伪装包不需要编码数据
     *
     * @param msg    要编码的消息 / Message to encode
     * @param buffer 网络缓冲区 / Network buffer
     */
    public static void encode(RemoveDisguisePacket msg, PacketBuffer buffer) {
        // 不需要编码任何数据 / No data needs to be encoded
    }

    /**
     * 解码网络包 / Decode network packet
     * 创建新的移除伪装包实例
     *
     * @param buffer 网络缓冲区 / Network buffer
     * @return 解码后的移除伪装包 / Decoded remove disguise packet
     */
    public static RemoveDisguisePacket decode(PacketBuffer buffer) {
        return new RemoveDisguisePacket();
    }

    /**
     * 处理网络包 / Handle network packet
     * 根据接收方向处理伪装移除逻辑
     *
     * @param msg 接收到的消息 / Received message
     * @param ctx 网络上下文 / Network context
     */
    public static void handle(RemoveDisguisePacket msg, Supplier<NetworkEvent.Context> ctx) {
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
    private static void handleClient(RemoveDisguisePacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            clearClientDisguiseData(mc.player);
            RotPYellowTemperanceAddon.LOGGER.debug("Client cleared disguise data for local player");
        }
    }

    private static void handleServer(RemoveDisguisePacket msg, PlayerEntity sender) {
        if (sender != null) {
            DisguiseManager.removeDisguise(sender);
            RotPYellowTemperanceAddon.LOGGER.debug("Server removed disguise for player: {}", sender.getName().getString());
        }
    }

    /**
     * 清除客户端的伪装数据 / Clear client disguise data
     * 包括缓存、渲染状态和NBT标签 / Including cache, render state and NBT tags
     *
     * @param player 目标玩家 / Target player
     */
    private static void clearClientDisguiseData(PlayerEntity player) {
        // 清除客户端伪装缓存 / Clear client disguise cache
        ClientDisguiseCache.clearPlayerDisguise(player.getUUID());

        // 清除渲染处理器中的伪装状态 / Clear disguise state in render handler
        DisguiseRenderHandler.CURRENT_DISGUISE_TYPES.remove(player.getUUID());
        DisguiseRenderHandler.PERSISTENT_DISGUISE_ENTITIES.remove(player.getUUID());

        // 清除玩家的伪装NBT标签 / Clear player's disguise NBT tags
        player.getPersistentData().remove(InitTags.YT_HAS_DISGUISE_TAG);
        player.getPersistentData().remove(InitTags.YT_DISGUISE_NAME_TAG);
        player.getPersistentData().remove("disguiseProfile");
        player.getPersistentData().remove(InitTags.YT_DISGUISE_UUID_TAG);

        // 强制刷新玩家渲染 / Force refresh player rendering

    }
}
