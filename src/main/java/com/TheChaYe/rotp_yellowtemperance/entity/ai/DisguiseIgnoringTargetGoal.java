package com.TheChaYe.rotp_yellowtemperance.entity.ai;

import com.TheChaYe.rotp_yellowtemperance.util.DisguisePlayerHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 伪装忽略目标目标 / Disguise Ignoring Target Goal
 * AI目标选择器，完全忽略伪装玩家
 */
public class DisguiseIgnoringTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    /**
     * 构造函数 / Constructor
     *
     * @param mob         执行AI的怪物实体 / Mob entity executing AI
     * @param targetClass 目标实体类型 / Target entity class
     */
    public DisguiseIgnoringTargetGoal(MobEntity mob, Class<T> targetClass) {
        super(mob, targetClass, true);
    }

    /**
     * 清除对特定玩家的仇恨 / Clear hatred for specific player
     * 彻底清除与指定玩家的所有仇恨关联
     */
    public static void clearPlayerHatredOnly(MobEntity mob, PlayerEntity player) {
        // 只清除针对此玩家的仇恨 / Only clear hatred for this player
        if (mob.getTarget() == player) {
            mob.setTarget(null);
        }

        // 如果最后被玩家伤害，清除相关属性 / If last hurt by player, clear related attributes
        if (mob.getLastHurtByMob() == player) {
            mob.setLastHurtByMob(null);
        }
    }

    /**
     * 判断是否可以使用此目标 / Determine if this goal can be used
     * 完全忽略伪装玩家，不将其作为攻击目标
     */
    @Override
    public boolean canUse() {
        // 只对伪装玩家进行特殊处理 / Special handling only for disguised players
        if (super.canUse() && this.target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) this.target;
            // 如果玩家伪装且没有被此怪物标记，则忽略 / If player is disguised and not marked by this mob, ignore
            if (DisguisePlayerHandler.isPlayerDisguised(player) && !isPlayerMarked(player)) {
                return false;
            }
        }
        return super.canUse();
    }

    /**
     * 判断是否可以继续使用此目标 / Determine if this goal can continue to be used
     * 持续检查并忽略伪装玩家
     */
    @Override
    public boolean canContinueToUse() {
        // 如果目标是伪装玩家且未被标记，停止攻击 / If target is disguised player and not marked, stop attacking
        if (this.target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) this.target;
            if (DisguisePlayerHandler.isPlayerDisguised(player) && !isPlayerMarked(player)) {
                return false;
            }
        }
        return super.canContinueToUse();
    }

    /**
     * 开始执行目标 / Start executing goal
     * 目标开始时检查并清除对伪装玩家的仇恨
     */
    @Override
    public void start() {
        super.start();
        // 如果目标是伪装玩家且未被标记，立即清除仇恨 / If target is disguised player and not marked, immediately clear hatred
        if (this.target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) this.target;
            if (DisguisePlayerHandler.isPlayerDisguised(player) && !isPlayerMarked(player)) {
                clearPlayerHatredOnly(mob, player);
            }
        }
    }

    /**
     * 检查玩家是否被此怪物标记 / Check if player is marked by this mob
     * 判断玩家是否已经被此怪物识别并标记为攻击目标
     */
    private boolean isPlayerMarked(PlayerEntity player) {
        return mob.getPersistentData().contains("DisguiseAttacker") &&
                mob.getPersistentData().getUUID("DisguiseAttacker").equals(player.getUUID());
    }
}