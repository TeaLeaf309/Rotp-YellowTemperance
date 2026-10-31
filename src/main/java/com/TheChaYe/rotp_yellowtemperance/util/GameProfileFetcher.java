package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 游戏档案获取器 / Game Profile Fetcher
 *
 * <p>只负责"取到真实档案"，不再伪造默认皮肤：拿不到就是拿不到，
 * 返回不带 textures 的纯 profile 交给上层决定（客户端会用自己的重试/兜底）。
 * 以前这里会给未知名额伪造一份 Steve/Alex 的 textures，客户端把它当真皮肤缓存，
 * 于是伪装玩家永远显示史蒂夫——这是本次修复的根因之一。</p>
 */
public class GameProfileFetcher {
    /** 官方 API 调用限速。伪装是低频操作，300ms 足够避免被限流，同时不再白等 1 秒。 */
    private static final RateLimiter RATE_LIMITER = new RateLimiter(300L);
    /** 单次 HTTP 连接/读取超时。 */
    private static final int HTTP_TIMEOUT_MS = 2500;
    /** 名字 -> 真皮肤档案 的成功缓存有效期（只缓存真的拿到了皮肤的档案）。 */
    private static final long LOOKUP_CACHE_MS = 6 * 60 * 60 * 1000L;
    /** 查询失败缓存有效期（避免反复打 API，但别让一次网络抖动锁太久）。 */
    private static final long LOOKUP_FAILURE_CACHE_MS = 5 * 60 * 1000L;
    /** 名字 -> UUID 缓存：UUID 基本不变，可以缓存很久；这样重试皮肤时不必再打名字接口。 */
    private static final long UUID_CACHE_MS = 24 * 60 * 60 * 1000L;

    /** name(小写) -> 真皮肤档案；profile 为 null 表示查过但没拿到（失败缓存）。 */
    private static final Map<String, CachedLookup> NAME_LOOKUPS = new ConcurrentHashMap<>();
    /** name(小写) -> UUID（只缓存成功结果）。 */
    private static final Map<String, CachedUuid> UUID_CACHE = new ConcurrentHashMap<>();

