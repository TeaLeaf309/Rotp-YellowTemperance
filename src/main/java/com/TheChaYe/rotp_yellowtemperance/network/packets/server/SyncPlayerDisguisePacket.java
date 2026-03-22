package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.client.DisguiseRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncPlayerDisguisePacket {
    private final UUID playerId;
    private final ResourceLocation disguiseType;
    private final boolean isDisguised;

    public SyncPlayerDisguisePacket(UUID playerId, EntityType<?> disguiseType, boolean isDisguised) {
        this.playerId = playerId;
        this.disguiseType = disguiseType != null ? ForgeRegistries.ENTITIES.getKey(disguiseType) : null;
        this.isDisguised = isDisguised;
    }

    public static void encode(SyncPlayerDisguisePacket msg, PacketBuffer buffer) {
        buffer.writeUUID(msg.playerId);
        buffer.writeBoolean(msg.disguiseType != null);
        if (msg.disguiseType != null) {
            buffer.writeResourceLocation(msg.disguiseType);
        }
        buffer.writeBoolean(msg.isDisguised);
    }

    public static SyncPlayerDisguisePacket decode(PacketBuffer buffer) {
        UUID playerId = buffer.readUUID();
        ResourceLocation disguiseType = null;
        if (buffer.readBoolean()) {
            disguiseType = buffer.readResourceLocation();
        }
        boolean isDisguised = buffer.readBoolean();
        return new SyncPlayerDisguisePacket(playerId, disguiseType != null ? ForgeRegistries.ENTITIES.getValue(disguiseType) : null, isDisguised);
    }

    public static void handle(SyncPlayerDisguisePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端更新伪装状态
            updateClientDisguiseState(msg.playerId, msg.disguiseType, msg.isDisguised);
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void updateClientDisguiseState(UUID playerId, ResourceLocation disguiseType, boolean isDisguised) {
        if (isDisguised && disguiseType != null) {
            // 应用伪装
            EntityType<?> entityType = ForgeRegistries.ENTITIES.getValue(disguiseType);
            if (entityType != null) {
                DisguiseRenderHandler.CURRENT_DISGUISE_TYPES.put(playerId, entityType);
                DisguiseRenderHandler.DISGUISE_REMOVAL_TIMERS.remove(playerId);


            }
        } else {
            // 移除伪装
            DisguiseRenderHandler.CURRENT_DISGUISE_TYPES.remove(playerId);
            DisguiseRenderHandler.DISGUISE_REMOVAL_TIMERS.put(playerId, 100); // 设置移除计时器


        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void restorePlayerOriginalProperties(UUID playerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            PlayerEntity player = mc.level.getPlayerByUUID(playerId);
            if (player != null) {
                // 恢复玩家的原始物理属性（直接调用相关方法）
                try {
                    // 使用反射调用私有方法
                    java.lang.reflect.Method method = DisguiseRenderHandler.class.getDeclaredMethod("restorePlayerPhysics", PlayerEntity.class);
                    method.setAccessible(true);
                    method.invoke(null, player);
                } catch (Exception e) {
                    // 如果反射失败，则手动恢复属性
                    restorePlayerPropertiesManually(player);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void restorePlayerPropertiesManually(PlayerEntity player) {
        // 手动恢复玩家属性的简化版本
        try {
            // 尝试玩家尺寸

        } catch (Exception e) {
            // 忽略异常
        }
    }
}