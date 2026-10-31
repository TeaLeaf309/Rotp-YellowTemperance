package com.TheChaYe.rotp_yellowtemperance.ai;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.TheChaYe.rotp_yellowtemperance.mixin.GoalSelectorAccessor;
import com.TheChaYe.rotp_yellowtemperance.mixin.NearestAttackableTargetGoalAccessor;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 伪装感知（服务端）：决定"某个生物是否把某个伪装玩家当作可见目标"。
 *
 * 语义（SPECIES 模式）：把玩家当成伪装物种 D，看这个生物自己的 target goal 会不会攻击 D：
 *  - 用 targetType.isAssignableFrom(D 的实体 Class) 做物种匹配（粗筛）；
 *  - 用 goal 自己的 EntityPredicate 对一个"真实 D 实体探针"求值（精判，处理 isBaby / IMob / getType 等谓词）；
 *  - 两者同时成立才算"会攻击该物种"。
 * 能直接锁玩家的 goal（targetType 可从 PlayerEntity 赋值）不参与物种判断，它们是"载体"。
 *
 * 挑衅例外：被玩家打过 / 处于 IAngerable 激怒状态时，伪装不再保护玩家（原版反击优先）。
 */
public final class MobPerception {

    public enum Mode {
        SPECIES,
        IGNORE_ALL,
        OFF
    }

    private static final long DECISION_CACHE_TICKS = 20L;
    private static final double CLEAR_TARGET_RADIUS = 64.0D;

    /** EntityType -> 实体 Class（伪装物种类型匹配用）。 */
    private static final Map<EntityType<?>, Optional<Class<?>>> DISGUISE_CLASSES = new HashMap<>();
    /** EntityType -> 不入世界的探针实体（跟着玩家位置走）。 */
    private static final Map<EntityType<?>, Entity> PROBES = new HashMap<>();
    /** 生物 -> 最近的判定缓存（避免每个 goal 都重新跑谓词/射线）。 */
    private static final Map<MobEntity, Decision> DECISIONS = new WeakHashMap<>();
    /** 维度 -> 当前处于实体伪装的玩家列表（10 tick 缓存，避免每个 goal 都扫一遍玩家）。 */
    private static final Map<World, DisguisedList> DISGUISED_PLAYERS = new WeakHashMap<>();
    private static final long DISGUISED_LIST_CACHE_TICKS = 10L;

    private MobPerception() {
    }

    // ------------------------------------------------------------------
    // 主入口
    // ------------------------------------------------------------------

    public static boolean canPerceive(MobEntity mob, PlayerEntity player) {
        if (mob == null || player == null) {
            return true;
        }
        Mode mode = mode();
        if (mode == Mode.OFF) {
            return true;
        }
        EntityType<?> disguise = disguiseType(player);
        if (disguise == null) {
            return true;
        }

        long now = mob.level.getGameTime();
        Decision cached = DECISIONS.get(mob);
        if (cached != null && cached.disguise == disguise && cached.expiresAt > now) {
            return cached.perceives;
        }

        boolean perceives;
        if (mode == Mode.IGNORE_ALL) {
            perceives = false;
        } else if (provokedOverride() && isProvoked(mob, player)) {
            perceives = true;
        } else {
            Class<?> disguiseClass = disguiseClass(disguise, mob.level);
            if (disguiseClass == null) {
                perceives = false; // 解析失败：保守当作看不见
            } else {
                LivingEntity probe = probeFor(disguise, mob.level, player);
                perceives = hostileToSpecies(mob, disguiseClass, probe);
            }
        }

        DECISIONS.put(mob, new Decision(disguise, now + DECISION_CACHE_TICKS, perceives));
        debug("mob={} player={} disguise={} perceives={}", id(mob.getType()), player.getName().getString(), id(disguise), perceives);
        return perceives;
    }

    /**
     * 伪装状态变化（应用 / 切换物种 / 清除）时调用：
     * 1) 清掉判定缓存；
     * 2) 对附近"正锁着该玩家"的生物清一次目标——下 tick goal 重新评估时，
     *    玩家 goal 会被 findTarget 过滤掉，村民/铁傀儡等 goal 正常接手。
     */
    public static void onDisguiseStateChanged(ServerPlayerEntity player) {
        if (player == null || player.level.isClientSide()) {
            return;
        }
        DECISIONS.clear();
        DISGUISED_PLAYERS.clear();
        if (mode() == Mode.OFF) {
            return;
        }
        for (MobEntity mob : player.level.getEntitiesOfClass(MobEntity.class,
                player.getBoundingBox().inflate(CLEAR_TARGET_RADIUS))) {
            if (mob.getTarget() == player && !canPerceive(mob, player)) {
                mob.setTarget(null);
            }
        }
    }

