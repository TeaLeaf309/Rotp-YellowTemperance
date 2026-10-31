package com.TheChaYe.rotp_yellowtemperance.network.packets.server;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.client.ClientDisguiseCache;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

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

    public static void handle(SyncNbtToClientPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncNbtToClientPacket msg) {
        // 立即尝试应用，如果失败则加入待处理队列
        if (!tryApply(msg.playerId, msg.nbtData)) {
            PENDING_DATA.put(msg.playerId, new PendingData(msg.nbtData, 0));
            RotPYellowTemperanceAddon.LOGGER.debug("Added pending disguise data for player {}", msg.playerId);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean tryApply(UUID playerId, CompoundNBT nbtData) {
        if (Minecraft.getInstance().level == null) {
            return false;
        }
        PlayerEntity clientPlayer = Minecraft.getInstance().level.getPlayerByUUID(playerId);
        if (clientPlayer != null) {
            applyData(clientPlayer, nbtData);
            return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyData(PlayerEntity clientPlayer, CompoundNBT nbtData) {
        // 处理伪装清除（两种历史键名都要认，否则客户端缓存不会清 → 第一人称手臂一直不换回来）
        boolean clearRequested =
                (nbtData.contains(InitTags.YT_REMOVE_DISGUISE) && nbtData.getBoolean(InitTags.YT_REMOVE_DISGUISE))
                || (nbtData.contains("clearDisguise") && nbtData.getBoolean("clearDisguise"));
        if (clearRequested) {
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

        RotPYellowTemperanceAddon.LOGGER.info("Synced disguise data for {}", clientPlayer.getName().getString());
    }

    @OnlyIn(Dist.CLIENT)
    private static void clearDisguise(PlayerEntity player) {
        CompoundNBT nbt = player.getPersistentData();
        nbt.remove("disguiseProfile");
        nbt.remove(InitTags.YT_DISGUISE_PROFILE_TAG);
        nbt.remove(InitTags.YT_DISGUISE_UUID_TAG);
        nbt.remove(InitTags.YT_DISGUISE_NAME_TAG);
        nbt.remove(InitTags.YT_HAS_DISGUISE_TAG);
        nbt.remove(InitTags.YT_DISGUISE_LOADING_TAG);
        ClientDisguiseCache.clearPlayerDisguise(player.getUUID());
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