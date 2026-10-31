package com.TheChaYe.rotp_yellowtemperance.client;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.util.GameProfileSerializer;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端伪装皮肤缓存 / Client disguise skin cache.
 *
 * <p>目标：伪装玩家皮肤必须"要么显示正确的目标皮肤，要么维持原皮肤"，
 * 绝不能出现"模型被隐藏、皮肤又没准备好"的隐身窗口，也不能把
 * 默认 Steve/Alex 皮肤当作最终结果永久缓存。</p>
 *
 * <p>解析顺序（都在客户端主线程完成，不做阻塞网络请求）：</p>
 * <ol>
 *   <li>服务器下发的 GameProfile 里的 textures 属性：本地 Base64/JSON 解码；
 *       若该皮肤已经被原版 tab 列表加载过（TextureManager 里已有同 hash 纹理），
 *       立刻切换；新皮肤则交给原版 {@code SkinManager.registerSkins} 下载，完成回调前保持原皮肤；</li>
 *   <li>profile 没有 textures 时，从 tab 列表里的在线玩家信息拿已加载的皮肤；</li>
 *   <li>都失败就退避重试；超过 {@link #FALLBACK_DELAY_MS} 才暂时使用默认皮肤，
 *       之后仍会低速重试，拿到真皮肤后自动替换。</li>
 * </ol>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID, value = Dist.CLIENT)
public class ClientDisguiseCache {
    private static final Logger LOGGER = LogManager.getLogger("rotp_yellowtemperance/skin");

    /** 超过这个时间仍拿不到真皮肤，才暂时退回默认皮肤（Steve/Alex）。 */
    private static final long FALLBACK_DELAY_MS = 6000L;
    /** 退回默认皮肤后的低速重试间隔。 */
    private static final long SLOW_RETRY_MS = 15000L;
    /**
     * 新注册的皮肤纹理要等原版 DownloadingTexture 把回退贴图真正上传后才会出现在渲染里，
     * 这里留两个 tick 再切换，避免刚变身那一帧绑到"还没加载的纹理"导致闪一下透明。
     */
    private static final long PENDING_SKIN_DELAY_MS = 100L;
    /**
     * 新皮肤最多等这么久（原版 DownloadingTexture 下载完成回调）；超时后先用默认皮肤顶上，
     * 但之后仍会通过回调/重试切换到真皮肤。
     */
    private static final long PENDING_SKIN_TIMEOUT_MS = 10000L;

    /** 每个被伪装玩家的解析状态。只在客户端主线程读写。 */
    private static final Map<UUID, SkinState> STATES = new ConcurrentHashMap<>();
    /**
     * 已经解析好的目标皮肤缓存（按目标 UUID / 名字索引）。
     * 与 STATES 分离：解除伪装后仍保留，所以再伪装同一个目标时无需重新解析/下载。
     */
    private static final Map<String, ResolvedSkin> RESOLVED = new ConcurrentHashMap<>();
    private static final int RESOLVED_CACHE_LIMIT = 256;

    private ClientDisguiseCache() {}

    /**
     * 服务器下发了伪装 profile。同一个 profile 重复下发（tracking + 广播）会被忽略，
     * 不会把已经解析好的皮肤清掉。
     */
    public static void cachePlayerDisguiseProfile(UUID playerId, CompoundNBT profileTag) {
        if (playerId == null || profileTag == null) {
            return;
        }
        SkinState state = STATES.computeIfAbsent(playerId, k -> new SkinState());
        if (state.profile != null && state.profile.equals(profileTag)) {
            return;
        }

        GameProfile profile = null;
        try {
            profile = GameProfileSerializer.deserialize(profileTag);
        } catch (Exception ignored) {
        }
        String newName = profile != null ? profile.getName() : null;
        boolean sameTarget = state.name == null
                || (newName != null && state.name.equalsIgnoreCase(newName));

        state.profile = profileTag.copy();
        if (newName != null && !newName.isEmpty()) {
            state.name = newName;
        }
        if (profile != null && profile.getId() != null) {
            state.disguiseUuid = profile.getId();
        }

        if (!sameTarget) {
            // 换了伪装目标：清掉旧皮肤，重新解析
            resetResolution(state);
        }
        if (state.skin != null) {
            // 同一个目标、已有可用皮肤：只尝试用新 profile 刷新纹理，不清空，避免闪一下原皮肤
            applyProfileSkin(playerId, state);
            return;
        }
        if (state.firstAttemptAt == 0L) {
            state.firstAttemptAt = System.currentTimeMillis();
        }
        attemptResolve(playerId, state, System.currentTimeMillis());
    }

