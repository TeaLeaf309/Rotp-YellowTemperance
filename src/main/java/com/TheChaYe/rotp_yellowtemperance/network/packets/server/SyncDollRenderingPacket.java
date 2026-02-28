package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncDollRenderingPacket {
    private final int entityId;
    private final boolean dollRendering;

    public SyncDollRenderingPacket(int entityId, boolean dollRendering) {
        this.entityId = entityId;
        this.dollRendering = dollRendering;
    }

    public static void encode(SyncDollRenderingPacket msg, PacketBuffer buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeBoolean(msg.dollRendering);
    }

    public static SyncDollRenderingPacket decode(PacketBuffer buffer) {
        return new SyncDollRenderingPacket(buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(SyncDollRenderingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId);
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.getCapability(CapabilityHandler.LIVING_DATA_CAPABILITY).ifPresent(data -> {
                    data.setDollRendering(msg.dollRendering);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}