package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.util.DisguiseAwareTargeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 伪装事件处理器 / Disguise Event Handler
 * 处理伪装相关的实体事件，包括尺寸调整和AI目标选择
 */
@Mod.EventBusSubscriber
public class DisguiseEventHandler {

    /**
     * 实体尺寸事件处理 / Entity size event handler
     * 调整伪装玩家的碰撞箱尺寸以匹配伪装实体
     */

    /**
     * 设置攻击目标事件处理 / Set attack target event handler
     * 处理怪物对伪装玩家的目标选择逻辑
     */
    @SubscribeEvent
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (event.getEntityLiving() instanceof MobEntity) {
            MobEntity mob = (MobEntity) event.getEntityLiving();
            LivingEntity target = event.getTarget();

            // 仅当目标为伪装玩家时才拦截 / Only intercept when target is disguised player
            if (target instanceof PlayerEntity &&
                    DisguiseAwareTargeting.isPlayerDisguised((PlayerEntity) target) &&
                    DisguiseAwareTargeting.shouldIgnoreTarget(mob, target)) {

                DisguiseAwareTargeting.interceptAndReevaluate(mob, target);
            }
        }
    }

    /**
     * 玩家攻击事件处理 / Player attack event handler
     * 处理伪装玩家攻击怪物时的标记逻辑
     */
    @SubscribeEvent
    public static void onPlayerAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof PlayerEntity) {
            PlayerEntity attacker = (PlayerEntity) event.getSource().getEntity();

            if (DisguiseAwareTargeting.isPlayerDisguised(attacker) &&
                    event.getEntityLiving() instanceof MobEntity) {

                // 标记被攻击的生物 / Mark attacked mob
                DisguiseAwareTargeting.markForRetaliation((MobEntity) event.getEntityLiving(), attacker);
            }
        }
    }
}