    /**
     * 异步获取游戏档案 / Get Game Profile Asynchronously
     */
    public static CompletableFuture<GameProfile> getProfileAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> getProfile(playerName));
    }

    /**
     * 获取玩家游戏档案 / Get Player Game Profile
     * 按优先级：在线玩家 -> 服务端档案缓存(补全属性) -> Mojang 名字查询 -> 纯 profile。
     */
    public static GameProfile getProfile(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return createBasicProfile("");
        }
        // 1. 在线玩家（其 GameProfile 通常已带 textures）
        GameProfile profile = getProfileFromOnlinePlayer(playerName);
        if (SkinModelUtil.hasSkinTexture(profile)) {
            return profile;
        }
        // 2. 服务端档案缓存 + session service 补全
        profile = getProfileFromServer(playerName);
        if (SkinModelUtil.hasSkinTexture(profile)) {
            return profile;
        }
        // 3. Mojang 名字查询（api.mojang.com，公开接口），再用 session service 补全皮肤属性
        profile = getProfileFromThirdParty(playerName);
        if (SkinModelUtil.hasSkinTexture(profile)) {
            return profile;
        }
        // 4. 拿不到真实皮肤：返回不带 textures 的纯 profile
        return createBasicProfile(playerName);
    }

    private static GameProfile getProfileFromOnlinePlayer(String playerName) {
        ServerPlayerEntity onlinePlayer = tryGetPlayerByName(playerName);
        if (onlinePlayer != null) {
            GameProfile onlineProfile = onlinePlayer.getGameProfile();
            if (SkinModelUtil.hasSkinTexture(onlineProfile)) {
                RotPYellowTemperanceAddon.LOGGER.debug("Found online player profile for: {}", playerName);
                return onlineProfile;
            }
        }
        return null;
    }

    private static ServerPlayerEntity tryGetPlayerByName(String playerName) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                return server.getPlayerList().getPlayerByName(playerName);
            }
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to get online player by name: {}", e.getMessage());
        }
        return null;
    }

    private static GameProfile getProfileFromServer(String playerName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        PlayerProfileCache profileCache = server.getProfileCache();
        if (profileCache == null) {
            return null;
        }
        GameProfile profile = profileCache.get(playerName);
        if (profile == null) {
            return null;
        }
        if (SkinModelUtil.hasSkinTexture(profile)) {
            return profile;
        }
        try {
            MinecraftSessionService sessionService = server.getSessionService();
            if (sessionService != null) {
                RATE_LIMITER.acquire();
                GameProfile filled = sessionService.fillProfileProperties(profile, false);
                if (SkinModelUtil.hasSkinTexture(filled)) {
                    return filled;
                }
            }
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to fill profile from official API: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 名字 -> UUID（Mojang 公开 API），再用 session service 补全 textures。
     * 支持"不在服务器档案缓存里"的玩家名（伪装任意正版玩家名的关键路径）。
     */
    private static GameProfile getProfileFromThirdParty(String playerName) {
        // 配置允许关闭外部查询（Mojang 对第三方 API 有 allow-list 政策/限流）
        if (!YellowTemperanceConfig.isExternalProfileLookupAllowed()) {
            return null;
        }
        String key = playerName.toLowerCase();
        CachedLookup cached = NAME_LOOKUPS.get(key);
        long now = System.currentTimeMillis();
        if (cached != null) {
            if (!cached.isExpired(now)) {
                return cached.profile;
            }
            NAME_LOOKUPS.remove(key);
        }

        GameProfile resolved = lookupProfileFromMojang(playerName);
        // 只缓存"真的带皮肤"的档案；拿不到皮肤只记失败缓存，绝不能把史蒂夫当成功缓存 6 小时
        if (resolved != null && SkinModelUtil.hasSkinTexture(resolved)) {
            NAME_LOOKUPS.put(key, new CachedLookup(resolved, now));
            return resolved;
        }
        NAME_LOOKUPS.put(key, new CachedLookup(null, now));
        return null;
    }

    private static GameProfile lookupProfileFromMojang(String playerName) {
        try {
            UUID uuid = getOrLookupUuid(playerName);
            if (uuid == null) {
                return null;
            }
            GameProfile profile = new GameProfile(uuid, playerName);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && server.getSessionService() != null) {
                RATE_LIMITER.acquire();
                GameProfile filled = server.getSessionService().fillProfileProperties(profile, false);
                if (SkinModelUtil.hasSkinTexture(filled)) {
                    return filled;
                }
            }
            return null;
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Mojang profile lookup failed for {}: {}", playerName, e.getMessage());
            return null;
        }
    }

    /**
     * 名字 -> UUID（Mojang 公开 API），成功结果缓存 24 小时。
     * 皮肤补全失败时只丢弃皮肤，UUID 仍留着，下次重试不用再打名字接口。
     */
    private static UUID getOrLookupUuid(String playerName) {
        String key = playerName.toLowerCase();
        CachedUuid cached = UUID_CACHE.get(key);
        long now = System.currentTimeMillis();
        if (cached != null) {
            if (!cached.isExpired(now)) {
                return cached.uuid;
            }
            UUID_CACHE.remove(key);
        }
        if (!MojangApiLimiter.acquire(4000L)) {
            RotPYellowTemperanceAddon.LOGGER.debug("Mojang name lookup skipped (rate limited) for {}", playerName);
            return null;
        }
        try {
            String encodedName = URLEncoder.encode(playerName, "UTF-8");
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + encodedName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "RotP-YellowTemperance/1.16.5");
            int code = connection.getResponseCode();
            MojangApiLimiter.reportResult(code, connection.getHeaderField("Retry-After"));
            if (code != 200) {
                RotPYellowTemperanceAddon.LOGGER.debug("Mojang name lookup returned {} for {}", code, playerName);
                return null;
            }
            String json;
            try (InputStream in = connection.getInputStream()) {
                json = readString(in);
            }
            JsonObject object = new JsonParser().parse(json).getAsJsonObject();
            if (object == null || !object.has("id")) {
                return null;
            }
            UUID uuid = parseUndashedUuid(object.get("id").getAsString());
            if (uuid != null) {
                UUID_CACHE.put(key, new CachedUuid(uuid, now));
            }
            return uuid;
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Mojang name lookup failed for {}: {}", playerName, e.getMessage());
            return null;
        }
    }

    /**
     * 把 32 位无横线 UUID 字符串转成 UUID。
     */
    private static UUID parseUndashedUuid(String raw) {
        try {
            if (raw == null) {
                return null;
            }
            String value = raw.replace("-", "");
            if (value.length() != 32) {
                return null;
            }
            return UUID.fromString(value.substring(0, 8) + "-"
                    + value.substring(8, 12) + "-"
                    + value.substring(12, 16) + "-"
                    + value.substring(16, 20) + "-"
                    + value.substring(20));
        } catch (Exception e) {
            return null;
        }
    }

    private static String readString(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // 管理员指令用的缓存管理 API / cache management for admin commands
    // ------------------------------------------------------------------

    /** 名字 -> 真档案 缓存条数。 */
    public static int getNameLookupCacheSize() {
        return NAME_LOOKUPS.size();
    }

    /** 名字 -> UUID 缓存条数。 */
    public static int getUuidCacheSize() {
        return UUID_CACHE.size();
    }

    /** 所有缓存过的名字（排序，供 /cache list 用）。 */
    public static java.util.List<String> getCachedNames() {
        java.util.TreeSet<String> names = new java.util.TreeSet<>();
        names.addAll(NAME_LOOKUPS.keySet());
        names.addAll(UUID_CACHE.keySet());
        return new java.util.ArrayList<>(names);
    }

    /** 清空全部服务端档案缓存。 */
    public static void clearCaches() {
        NAME_LOOKUPS.clear();
        UUID_CACHE.clear();
    }

    /** 忘掉某个名字的缓存（返回是否真的删掉了东西）。 */
    public static boolean forget(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String key = name.toLowerCase();
        boolean removed = NAME_LOOKUPS.remove(key) != null;
        if (UUID_CACHE.remove(key) != null) {
            removed = true;
        }
        return removed;
    }

    /**
     * 强制重新查一次（忽略上面的缓存，但仍受全局 Mojang 限速/配置开关约束）。
     * 结果只用于展示/诊断，不写回缓存。
     */
    public static CompletableFuture<GameProfile> lookupFreshAsync(String name) {
        return CompletableFuture.supplyAsync(() -> lookupFresh(name));
    }

    private static GameProfile lookupFresh(String name) {
        GameProfile profile = getProfileFromOnlinePlayer(name);
        if (SkinModelUtil.hasSkinTexture(profile)) {
            return profile;
        }
        profile = getProfileFromServer(name);
        if (SkinModelUtil.hasSkinTexture(profile)) {
            return profile;
        }
        if (YellowTemperanceConfig.isExternalProfileLookupAllowed()) {
            profile = lookupProfileFromMojang(name);
            if (SkinModelUtil.hasSkinTexture(profile)) {
                return profile;
            }
        }
        return createBasicProfile(name);
    }

    /**
     * 纯 profile：只有 UUID + 名字，没有 textures。
     */
    private static GameProfile createBasicProfile(String playerName) {
        return new GameProfile(generateOfflineUUID(playerName), playerName);
    }

    private static UUID generateOfflineUUID(String playerName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 名字查询缓存条目（失败结果也缓存，避免反复打 API）。
     */
    private static final class CachedUuid {
        private final UUID uuid;
        private final long timestamp;

        private CachedUuid(UUID uuid, long timestamp) {
            this.uuid = uuid;
            this.timestamp = timestamp;
        }

        private boolean isExpired(long now) {
            return now - this.timestamp > UUID_CACHE_MS;
        }
    }

    private static final class CachedLookup {
        private final GameProfile profile;
        private final long timestamp;

        private CachedLookup(GameProfile profile, long timestamp) {
            this.profile = profile;
            this.timestamp = timestamp;
        }

        private boolean isExpired(long now) {
            long ttl = this.profile != null ? LOOKUP_CACHE_MS : LOOKUP_FAILURE_CACHE_MS;
            return now - this.timestamp > ttl;
        }
    }

    /**
     * 简单的速率限制器 / Simple rate limiter
     */
    private static class RateLimiter {
        private final long intervalMillis;
        private final AtomicLong lastCall = new AtomicLong(0);

        private RateLimiter(long intervalMillis) {
            this.intervalMillis = intervalMillis;
        }

        private void acquire() {
            while (true) {
                long now = System.currentTimeMillis();
                long last = lastCall.get();
                if (now - last >= intervalMillis) {
                    if (lastCall.compareAndSet(last, now)) {
                        break;
                    }
                } else {
                    long sleep = intervalMillis - (now - last);
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}