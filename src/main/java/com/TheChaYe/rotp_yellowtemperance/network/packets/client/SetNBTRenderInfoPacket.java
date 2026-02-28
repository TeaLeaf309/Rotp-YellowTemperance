// SetNBTRenderInfoPacket.java
package com.TheChaYe.rotp_yellowtemperance.network.packets.client;

import com.TheChaYe.rotp_yellowtemperance.network.ModNetwork;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SetNBTRenderInfoPacket {
    private final String nbtKey;
    private final String nbtValue;

    public SetNBTRenderInfoPacket(String nbtKey, String nbtValue) {
        this.nbtKey = nbtKey;
        this.nbtValue = nbtValue;
    }

    public static void encode(SetNBTRenderInfoPacket msg, PacketBuffer buffer) {
        buffer.writeUtf(msg.nbtKey, 128);
        buffer.writeUtf(msg.nbtValue, 32767);
    }

    public static SetNBTRenderInfoPacket decode(PacketBuffer buffer) {
        return new SetNBTRenderInfoPacket(buffer.readUtf(128), buffer.readUtf(32767));
    }

    public static void handle(SetNBTRenderInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = ctx.get().getSender();
            if (player != null) {
                // 1. 服务器端设置NBT
                player.getPersistentData().putString(msg.nbtKey, msg.nbtValue);

                // 2. 创建同步数据包
                CompoundNBT syncData = new CompoundNBT();
                syncData.putString(msg.nbtKey, msg.nbtValue);

                // 3. 发送给客户端
                ModNetwork.sendNbtSyncToClient(player, syncData);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}