    /**
     * 【补目标】给"物种 goal"补充伪装玩家候选。
     *
     * <p>原版 {@link NearestAttackableTargetGoal#findTarget()} 只按 targetType 找**真实实体**，
     * 所以像铁傀儡的"攻击怪物"goal（targetType=MobEntity）永远找不到玩家。
     * 这里在它没找到真实目标时，把附近伪装成匹配物种的玩家塞进去，
     * 让"物种 goal"也能把玩家当成该物种对待。</p>
     *
     * <p>只对物种 goal 生效（targetType 不是 PlayerEntity 的父类型）；
     * 玩家正在被伪装成 PLAYER 类型时返回 null（玩家形态仍走原版玩家目标）。</p>
     *
     * @param targetType goal 的目标类型
     * @param conditions goal 自己的 EntityPredicate（用探针求值）
     * @return 第一个匹配的伪装玩家，没有则 null
     */
    @Nullable
    public static PlayerEntity findDisguisedTargetForGoal(MobEntity mob, Class<?> targetType,
                                                          @Nullable EntityPredicate conditions) {
        if (mob == null || targetType == null || mob.level == null) {
            return null;
        }
        if (mode() != Mode.SPECIES || !speciesTargeting()) {
            return null;
        }
        for (PlayerEntity player : disguisedPlayers(mob.level)) {
            if (player == null || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (!mob.canAttack(player)) {
                continue;
            }
            EntityType<?> disguise = disguiseType(player);
            if (disguise == null || disguise == EntityType.PLAYER) {
                continue;
            }
            Class<?> disguiseClass = disguiseClass(disguise, mob.level);
            if (disguiseClass == null || !targetType.isAssignableFrom(disguiseClass)) {
                continue;
            }
            LivingEntity probe = probeFor(disguise, mob.level, player);
            if (probe == null) {
                // 探针建不出来时退化为纯类型匹配（与 canPerceive 的处理一致）
                return player;
            }
            Boolean accepted = test(conditions, mob, probe);
            if (accepted != null && accepted) {
                return player;
            }
        }
        return null;
    }

    /**
     * 当前处于实体伪装的玩家（带短缓存；伪装状态变化时缓存会被清掉）。
     */
    private static List<PlayerEntity> disguisedPlayers(World world) {
        long now = world.getGameTime();
        DisguisedList cached = DISGUISED_PLAYERS.get(world);
        if (cached != null && cached.expiresAt > now) {
            return cached.players;
        }
        List<PlayerEntity> players = new ArrayList<>();
        for (PlayerEntity player : world.players()) {
            if (player != null && player.isAlive() && disguiseType(player) != null) {
                players.add(player);
            }
        }
        DISGUISED_PLAYERS.put(world, new DisguisedList(players, now + DISGUISED_LIST_CACHE_TICKS));
        return players;
    }

    // ------------------------------------------------------------------
    // 判定细节
    // ------------------------------------------------------------------

    /**
     * 生物是否会攻击"伪装物种 D"。
     *
     * @param probe 真实 D 实体（跟着玩家位置）；为 null 时退化为只看 targetType 匹配
     */
    private static boolean hostileToSpecies(MobEntity mob, Class<?> disguiseClass, @Nullable LivingEntity probe) {
        GoalSelector selector = mob.targetSelector;
        if (!(selector instanceof GoalSelectorAccessor)) {
            return true; // 访问器 mixin 未生效：不干预
        }
        Set<PrioritizedGoal> goals = ((GoalSelectorAccessor) selector).yt$availableGoals();
        boolean typeMatched = false;
        for (PrioritizedGoal prioritized : goals) {
            Goal goal = prioritized.getGoal();
            if (!(goal instanceof NearestAttackableTargetGoal)) {
                continue;
            }
            if (!(goal instanceof NearestAttackableTargetGoalAccessor)) {
                continue;
            }
            NearestAttackableTargetGoalAccessor accessor = (NearestAttackableTargetGoalAccessor) goal;
            Class<?> targetType = accessor.yt$targetType();
            if (targetType == null) {
                continue;
            }
            // 能直接锁玩家的 goal 是"载体"，不携带物种信息（否则任何攻击玩家的怪都会匹配所有伪装）
            if (targetType.isAssignableFrom(PlayerEntity.class)) {
                continue;
            }
            if (!targetType.isAssignableFrom(disguiseClass)) {
                continue;
            }
            typeMatched = true;
            if (probe == null) {
                continue;
            }
            Boolean accepted = test(accessor.yt$targetConditions(), mob, probe);
            if (accepted != null && accepted) {
                return true;
            }
        }
        // 探针创建失败时退化为类型匹配（否则正常生物类型都能建出探针，不会走这里）
        return typeMatched && probe == null;
    }

    private static boolean isProvoked(MobEntity mob, PlayerEntity player) {
        if (mob.getLastHurtByMob() == player) {
            return true;
        }
        return mob instanceof IAngerable && ((IAngerable) mob).isAngryAt(player);
    }

    @Nullable
    private static Boolean test(@Nullable EntityPredicate conditions, MobEntity mob, LivingEntity target) {
        if (conditions == null) {
            return Boolean.TRUE;
        }
        try {
            return conditions.test(mob, target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 数据解析 / 探针
    // ------------------------------------------------------------------

    @Nullable
    public static EntityType<?> disguiseType(PlayerEntity player) {
        IStandPower power = IStandPower.getStandPowerOptional(player).orElse(null);
        if (power == null) {
            return null;
        }
        if (!(power.getStandManifestation() instanceof AbstractDisguiseStandEntity)) {
            return null;
        }
        AbstractDisguiseStandEntity stand = (AbstractDisguiseStandEntity) power.getStandManifestation();
        if (!stand.isUserDisguised()) {
            return null;
        }
        return stand.getUserDisguiseEntity().orElse(null);
    }

    @Nullable
    private static Class<?> disguiseClass(EntityType<?> type, World world) {
        if (type == EntityType.PLAYER) {
            return PlayerEntity.class;
        }
        Optional<Class<?>> cached = DISGUISE_CLASSES.get(type);
        if (cached != null) {
            return cached.orElse(null);
        }
        Class<?> cls = null;
        try {
            Entity created = type.create(world);
            if (created != null) {
                cls = created.getClass();
            }
        } catch (Throwable t) {
            debug("failed to resolve entity class for {}: {}", id(type), t.toString());
        }
        DISGUISE_CLASSES.put(type, Optional.ofNullable(cls));
        return cls;
    }

    @Nullable
    private static LivingEntity probeFor(EntityType<?> type, World world, PlayerEntity player) {
        if (type == EntityType.PLAYER) {
            return null;
        }
        LivingEntity probe;
        Entity cached = PROBES.get(type);
        if (cached instanceof LivingEntity && cached.level == world) {
            probe = (LivingEntity) cached;
        } else {
            try {
                Entity created = type.create(world);
                if (!(created instanceof LivingEntity)) {
                    debug("probe for {} is not living", id(type));
                    return null;
                }
                probe = (LivingEntity) created;
                PROBES.put(type, probe);
            } catch (Throwable t) {
                debug("probe creation failed for {}: {}", id(type), t.toString());
                return null;
            }
        }
        // 探针跟随玩家位置：距离/视线/是否在水中等谓词都按玩家位置计算
        probe.setPos(player.getX(), player.getY(), player.getZ());
        probe.yRot = player.yRot;
        probe.xRot = player.xRot;
        return probe;
    }

    // ------------------------------------------------------------------
    // 配置 / 工具
    // ------------------------------------------------------------------

    private static Mode mode() {
        try {
            String value = YellowTemperanceConfig.aiPerceptionMode.get();
            if (value != null) {
                if ("OFF".equalsIgnoreCase(value)) {
                    return Mode.OFF;
                }
                if ("IGNORE_ALL".equalsIgnoreCase(value)) {
                    return Mode.IGNORE_ALL;
                }
            }
        } catch (Throwable ignored) {
            // 配置未加载时按默认值处理
        }
        return Mode.SPECIES;
    }

    private static boolean provokedOverride() {
        try {
            return YellowTemperanceConfig.aiPerceptionProvokedOverride.get();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean speciesTargeting() {
        try {
            return YellowTemperanceConfig.aiPerceptionSpeciesTargeting.get();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void debug(String msg, Object... args) {
        try {
            if (YellowTemperanceConfig.aiPerceptionDebug.get()) {
                RotPYellowTemperanceAddon.LOGGER.info("[YT] perception: " + msg, args);
            }
        } catch (Throwable ignored) {
            // 调试日志不允许影响玩法
        }
    }

    private static String id(EntityType<?> type) {
        return type.getRegistryName() == null ? String.valueOf(type) : type.getRegistryName().toString();
    }

    private static final class DisguisedList {
        private final List<PlayerEntity> players;
        private final long expiresAt;

        private DisguisedList(List<PlayerEntity> players, long expiresAt) {
            this.players = players;
            this.expiresAt = expiresAt;
        }
    }

    private static final class Decision {
        private final EntityType<?> disguise;
        private final long expiresAt;
        private final boolean perceives;

        private Decision(EntityType<?> disguise, long expiresAt, boolean perceives) {
            this.disguise = disguise;
            this.expiresAt = expiresAt;
            this.perceives = perceives;
        }
    }
}