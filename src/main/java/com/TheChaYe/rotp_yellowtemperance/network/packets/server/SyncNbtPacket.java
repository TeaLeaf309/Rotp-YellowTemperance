package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncNbtPacket {
    private final UUID playerId;
    private final CompoundNBT nbt;

    public SyncNbtPacket(UUID playerId, CompoundNBT nbt) {
        this.playerId = playerId;
        this.nbt = nbt;
    }

    public static void encode(SyncNbtPacket msg, PacketBuffer buffer) {
        buffer.writeUUID(msg.playerId);
        buffer.writeNbt(msg.nbt);
    }

    public static SyncNbtPacket decode(PacketBuffer buffer) {
        return new SyncNbtPacket(buffer.readUUID(), buffer.readNbt());
    }

    public static void handle(SyncNbtPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncNbtPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            PlayerEntity player = mc.level.getPlayerByUUID(msg.playerId);
            if (player != null) {
                player.getPersistentData().merge(msg.nbt);

                // 处理伪装相关的客户端缓存
                handleDisguiseSync(msg.playerId, msg.nbt);
            }
        }
    }

    private static void handleDisguiseSync(UUID playerId, CompoundNBT nbt) {
        try {
            // 检查是否需要清理伪装
            if (nbt.contains("clearDisguise") && nbt.getBoolean("clearDisguise")) {
                com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache.clearPlayerDisguise(playerId);
                return;
            }

            // 处理伪装profile同步
            if (nbt.contains("disguiseProfile")) {
                CompoundNBT profileTag = nbt.getCompound("disguiseProfile");
                com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache.cachePlayerDisguiseProfile(playerId, profileTag);

                // 记录同步信息
                com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.debug(
                        "Synced disguise profile for player {} from server", playerId);
            }

            // 处理伪装名称同步
            if (nbt.contains(com.TheChaYe.rotp_yellowtemperance.init.InitTags.YT_DISGUISE_NAME_TAG)) {
                String disguiseName = nbt.getString(com.TheChaYe.rotp_yellowtemperance.init.InitTags.YT_DISGUISE_NAME_TAG);
                com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.debug(
                        "Synced disguise name '{}' for player {} from server", disguiseName, playerId);
            }
        } catch (Exception e) {
            com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.error(
                    "Failed to handle disguise sync for player {}: {}", playerId, e.getMessage());
        }
    }
}