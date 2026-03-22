// 文档5: ClientDisguiseCache.java
package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.util.GameProfileFetcher;
import com.TheChaYe.rotp_yellowtemperance.util.GameProfileSerializer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 客户端伪装缓存 / Client Disguise Cache
 * 管理玩家伪装的皮肤纹理和配置文件缓存
 */
@OnlyIn(Dist.CLIENT)
public class ClientDisguiseCache {
    /**
     * 皮肤纹理缓存 / Skin texture cache
     */
    public static final ConcurrentHashMap<UUID, ResourceLocation> SKIN_TEXTURE_CACHE = new ConcurrentHashMap<>();
    /**
     * 玩家配置文件缓存 / Player profile cache
     */
    private static final ConcurrentHashMap<UUID, CompoundNBT> PROFILE_CACHE = new ConcurrentHashMap<>();
    /**
     * UUID映射缓存 / UUID mapping cache
     */
    private static final ConcurrentHashMap<UUID, UUID> UUID_CACHE = new ConcurrentHashMap<>();
    /**
     * 异步加载状态跟踪 / Async loading status tracking
     */
    private static final ConcurrentHashMap<UUID, Boolean> LOADING_STATUS = new ConcurrentHashMap<>();
    /**
     * 异步执行器 / Async executor
     */
    private static final Executor ASYNC_EXECUTOR = Util.backgroundExecutor();

