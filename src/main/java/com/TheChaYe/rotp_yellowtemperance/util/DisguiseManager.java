package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitCapabilities;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.BatchDisguiseSyncPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncNbtPacket;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.SyncNbtToClientPacket;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 伪装管理系统 / Disguise Management System
 * 处理玩家伪装的核心逻辑、数据管理和网络同步
 * Manages core disguise logic, data handling and network synchronization for player disguises
 */
public class DisguiseManager {
    // 缓存最近查询的玩家数据，避免重复网络请求 / Cache recent player data to avoid duplicate network requests
    private static final Map<String, GameProfile> profileCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_TIME = 3600000; // 60分钟缓存时间 / 60 minute cache time

    /**
     * 应用玩家伪装 / Apply Player Disguise
     * 为主线程快速初始化伪装状态，然后异步处理完整的profile获取
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     */
    public static void applyDisguise(PlayerEntity player, String name) {
        // 立即在主线程设置基本伪装状态，防止渲染空白 / Initialize basic disguise state immediately on main thread to prevent rendering blanks
        initializeBasicDisguise(player, name);

        // 立即发送准备状态给客户端 / Send preparation status to client immediately
        sendImmediatePreparationPacket(player, name);

        // 完全异步处理复杂的profile获取 / Fully asynchronous processing of complex profile fetching
        processDisguiseAsync(player, name);
    }

