package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncSearchHelperToClientPacket {
    private final UUID playerId;
    private final boolean searchHelperEnabled;

    public SyncSearchHelperToClientPacket(UUID playerId, boolean searchHelperEnabled) {
        this.playerId = playerId;
        this.searchHelperEnabled = searchHelperEnabled;
    }

    public static void encode(SyncSearchHelperToClientPacket msg, PacketBuffer buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.searchHelperEnabled);
    }

    public static SyncSearchHelperToClientPacket decode(PacketBuffer buf) {
        return new SyncSearchHelperToClientPacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(SyncSearchHelperToClientPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncSearchHelperToClientPacket msg) {
        // 在客户端更新所有玩家的搜索辅助状态
        updateAllPlayersSearchHelperState(msg.searchHelperEnabled);
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateAllPlayersSearchHelperState(boolean enabled) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            // 更新所有玩家的Capability
            for (PlayerEntity player : mc.level.players()) {
                player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
                    cap.setSearchHelperEnabled(enabled);
                });
            }

            // 如果当前有打开的伪装配置GUI，也需要更新其显示状态
            if (mc.screen instanceof com.TheChaYe.rotp_yellowtemperance.client.ui.DisguiseConfigGUI) {
                com.TheChaYe.rotp_yellowtemperance.client.ui.DisguiseConfigGUI gui =
                        (com.TheChaYe.rotp_yellowtemperance.client.ui.DisguiseConfigGUI) mc.screen;
                gui.onSearchHelperStateUpdate(enabled);
            }
        }
    }
}