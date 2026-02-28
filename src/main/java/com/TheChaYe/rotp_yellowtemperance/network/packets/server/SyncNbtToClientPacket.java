package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SyncNbtToClientPacket {
    private static final Map<UUID, PendingData> PENDING_DATA = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 100; // 最多尝试100 tick（约5秒）

    private final UUID playerId;
    private final CompoundNBT nbtData;

    public SyncNbtToClientPacket(UUID playerId, CompoundNBT nbtData) {
        this.playerId = playerId;
        this.nbtData = nbtData;
    }

    public static void encode(SyncNbtToClientPacket msg, PacketBuffer buffer) {
        buffer.writeUUID(msg.playerId);
        buffer.writeNbt(msg.nbtData);
    }

    public static SyncNbtToClientPacket decode(PacketBuffer buffer) {
        return new SyncNbtToClientPacket(buffer.readUUID(), buffer.readNbt());
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SyncNbtToClientPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 立即尝试应用，如果失败则加入待处理队列
            if (!tryApply(msg.playerId, msg.nbtData)) {
                PENDING_DATA.put(msg.playerId, new PendingData(msg.nbtData, 0));
                RotPYellowTemperanceAddon.LOGGER.debug("Added pending disguise data for player {}", msg.playerId);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean tryApply(UUID playerId, CompoundNBT nbtData) {
        PlayerEntity clientPlayer = Minecraft.getInstance().level.getPlayerByUUID(playerId);
        if (clientPlayer != null) {
            applyData(clientPlayer, nbtData);
            return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyData(PlayerEntity clientPlayer, CompoundNBT nbtData) {
        // 处理伪装清除
        if (nbtData.contains(InitTags.YT_REMOVE_DISGUISE) &&
                nbtData.getBoolean(InitTags.YT_REMOVE_DISGUISE)) {
            clearDisguise(clientPlayer);
            return;
        }

        // 更新伪装名称
        if (nbtData.contains(InitTags.YT_DISGUISE_NAME_TAG)) {
            clientPlayer.getPersistentData().putString(
                    InitTags.YT_DISGUISE_NAME_TAG,
                    nbtData.getString(InitTags.YT_DISGUISE_NAME_TAG)
            );
        }

        // 更新伪装UUID
        if (nbtData.contains(InitTags.YT_DISGUISE_UUID_TAG)) {
            UUID disguiseId = nbtData.getUUID(InitTags.YT_DISGUISE_UUID_TAG);
            ClientDisguiseCache.cachePlayerDisguiseUUID(clientPlayer.getUUID(), disguiseId);
        }

        // 更新伪装资料
        if (nbtData.contains("disguiseProfile")) {
            CompoundNBT profileTag = nbtData.getCompound("disguiseProfile");
            ClientDisguiseCache.cachePlayerDisguiseProfile(clientPlayer.getUUID(), profileTag);
        } else if (nbtData.contains(InitTags.YT_DISGUISE_PROFILE_TAG)) {
            CompoundNBT profileTag = nbtData.getCompound(InitTags.YT_DISGUISE_PROFILE_TAG);
            ClientDisguiseCache.cachePlayerDisguiseProfile(clientPlayer.getUUID(), profileTag);
        }

        // 标记是否有伪装
        if (nbtData.contains(InitTags.YT_HAS_DISGUISE_TAG)) {
            boolean hasDisguise = nbtData.getBoolean(InitTags.YT_HAS_DISGUISE_TAG);
            clientPlayer.getPersistentData().putBoolean(
                    InitTags.YT_HAS_DISGUISE_TAG,
                    hasDisguise
            );
            RotPYellowTemperanceAddon.LOGGER.debug("Setting YT_HAS_DISGUISE_TAG to {} for player: {}", hasDisguise, clientPlayer.getName().getString());
        }

        // 处理伪装加载完成状态
        if (nbtData.contains(InitTags.YT_DISGUISE_LOADING_TAG)) {
            boolean isLoading = nbtData.getBoolean(InitTags.YT_DISGUISE_LOADING_TAG);
            clientPlayer.getPersistentData().putBoolean(
                    InitTags.YT_DISGUISE_LOADING_TAG,
                    isLoading
            );
            RotPYellowTemperanceAddon.LOGGER.debug("Setting YT_DISGUISE_LOADING_TAG to {} for player: {}", isLoading, clientPlayer.getName().getString());
        }

        // 强制重新渲染
        forceEntityRerender(clientPlayer);

        RotPYellowTemperanceAddon.LOGGER.info("Synced disguise data for {}", clientPlayer.getName().getString());
    }

    @OnlyIn(Dist.CLIENT)
    private static void forceEntityRerender(PlayerEntity player) {
        // 方法1: 使用反射调用私有方法
        try {
            Method setSectionDirtyMethod = WorldRenderer.class.getDeclaredMethod("setSectionDirty", int.class, int.class, int.class, boolean.class);
            setSectionDirtyMethod.setAccessible(true);

            ChunkPos chunkPos = new ChunkPos(player.blockPosition());
            setSectionDirtyMethod.invoke(
                    Minecraft.getInstance().levelRenderer,
                    chunkPos.x,
                    player.getY() / 16, // 计算垂直区块位置
                    chunkPos.z,
                    true
            );
        } catch (Exception e) {
            // 如果反射失败，使用其他方法
            RotPYellowTemperanceAddon.LOGGER.warn("Failed to set section dirty via reflection, using fallback method", e);
            forceEntityRerenderFallback(player);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void forceEntityRerenderFallback(PlayerEntity player) {
        // 方法2: 轻微移动实体位置
        BlockPos originalPos = player.blockPosition();
        double originalX = player.getX();
        double originalY = player.getY();
        double originalZ = player.getZ();

        // 轻微移动实体
        player.setPos(originalX, originalY + 0.0001, originalZ);

        // 立即移回
        player.setPos(originalX, originalY, originalZ);

        // 标记实体需要更新
        player.hasImpulse = true;
    }

    @OnlyIn(Dist.CLIENT)
    private static void clearDisguise(PlayerEntity player) {
        player.getPersistentData().remove("disguiseProfile");
        player.getPersistentData().remove(InitTags.YT_DISGUISE_UUID_TAG);
        player.getPersistentData().remove(InitTags.YT_DISGUISE_NAME_TAG);
        player.getPersistentData().remove(InitTags.YT_HAS_DISGUISE_TAG);
        ClientDisguiseCache.clearPlayerDisguise(player.getUUID());

        // 强制重新渲染
        forceEntityRerender(player);
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID, value = Dist.CLIENT)
    public static class PendingDataHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !PENDING_DATA.isEmpty()) {
                PENDING_DATA.entrySet().removeIf(entry -> {
                    UUID playerId = entry.getKey();
                    PendingData data = entry.getValue();
                    if (tryApply(playerId, data.nbtData)) {
                        RotPYellowTemperanceAddon.LOGGER.debug("Applied pending disguise data for player {}", playerId);
                        return true;
                    } else {
                        data.attempts++;
                        if (data.attempts >= MAX_ATTEMPTS) {
                            RotPYellowTemperanceAddon.LOGGER.warn("Failed to apply disguise data for player {} after {} attempts", playerId, data.attempts);
                            return true; // 移除
                        }
                        return false;
                    }
                });
            }
        }
    }

    private static class PendingData {
        final CompoundNBT nbtData;
        int attempts;

        PendingData(CompoundNBT nbtData, int attempts) {
            this.nbtData = nbtData;
            this.attempts = attempts;
        }
    }
}