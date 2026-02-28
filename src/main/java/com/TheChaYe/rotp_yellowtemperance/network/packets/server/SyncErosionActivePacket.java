package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncErosionActivePacket {
    private final int entityId;
    private final boolean erosionActive;

    public SyncErosionActivePacket(int entityId, boolean erosionActive) {
        this.entityId = entityId;
        this.erosionActive = erosionActive;
    }

    public static void encode(SyncErosionActivePacket msg, PacketBuffer buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeBoolean(msg.erosionActive);
    }

    public static SyncErosionActivePacket decode(PacketBuffer buffer) {
        return new SyncErosionActivePacket(buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(SyncErosionActivePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId);
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.getCapability(CapabilityHandler.LIVING_DATA_CAPABILITY).ifPresent(data -> {
                    data.setErosionActive(msg.erosionActive);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}