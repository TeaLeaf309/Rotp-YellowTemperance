package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 游戏档案获取器 / Game Profile Fetcher
 * 处理玩家游戏档案的异步获取、缓存和管理
 * Handles asynchronous fetching, caching and management of player game profiles
 */
public class GameProfileFetcher {
    // 添加速率限制器：每秒最多 1 次调用 / Add rate limiter: max 1 call per second
    private static final RateLimiter rateLimiter = new RateLimiter(1000); // 1000ms = 1秒 / 1 second

    /**
     * 异步获取游戏档案 / Get Game Profile Asynchronously
     * 异步获取指定玩家名的完整游戏档案信息
     * @param playerName 玩家名称 / Player name
     * @return 游戏档案的CompletableFuture / CompletableFuture of game profile
     */
    public static CompletableFuture<GameProfile> getProfileAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> getProfile(playerName));
    }

    /**
     * 获取玩家游戏档案 / Get Player Game Profile
     * 按优先级顺序获取指定玩家的完整游戏档案
     * @param playerName 玩家名称 / Player name
     * @return 玩家的游戏档案 / Player's game profile
     */
    public static GameProfile getProfile(String playerName) {
        // 1. 尝试从服务器缓存获取 / 1. Try to get from server cache
        GameProfile profile = getProfileFromServer(playerName);
        if (profile != null && profile.getProperties().containsKey("textures")) {
            return profile;
        }

        // 2. 尝试从在线玩家获取 / 2. Try to get from online players
        profile = getProfileFromOnlinePlayer(playerName);
        if (profile != null && profile.getProperties().containsKey("textures")) {
            return profile;
        }

        // 3. 尝试从第三方API获取（支持离线玩家）/ 3. Try to get from third-party API (supports offline players)
        profile = getProfileFromThirdParty(playerName);
        if (profile != null && profile.getProperties().containsKey("textures")) {
            return profile;
        }

        // 4. 创建带有默认皮肤的profile / 4. Create profile with default skin
        return createProfileWithDefaultSkin(playerName);
    }

    /**
     * 从在线玩家获取档案 / Get Profile From Online Player
     * 尝试从当前在线的同名玩家获取游戏档案
     * @param playerName 玩家名称 / Player name
     * @return 在线玩家的档案或null / Online player's profile or null
     */
    private static GameProfile getProfileFromOnlinePlayer(String playerName) {
        try {
            if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
                net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
                net.minecraft.entity.player.ServerPlayerEntity onlinePlayer = server.getPlayerList().getPlayerByName(playerName);
                if (onlinePlayer != null) {
                    GameProfile onlineProfile = onlinePlayer.getGameProfile();
                    if (onlineProfile.getProperties().containsKey("textures")) {
                        RotPYellowTemperanceAddon.LOGGER.debug("Found online player profile for: {}", playerName);
                        return onlineProfile;
                    }
                }
            }
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to get online player profile: {}", e.getMessage());
        }
        return null;
    }

    private static GameProfile getProfileFromServer(String playerName) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            PlayerProfileCache profileCache = ServerLifecycleHooks.getCurrentServer().getProfileCache();
            if (profileCache != null) {
                GameProfile profile = profileCache.get(playerName);
                if (profile != null && profile.getProperties().containsKey("textures")) {
                    return profile;
                }

                // 尝试填充属性 / Try to fill properties
                try {
                    MinecraftSessionService sessionService = ServerLifecycleHooks.getCurrentServer().getSessionService();
                    if (sessionService != null && profile != null) {
                        // 调用前进行速率限制（每秒最多1次）/ Apply rate limiting before calling (max 1 per second)
                        rateLimiter.acquire();
                        GameProfile filledProfile = sessionService.fillProfileProperties(profile, false);
                        if (filledProfile != null && filledProfile.getProperties().containsKey("textures")) {
                            return filledProfile;
                        }
                    }
                } catch (Exception e) {
                    RotPYellowTemperanceAddon.LOGGER.debug("Failed to fill profile from official API: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * 从第三方API获取档案 / Get Profile From Third Party API
     * 从外部API获取玩家档案信息（当前已禁用）
     * @param playerName 玩家名称 / Player name
     * @return 第三方档案或null / Third-party profile or null
     */
    private static GameProfile getProfileFromThirdParty(String playerName) {
        // 已禁用第三方API以避免阻塞 / Third-party API disabled to avoid blocking
        RotPYellowTemperanceAddon.LOGGER.debug("Third-party API disabled for player: {}", playerName);
        return null;
    }

    /*
     * 已禁用：API请求可能导致阻塞 / Disabled: API requests may cause blocking
     * private static GameProfile fetchProfileFromAPI(String playerName)
     */

    /**
     * 创建带默认皮肤的档案 / Create Profile With Default Skin
     * 为指定玩家创建带默认皮肤的离线GameProfile
     * @param playerName 玩家名称 / Player name
     * @return 带默认皮肤的GameProfile / GameProfile with default skin
     */
    private static GameProfile createProfileWithDefaultSkin(String playerName) {
        UUID playerUUID = generateOfflineUUID(playerName);
        GameProfile profile = new GameProfile(playerUUID, playerName);

        // 优先根据在线玩家判断，然后才是UUID / Prioritize online player detection, then UUID
        boolean isAlexModel = isPlayerSlimModel(playerName) || isAlexSkin(playerUUID);
        String defaultSkinUrl = isAlexModel ?
                "http://textures.minecraft.net/texture/3b60a1f6d562f52aaebbf1434f1de147933a3affe0e764fa49ea057536623cd3" : // Alex默认皮肤 / Alex default skin
                "http://textures.minecraft.net/texture/1a4af718455d4aab528e7a61f86fa25e6a369d1768dcb13f7df319a713eb810b";   // Steve默认皮肤 / Steve default skin

        String textureValue = createTextureProperty(defaultSkinUrl, playerName);
        profile.getProperties().put("textures", new Property("textures", textureValue));

        String modelType = isAlexModel ? "Alex" : "Steve";
        RotPYellowTemperanceAddon.LOGGER.debug("Created profile with {} default skin for: {}", modelType, playerName);
        return profile;
    }

    /**
     * 生成离线UUID / Generate Offline UUID
     * 根据玩家名称生成标准的离线模式UUID
     * @param playerName 玩家名称 / Player name
     * @return 离线UUID / Offline UUID
     */
    private static UUID generateOfflineUUID(String playerName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
    }

    /**
     * 从API获取皮肤URL / Get Skin URL From API
     * 从指定API获取玩家皮肤的URL地址
     * @param apiUrl API地址 / API URL
     * @param playerName 玩家名称 / Player name
     * @return 皮肤URL或null / Skin URL or null
     */
    private static String getSkinUrlFromAPI(String apiUrl, String playerName) {
        // 这里应该实现HTTP请求逻辑 / HTTP request logic should be implemented here
        // 为了简化，返回null让它使用默认皮肤 / For simplicity, return null to use default skin
        return null;
    }

    /**
     * 创建纹理属性 / Create Texture Property
     * 创建包含皮肤URL和模型信息的Base64编码纹理属性
     * @param skinUrl 皮肤URL / Skin URL
     * @param playerName 玩家名称 / Player name
     * @return Base64编码的纹理属性 / Base64 encoded texture property
     */
    private static String createTextureProperty(String skinUrl, String playerName) {
        UUID playerUUID = generateOfflineUUID(playerName);
        // 优先根据玩家名判断，然后才是UUID / Prioritize player name detection, then UUID
        boolean isAlexModel = isPlayerSlimModel(playerName) || isAlexSkin(playerUUID);
        String skinModel = isAlexModel ? "slim" : "default";

        // 创建带有模型信息的texture属性 / Create texture property with model information
        String textureJson = String.format(
                "{\"timestamp\":%d,\"profileId\":\"%s\",\"profileName\":\"%s\",\"textures\":{\"SKIN\":{\"url\":\"%s\",\"metadata\":{\"model\":\"%s\"}}}}",
                System.currentTimeMillis(),
                playerUUID.toString().replace("-", ""),
                playerName,
                skinUrl,
                skinModel
        );

        RotPYellowTemperanceAddon.LOGGER.debug("Created texture property for {}: model={}", playerName, skinModel);

        // Base64编码 / Base64 encoding
        return java.util.Base64.getEncoder().encodeToString(textureJson.getBytes());
    }

    /**
     * 判断玩家是否为Slim模型 / Determine if Player Uses Slim Model
     * 判断指定玩家应该使用Alex(slim)还是Steve(default)皮肤模型
     * @param playerName 玩家名称 / Player name
     * @return 是否为Slim模型 / Whether uses Slim model
     */
    private static boolean isPlayerSlimModel(String playerName) {
        // 特殊情况：直接根据知名玩家名判断 / Special case: direct judgment based on known player names
        if ("Alex".equalsIgnoreCase(playerName)) {
            RotPYellowTemperanceAddon.LOGGER.debug("Player {} is Alex, using slim model", playerName);
            return true;
        }
        if ("Steve".equalsIgnoreCase(playerName)) {
            RotPYellowTemperanceAddon.LOGGER.debug("Player {} is Steve, using default model", playerName);
            return false;
        }

        // 检查在线玩家的实际皮肤模型（被伪装的玩家）/ Check actual skin model of online players (disguised players)
        try {
            if (net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer() != null) {
                net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
                net.minecraft.entity.player.ServerPlayerEntity onlinePlayer = server.getPlayerList().getPlayerByName(playerName);
                if (onlinePlayer != null) {
                    // 通过GameProfile的textures属性检查皮肤模型 / Check skin model through GameProfile's textures property
                    GameProfile profile = onlinePlayer.getGameProfile();
                    if (profile.getProperties().containsKey("textures")) {
                        Property textureProperty = profile.getProperties().get("textures").iterator().next();
                        String decodedJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));
                        boolean isSlim = decodedJson.contains("\"model\":\"slim\"");
                        RotPYellowTemperanceAddon.LOGGER.debug("Online player {} texture data contains slim model: {}", playerName, isSlim);
                        return isSlim;
                    } else {
                        // 如果没有textures属性，使用被伪装玩家的UUID判断 / If no textures property, use disguised player's UUID for judgment
                        boolean isSlim = net.minecraft.client.resources.DefaultPlayerSkin.getSkinModelName(profile.getId()).equals("slim");
                        RotPYellowTemperanceAddon.LOGGER.debug("Online player {} no texture data, using target player UUID algorithm: {}", playerName, isSlim);
                        return isSlim;
                    }
                }
            }
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to check online player model: {}", e.getMessage());
        }

        // 最后的回退：根据被伪装玩家名生成UUID并判断 / Final fallback: generate UUID from disguised player name and judge
        UUID playerUUID = generateOfflineUUID(playerName);
        boolean isSlim = net.minecraft.client.resources.DefaultPlayerSkin.getSkinModelName(playerUUID).equals("slim");
        RotPYellowTemperanceAddon.LOGGER.debug("Player {} not online, using target player generated UUID algorithm: {}", playerName, isSlim);
        return isSlim;
    }

    /**
     * 判断是否为Alex皮肤 / Determine if Alex Skin
     * 使用Minecraft官方算法判断指定UUID是否对应Alex皮肤模型
     * @param uuid 玩家UUID / Player UUID
     * @return 是否为Alex皮肤 / Whether is Alex skin
     */
    private static boolean isAlexSkin(UUID uuid) {
        // 使用Minecraft官方的Alex模型判断逻辑 / Use Minecraft's official Alex model determination logic
        if (uuid == null) {
            return false;
        }
        // 使用Minecraft官方的DefaultPlayerSkin算法 / Use Minecraft's official DefaultPlayerSkin algorithm
        return net.minecraft.client.resources.DefaultPlayerSkin.getSkinModelName(uuid).equals("slim");
    }

    /**
     * 获取皮肤纹理 / Get Skin Texture
     * 异步获取指定GameProfile对应的皮肤纹理资源位置
     * @param profile 玩家档案 / Player profile
     * @return 皮肤纹理的CompletableFuture / CompletableFuture of skin texture
     */
    public static CompletableFuture<ResourceLocation> getSkinTexture(GameProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. 尝试从官方服务获取 / 1. Try to get from official service
            try {
                MinecraftSessionService sessionService = Minecraft.getInstance().getMinecraftSessionService();
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = sessionService.getTextures(profile, false);

                if (textures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    ResourceLocation skinTexture = Minecraft.getInstance().getSkinManager().registerTexture(
                            textures.get(MinecraftProfileTexture.Type.SKIN),
                            MinecraftProfileTexture.Type.SKIN
                    );
                    RotPYellowTemperanceAddon.LOGGER.debug("Successfully loaded official skin for {}", profile.getName());
                    return skinTexture;
                }
            } catch (Exception e) {
                RotPYellowTemperanceAddon.LOGGER.debug("Failed to fetch official skin for {}: {}", profile.getName(), e.getMessage());
            }

            // 2. 尝试从自定义属性获取 / 2. Try to get from custom properties
            try {
                if (profile.getProperties().containsKey("textures")) {
                    Property textureProperty = profile.getProperties().get("textures").iterator().next();
                    ResourceLocation customSkin = loadCustomSkinTexture(profile.getName(), textureProperty.getValue());
                    if (customSkin != null) {
                        RotPYellowTemperanceAddon.LOGGER.debug("Successfully loaded custom skin for {}", profile.getName());
                        return customSkin;
                    }
                }
            } catch (Exception e) {
                RotPYellowTemperanceAddon.LOGGER.debug("Failed to load custom skin for {}: {}", profile.getName(), e.getMessage());
            }

            // 3. 使用默认皮肤（根据玩家名生成稳定的皮肤）/ 3. Use default skin (generate stable skin based on player name)
            ResourceLocation defaultSkin = getStableDefaultSkin(profile.getName(), profile.getId());
            RotPYellowTemperanceAddon.LOGGER.debug("Using stable default skin for {}", profile.getName());
            return defaultSkin;
        });
    }

    /**
     * 加载自定义皮肤纹理 / Load Custom Skin Texture
     * 从Base64编码的纹理数据加载自定义皮肤
     * @param playerName 玩家名称 / Player name
     * @param textureData 纹理数据 / Texture data
     * @return 皮肤纹理资源位置或null / Skin texture resource location or null
     */
    private static ResourceLocation loadCustomSkinTexture(String playerName, String textureData) {
        try {
            // 解码Base64数据 / Decode Base64 data
            String decodedJson = new String(java.util.Base64.getDecoder().decode(textureData));
            // 这里可以解析JSON并加载皮肤，但为了简化返回null / JSON can be parsed and skin loaded here, but returns null for simplicity
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取稳定的默认皮肤 / Get Stable Default Skin
     * 根据玩家信息获取稳定的默认皮肤资源位置
     * @param playerName 玩家名称 / Player name
     * @param playerId 玩家UUID / Player UUID
     * @return 默认皮肤资源位置 / Default skin resource location
     */
    private static ResourceLocation getStableDefaultSkin(String playerName, UUID playerId) {
        // 根据玩家名的哈希值选择固定的默认皮肤 / Select fixed default skin based on player name hash
        if (playerId != null) {
            return DefaultPlayerSkin.getDefaultSkin(playerId);
        }

        // 如果UUID为null，根据玩家名生成 / If UUID is null, generate based on player name
        UUID generatedId = generateOfflineUUID(playerName);
        return DefaultPlayerSkin.getDefaultSkin(generatedId);
    }

    /**
     * 速率限制器类 / Rate Limiter Class
     * 控制API调用频率，防止请求过于频繁
     */
    // 简单的速率限制器 / Simple rate limiter
    private static class RateLimiter {
        private final long intervalMillis;
        private final AtomicLong lastCall = new AtomicLong(0);

        /**
         * 构造速率限制器 / Construct Rate Limiter
         * @param intervalMillis 间隔毫秒数 / Interval in milliseconds
         */
        public RateLimiter(long intervalMillis) {
            this.intervalMillis = intervalMillis;
        }

        /**
         * 获取执行许可 / Acquire Execution Permit
         * 等待直到可以执行下一次调用
         */
        public void acquire() {
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