    public static void cachePlayerDisguiseUUID(UUID playerId, UUID disguiseUUID) {
        if (playerId == null || disguiseUUID == null) {
            return;
        }
        STATES.computeIfAbsent(playerId, k -> new SkinState()).disguiseUuid = disguiseUUID;
    }

    /**
     * 渲染层用：返回可用的伪装皮肤。返回 null 表示"还没准备好，请照常渲染原玩家"。
     */
    public static ResourceLocation getRenderableSkin(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        SkinState state = STATES.get(playerId);
        if (state == null) {
            return null;
        }
        if (state.skin != null) {
            return state.skin;
        }
        if (state.fallbackApplied) {
            return state.fallback;
        }
        return null;
    }

    public static boolean isSkinReady(UUID playerId) {
        return getRenderableSkin(playerId) != null;
    }

    /**
     * 旧 API 名，等价于 {@link #getRenderableSkin(UUID)}。
     */
    public static ResourceLocation getCachedSkinTexture(UUID playerId) {
        return getRenderableSkin(playerId);
    }

    public static boolean isSlimSkinModel(UUID playerId) {
        SkinState state = STATES.get(playerId);
        if (state != null && state.slim != null) {
            return state.slim;
        }
        UUID uuid = state != null && state.disguiseUuid != null ? state.disguiseUuid : playerId;
        if (uuid == null) {
            return false;
        }
        return "slim".equals(DefaultPlayerSkin.getSkinModelName(uuid));
    }

    public static CompoundNBT getCachedProfile(UUID playerId) {
        SkinState state = STATES.get(playerId);
        return state != null ? state.profile : null;
    }

    public static UUID getCachedDisguiseUUID(UUID playerId) {
        SkinState state = STATES.get(playerId);
        return state != null ? state.disguiseUuid : null;
    }

