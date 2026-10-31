package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncNbtPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncNbtToClientPacket;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 伪装管理系统 / Disguise Management System
 *
 * <p>服务端只缓存"真的拿到了皮肤"的档案；拿不到就重试，绝不伪造 Steve/Alex。
 * 客户端那份缓存的职责是渲染，这里的职责是"尽快把真实 GameProfile 送到客户端"。</p>
 */
public class DisguiseManager {
    /** 真实档案缓存（含过期时间）。只缓存"确实拿到皮肤"的档案，可以存久一点（6 小时）。 */
    private static final Map<String, CachedProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 6L * 60L * 60L * 1000L;
    /** 皮肤还没拿到时的后台重试次数与间隔。 */
    private static final int MAX_SKIN_RETRIES = 3;

    private static final ScheduledExecutorService RETRY_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "YT-DisguiseSkinRetry");
                thread.setDaemon(true);
                return thread;
            });

    private DisguiseManager() {}

    /**
     * 应用玩家伪装 / Apply Player Disguise
     * 主线程先建立基础状态并通知客户端（显示"准备中"），完整 profile 在后台取。
     */
    public static void applyDisguise(PlayerEntity player, String name) {
        if (player == null || player.level.isClientSide() || name == null || name.isEmpty()) {
            return;
        }
        initializeBasicDisguise(player, name);

        // 快路径：服务端已经能"同步"拿到真档案（在线玩家 / 上次解析过的缓存）时，
        // 直接一步下发完整 profile，省掉异步往返，客户端可以立刻换皮。
        GameProfile quick = findProfileSync(name);
        if (quick != null) {
            finishDisguiseNow(player, name, quick, true);
            return;
        }

        // 慢路径：需要网络查询。先发"准备包"（只有名字），客户端可以先按 tab 列表把皮肤挂上。
        sendImmediatePreparationPacket(player, name);
        processDisguiseAsync(player, name);
    }

    /**
     * 同步（不联网）查找真档案：服务端真档案缓存 -> 在线玩家的 GameProfile。
     */
    private static GameProfile findProfileSync(String name) {
        GameProfile cached = getCachedProfile(name);
        if (SkinModelUtil.hasSkinTexture(cached)) {
            return cached;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayerEntity online = server.getPlayerList().getPlayerByName(name);
            if (online != null && SkinModelUtil.hasSkinTexture(online.getGameProfile())) {
                return online.getGameProfile();
            }
        }
        return null;
    }

    private static void initializeBasicDisguise(PlayerEntity player, String name) {
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            cap.setDisguiseName(name);
            cap.setHasDisguise(true);
        });

        CompoundNBT nbt = player.getPersistentData();
        nbt.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
        nbt.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
        nbt.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, true);
        // 上一份 profile 必须清掉：否则异步完成前，其他客户端/新追踪者会拿到上一个伪装目标的数据
        nbt.remove("disguiseProfile");
        nbt.remove(InitTags.YT_DISGUISE_PROFILE_TAG);
    }

    private static void sendImmediatePreparationPacket(PlayerEntity player, String name) {
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        CompoundNBT syncData = new CompoundNBT();
        syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
        syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
        syncData.putBoolean("preparingDisguise", true);
        syncData.putLong("disguiseStartTime", System.currentTimeMillis());
        PacketHandler.sendToAllTracking(new SyncNbtPacket(serverPlayer.getUUID(), syncData), serverPlayer);
    }

    private static void processDisguiseAsync(PlayerEntity player, String name) {
        final UUID playerId = player.getUUID();
        final MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            GameProfile profile;
            boolean wasCached;
            try {
                profile = getCachedProfile(name);
                wasCached = profile != null;
                if (profile == null) {
                    profile = GameProfileFetcher.getProfile(name);
                }
            } catch (Exception e) {
                final Exception error = e;
                server.execute(() -> {
                    ServerPlayerEntity online = server.getPlayerList().getPlayer(playerId);
                    if (online != null) {
                        handleAsyncError(online, name, error);
                    }
                });
                return;
            }

            final GameProfile finalProfile = profile;
            final boolean finalWasCached = wasCached;
            server.execute(() -> {
                ServerPlayerEntity online = server.getPlayerList().getPlayer(playerId);
                if (online == null || online.connection == null) {
                    return; // 玩家已离线或实体已重建，放弃这次异步结果
                }
                completeDisguiseProcess(online, name, finalProfile, finalWasCached);
            });
        });
    }

    // ------------------------------------------------------------------
    // 管理员指令用的缓存管理 API / cache management for admin commands
    // ------------------------------------------------------------------

    /** 服务端真档案缓存条数。 */
    public static int getProfileCacheSize() {
        return PROFILE_CACHE.size();
    }

    /** 清空服务端真档案缓存。 */
    public static void clearProfileCache() {
        PROFILE_CACHE.clear();
    }

    /** 删掉某个伪装目标名的服务端真档案缓存（返回是否真的删掉了）。 */
    public static boolean forgetProfile(String name) {
        return name != null && !name.isEmpty() && PROFILE_CACHE.remove(name.toLowerCase()) != null;
    }

    private static GameProfile getCachedProfile(String name) {
        String key = name.toLowerCase();
        CachedProfile cached = PROFILE_CACHE.get(key);
        if (cached == null) {
            return null;
        }
        if (cached.isExpired()) {
            PROFILE_CACHE.remove(key);
            return null;
        }
        return cached.profile;
    }

    /**
     * 在服务器主线程完成伪装：写入 NBT、下发 profile。
     */
    /**
     * 同步完成（调用方保证已在服务器主线程）：通常用于"不用联网就有真档案"的快路径。
     */
    private static void finishDisguiseNow(PlayerEntity player, String name, GameProfile profile, boolean wasCached) {
        if (!(player instanceof ServerPlayerEntity) || player.level.isClientSide()) {
            return;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        if (serverPlayer.connection == null) {
            return;
        }
        CompoundNBT current = serverPlayer.getPersistentData();
        if (!current.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)
                || !name.equals(current.getString(InitTags.YT_DISGUISE_NAME_TAG))) {
            return;
        }
        final GameProfile validProfile = profile != null ? profile : createBasicProfile(name);
        rememberProfile(name, validProfile);
        applyDisguiseData(serverPlayer, name, validProfile, wasCached);
    }

    private static void completeDisguiseProcess(PlayerEntity player, String name, GameProfile profile, boolean wasCached) {
        if (player == null || player.level.isClientSide() || !(player instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        if (serverPlayer.connection == null) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        // 过期结果保护：玩家可能已经解除/换成了别的伪装目标
        CompoundNBT current = serverPlayer.getPersistentData();
        if (!current.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)
                || !name.equals(current.getString(InitTags.YT_DISGUISE_NAME_TAG))) {
            return;
        }
        final UUID playerId = serverPlayer.getUUID();
        final GameProfile validProfile = profile != null ? profile : createBasicProfile(name);
        final boolean realSkin = SkinModelUtil.hasSkinTexture(validProfile);
        rememberProfile(name, validProfile);

        server.execute(() -> {
            // 延迟一个 tick 执行期间玩家可能已离线/换目标，重新取实体并再校验一次
            ServerPlayerEntity online = server.getPlayerList().getPlayer(playerId);
            if (online == null || online.connection == null) {
                return;
            }
            CompoundNBT nbt = online.getPersistentData();
            if (!nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)
                    || !name.equals(nbt.getString(InitTags.YT_DISGUISE_NAME_TAG))) {
                return;
            }
            applyDisguiseData(online, name, validProfile, wasCached);
        });

        if (!realSkin) {
            scheduleSkinRetry(playerId, name, 1);
        }
    }

    /**
     * 只缓存真皮肤档案；假档案（没有 textures）不缓存，留给重试。
     */
    private static void rememberProfile(String name, GameProfile profile) {
        if (SkinModelUtil.hasSkinTexture(profile)) {
            PROFILE_CACHE.put(name.toLowerCase(), new CachedProfile(profile));
        } else {
            PROFILE_CACHE.remove(name.toLowerCase());
        }
    }

    /**
     * 真正写数据 + 下发（调用方保证在服务器主线程、玩家仍在线且目标未被换掉）。
     */
    private static void applyDisguiseData(ServerPlayerEntity online, String name, GameProfile validProfile, boolean wasCached) {
        final boolean realSkin = SkinModelUtil.hasSkinTexture(validProfile);
        final CompoundNBT profileNbt = GameProfileSerializer.serialize(validProfile);
        try {
            online.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> cap.setDisguiseProfile(profileNbt));
            CompoundNBT nbt = online.getPersistentData();
            nbt.put("disguiseProfile", profileNbt);
            nbt.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
            nbt.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
            if (realSkin) {
                nbt.remove(InitTags.YT_DISGUISE_LOADING_TAG);
            } else {
                nbt.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, true);
            }

            sendFinalDisguisePacket(online, name, profileNbt, realSkin, wasCached);
            logDisguiseCompletion(name, online.getName().getString(), validProfile, wasCached, realSkin);
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.error("Error finalizing disguise for {}: {}", name, e.getMessage());
        }
    }

    private static void sendFinalDisguisePacket(ServerPlayerEntity player, String name, CompoundNBT profileNbt,
                                                boolean realSkin, boolean wasCached) {
        CompoundNBT syncData = new CompoundNBT();
        syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
        syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
        syncData.put("disguiseProfile", profileNbt);
        syncData.putBoolean("disguiseReady", realSkin);
        syncData.putBoolean("wasCached", wasCached);
        syncData.putLong("disguiseCompleteTime", System.currentTimeMillis());
        if (realSkin) {
            syncData.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, false);
        } else {
            syncData.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, true);
        }
        // 只发给追踪该玩家（含自己）的客户端即可；登录/开始追踪时 PlayerLoginEventHandler 会补发
        PacketHandler.sendToAllTracking(new SyncNbtToClientPacket(player.getUUID(), syncData), player);
    }

    /**
     * 皮肤没拿到时后台重试：成功后补发一份带真实 textures 的 profile，客户端会自动换皮。
     * 跑在专用守护线程里（阻塞的 HTTP 不会占用服务器线程池）。
     */
    private static void scheduleSkinRetry(UUID playerId, String name, int attempt) {
        if (attempt > MAX_SKIN_RETRIES) {
            RotPYellowTemperanceAddon.LOGGER.debug("Giving up disguise skin retries for {}", name);
            return;
        }
        long delaySeconds = 3L * attempt;
        RETRY_EXECUTOR.schedule(() -> {
            try {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) {
                    return;
                }
                final GameProfile profile = GameProfileFetcher.getProfile(name);
                if (SkinModelUtil.hasSkinTexture(profile)) {
                    server.execute(() -> {
                        ServerPlayerEntity online = server.getPlayerList().getPlayer(playerId);
                        if (online != null && online.connection != null) {
                            CompoundNBT nbt = online.getPersistentData();
                            if (nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)
                                    && name.equals(nbt.getString(InitTags.YT_DISGUISE_NAME_TAG))) {
                                completeDisguiseProcess(online, name, profile, true);
                            }
                        }
                    });
                } else {
                    scheduleSkinRetry(playerId, name, attempt + 1);
                }
            } catch (Throwable throwable) {
                RotPYellowTemperanceAddon.LOGGER.debug("Disguise skin retry failed for {}: {}", name, throwable.toString());
                scheduleSkinRetry(playerId, name, attempt + 1);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private static void handleAsyncError(PlayerEntity player, String name, Exception error) {
        RotPYellowTemperanceAddon.LOGGER.error("Async disguise error for {}: {}", name, error.getMessage());
        if (player instanceof ServerPlayerEntity && !player.level.isClientSide()
                && ((ServerPlayerEntity) player).connection != null) {
            sendErrorPacket((ServerPlayerEntity) player, name);
        }
    }

    private static void sendErrorPacket(ServerPlayerEntity player, String name) {
        CompoundNBT errorData = new CompoundNBT();
        errorData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, false);
        errorData.putString(InitTags.YT_DISGUISE_NAME_TAG, "");
        errorData.putBoolean("disguiseError", true);
        errorData.putString("errorMessage", "Failed to fetch player data");
        PacketHandler.sendToAllTracking(new SyncNbtToClientPacket(player.getUUID(), errorData), player);
    }

    private static GameProfile createBasicProfile(String name) {
        return new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()), name);
    }

    private static void logDisguiseCompletion(String name, String playerName, GameProfile profile,
                                              boolean wasCached, boolean realSkin) {
        String source = wasCached ? "cache" : "network";
        RotPYellowTemperanceAddon.LOGGER.info("Disguise '{}' applied for {} from {} (UUID: {}, realSkin: {})",
                name, playerName, source, profile.getId(), realSkin);
    }

    /**
     * 移除玩家伪装 / Remove Player Disguise
     */
    public static boolean removeDisguise(PlayerEntity player) {
        boolean hadDisguise = player.getPersistentData().getBoolean(InitTags.YT_HAS_DISGUISE_TAG);

        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            cap.setHasDisguise(false);
            cap.setDisguiseName("");
        });

        CompoundNBT nbt = player.getPersistentData();
        nbt.remove(InitTags.YT_HAS_DISGUISE_TAG);
        nbt.remove(InitTags.YT_DISGUISE_NAME_TAG);
        nbt.remove(InitTags.YT_DISGUISE_UUID_TAG);
        nbt.remove(InitTags.YT_DISGUISE_PROFILE_TAG);
        nbt.remove("disguiseProfile");
        nbt.remove(InitTags.YT_DISGUISE_LOADING_TAG);

        if (!player.level.isClientSide() && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            CompoundNBT syncData = new CompoundNBT();
            syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, false);
            syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, "");
            syncData.putBoolean("clearDisguise", true);
            // 历史客户端只认 YT_REMOVE_DISGUISE 这个键；两个都带上，保证缓存一定被清
            syncData.putBoolean(InitTags.YT_REMOVE_DISGUISE, true);
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                ServerLifecycleHooks.getCurrentServer().execute(() ->
                        PacketHandler.sendToAllTracking(new SyncNbtToClientPacket(player.getUUID(), syncData), serverPlayer));
            }
        }

        RotPYellowTemperanceAddon.LOGGER.debug("Removed disguise for player: {}", player.getName().getString());
        return hadDisguise;
    }

    /**
     * 缓存条目（带时间戳）。
     */
    private static final class CachedProfile {
        private final GameProfile profile;
        private final long timestamp;

        private CachedProfile(GameProfile profile) {
            this.profile = profile;
            this.timestamp = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > CACHE_EXPIRY_MS;
        }
    }
}