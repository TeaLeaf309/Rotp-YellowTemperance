package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncErosionLevelPacket {
    private final int entityId;
    private final int erosionLevel;

    public SyncErosionLevelPacket(int entityId, int erosionLevel) {
        this.entityId = entityId;
        this.erosionLevel = erosionLevel;
    }

    public static void encode(SyncErosionLevelPacket msg, PacketBuffer buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeInt(msg.erosionLevel);
    }

    public static SyncErosionLevelPacket decode(PacketBuffer buffer) {
        return new SyncErosionLevelPacket(buffer.readInt(), buffer.readInt());
    }

    public static void handle(SyncErosionLevelPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId);
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.getCapability(CapabilityHandler.LIVING_DATA_CAPABILITY).ifPresent(data -> {
                    data.setErosionLevel(msg.erosionLevel);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}