    /**
     * 渲染用的唯一开关：既要有伪装标记，又要有可用皮肤。
     * 只有 NBT 标记为 true 时才允许用伪装皮肤——避免解除伪装后客户端缓存残留
     * 导致第一人称手臂一直是伪装皮肤换不回来。
     */
    public static boolean shouldRenderDisguise(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        CompoundNBT nbt = player.getPersistentData();
        if (!nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) || !nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
            return false;
        }
        return getRenderableSkin(player.getUUID()) != null;
    }

    /**
     * GUI 点"应用"时先预取：目标在线时立刻记住其皮肤，等服务端确认后可以零延迟换皮。
     */
    public static void prefetchByName(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientPlayNetHandler connection = mc.getConnection();
        if (connection == null) {
            return;
        }
        NetworkPlayerInfo info = connection.getPlayerInfo(name);
        if (info == null || !info.isSkinLoaded()) {
            return;
        }
        ResourceLocation skin = info.getSkinLocation();
        if (skin == null) {
            return;
        }
        boolean slim = "slim".equalsIgnoreCase(info.getModelName());
        GameProfile profile = info.getProfile();
        if (profile != null && profile.getId() != null) {
            rememberResolvedKey(resolvedKeyUuid(profile.getId()), skin, slim);
        }
        rememberResolvedKey(resolvedKeyName(name), skin, slim);
    }

    public static boolean hasDisguise(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        if (STATES.containsKey(player.getUUID())) {
            return true;
        }
        CompoundNBT nbt = player.getPersistentData();
        return nbt.contains(InitTags.YT_HAS_DISGUISE_TAG) && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG);
    }

    public static void clearPlayerDisguise(UUID playerId) {
        if (playerId != null) {
            STATES.remove(playerId);
        }
    }

    /**
     * 管理员指令用：清空所有客户端伪装缓存（已解析皮肤 + 每个玩家的解析状态）。
     */
    public static void clearAllCaches() {
        STATES.clear();
        RESOLVED.clear();
        LOGGER.debug("[YT] client disguise caches cleared");
    }

    /**
     * 管理员指令用：忘掉某个伪装目标（按名字）的客户端缓存。
     */
    public static void forgetTarget(String targetName) {
        if (targetName == null || targetName.isEmpty()) {
            return;
        }
        RESOLVED.remove(resolvedKeyName(targetName));
        java.util.Iterator<Map.Entry<UUID, SkinState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SkinState> entry = iterator.next();
            SkinState state = entry.getValue();
            if (state.name != null && state.name.equalsIgnoreCase(targetName)) {
                if (state.disguiseUuid != null) {
                    RESOLVED.remove(resolvedKeyUuid(state.disguiseUuid));
                }
                iterator.remove();
            }
        }
        LOGGER.debug("[YT] client disguise cache forgotten for target {}", targetName);
    }

    /**
     * 管理员指令用：只清某个被伪装玩家的客户端缓存并做一次重新解析。
     */
    public static void clearPlayerCaches(UUID playerId) {
        if (playerId == null) {
            return;
        }
        SkinState state = STATES.remove(playerId);
        if (state != null) {
            if (state.name != null) {
                RESOLVED.remove(resolvedKeyName(state.name));
            }
            if (state.disguiseUuid != null) {
                RESOLVED.remove(resolvedKeyUuid(state.disguiseUuid));
            }
        }
        LOGGER.debug("[YT] client disguise caches cleared for player {}", playerId);
    }

    public static void clearCache() {
        STATES.clear();
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        clearCache();
    }

    // ------------------------------------------------------------------
    // 解析逻辑
    // ------------------------------------------------------------------

    private static void resetResolution(SkinState state) {
        state.skin = null;
        state.pendingSkin = null;
        state.pendingReadySince = 0L;
        state.pendingTimeoutAt = 0L;
        state.fallback = null;
        state.fallbackApplied = false;
        state.slim = null;
        state.skinReal = false;
        state.attempts = 0;
        state.nextAttemptAt = 0L;
        state.lastLookupAt = 0L;
        state.lookupAttempts = 0;
        state.firstAttemptAt = System.currentTimeMillis();
    }

    /**
     * 客户端 tick：补抓"只有名字的准备包"、退避重试、默认皮肤兜底。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (PlayerEntity player : mc.level.players()) {
            if (player == null) {
                continue;
            }
            CompoundNBT nbt = player.getPersistentData();
            boolean disguised = nbt.contains(InitTags.YT_HAS_DISGUISE_TAG)
                    && nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG);
            if (!disguised) {
                // 标记已被清掉（解除伪装 / 服务端纠正）：顺带清掉残留的渲染状态
                STATES.remove(player.getUUID());
                continue;
            }
            SkinState state = STATES.get(player.getUUID());
            if (state == null) {
                // 服务器只发了准备包（只有名字）时，也尽早按 tab 列表把皮肤挂上
                state = new SkinState();
                String name = nbt.getString(InitTags.YT_DISGUISE_NAME_TAG);
                if (name != null && !name.isEmpty()) {
                    state.name = name;
                }
                state.firstAttemptAt = now;
                STATES.put(player.getUUID(), state);
            } else if (state.name == null) {
                String name = nbt.getString(InitTags.YT_DISGUISE_NAME_TAG);
                if (name != null && !name.isEmpty()) {
                    state.name = name;
                }
            }
            if (state.pendingSkin != null) {
                // 等原版纹理加载器把回退贴图挂上后再切，避免闪一帧透明；
                // 真皮肤下载完成时 SkinManager 回调会把 pendingReadySince 打上。
                boolean callbackReady = state.pendingReadySince > 0
                        && now - state.pendingReadySince >= PENDING_SKIN_DELAY_MS;
                boolean timedOut = state.pendingTimeoutAt > 0 && now >= state.pendingTimeoutAt;
                if (callbackReady || timedOut) {
                    state.skin = state.pendingSkin;
                    state.skinReal = callbackReady;
                    state.pendingSkin = null;
                    state.pendingReadySince = 0L;
                    state.pendingTimeoutAt = 0L;
                    state.attempts = 0;
                    state.nextAttemptAt = 0L;
                    state.fallbackApplied = false;
                }
                continue;
            }
            if (state.skin != null && state.skinReal) {
                // 已经是真皮肤：完成
                continue;
            }
            if (now < state.nextAttemptAt) {
                continue;
            }
            // 服务端查档案可能很慢/失败：客户端并行自己查一份（名字 -> UUID -> 皮肤），
            // 查到就交给同一个缓存，谁先到用谁。
            if (state.name != null && !state.name.isEmpty()
                    && state.lookupAttempts < 3
                    && now - state.firstAttemptAt >= 1500L
                    && now - state.lastLookupAt >= 10000L) {
                state.lastLookupAt = now;
                if (DisguiseProfileLookup.request(player.getUUID(), state.name)) {
                    state.lookupAttempts++;
                }
            }
            attemptResolve(player.getUUID(), state, now);
        }
    }

    private static void attemptResolve(UUID playerId, SkinState state, long now) {
        if ((state.skin != null && state.skinReal) || state.pendingSkin != null) {
            return;
        }
        // 0) 之前解析过的同一目标：直接复用，零成本
        ResolvedSkin resolved = lookupResolved(state);
        if (resolved != null) {
            state.skin = resolved.location;
            if (resolved.slim != null) {
                state.slim = resolved.slim;
            }
            state.attempts = 0;
            state.nextAttemptAt = 0L;
            state.fallbackApplied = false;
            state.skinReal = true;
            return;
        }
        if (state.firstAttemptAt == 0L) {
            state.firstAttemptAt = now;
        }
        if (!state.fallbackApplied && now - state.firstAttemptAt >= FALLBACK_DELAY_MS) {
            state.fallback = getStableDefaultSkin(playerId, state);
            state.fallbackApplied = true;
            LOGGER.debug("[YT] disguise skin not available for {}, using default skin for now", playerId);
        }
        state.attempts++;
        boolean provisionalSkin = state.skin != null;
        state.nextAttemptAt = now + ((state.fallbackApplied || provisionalSkin) ? SLOW_RETRY_MS : retryDelayMs(state.attempts));

        // 1) 服务器下发的 profile 自带 textures：本地解码后注册（多半是零成本，纹理已存在）
        if (applyProfileSkin(playerId, state)) {
            return;
        }
        // 2) profile 没带 textures：用 tab 列表里在线玩家已经加载好的皮肤
        if (applyOnlinePlayerSkin(playerId, state)) {
            return;
        }
    }

    private static long retryDelayMs(int attempts) {
        if (attempts <= 1) {
            return 250L;
        }
        if (attempts == 2) {
            return 500L;
        }
        if (attempts == 3) {
            return 1000L;
        }
        return 2000L;
    }

    /**
     * 从 profile 的 textures 属性解析并发起纹理注册。
     *
     * <p>{@code MinecraftSessionService.getTextures} 只是解码 profile 里已有的属性，
     * 不发网络请求；真正加载 PNG 由原版 DownloadingTexture 异步完成（已缓存时是瞬时的）。</p>
     */
    private static boolean applyProfileSkin(UUID playerId, SkinState state) {
        if (state.profile == null) {
            return false;
        }
        try {
            GameProfile profile = GameProfileSerializer.deserialize(state.profile);
            if (profile == null) {
                return false;
            }
            MinecraftSessionService sessionService = Minecraft.getInstance().getMinecraftSessionService();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures =
                    sessionService.getTextures(profile, false);
            if (textures == null) {
                return false;
            }
            MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
            if (skin == null) {
                return false;
            }
            ResourceLocation expected = new ResourceLocation(
                    "skins/" + Hashing.sha1().hashUnencodedChars(skin.getHash()).toString());
            if (Minecraft.getInstance().getTextureManager().getTexture(expected) != null) {
                // 该皮肤已被原版加载过（在线玩家的 tab 列表），立刻切换，零下载
                markSkinReady(state, expected, skin);
                LOGGER.debug("[YT] disguise skin for {} already loaded: {}", playerId, expected);
                return true;
            }

            // 新皮肤：交给原版 SkinManager 走它自己的下载/缓存流程，
            // 下载完成回调在 onSkinTextureAvailable 里；完成前保持原皮肤，不闪史蒂夫。
            long now = System.currentTimeMillis();
            state.pendingSkin = expected;
            state.pendingReadySince = 0L;
            state.pendingTimeoutAt = now + PENDING_SKIN_TIMEOUT_MS;
            try {
                Minecraft.getInstance().getSkinManager().registerSkins(profile,
                        (type, location, texture) -> {
                            if (type != MinecraftProfileTexture.Type.SKIN) {
                                return;
                            }
                            Minecraft.getInstance().execute(() -> onSkinDownloaded(playerId, location, texture));
                        }, false);
            } catch (Throwable throwable) {
                state.pendingSkin = null;
                state.pendingTimeoutAt = 0L;
                LOGGER.debug("[YT] failed to register disguise skin for {}: {}", playerId, throwable.toString());
                return false;
            }
            LOGGER.debug("[YT] disguise skin for {} downloading: {}", playerId, expected);
            return true;
        } catch (Throwable throwable) {
            LOGGER.debug("[YT] failed to apply disguise skin for {}: {}", playerId, throwable.toString());
            return false;
        }
    }

    /**
     * 在线玩家的 tab 列表信息：皮肤通常已由原版 SkinManager 注册并下载好，直接拿来用。
     */
    private static boolean applyOnlinePlayerSkin(UUID playerId, SkinState state) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayNetHandler connection = mc.getConnection();
        if (connection == null) {
            return false;
        }
        NetworkPlayerInfo info = null;
        if (state.disguiseUuid != null) {
            info = connection.getPlayerInfo(state.disguiseUuid);
        }
        if (info == null && state.name != null && !state.name.isEmpty()) {
            info = connection.getPlayerInfo(state.name);
        }
        if (info == null || !info.isSkinLoaded()) {
            return false;
        }
        ResourceLocation skin = info.getSkinLocation();
        if (skin == null) {
            return false;
        }
        boolean real = !isVanillaDefaultSkin(skin);
        state.skin = skin;
        state.skinReal = real;
        state.fallbackApplied = false;
        if (real) {
            state.attempts = 0;
            state.nextAttemptAt = 0L;
        } else {
            // 默认皮肤只是"能显示"，不算解析成功：保持慢速重试，等真皮肤
            state.nextAttemptAt = System.currentTimeMillis() + SLOW_RETRY_MS;
        }
        if (real) {
            // 只有真皮肤才进"已解析"缓存；默认 Steve/Alex 不算成功，之后拿到真皮肤还可以替换
            rememberResolved(state, skin);
        }
        GameProfile profile = info.getProfile();
        if (state.disguiseUuid == null && profile != null) {
            state.disguiseUuid = profile.getId();
        }
        String model = info.getModelName();
        if (model != null) {
            state.slim = "slim".equalsIgnoreCase(model);
        } else if (profile != null && profile.getId() != null) {
            state.slim = "slim".equals(DefaultPlayerSkin.getSkinModelName(profile.getId()));
        }
        LOGGER.debug("[YT] disguise skin for {} resolved from tab list: {}", playerId, skin);
        return true;
    }

    /**
     * 原版 SkinManager 下载/缓存完成回调（可能来自后台线程）。
     */
    private static void onSkinDownloaded(UUID playerId, ResourceLocation location, MinecraftProfileTexture texture) {
        SkinState state = STATES.get(playerId);
        if (state == null || location == null) {
            return;
        }
        if (state.pendingSkin == null) {
            // 已经超时兜底用默认皮肤了，但真皮肤其实下载完了：升级回真皮肤
            if (!state.skinReal && location.equals(state.skin)) {
                state.skinReal = true;
            }
            return;
        }
        state.pendingSkin = location;
        state.pendingReadySince = System.currentTimeMillis();
        if (texture != null) {
            String model = texture.getMetadata("model");
            if (model != null) {
                state.slim = "slim".equalsIgnoreCase(model);
            }
        }
    }

    private static void markSkinReady(SkinState state, ResourceLocation location, MinecraftProfileTexture texture) {
        state.skin = location;
        state.skinReal = true;
        rememberResolved(state, location);
        state.pendingSkin = null;
        state.pendingReadySince = 0L;
        state.pendingTimeoutAt = 0L;
        state.attempts = 0;
        state.nextAttemptAt = 0L;
        state.fallbackApplied = false;
        if (texture != null) {
            String model = texture.getMetadata("model");
            if (model != null) {
                state.slim = "slim".equalsIgnoreCase(model);
            }
        }
    }

    private static String resolvedKeyName(String name) {
        return "n:" + name.toLowerCase(java.util.Locale.ROOT);
    }

    private static String resolvedKeyUuid(UUID uuid) {
        return "u:" + uuid;
    }

    private static ResolvedSkin lookupResolved(SkinState state) {
        if (state.name != null && !state.name.isEmpty()) {
            ResolvedSkin byName = RESOLVED.get(resolvedKeyName(state.name));
            if (byName != null) {
                return byName;
            }
        }
        if (state.disguiseUuid != null) {
            return RESOLVED.get(resolvedKeyUuid(state.disguiseUuid));
        }
        return null;
    }

    private static void rememberResolved(SkinState state, ResourceLocation location) {
        if (location == null) {
            return;
        }
        Boolean slim = state.slim;
        if (state.name != null && !state.name.isEmpty()) {
            rememberResolvedKey(resolvedKeyName(state.name), location, slim);
        }
        if (state.disguiseUuid != null) {
            rememberResolvedKey(resolvedKeyUuid(state.disguiseUuid), location, slim);
        }
    }

    private static void rememberResolvedKey(String key, ResourceLocation location, Boolean slim) {
        if (RESOLVED.size() >= RESOLVED_CACHE_LIMIT && !RESOLVED.containsKey(key)) {
            RESOLVED.clear();
        }
        RESOLVED.put(key, new ResolvedSkin(location, slim));
    }

    /**
     * 原版默认皮肤（textures/entity/steve.png / alex.png）。
     * 离线模式玩家没有 textures 时，tab 列表给的就是这个——可以用于显示，但绝不能当成"解析成功"缓存。
     */
    private static boolean isVanillaDefaultSkin(ResourceLocation location) {
        if (location == null || location.getNamespace().equals("minecraft") == false) {
            return false;
        }
        String path = location.getPath();
        return "textures/entity/steve.png".equals(path) || "textures/entity/alex.png".equals(path);
    }

    private static ResourceLocation getStableDefaultSkin(UUID playerId, SkinState state) {
        UUID uuid = state.disguiseUuid != null ? state.disguiseUuid : playerId;
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }

    /**
     * 已解析的目标皮肤（可跨多次伪装复用）。
     */
    private static final class ResolvedSkin {
        private final ResourceLocation location;
        private final Boolean slim;

        private ResolvedSkin(ResourceLocation location, Boolean slim) {
            this.location = location;
            this.slim = slim;
        }
    }

    /**
     * 中间状态容器（仅客户端主线程读写）。
     */
    private static final class SkinState {
        private CompoundNBT profile;
        private String name;
        private UUID disguiseUuid;
        private ResourceLocation skin;
        private boolean skinReal;
        private ResourceLocation pendingSkin;
        private long pendingReadySince;
        private long pendingTimeoutAt;
        private ResourceLocation fallback;
        private Boolean slim;
        private boolean fallbackApplied;
        private int attempts;
        private long nextAttemptAt;
        private long firstAttemptAt;
        private long lastLookupAt;
        private int lookupAttempts;
    }
}