package com.TheChaYe.rotp_yellowtemperance.entity.ai;

import com.TheChaYe.rotp_yellowtemperance.util.DisguisePlayerHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 伪装感知目标目标 / Disguise Aware Target Goal
 * AI目标选择器，能够识别并避开伪装玩家
 */
public class DisguiseAwareTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    /**
     * 构造函数 / Constructor
     *
     * @param mob         执行AI的怪物实体 / Mob entity executing AI
     * @param targetClass 目标实体类型 / Target entity class
     */
    public DisguiseAwareTargetGoal(MobEntity mob, Class<T> targetClass) {
        super(mob, targetClass, true);
    }

    /**
     * 清除对特定玩家的仇恨 / Clear hatred for specific player
     * 只清除针对指定玩家的仇恨目标
     */
    public static void clearPlayerHatredOnly(MobEntity mob, PlayerEntity player) {
        // 只清除针对此玩家的仇恨 / Only clear hatred for this player
        if (mob.getTarget() == player) {
            mob.setTarget(null);
        }
    }

    /**
     * 判断是否可以使用此目标 / Determine if this goal can be used
     * 使用自定义的玩家选择逻辑，避开伪装玩家
     */
    @Override
    public boolean canUse() {
        // 使用自定义的玩家选择逻辑 / Use custom player selection logic
        PlayerEntity player = findNearestPlayer();
        if (player != null) {
            this.target = player;
            return true;
        }
        return false;
    }

    /**
     * 查找最近的玩家 / Find nearest player
     * 自定义玩家查找逻辑，会避开未标记的伪装玩家
     */
    private PlayerEntity findNearestPlayer() {
        // 使用原版逻辑查找最近的玩家 / Use vanilla logic to find nearest player
        PlayerEntity player = this.mob.level.getNearestPlayer(this.targetConditions, this.mob);

        // 如果玩家伪装且未被标记，返回null / If player is disguised and not marked, return null
        if (player != null && DisguisePlayerHandler.isPlayerDisguised(player) &&
                !DisguisePlayerHandler.isPlayerMarked(this.mob, player)) {
            return null;
        }
        return player;
    }

    /**
     * 判断是否可以继续使用此目标 / Determine if this goal can continue to be used
     * 持续检查目标是否仍为有效的攻击目标
     */
    @Override
    public boolean canContinueToUse() {
        // 如果目标是伪装玩家且未被标记，停止攻击 / If target is disguised player and not marked, stop attacking
        if (this.target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) this.target;
            if (DisguisePlayerHandler.isPlayerDisguised(player) &&
                    !DisguisePlayerHandler.isPlayerMarked(this.mob, player)) {
                return false;
            }
        }
        return super.canContinueToUse();
    }

    /**
     * 开始执行目标 / Start executing goal
     * 目标开始时的初始化逻辑
     */
    @Override
    public void start() {
        super.start();
        // 如果目标是伪装玩家且未被标记，立即清除仇恨 / If target is disguised player and not marked, immediately clear hatred
        if (this.target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) this.target;
            if (DisguisePlayerHandler.isPlayerDisguised(player) &&
                    !DisguisePlayerHandler.isPlayerMarked(this.mob, player)) {
                clearPlayerHatredOnly(mob, player);
            }
        }
    }
}