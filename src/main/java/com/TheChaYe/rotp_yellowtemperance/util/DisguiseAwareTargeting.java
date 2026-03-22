package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * 伪装感知目标选择器 / Disguise Aware Target Selector
 * 处理生物AI对伪装玩家的目标识别和选择逻辑
 * Manages how mobs perceive and select targets when players are disguised
 */
public class DisguiseAwareTargeting {

    /**
     * 检查玩家伪装状态 / Check Player Disguise Status
     * 检查指定玩家是否启用了黄色节制替身的伪装能力
     *
     * @param player 目标玩家 / Target player
     * @return 玩家是否处于伪装状态 / Whether the player is disguised
     */
    public static boolean isPlayerDisguised(PlayerEntity player) {
        IStandPower standPower = IStandPower.getStandPowerOptional(player).orElse(null);
        if (standPower != null && standPower.getStandManifestation() instanceof AbstractDisguiseStandEntity) {
            AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity)
                    standPower.getStandManifestation();
            return disguiseStand.isUserDisguised();
        }
        return false;
    }

    /**
     * 检查玩家是否被生物标记 / Check if Player is Marked by Mob
     * 检查指定生物是否已经标记了该玩家（用于仇恨机制）
     *
     * @param mob    生物实体 / Mob entity
     * @param player 目标玩家 / Target player
     * @return 玩家是否被该生物标记 / Whether the player is marked by this mob
     */
    public static boolean isPlayerMarked(MobEntity mob, PlayerEntity player) {
        CompoundNBT tag = mob.getPersistentData();
        if (tag.contains("DisguiseAttacker")) {
            UUID attackerId = tag.getUUID("DisguiseAttacker");
            long markTime = tag.getLong("DisguiseAttackerTime");

            // 检查标记是否过期（30秒）/ Check if mark has expired (30 seconds)
            if (System.currentTimeMillis() - markTime > 30000) {
                tag.remove("DisguiseAttacker");
                tag.remove("DisguiseAttackerTime");
                return false;
            }

            return player.getUUID().equals(attackerId);
        }
        return false;
    }

    /**
     * 判断是否应该忽略目标 / Determine if Target Should be Ignored
     * 在生物产生仇恨前判断目标是否为未被标记的伪装玩家
     *
     * @param mob    生物实体 / Mob entity
     * @param target 目标实体 / Target entity
     * @return 是否应该忽略该目标 / Whether the target should be ignored
     */
    public static boolean shouldIgnoreTarget(MobEntity mob, LivingEntity target) {
        if (target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) target;

            // 如果玩家伪装且未被标记，则忽略
            return isPlayerDisguised(player) && !isPlayerMarked(mob, player);
        }
        return false;
    }

    /**
     * 拦截并重新评估目标 / Intercept and Re-evaluate Target
     * 当发现无效目标时，拦截当前目标并寻找新的有效目标
     *
     * @param mob    生物实体 / Mob entity
     * @param target 当前目标 / Current target
     */
    public static void interceptAndReevaluate(MobEntity mob, LivingEntity target) {
        // 保存当前目标状态 / Save current target state
        LivingEntity previousTarget = mob.getTarget();

        // 清除当前目标设置 / Clear current target setting
        mob.setTarget(null);

        // 寻找新的有效目标 / Find new valid target
        LivingEntity newTarget = findValidTarget(mob);

        if (newTarget != null) {
            // 设置新目标 / Set new target
            mob.setTarget(newTarget);
        } else if (previousTarget != null && mob.canAttack(previousTarget) &&
                !shouldIgnoreTarget(mob, previousTarget)) {
            // 恢复原目标（如果有效且不是伪装玩家）/ Restore original target (if valid and not disguised)
            mob.setTarget(previousTarget);
        }
        // 否则保持无目标状态 / Otherwise remain without target
    }

    /**
     * 寻找有效的攻击目标 / Find Valid Attack Target
     * 按优先级顺序寻找生物的有效攻击目标
     *
     * @param mob 生物实体 / Mob entity
     * @return 有效的攻击目标或null / Valid attack target or null
     */
    public static LivingEntity findValidTarget(MobEntity mob) {
        World world = mob.level;

        double attackRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (attackRange <= 0) {
            attackRange = 16.0;
        }

        // 1. 只有当生物会主动攻击铁傀儡时才寻找铁傀儡 / 1. Only search for iron golems if mob actively attacks them
        if (shouldAttackGolems(mob)) {
            IronGolemEntity golem = findNearestGolem(mob, attackRange);
            if (golem != null && mob.canAttack(golem)) {
                return golem;
            }
        }

        // 2. 寻找玩家（排除伪装玩家但包括被标记的玩家）/ 2. Search for players (exclude disguised but include marked players)
        PlayerEntity otherPlayer = findNearestPlayer(mob, attackRange);
        if (otherPlayer != null && mob.canAttack(otherPlayer)) {
            return otherPlayer;
        }

        // 3. 只有当生物会主动攻击村民时才寻找村民 / 3. Only search for villagers if mob actively attacks them
        if (shouldAttackVillagers(mob)) {
            AbstractVillagerEntity villager = findNearestVillager(mob, attackRange);
            if (villager != null && mob.canAttack(villager)) {
                return villager;
            }
        }

        // 4. 寻找其他有明确仇恨关系的目标 / 4. Search for other targets with clear hostility
        LivingEntity otherTarget = findNearestHostile(mob, attackRange);
        if (otherTarget != null && mob.canAttack(otherTarget)) {
            return otherTarget;
        }

        return null;
    }

    /**
     * 判断生物是否应该攻击铁傀儡 / Determine if Mob Should Attack Iron Golems
     * 根据生物类型判断是否会主动攻击铁傀儡
     *
     * @param mob 生物实体 / Mob entity
     * @return 是否应该攻击铁傀儡 / Whether should attack iron golems
     */
    private static boolean shouldAttackGolems(MobEntity mob) {
        return mob instanceof ZombieEntity ||          // 僵尸类
                mob instanceof AbstractSkeletonEntity || // 骷髅类（包括普通骷髅、流浪者）
                mob instanceof SpiderEntity ||          // 蜘蛛类（包括洞穴蜘蛛）
                mob instanceof WitherSkeletonEntity ||  // 凋零骷髅
                mob instanceof AbstractIllagerEntity || // 灾厄村民类
                mob instanceof RavagerEntity;           // 劫掠兽
    }

    /**
     * 判断生物是否应该攻击村民 / Determine if Mob Should Attack Villagers
     * 根据生物类型判断是否会主动攻击村民
     *
     * @param mob 生物实体 / Mob entity
     * @return 是否应该攻击村民 / Whether should attack villagers
     */
    private static boolean shouldAttackVillagers(MobEntity mob) {
        return mob instanceof ZombieEntity ||          // 僵尸类
                mob instanceof AbstractIllagerEntity || // 灾厄村民类
                mob instanceof RavagerEntity;           // 劫掠兽
    }

    /**
     * 寻找最近的有效玩家 / Find Nearest Valid Player
     * 寻找范围内最近的可攻击玩家（排除未标记的伪装玩家）
     *
     * @param mob   生物实体 / Mob entity
     * @param range 搜索范围 / Search range
     * @return 最近的有效玩家或null / Nearest valid player or null
     */
    private static PlayerEntity findNearestPlayer(MobEntity mob, double range) {
        List<PlayerEntity> players = mob.level.getEntitiesOfClass(
                PlayerEntity.class,
                mob.getBoundingBox().inflate(range),
                player -> {
                    // 被标记的玩家总是可以被攻击 / Marked players can always be attacked
                    if (isPlayerMarked(mob, player)) {
                        return true;
                    }
                    // 未被标记的伪装玩家被忽略 / Unmarked disguised players are ignored
                    return !shouldIgnoreTarget(mob, player) && mob.canAttack(player);
                }
        );

        return findNearestEntity(mob, players);
    }

    /**
     * 寻找最近的铁傀儡 / Find Nearest Iron Golem
     * 按优先级寻找范围内最近的铁傀儡
     *
     * @param mob   生物实体 / Mob entity
     * @param range 搜索范围 / Search range
     * @return 最近的铁傀儡或null / Nearest iron golem or null
     */
    private static IronGolemEntity findNearestGolem(MobEntity mob, double range) {
        List<IronGolemEntity> golems = mob.level.getEntitiesOfClass(
                IronGolemEntity.class,
                mob.getBoundingBox().inflate(range),
                golem -> mob.canAttack(golem)
        );

        // 优先选择正在攻击该生物的铁傀儡 / Priority: iron golems currently attacking this mob
        for (IronGolemEntity golem : golems) {
            if (golem.getTarget() == mob) {
                return golem;
            }
        }

        // 其次选择该生物最近被攻击的铁傀儡 / Secondary: iron golems that recently attacked this mob
        if (mob.getLastHurtByMob() instanceof IronGolemEntity) {
            IronGolemEntity lastAttacker = (IronGolemEntity) mob.getLastHurtByMob();
            if (golems.contains(lastAttacker)) {
                return lastAttacker;
            }
        }

        // 最后选择最近的铁傀儡 / Finally: nearest iron golem
        return findNearestEntity(mob, golems);
    }

    /**
     * 寻找最近的村民 / Find Nearest Villager
     * 寻找范围内最近的可攻击村民
     *
     * @param mob   生物实体 / Mob entity
     * @param range 搜索范围 / Search range
     * @return 最近的村民或null / Nearest villager or null
     */
    private static AbstractVillagerEntity findNearestVillager(MobEntity mob, double range) {
        List<AbstractVillagerEntity> villagers = mob.level.getEntitiesOfClass(
                AbstractVillagerEntity.class,
                mob.getBoundingBox().inflate(range),
                villager -> mob.canAttack(villager)
        );

        return findNearestEntity(mob, villagers);
    }

    /**
     * 寻找最近的敌对生物 / Find Nearest Hostile Entity
     * 寻找范围内有明确仇恨关系的敌对生物
     *
     * @param mob   生物实体 / Mob entity
     * @param range 搜索范围 / Search range
     * @return 最近的敌对生物或null / Nearest hostile entity or null
     */
    private static LivingEntity findNearestHostile(MobEntity mob, double range) {
        List<LivingEntity> entities = mob.level.getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(range),
                entity -> {
                    // 排除自身、玩家、铁傀儡、村民和同类生物 / Exclude self, players, golems, villagers and same-type mobs
                    if (entity == mob ||
                            entity instanceof PlayerEntity ||
                            entity instanceof IronGolemEntity ||
                            entity instanceof AbstractVillagerEntity ||
                            entity.getClass() == mob.getClass()) {
                        return false;
                    }

                    // 只选择有明确仇恨关系的目标 / Only select targets with clear hostility
                    return mob.canAttack(entity) &&
                            (mob.getTarget() == entity || // 已有仇恨目标
                                    mob.getLastHurtByMob() == entity || // 最近被此实体攻击
                                    mob.getLastHurtMob() == entity); // 最近攻击过此实体
                }
        );

        return findNearestEntity(mob, entities);
    }

    /**
     * 寻找最近的实体 / Find Nearest Entity
     * 从实体列表中找到距离最近的实体
     *
     * @param mob      参考生物 / Reference mob
     * @param entities 实体列表 / Entity list
     * @return 最近的实体或null / Nearest entity or null
     */
    private static <T extends LivingEntity> T findNearestEntity(MobEntity mob, List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        T nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (T entity : entities) {
            double distance = mob.distanceToSqr(entity);
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    /**
     * 标记生物进行反击 / Mark Mob for Retaliation
     * 当玩家攻击生物时，标记该生物以便后续识别
     *
     * @param mob    被攻击的生物 / Attacked mob
     * @param player 攻击玩家 / Attacking player
     */
    public static void markForRetaliation(MobEntity mob, PlayerEntity player) {
        CompoundNBT tag = mob.getPersistentData();
        tag.putUUID("DisguiseAttacker", player.getUUID());
        tag.putLong("DisguiseAttackerTime", System.currentTimeMillis());

        // 确保生物会立即攻击标记的玩家 / Ensure mob will immediately attack the marked player
        if (mob.getTarget() != player) {
            mob.setTarget(player);
        }
    }

    /**
     * 清除玩家重新伪装时的标记 / Clear Marks When Player Re-disguises
     * 当玩家重新启用伪装时，清除周围生物对该玩家的标记
     *
     * @param player 重新伪装的玩家 / Re-disguising player
     */
    public static void onPlayerReDisguise(PlayerEntity player) {
        player.level.getEntitiesOfClass(MobEntity.class, player.getBoundingBox().inflate(128))
                .forEach(mob -> {
                    CompoundNBT tag = mob.getPersistentData();
                    if (tag.contains("DisguiseAttacker") &&
                            tag.getUUID("DisguiseAttacker").equals(player.getUUID())) {
                        tag.remove("DisguiseAttacker");
                        tag.remove("DisguiseAttackerTime");

                        // 如果生物当前目标是该玩家，清除目标 / Clear target if mob's current target is this player
                        if (mob.getTarget() == player) {
                            mob.setTarget(null);
                        }
                    }
                });
    }
}