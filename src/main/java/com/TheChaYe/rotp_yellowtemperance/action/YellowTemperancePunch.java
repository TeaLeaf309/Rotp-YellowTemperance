package com.TheChaYe.rotp_yellowtemperance.action;

import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.effects.YellowTemperanceErosionEffect;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.TheChaYe.rotp_yellowtemperance.init.InitSounds;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandEntityLightAttack;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityTask;
import com.github.standobyte.jojo.entity.stand.StandStatFormulas;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;

/**
 * 黄金体验镇魂曲拳击能力 / Yellow Temperance Punch Ability
 * 处理替身的轻攻击逻辑，包括伤害计算和侵蚀效果应用
 * Handles the stand's light attack logic, including damage calculation and erosion effect application
 */
public class YellowTemperancePunch extends StandEntityLightAttack {
    


    /**
     * 构造函数 / Constructor
     * @param builder 动作构建器 / Action builder
     */
    public YellowTemperancePunch(Builder builder) {
        super(builder);
    }

    /**
     * 执行拳击攻击 / Execute punch attack
     * @param world 当前世界 / Current world
     * @param user 攻击者 / Attacker
     * @param userPower 用户替身能力 / User stand power
     * @param target 目标 / Target
     * @param sound 音效 / Sound effect
     * @param volume 音量 / Volume
     * @param pitch 音调 / Pitch
     */
    public static void punchPerform(World world, LivingEntity user, IStandPower userPower, ActionTarget target, SoundEvent sound, float volume, float pitch) {
        if (target.getType() == ActionTarget.TargetType.ENTITY) {
            Entity entity = target.getEntity();
            if (!world.isClientSide && entity instanceof LivingEntity) {
                LivingEntity targetEntity = (LivingEntity) entity;
                DamageSource damageSource = user instanceof PlayerEntity ?
                        DamageSource.playerAttack((PlayerEntity) user) :
                        DamageSource.mobAttack(user);

                if (targetEntity.hurt(damageSource, getDamage(world, userPower))) {
                    world.playSound(null, targetEntity.getX(), targetEntity.getEyeY(), targetEntity.getZ(), sound, targetEntity.getSoundSource(), volume, pitch);
                    targetEntity.knockback(1.0F, user.getX() - targetEntity.getX(), user.getZ() - targetEntity.getZ());

                    // 应用侵蚀效果 / Apply erosion effect
                    applyErosionEffect(world, targetEntity, user);
                }
            }
        }
    }

    /**
     * 应用侵蚀效果 / Apply erosion effect
     * @param world 当前世界 / Current world
     * @param targetEntity 目标实体 / Target entity
     * @param attacker 攻击者 / Attacker
     */
    private static void applyErosionEffect(World world, LivingEntity targetEntity, LivingEntity attacker) {
        // 获取配置值 / Get config values
        YellowTemperanceConfig.Common config = YellowTemperanceConfig.getCommonConfigInstance(!world.isClientSide());
        int maxErosionLevel = config.maxErosionLevel.get();
        int levelIncrease = config.erosionLevelIncreasePerAttack.get();
        
        Effect yellowTemperance = InitEffects.YELLOW_TEMPERANCE_EROSION.get();
        EffectInstance currentEffect = targetEntity.getEffect(yellowTemperance);

        // 检查等级上限 / Check level cap
        if (currentEffect != null && currentEffect.getAmplifier() >= maxErosionLevel) {
            return; // 已达到最高等级 / Already at maximum level
        }

        int newAmplifier = 0;

        if (currentEffect != null) {
            int currentLevel = currentEffect.getAmplifier();
            newAmplifier = Math.min(currentLevel + levelIncrease, maxErosionLevel);

            if (newAmplifier > currentLevel) {
                targetEntity.removeEffect(yellowTemperance);
            }
        }

        // 创建侵蚀效果实例 / Create erosion effect instance
        EffectInstance effectInstance = new EffectInstance(
                yellowTemperance,
                200,
                newAmplifier,
                false, false, false
        );

        targetEntity.addEffect(effectInstance);

        // 记录施法者信息 / Record caster information
        if (attacker instanceof PlayerEntity) {
            CompoundNBT entityData = targetEntity.getPersistentData();
            entityData.putUUID(YellowTemperanceErosionEffect.CASTER_UUID_TAG, attacker.getUUID());
        }

        // 更新攻击者的侵蚀状态 / Update attacker's erosion state
        if (attacker != null && !(attacker instanceof FakePlayer)) {
            attacker.getCapability(CapabilityHandler.LIVING_DATA_CAPABILITY).ifPresent(data -> {
                data.setErosionActive(true);
                data.setErosionLevel(Math.min(data.getErosionLevel() + levelIncrease, maxErosionLevel));
            });
        }
    }

    /**
     * 计算攻击伤害 / Calculate attack damage
     * @param world 当前世界 / Current world
     * @param power 替身能力 / Stand power
     * @return 伤害值 / Damage value
     */
    public static float getDamage(World world, IStandPower power) {
        double powerStat = power.getType().getStats().getBasePower();
        return StandStatFormulas.getLightAttackDamage(powerStat);
    }

    /**
     * 执行替身攻击 / Execute stand attack
     * @param world 当前世界 / Current world
     * @param standEntity 替身实体 / Stand entity
     * @param userPower 用户替身能力 / User stand power
     * @param task 替身任务 / Stand task
     */
    @Override
    public void standPerform(World world, StandEntity standEntity, IStandPower userPower, StandEntityTask task) {
        // 继承自父类的空实现 / Empty implementation inherited from parent class
    }

    /**
     * 执行攻击动作 / Perform attack action
     * @param world 当前世界 / Current world
     * @param user 用户实体 / User entity
     * @param power 替身能力 / Stand power
     * @param target 目标 / Target
     * @param extraInput 额外输入数据 / Extra input data
     */
    @Override
    protected void perform(World world, LivingEntity user, IStandPower power, ActionTarget target, @Nullable PacketBuffer extraInput) {
        punchPerform(world, user, power, target, InitSounds.WHITE_ALBUM_PUNCH_LIGHT.get(), 1F, 1F);
    }
}