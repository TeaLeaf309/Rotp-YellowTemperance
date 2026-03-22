package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 效果事件处理器 / Effect Event Handler
 * 处理黄色节制侵蚀效果的等级升级和持续时间管理
 */
@Mod.EventBusSubscriber
public class EffectEventHandler {

    /**
     * 生物更新事件处理 / Living update event handler
     * 管理侵蚀效果的自动升级逻辑
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        Effect upgradableEffect = InitEffects.YELLOW_TEMPERANCE_EROSION.get();

        // 检查实体是否有所需效果 / Check if entity has the required effect
        EffectInstance activeEffect = entity.getEffect(upgradableEffect);
        if (activeEffect == null) return;

        int duration = activeEffect.getDuration();
        int amplifier = activeEffect.getAmplifier();

        // 获取配置值 / Get config values
        int triggerTicks = YellowTemperanceConfig.erosionTriggerTicks.get();
        int resetDuration = YellowTemperanceConfig.erosionResetDuration.get();
        int fireResetDuration = YellowTemperanceConfig.erosionFireResetDuration.get();
        int maxLevel = YellowTemperanceConfig.erosionMaxLevel.get();

        // 检查剩余时间是否≤配置的时间阈值 / Check if remaining time is ≤ configured time threshold
        if (duration <= triggerTicks) {
            // 检查是否达到最大等级 / Check if maximum level is reached
            if (amplifier >= maxLevel - 1) {
                // 已达到最大等级，不升级但可延长持续时间 / Reached max level, don't upgrade but can extend duration
                // 满级时无论是否着火都使用标准重置时间 / Use standard reset time regardless of fire state at max level
                // 移除粒子效果 / Remove particle effects
                EffectInstance extendedEffect = new EffectInstance(
                        upgradableEffect,
                        resetDuration,
                        maxLevel - 1, // 保持最大等级 / Maintain maximum level
                        activeEffect.isAmbient(),
                        false, // 不显示粒子效果 / Don't show particle effects
                        activeEffect.showIcon()
                );

                entity.removeEffect(upgradableEffect);
                entity.addEffect(extendedEffect);
                return;
            }

            // 移除旧效果 / Remove old effect
            entity.removeEffect(upgradableEffect);

            // 创建新效果实例（等级+1） / Create new effect instance (level + 1)
            // 根据是否着火决定持续时间 / Decide duration based on whether on fire
            int newDuration = entity.isOnFire() ? fireResetDuration : resetDuration;
            // 移除粒子效果 / Remove particle effects
            EffectInstance newEffect = new EffectInstance(
                    upgradableEffect,
                    newDuration,
                    amplifier + 1,
                    activeEffect.isAmbient(),
                    false, // 不显示粒子效果 / Don't show particle effects
                    activeEffect.showIcon()
            );

            // 添加视觉/声音反馈 / Add visual/audio feedback
            if (entity.level.isClientSide) {
                // 播放音效 / Play sound effect
                // entity.playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.0F);
            }

            // 添加效果 / Add effect
            entity.addEffect(newEffect);
        }
    }
}