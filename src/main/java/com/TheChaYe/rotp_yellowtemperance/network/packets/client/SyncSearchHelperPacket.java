package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncSearchHelperToClientPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.function.Supplier;

public class SyncSearchHelperPacket {
    private final boolean searchHelperEnabled;

    public SyncSearchHelperPacket(boolean searchHelperEnabled) {
        this.searchHelperEnabled = searchHelperEnabled;
    }

    public static void encode(SyncSearchHelperPacket msg, PacketBuffer buf) {
        buf.writeBoolean(msg.searchHelperEnabled);
    }

    public static SyncSearchHelperPacket decode(PacketBuffer buf) {
        return new SyncSearchHelperPacket(buf.readBoolean());
    }

    public static void handle(SyncSearchHelperPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                // 更新发送者的Capability
                player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
                    cap.setSearchHelperEnabled(msg.searchHelperEnabled);
                });

                // 广播给所有在线玩家
                broadcastSearchHelperState(player, msg.searchHelperEnabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 广播搜索辅助状态给所有在线玩家
     */
    private static void broadcastSearchHelperState(ServerPlayerEntity sourcePlayer, boolean enabled) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // 发送给所有在线玩家（包括发送者自己）
            for (ServerPlayerEntity onlinePlayer : server.getPlayerList().getPlayers()) {
                // 更新每个玩家的Capability
                onlinePlayer.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
                    cap.setSearchHelperEnabled(enabled);
                });

                // 发送同步包到客户端
                PacketHandler.sendToClient(new SyncSearchHelperToClientPacket(
                        sourcePlayer.getUUID(),
                        enabled
                ), onlinePlayer);
            }
        }
    }
}