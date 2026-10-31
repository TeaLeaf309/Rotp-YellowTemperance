package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.util.GameProfileSerializer;
import com.TheChaYe.rotp_yellowtemperance.util.MojangApiLimiter;
import com.TheChaYe.rotp_yellowtemperance.util.SkinModelUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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

/**
 * 客户端侧"名字 -> 档案"补查（慢路径加速 / 兜底）。
 *
 * <p>服务端查档案要经过 api.mojang.com + session server 两次 HTTP，网络不好时会慢甚至失败。
 * 客户端本来就要下载皮肤贴图，网络通常是通的，所以并行地自己查一遍：
 * 查到就通过 {@link ClientDisguiseCache#cachePlayerDisguiseProfile} 交给渲染层。</p>
 *
 * <p>只查一次，失败 10 秒后由调用方决定是否重试；全程后台线程，不阻塞渲染。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class DisguiseProfileLookup {
    private static final int HTTP_TIMEOUT_MS = 2500;
    /** playerId -> 正在查询的玩家名（小写）。 */
    private static final Map<UUID, String> IN_FLIGHT = new ConcurrentHashMap<>();

    private DisguiseProfileLookup() {}

    /**
     * 发起一次异步查询（同一玩家同时只跑一个）。
     *
     * @return true 表示已接受请求（用于调用方统计尝试次数）
     */
    public static boolean request(UUID playerId, String name) {
        if (playerId == null || name == null || name.isEmpty()) {
            return false;
        }
        if (!YellowTemperanceConfig.isExternalProfileLookupAllowed()) {
            return false;
        }
        // 全局限速：整个 JVM 对 Mojang 名字查询保持 ~50 req/min
        if (!MojangApiLimiter.acquire(0L)) {
            return false;
        }
        String key = name.toLowerCase(java.util.Locale.ROOT);
        if (IN_FLIGHT.putIfAbsent(playerId, key) != null) {
            return false;
        }
        CompletableFuture.supplyAsync(() -> lookup(name), Util.backgroundExecutor())
                .whenComplete((profileTag, throwable) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc == null) {
                        return;
                    }
                    mc.execute(() -> {
                        IN_FLIGHT.remove(playerId);
                        if (profileTag == null || mc.level == null) {
                            return;
                        }
                        PlayerEntity player = mc.level.getPlayerByUUID(playerId);
                        if (player == null) {
                            return;
                        }
                        CompoundNBT nbt = player.getPersistentData();
                        if (!nbt.contains(InitTags.YT_HAS_DISGUISE_TAG)
                                || !nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
                            return;
                        }
                        if (!name.equalsIgnoreCase(nbt.getString(InitTags.YT_DISGUISE_NAME_TAG))) {
                            return;
                        }
                        ClientDisguiseCache.cachePlayerDisguiseProfile(playerId, profileTag);
                    });
                });
        return true;
    }

    private static CompoundNBT lookup(String name) {
        try {
            UUID uuid = lookupUuid(name);
            if (uuid == null) {
                return null;
            }
            GameProfile profile = new GameProfile(uuid, name);
            MinecraftSessionService sessionService = Minecraft.getInstance().getMinecraftSessionService();
            if (sessionService != null) {
                GameProfile filled = sessionService.fillProfileProperties(profile, false);
                if (filled != null) {
                    profile = filled;
                }
            }
            if (!SkinModelUtil.hasSkinTexture(profile)) {
                return null;
            }
            return GameProfileSerializer.serialize(profile);
        } catch (Throwable throwable) {
            return null;
        }
    }

    /**
     * Mojang 公开接口：名字 -> UUID。
     */
    private static UUID lookupUuid(String name) {
        try {
            String encodedName = URLEncoder.encode(name, "UTF-8");
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + encodedName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "RotP-YellowTemperance/1.16.5");
            int code = connection.getResponseCode();
            MojangApiLimiter.reportResult(code, connection.getHeaderField("Retry-After"));
            if (code != 200) {
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
            return parseUndashedUuid(object.get("id").getAsString());
        } catch (Throwable throwable) {
            return null;
        }
    }

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
}