    /**
     * 初始化基础伪装状态 / Initialize Basic Disguise State
     * 在主线程快速设置基础伪装数据，确保客户端能立即显示伪装效果
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     */
    private static void initializeBasicDisguise(PlayerEntity player, String name) {
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            cap.setDisguiseName(name);
            cap.setHasDisguise(true);
        });

        CompoundNBT nbt = player.getPersistentData();
        nbt.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
        nbt.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
    }

    /**
     * 发送伪装准备状态包 / Send Disguise Preparation Packet
     * 立即向追踪该玩家的客户端发送伪装准备状态
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     */
    private static void sendImmediatePreparationPacket(PlayerEntity player, String name) {
        if (!player.level.isClientSide() && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            CompoundNBT syncData = createPreparationSyncData(name);

            // 直接执行，不等待 / Execute directly without waiting
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                PacketHandler.sendToAllTracking(new SyncNbtPacket(player.getUUID(), syncData), serverPlayer);
            });
        }
    }

    /**
     * 异步处理完整伪装流程 / Process Complete Disguise Asynchronously
     * 在后台线程中处理复杂的玩家数据获取和皮肤加载
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     */
    private static void processDisguiseAsync(PlayerEntity player, String name) {
        CompletableFuture.runAsync(() -> {
            try {
                // 第一步：检查缓存 / Step 1: Check cache
                GameProfile profile = checkCacheFirst(name);
                if (profile != null) {
                    completeDisguiseProcess(player, name, profile, true);
                    return;
                }

                // 第二步：如果缓存未命中，在后台线程获取完整数据 / Step 2: If cache miss, fetch complete data in background thread
                profile = fetchFullProfileBackground(name);
                completeDisguiseProcess(player, name, profile, false);

            } catch (Exception e) {
                handleAsyncError(player, name, e);
            }
        });
    }

    /**
     * 快速缓存检查 / Quick Cache Check
     * 快速检查指定玩家名是否在缓存中且未过期
     * @param name 玩家名称 / Player name
     * @return 缓存的GameProfile或null / Cached GameProfile or null
     */
    private static GameProfile checkCacheFirst(String name) {
        CachedProfile cached = getCachedProfile(name);
        if (cached != null && !cached.isExpired()) {
            RotPYellowTemperanceAddon.LOGGER.debug("Using cached profile for player: {}", name);
            return cached.profile;
        }
        return null;
    }

    /**
     * 后台获取完整玩家档案 / Background Full Profile Fetch
     * 在专用后台线程中获取完整的玩家档案数据
     * @param name 玩家名称 / Player name
     * @return 完整的GameProfile / Complete GameProfile
     */
    private static GameProfile fetchFullProfileBackground(String name) {
        // 使用GameProfileFetcher的异步方法 / Use GameProfileFetcher's async method
        try {
            return GameProfileFetcher.getProfileAsync(name).get(5000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.warn("Background profile fetch timeout for {}, creating basic profile: {}", name, e.getMessage());
            return createBasicProfile(name);
        }
    }

    /**
     * 完成伪装过程 / Complete Disguise Process
     * 在服务器主线程中完成伪装的最终设置和网络同步
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     * @param profile 玩家档案 / Player profile
     * @param wasCached 是否来自缓存 / Whether from cache
     */
    private static void completeDisguiseProcess(PlayerEntity player, String name, final GameProfile profile, boolean wasCached) {
        // 确保profile有效（使用final变量）/ Ensure profile is valid (using final variable)
        final GameProfile validProfile = (profile != null) ? profile : createBasicProfile(name);

        // 确保有皮肤属性，并且包含准确的模型信息 / Ensure has skin properties with accurate model information
        if (!validProfile.getProperties().containsKey("textures")) {
            addDefaultTextureProperty(validProfile, name);
        } else {
            // 如果已有textures属性，验证并修正模型信息 / If textures property exists, validate and correct model info
            validateAndUpdateTextureProperty(validProfile, name);
        }

        // 序列化profile用于传输 / Serialize profile for transmission
        final CompoundNBT profileNbt = GameProfileSerializer.serialize(validProfile);

        // 在服务器线程中完成最终更新和通知 / Complete final updates and notifications in server thread
        if (!player.level.isClientSide() && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                try {
                    // 更新本地数据 / Update local data
                    finalizeDisguiseData(player, name, profileNbt);

                    // 发送最终数据包 / Send final data packet
                    sendFinalDisguisePacket(serverPlayer, name, profileNbt, wasCached);
                    
                    // 新增：全局广播，确保所有在线玩家（包括新登录的）都能收到 / New: Global broadcast to ensure all online players (including newly logged in) receive it
                    CompoundNBT globalSyncData = new CompoundNBT();
                    globalSyncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
                    globalSyncData.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
                    globalSyncData.put("disguiseProfile", profileNbt);
                    globalSyncData.putBoolean("disguiseReady", true);
                    globalSyncData.putBoolean("wasCached", wasCached);
                    globalSyncData.putLong("disguiseCompleteTime", System.currentTimeMillis());
                    PacketHandler.sendToAllClients(new SyncNbtToClientPacket(player.getUUID(), globalSyncData));

                    // 记录日志 / Log the event
                    String playerName = serverPlayer.getName().getString();
                    logDisguiseCompletion(name, playerName, validProfile, wasCached);
                } catch (Exception e) {
                    RotPYellowTemperanceAddon.LOGGER.error("Error finalizing disguise for {}: {}", name, e.getMessage());
                }
            });
        }
    }

    /**
     * 创建伪装准备同步数据 / Create Disguise Preparation Sync Data
     * 创建用于客户端初始显示的伪装准备状态数据
     * @param name 伪装的玩家名称 / Disguise player name
     * @return 准备状态的NBT数据 / Preparation state NBT data
     */
    private static CompoundNBT createPreparationSyncData(String name) {
        CompoundNBT syncData = new CompoundNBT();
        syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
        syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
        syncData.putBoolean("preparingDisguise", true);
        syncData.putLong("disguiseStartTime", System.currentTimeMillis());
        return syncData;
    }

    /**
     * 最终化伪装数据 / Finalize Disguise Data
     * 更新玩家的Capability和NBT数据以完成伪装设置
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     * @param profileNbt 玩家档案NBT / Player profile NBT
     */
    private static void finalizeDisguiseData(PlayerEntity player, String name, CompoundNBT profileNbt) {
        // 更新Capability / Update Capability
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            cap.setDisguiseProfile(profileNbt);
        });

        // 更新NBT / Update NBT
        CompoundNBT nbt = player.getPersistentData();
        nbt.put("disguiseProfile", profileNbt);
    }

    /**
     * 发送最终伪装数据包 / Send Final Disguise Packet
     * 向追踪该玩家的所有客户端发送完整的伪装数据
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     * @param profileNbt 玩家档案NBT / Player profile NBT
     * @param wasCached 是否来自缓存 / Whether from cache
     */
    private static void sendFinalDisguisePacket(ServerPlayerEntity player, String name, CompoundNBT profileNbt, boolean wasCached) {
        CompoundNBT syncData = new CompoundNBT();
        syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
        syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
        syncData.put("disguiseProfile", profileNbt);
        syncData.putBoolean("disguiseReady", true);
        syncData.putBoolean("wasCached", wasCached);
        syncData.putLong("disguiseCompleteTime", System.currentTimeMillis());

        // 使用 SyncNbtToClientPacket 并通过 PacketHandler 发送给追踪此玩家的所有客户端 / Use SyncNbtToClientPacket and send to all clients tracking this player via PacketHandler
        PacketHandler.sendToAllTracking(new SyncNbtToClientPacket(player.getUUID(), syncData), player);
    }


    /**
     * 记录伪装完成日志 / Log Disguise Completion
     * 记录伪装应用成功的详细日志信息
     * @param name 伪装的玩家名称 / Disguise player name
     * @param playerName 实际玩家名称 / Actual player name
     * @param profile 玩家档案 / Player profile
     * @param wasCached 是否来自缓存 / Whether from cache
     */
    private static void logDisguiseCompletion(String name, String playerName, GameProfile profile, boolean wasCached) {
        boolean isSlim = isAlexSkin(profile.getId());
        String source = wasCached ? "cache" : "network";
        RotPYellowTemperanceAddon.LOGGER.info("Disguise '{}' applied for {} from {} (UUID: {}, isSlim: {})",
                name, playerName, source, profile.getId(), isSlim);
    }

    /**
     * 处理异步伪装错误 / Handle Async Disguise Error
     * 处理伪装过程中发生的异步错误
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     * @param error 错误异常 / Error exception
     */
    private static void handleAsyncError(PlayerEntity player, String name, Exception error) {
        RotPYellowTemperanceAddon.LOGGER.error("Async disguise error for {}: {}", name, error.getMessage());

        if (!player.level.isClientSide() && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                sendErrorPacket(serverPlayer, name);
            });
        }
    }

    /**
     * 发送伪装错误数据包 / Send Disguise Error Packet
     * 向客户端发送伪装失败的错误信息
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     */
    private static void sendErrorPacket(ServerPlayerEntity player, String name) {
        CompoundNBT errorData = new CompoundNBT();
        errorData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, false);
        errorData.putString(InitTags.YT_DISGUISE_NAME_TAG, "");
        errorData.putBoolean("disguiseError", true);
        errorData.putString("errorMessage", "Failed to fetch player data");

        PacketHandler.sendToAllTracking(new SyncNbtToClientPacket(player.getUUID(), errorData), player);
    }

    /**
     * 获取缓存的玩家档案 / Get Cached Player Profile
     * 从缓存中获取指定玩家名的档案信息
     * @param name 玩家名称 / Player name
     * @return 缓存条目或null / Cached entry or null
     */
    private static CachedProfile getCachedProfile(String name) {
        GameProfile profile = profileCache.get(name.toLowerCase());
        return profile != null ? new CachedProfile(profile) : null;
    }

    /**
     * 处理获取到的玩家档案 / Process Retrieved Player Profile
     * 处理成功获取的玩家档案数据并应用伪装
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     * @param profile 玩家档案 / Player profile
     */
    private static void handleProfileResult(PlayerEntity player, String name, GameProfile profile) {
        // 确保 profile 始终有效 / Ensure profile is always valid
        if (profile == null) {
            RotPYellowTemperanceAddon.LOGGER.warn("Failed to create profile for {}, creating basic profile", name);
            profile = createBasicProfile(name);
        }

        // 如果 profile 没有皮肤属性，添加默认属性 / If profile lacks skin properties, add default properties
        if (!profile.getProperties().containsKey("textures")) {
            addDefaultTextureProperty(profile, name);
        }

        // 批量更新所有数据 / Bulk update all data
        updateDisguiseData(player, name, profile);

        // 发送最终的伪装数据 / Send final disguise data
        sendFinalDisguisePacket(player, name, profile);

        // 调试日志 / Debug logging
        logDisguiseInfo(name, player, profile);
    }

    /**
     * 批量更新伪装数据 / Bulk Update Disguise Data
     * 同时更新玩家的Capability和NBT数据以提高性能
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     * @param profile 玩家档案 / Player profile
     */
    private static void updateDisguiseData(PlayerEntity player, String name, GameProfile profile) {
        // 同时更新Capability和NBT，减少重复操作 / Update both Capability and NBT simultaneously to reduce redundant operations
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            cap.setDisguiseProfile(GameProfileSerializer.serialize(profile));
        });

        CompoundNBT nbt = player.getPersistentData();
        nbt.put("disguiseProfile", GameProfileSerializer.serialize(profile));
    }

    /**
     * 发送最终伪装数据包 / Send Final Disguise Packet
     * 向追踪玩家的客户端发送序列化的伪装数据
     * @param player 目标玩家 / Target player
     * @param name 伪装的玩家名称 / Disguise player name
     * @param profile 玩家档案 / Player profile
     */
    private static void sendFinalDisguisePacket(PlayerEntity player, String name, GameProfile profile) {
        if (!player.level.isClientSide() && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            CompoundNBT syncData = new CompoundNBT();
            syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, true);
            syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, name);
            syncData.put("disguiseProfile", GameProfileSerializer.serialize(profile));
            syncData.putBoolean("disguiseReady", true);

            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                PacketHandler.sendToAllTracking(new SyncNbtToClientPacket(player.getUUID(), syncData), serverPlayer);
            });
        }
    }

    /**
     * 记录伪装信息日志 / Log Disguise Information
     * 记录伪装应用的详细调试信息
     * @param name 伪装的玩家名称 / Disguise player name
     * @param player 目标玩家 / Target player
     * @param profile 玩家档案 / Player profile
     */
    private static void logDisguiseInfo(String name, PlayerEntity player, GameProfile profile) {
        boolean isSlim = isAlexSkin(profile.getId());
        RotPYellowTemperanceAddon.LOGGER.debug("Applied disguise '{}' for player: {} (Profile UUID: {}, isSlim: {})",
                name, player.getName().getString(), profile.getId(), isSlim);

        // 输出皮肤属性信息 / Output skin property information
        if (profile.getProperties().containsKey("textures")) {
            Property textureProperty = profile.getProperties().get("textures").iterator().next();
            try {
                String decodedJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));
                RotPYellowTemperanceAddon.LOGGER.debug("Texture JSON: {}", decodedJson);
            } catch (Exception e) {
                RotPYellowTemperanceAddon.LOGGER.debug("Failed to decode texture JSON: {}", e.getMessage());
            }
        }
    }

    /**
     * 创建基础玩家档案 / Create Basic Player Profile
     * 为指定玩家名创建基础的离线GameProfile
     * @param name 玩家名称 / Player name
     * @return 基础GameProfile / Basic GameProfile
     */
    private static GameProfile createBasicProfile(String name) {
        UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        return new GameProfile(offlineUUID, name);
    }

    /**
     * 添加默认纹理属性 / Add Default Texture Property
     * 为GameProfile添加默认的皮肤纹理属性
     * @param profile 目标档案 / Target profile
     * @param name 玩家名称 / Player name
     */
    private static void addDefaultTextureProperty(GameProfile profile, String name) {
        try {
            // 更精确地确定皮肤模型类型 / More accurately determine skin model type
            boolean isAlexModel = determineAccurateSkinModel(name, profile.getId());
            String skinModel = isAlexModel ? "slim" : "default";

            RotPYellowTemperanceAddon.LOGGER.debug("Determined skin model for {} (UUID: {}): {} (isAlexModel: {})", name, profile.getId(), skinModel, isAlexModel);

            // 创建带有模型信息的皮肤属性 / Create skin property with model information
            String textureJson = String.format(
                    "{\"timestamp\":%d,\"profileId\":\"%s\",\"profileName\":\"%s\",\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/default\",\"metadata\":{\"model\":\"%s\"}}}}",
                    System.currentTimeMillis(),
                    profile.getId().toString().replace("-", ""),
                    profile.getName(),
                    skinModel
            );

            String encodedTexture = java.util.Base64.getEncoder().encodeToString(textureJson.getBytes());
            profile.getProperties().put("textures", new Property("textures", encodedTexture));

            RotPYellowTemperanceAddon.LOGGER.debug("Added {} model texture property for {} (name: {}, JSON: {})", skinModel, profile.getName(), name, textureJson);
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.warn("Failed to add default texture property for {}: {}", profile.getName(), e.getMessage());
        }
    }

    /**
     * 精确皮肤模型判断 / Accurate Skin Model Detection
     * 通过多种方式精确判断玩家应该使用的皮肤模型类型
     * @param playerName 玩家名称 / Player name
     * @param playerUUID 玩家UUID / Player UUID
     * @return 是否为Alex模型（slim）/ Whether is Alex model (slim)
     */
    private static boolean determineAccurateSkinModel(String playerName, UUID playerUUID) {
        // 1. 首先检查在线玩家的实际皮肤模型 / 1. First check actual skin model of online players
        try {
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                ServerPlayerEntity onlinePlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
                if (onlinePlayer != null) {
                    GameProfile onlineProfile = onlinePlayer.getGameProfile();
                    if (onlineProfile.getProperties().containsKey("textures")) {
                        Property textureProperty = onlineProfile.getProperties().get("textures").iterator().next();
                        String decodedJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));
                        boolean isSlim = decodedJson.contains("\"model\":\"slim\"") || decodedJson.contains("\"model\" : \"slim\"");
                        RotPYellowTemperanceAddon.LOGGER.debug("Online player {} has slim model: {}", playerName, isSlim);
                        return isSlim;
                    }
                }
            }
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to check online player model for {}: {}", playerName, e.getMessage());
        }

        // 2. 检查缓存中的profile数据 / 2. Check profile data in cache
        GameProfile cachedProfile = profileCache.get(playerName.toLowerCase());
        if (cachedProfile != null && cachedProfile.getProperties().containsKey("textures")) {
            try {
                Property textureProperty = cachedProfile.getProperties().get("textures").iterator().next();
                String decodedJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));
                boolean isSlim = decodedJson.contains("\"model\":\"slim\"") || decodedJson.contains("\"model\" : \"slim\"");
                RotPYellowTemperanceAddon.LOGGER.debug("Cached profile for {} has slim model: {}", playerName, isSlim);
                return isSlim;
            } catch (Exception e) {
                RotPYellowTemperanceAddon.LOGGER.debug("Failed to parse cached profile for {}: {}", playerName, e.getMessage());
            }
        }

        // 3. 使用更可靠的UUID算法（基于玩家名生成）/ 3. Use more reliable UUID algorithm (generated from player name)
        if (playerUUID != null) {
            // 对于离线UUID，使用Minecraft官方算法
            boolean isSlim = net.minecraft.client.resources.DefaultPlayerSkin.getSkinModelName(playerUUID).equals("slim");
            RotPYellowTemperanceAddon.LOGGER.debug("UUID-based model detection for {}: {} (UUID: {})", playerName, isSlim, playerUUID);
            return isSlim;
        }

        // 4. 最后的回退方案 / 4. Final fallback solution
        UUID fallbackUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
        boolean isSlim = net.minecraft.client.resources.DefaultPlayerSkin.getSkinModelName(fallbackUUID).equals("slim");
        RotPYellowTemperanceAddon.LOGGER.debug("Fallback model detection for {}: {} (generated UUID: {})", playerName, isSlim, fallbackUUID);
        return isSlim;
    }

    /**
     * 验证并更新纹理属性 / Validate and Update Texture Properties
     * 验证现有纹理属性的完整性并添加缺失的模型信息
     * @param profile 目标档案 / Target profile
     * @param name 玩家名称 / Player name
     */
    private static void validateAndUpdateTextureProperty(GameProfile profile, String name) {
        try {
            Property textureProperty = profile.getProperties().get("textures").iterator().next();
            String decodedJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));
            
            // 检查是否包含模型信息 / Check if model information is included
            if (!decodedJson.contains("\"model\"")) {
                // 如果没有模型信息，添加准确的模型信息 / If no model information, add accurate model information
                boolean isAlexModel = determineAccurateSkinModel(name, profile.getId());
                String skinModel = isAlexModel ? "slim" : "default";
                
                // 重构JSON以包含模型信息 / Restructure JSON to include model information
                String updatedJson = decodedJson.replace("}}}", "},\"metadata\":{\"model\":\"" + skinModel + "\"}}}");
                String encodedTexture = java.util.Base64.getEncoder().encodeToString(updatedJson.getBytes());
                
                // 替换原有的textures属性 / Replace original textures property
                profile.getProperties().removeAll("textures");
                profile.getProperties().put("textures", new Property("textures", encodedTexture));
                
                RotPYellowTemperanceAddon.LOGGER.debug("Updated texture property for {} with model: {}", name, skinModel);
            }
        } catch (Exception e) {
            RotPYellowTemperanceAddon.LOGGER.debug("Failed to validate/update texture property for {}: {}", name, e.getMessage());
        }
    }

    /**
     * 判断是否为Alex皮肤模型 / Determine if Alex Skin Model
     * 使用Minecraft官方算法判断指定UUID是否对应Alex模型
     * @param uuid 玩家UUID / Player UUID
     * @return 是否为Alex模型 / Whether is Alex model
     */
    private static boolean isAlexSkin(UUID uuid) {
        // 使用Minecraft官方的Alex模型判断逻辑 / Use Minecraft's official Alex model determination logic
        if (uuid == null) {
            return false;
        }

        // 使用Minecraft官方的DefaultPlayerSkin算法 / Use Minecraft's official DefaultPlayerSkin algorithm
        boolean isAlex = net.minecraft.client.resources.DefaultPlayerSkin.getSkinModelName(uuid).equals("slim");
        RotPYellowTemperanceAddon.LOGGER.debug("UUID {} isAlex: {}", uuid, isAlex);
        return isAlex;
    }

    /**
     * 移除玩家伪装 / Remove Player Disguise
     * 移除指定玩家的伪装状态并同步到客户端
     * @param player 目标玩家 / Target player
     */
    public static void removeDisguise(PlayerEntity player) {
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            cap.setHasDisguise(false);
        });

        CompoundNBT nbt = player.getPersistentData();
        nbt.remove(InitTags.YT_HAS_DISGUISE_TAG);
        nbt.remove(InitTags.YT_DISGUISE_NAME_TAG);

        // 只在服务端发送网络包 / Only send network packets on server side / Only send network packets on server side
        if (!player.level.isClientSide() && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            CompoundNBT syncData = new CompoundNBT();
            syncData.putBoolean(InitTags.YT_HAS_DISGUISE_TAG, false);
            syncData.putString(InitTags.YT_DISGUISE_NAME_TAG, "");
            syncData.putBoolean("clearDisguise", true); // 标记需要清理缓存 / Mark for cache cleanup

            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                PacketHandler.sendToAllTracking(new SyncNbtPacket(player.getUUID(), syncData), serverPlayer);
            });
        }

        RotPYellowTemperanceAddon.LOGGER.debug("Removed disguise for player: {}", player.getName().getString());
    }

    /**
     * 同步所有在线玩家的伪装状态 / Synchronize All Online Players' Disguise States
     * 向指定玩家发送服务器上所有在线玩家的伪装状态信息
     * @param targetPlayer 目标玩家 / Target player
     */
    public static void syncAllDisguisesToPlayer(ServerPlayerEntity targetPlayer) {
        if (targetPlayer == null || targetPlayer.level.isClientSide()) {
            return;
        }

        MinecraftServer server = targetPlayer.getServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            try {
                List<BatchDisguiseSyncPacket.PlayerDisguiseData> disguiseDataList = new ArrayList<>();
                
                // 收集所有在线玩家的伪装数据（除了目标玩家自己）/ Collect disguise data of all online players (except target player)/ Collect disguise data of all online players (except target player)
                for (net.minecraft.world.server.ServerWorld world : server.getAllLevels()) {
                    for (PlayerEntity player : world.players()) {
                        if (!(player instanceof ServerPlayerEntity) || player.getUUID().equals(targetPlayer.getUUID())) {
                            continue;
                        }
                        
                        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                        BatchDisguiseSyncPacket.PlayerDisguiseData disguiseData = getPlayerDisguiseData(serverPlayer);
                        if (disguiseData != null) {
                            disguiseDataList.add(disguiseData);
                        }
                    }
                }
                
                // 发送批量同步包 / Send batch synchronization packet / Send batch synchronization packet
                if (!disguiseDataList.isEmpty()) {
                    BatchDisguiseSyncPacket syncPacket = new BatchDisguiseSyncPacket(disguiseDataList);
                    PacketHandler.sendToClient(syncPacket, targetPlayer);
                    RotPYellowTemperanceAddon.LOGGER.info("Sent {} players' disguise data to {}", 
                            disguiseDataList.size(), targetPlayer.getName().getString());
                }
                
            } catch (Exception e) {
                RotPYellowTemperanceAddon.LOGGER.error("Failed to sync disguises to player {}: {}", 
                        targetPlayer.getName().getString(), e.getMessage());
            }
        });
    }

    /**
     * 获取单个玩家的伪装数据 / Get Single Player's Disguise Data
     * 获取指定玩家的完整伪装状态信息
     * @param player 目标玩家 / Target player
     * @return 玩家伪装数据或null / Player disguise data or null / Get Single Player's Disguise Data
     * 获取指定玩家的完整伪装状态信息 / Get Single Player's Disguise Data
     * 获取指定玩家的完整伪装状态信息
     * @param player 目标玩家 / Target player
     * @return 玩家伪装数据或null / Player disguise data or null
     */
    private static BatchDisguiseSyncPacket.PlayerDisguiseData getPlayerDisguiseData(ServerPlayerEntity player) {
        final boolean[] hasDisguise = {false};
        final String[] disguiseName = {""};
        final CompoundNBT[] profileTag = {null};
        
        // 检查Capability / Check Capability / Check Capability / Check Capability
        player.getCapability(InitCapabilities.DISGUISE_CAPABILITY).ifPresent(cap -> {
            if (cap.hasDisguise()) {
                hasDisguise[0] = true;
                disguiseName[0] = cap.getDisguiseName();
                profileTag[0] = cap.getDisguiseProfile();
            }
        });
        
        boolean finalHasDisguise = hasDisguise[0];
        String finalDisguiseName = disguiseName[0];
        CompoundNBT finalProfileTag = profileTag[0];
        
        // 如果Capability中没有，检查NBT数据 / If not in Capability, check NBT data / If not in Capability, check NBT data / If not in Capability, check NBT data
        if (!finalHasDisguise) {
            CompoundNBT playerNbt = player.getPersistentData();
            if (playerNbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && 
                playerNbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
                finalHasDisguise = true;
                finalDisguiseName = playerNbt.getString(InitTags.YT_DISGUISE_NAME_TAG);
                if (playerNbt.contains("disguiseProfile")) {
                    finalProfileTag = playerNbt.getCompound("disguiseProfile");
                }
            }
        }
        
        if (finalHasDisguise) {
            return new BatchDisguiseSyncPacket.PlayerDisguiseData(
                    player.getUUID(), 
                    true, 
                    finalDisguiseName, 
                    finalProfileTag
            );
        }
        
        return null;
    }

    /**
     * 缓存条目类 / Cache Entry Class
     * 用于存储带时间戳的玩家档案缓存条目
     */
    private static class CachedProfile {
        final GameProfile profile;
        final long timestamp;

        CachedProfile(GameProfile profile) {
            this.profile = profile;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_TIME;
        }
    }
}