    /**
     * 缓存玩家伪装配置文件 / Cache player disguise profile
     * 存储玩家的伪装配置文件并启动皮肤纹理异步加载
     */
    public static void cachePlayerDisguiseProfile(UUID playerId, CompoundNBT profileTag) {
        PROFILE_CACHE.put(playerId, profileTag);
        SKIN_TEXTURE_CACHE.remove(playerId); // 清除旧纹理缓存 / Clear old texture cache
        LOADING_STATUS.put(playerId, true); // 标记为正在加载 / Mark as loading

        // 记录被伪装玩家的UUID以便后续使用 / Record disguised player's UUID for later use
        try {
            GameProfile profile = com.TheChaYe.rotp_yellowtemperance.util.GameProfileSerializer.deserialize(profileTag);
            if (profile != null && profile.getId() != null) {
                UUID_CACHE.put(playerId, profile.getId());

                // 记录详细信息用于调试，包括textures属性 / Record detailed info for debugging, including textures property
                boolean isSlim = DefaultPlayerSkin.getSkinModelName(profile.getId()).equals("slim");

                // 检查并记录textures属性 / Check and record textures property
                if (profile.getProperties().containsKey("textures")) {
                    try {
                        Property textureProperty = profile.getProperties().get("textures").iterator().next();
                        String decodedJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));
                    } catch (Exception ex) {
                    }
                }

                /*                RotPYellowTemperanceAddon.LOGGER.debug("Cached disguise UUID {} for player {} (target: {}, isSlim: {})", profile.getId(), playerId, profile.getName(), isSlim);*/
            }
        } catch (Exception e) {
//            RotPYellowTemperanceAddon.LOGGER.debug("Failed to extract UUID from profile: {}", e.getMessage());
        }

        // 立即启动皮肤纹理的异步加载 / Immediately start async skin texture loading
        loadSkinAsync(playerId).thenAccept(skin -> {
            if (skin != null) {
                SKIN_TEXTURE_CACHE.put(playerId, skin);
            }
            // 加载完成后更新状态 / Update status after loading complete
            LOADING_STATUS.put(playerId, false);

            // 通知服务器伪装加载已完成 / Notify server that disguise loading is complete
            notifyServerLoadingComplete(playerId);
        }).exceptionally(throwable -> {
//            System.err.println("Failed to load skin for player " + playerId + ": " + throwable.getMessage());
            // 加载失败也更新状态 / Update status even on loading failure
            LOADING_STATUS.put(playerId, false);
            // 通知服务器加载失败 / Notify server of loading failure
            notifyServerLoadingComplete(playerId);
            return null;
        });
    }

    public static boolean isSlimSkinModel(UUID playerId) {
        // 首先检查是否有缓存的profile数据
        CompoundNBT profileTag = PROFILE_CACHE.get(playerId);
        if (profileTag != null) {
            try {
                GameProfile profile = GameProfileSerializer.deserialize(profileTag);

                // 首先检查textures属性中的模型信息（最准确）
                if (profile.getProperties().containsKey("textures")) {
                    Property textureProperty = profile.getProperties().get("textures").iterator().next();
                    String decodedJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));


                    // 检查JSON中的模型类型（多种格式兼容）
                    if (decodedJson.contains("\"model\" : \"slim\"") ||
                            decodedJson.contains("\"model\":\"slim\"") ||
                            decodedJson.contains("\"model\" : \"slim\"") ||
                            decodedJson.contains("\"model\" :\"slim\"")) {
                        return true;
                    } else if (decodedJson.contains("\"model\" : \"default\"") ||
                            decodedJson.contains("\"model\":\"default\"") ||
                            decodedJson.contains("\"model\" : \"default\"") ||
                            decodedJson.contains("\"model\" :\"default\"")) {
                        return false;
                    }

                    // 如果有SKIN但没有明确的model字段，使用更可靠的判断方法
                    if (decodedJson.contains("\"SKIN\"") && profile.getId() != null) {
                        boolean isSlim = determineReliableSkinModel(profile.getName(), profile.getId());
                        return isSlim;
                    }
                }

                // 如果没有textures属性，使用更可靠的判断方法
                if (profile.getId() != null) {
                    boolean isSlim = determineReliableSkinModel(profile.getName(), profile.getId());
                    return isSlim;
                }
            } catch (Exception e) {
//                RotPYellowTemperanceAddon.LOGGER.debug("Failed to parse skin model for {}: {}", playerId, e.getMessage());
            }
        }

        // 尝试使用缓存的被伪装玩家UUID
        UUID disguiseUUID = UUID_CACHE.get(playerId);
        if (disguiseUUID != null) {
            boolean isSlim = determineReliableSkinModel(null, disguiseUUID);
            return isSlim;
        }

        // 最后使用当前玩家UUID默认算法（作为后备）
        boolean isSlim = DefaultPlayerSkin.getSkinModelName(playerId).equals("slim");
        return isSlim;
    }


    public static void cachePlayerDisguiseUUID(UUID playerId, UUID disguiseUUID) {
        UUID_CACHE.put(playerId, disguiseUUID);
    }

    public static ResourceLocation getCachedSkinTexture(UUID playerId) {
        // 检查是否仍在加载中
        if (LOADING_STATUS.getOrDefault(playerId, false)) {
            // 皮肤仍在加载中，返回null表示尚未准备好，这会让渲染系统等待
            return null;
        }

        // 直接从缓存中获取皮肤纹理，如果不存在则返回null
        // 这样可以确保只有在纹理完全加载后才会进行渲染
        ResourceLocation cachedSkin = SKIN_TEXTURE_CACHE.get(playerId);
        if (cachedSkin != null) {
            // 确保缓存的皮肤资源已完全加载
            return cachedSkin;
        }

        // 检查是否正在进行加载过程
        if (PROFILE_CACHE.containsKey(playerId)) {
            // 皮肤正在加载中，返回null表示尚未准备好
            return null;
        }

        return cachedSkin;
    }

    private static CompletableFuture<ResourceLocation> loadSkinAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            CompoundNBT profileTag = PROFILE_CACHE.get(playerId);
            if (profileTag == null) {
                // 没有profile缓存，使用默认皮肤
                return getStableDefaultSkin(playerId);
            }

            try {
                GameProfile profile = GameProfileSerializer.deserialize(profileTag);

                // 尝试获取皮肤纹理，带有重试机制
                ResourceLocation skinTexture = getSkinTextureWithRetry(profile, 2);

                // 如果获取失败，使用稳定的默认皮肤
                if (skinTexture == null || skinTexture.equals(DefaultPlayerSkin.getDefaultSkin())) {
                    return getStableDefaultSkin(profile.getId() != null ? profile.getId() : playerId);
                }

                return skinTexture;

            } catch (Exception e) {
                // 加载伪装皮肤失败，使用默认皮肤
                return getStableDefaultSkin(playerId);
            }
        }, ASYNC_EXECUTOR);
    }

    private static ResourceLocation getSkinTextureWithRetry(GameProfile profile, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                ResourceLocation skin = GameProfileFetcher.getSkinTexture(profile).get(1500, TimeUnit.MILLISECONDS);
                if (skin != null) {
                    // 皮肤加载成功
                    return skin;
                }
            } catch (Exception e) {
                // 皮肤加载尝试失败
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(300); // 等待300ms后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 皮肤加载失败，使用默认皮肤
        return null; // 返回null让调用者处理
    }

    /**
     * 更可靠的皮肤模型判断方法
     */
    private static boolean determineReliableSkinModel(String playerName, UUID playerUUID) {
        // 1. 首先尝试通过Minecraft会话服务获取准确信息
        try {
            if (playerName != null && net.minecraft.client.Minecraft.getInstance().getMinecraftSessionService() != null) {
                GameProfile tempProfile = new GameProfile(playerUUID, playerName);
                GameProfile filledProfile = net.minecraft.client.Minecraft.getInstance().getMinecraftSessionService()
                        .fillProfileProperties(tempProfile, false);

                if (filledProfile.getProperties().containsKey("textures")) {
                    Property textureProperty = filledProfile.getProperties().get("textures").iterator().next();
                    String decodedJson = new String(java.util.Base64.getDecoder().decode(textureProperty.getValue()));
                    boolean isSlim = decodedJson.contains("\"model\":\"slim\"") || decodedJson.contains("\"model\" : \"slim\"");
                    return isSlim;
                }
            }
        } catch (Exception e) {
        }

        // 2. 使用Minecraft官方的DefaultPlayerSkin算法
        if (playerUUID != null) {
            boolean isSlim = net.minecraft.client.resources.DefaultPlayerSkin.getSkinModelName(playerUUID).equals("slim");
            return isSlim;
        }

        // 3. 最后的回退
        return false;
    }

    private static ResourceLocation getStableDefaultSkin(UUID playerId) {
        // 优先使用被伪装玩家的UUID来获取默认皮肤
        UUID disguiseUUID = UUID_CACHE.get(playerId);
        if (disguiseUUID != null) {
            return DefaultPlayerSkin.getDefaultSkin(disguiseUUID);
        }

        if (playerId != null) {
            return DefaultPlayerSkin.getDefaultSkin(playerId);
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    public static CompoundNBT getCachedProfile(UUID playerId) {
        return PROFILE_CACHE.get(playerId);
    }

    public static UUID getCachedDisguiseUUID(UUID playerId) {
        return UUID_CACHE.get(playerId);
    }

    public static boolean hasDisguise(PlayerEntity player) {
        return PROFILE_CACHE.containsKey(player.getUUID()) ||
                player.getPersistentData().contains(InitTags.YT_HAS_DISGUISE_TAG);
    }

    public static void clearPlayerDisguise(UUID playerId) {
        PROFILE_CACHE.remove(playerId);
        UUID_CACHE.remove(playerId);
        SKIN_TEXTURE_CACHE.remove(playerId);
        LOADING_STATUS.remove(playerId); // 清除加载状态
    }

    public static void clearCache() {
        PROFILE_CACHE.clear();
        UUID_CACHE.clear();
        SKIN_TEXTURE_CACHE.clear();
        LOADING_STATUS.clear();
    }

    // 通知服务器伪装加载已完成
    private static void notifyServerLoadingComplete(UUID playerId) {
        try {
            // 发送客户端包到服务器，告知伪装加载已完成
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getConnection() != null && mc.player != null) {
                // 创建一个简单的包来通知服务器
                net.minecraft.nbt.CompoundNBT notificationData = new net.minecraft.nbt.CompoundNBT();
                notificationData.putBoolean(InitTags.YT_DISGUISE_LOADING_TAG, false);
                notificationData.putUUID("playerId", playerId);

                // 通过现有的网络系统发送
                com.TheChaYe.rotp_yellowtemperance.network.ModNetwork.sendToServer(
                        new com.TheChaYe.rotp_yellowtemperance.network.packets.client.ClientSyncNbtPacket(playerId, notificationData)
                );
            }
        } catch (Exception e) {
//            System.err.println("Failed to notify server of disguise loading completion: " + e.getMessage());
        }
    }
}