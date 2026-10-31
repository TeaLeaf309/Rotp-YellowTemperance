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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        // 只更新消息对应的那个玩家的设置（搜索辅助是每个玩家独立的）
        PlayerEntity target = mc.level.getPlayerByUUID(msg.playerId);
        if (target != null) {
            target.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
                cap.setSearchHelperEnabled(msg.searchHelperEnabled);
            });
        }

        // 本地玩家自己的设置变化时，刷新已打开的配置界面
        if (mc.player != null && mc.player.getUUID().equals(msg.playerId)
                && mc.screen instanceof com.TheChaYe.rotp_yellowtemperance.client.ui.DisguiseConfigGUI) {
            com.TheChaYe.rotp_yellowtemperance.client.ui.DisguiseConfigGUI gui =
                    (com.TheChaYe.rotp_yellowtemperance.client.ui.DisguiseConfigGUI) mc.screen;
            gui.onSearchHelperStateUpdate(msg.searchHelperEnabled);
        }
    }
}