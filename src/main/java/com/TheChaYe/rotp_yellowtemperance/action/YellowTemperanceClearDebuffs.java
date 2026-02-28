package com.TheChaYe.rotp_yellowtemperance.action;

import com.TheChaYe.rotp_yellowtemperance.effects.YellowTemperanceErosionEffect;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/**
 * 黄色节制清除Debuff能力 / Yellow Temperance Clear Debuffs Ability
 * 清除玩家施加的所有侵蚀效果
 * Clears all erosion effects applied by the player
 */
public class YellowTemperanceClearDebuffs extends StandAction {

    /**
     * 构造函数 / Constructor
     * @param builder 动作构建器 / Action builder
     */
    public YellowTemperanceClearDebuffs(Builder builder) {
        super(builder);
    }

    /**
     * 执行清除Debuff动作 / Perform clear debuffs action
     * @param world 当前世界 / Current world
     * @param user 用户实体 / User entity
     * @param power 替身能力 / Stand power
     * @param target 目标 / Target
     */
    @Override
    public void perform(World world, LivingEntity user, IStandPower power, ActionTarget target) {
        if (!world.isClientSide() && user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            clearPlayerDebuffs(world, player);
        }
    }

    /**
     * 清除玩家施加的所有debuff / Clear all debuffs applied by player
     * @param world 世界对象 / World object
     * @param player 玩家对象 / Player object
     */
    private void clearPlayerDebuffs(World world, PlayerEntity player) {
        // 遍历世界中的所有实体 / Iterate through all entities in the world
        if (world instanceof ServerWorld) {
            ((ServerWorld) world).getAllEntities().forEach(entity -> {
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    
                    // 检查侵蚀效果 / Check for erosion effect
                    EffectInstance effect = livingEntity.getEffect(InitEffects.YELLOW_TEMPERANCE_EROSION.get());
                    if (effect != null) {
                        // 验证施法者身份 / Verify caster identity
                        CompoundNBT entityData = livingEntity.getPersistentData();
                        if (entityData.contains(YellowTemperanceErosionEffect.CASTER_UUID_TAG)) {
                            if (entityData.getUUID(YellowTemperanceErosionEffect.CASTER_UUID_TAG).equals(player.getUUID())) {
                                // 移除效果和标记 / Remove effect and marker
                                livingEntity.removeEffect(InitEffects.YELLOW_TEMPERANCE_EROSION.get());
                                entityData.remove(YellowTemperanceErosionEffect.CASTER_UUID_TAG);
                            }
                        }
                    }
                }
            });
        }
    }
}