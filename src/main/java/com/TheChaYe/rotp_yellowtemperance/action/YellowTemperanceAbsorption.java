package com.TheChaYe.rotp_yellowtemperance.action;

import com.TheChaYe.rotp_yellowtemperance.effects.YellowTemperanceErosionEffect;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.stand.StandAction;
import com.github.standobyte.jojo.client.particle.custom.CustomParticlesHelper;
import com.github.standobyte.jojo.client.sound.ClientTickingSoundsHelper;
import com.github.standobyte.jojo.client.sound.HamonSparksLoopSound;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonUtil;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

/**
 * 黄色节制吸收能力 / Yellow Temperance Absorption Ability
 * 处理替身的吸收攻击逻辑，包括伤害计算、侵蚀效果应用等
 * Handles the absorption attack logic for the stand, including damage calculation and erosion effect application
 */
public class YellowTemperanceAbsorption extends StandAction {
    /**
     * 血液汲取效果数组 / Blood drain effects array
     * 定义吸收时施加的负面效果类型
     * Defines the negative effect types applied during absorption
     */
    private static final Effect[] BLOOD_DRAIN_EFFECTS = {
            Effects.MOVEMENT_SLOWDOWN,
            Effects.WEAKNESS
    };
    
    /**
     * 伤害计数器 / Damage counter
     * 用于记录造成伤害的次数，控制饥饿值恢复
     * Used to record the number of damages dealt, controlling hunger restoration
     */
    private int hurtCount = 0;

    /**
     * 构造函数 / Constructor
     * @param builder 动作构建器 / Action builder
     */
    public YellowTemperanceAbsorption(Builder builder) {
        super(builder.holdType());
    }

    /**
     * 执行吸收逻辑 / Execute absorption logic
     * @param world 当前世界 / Current world
     * @param attacker 攻击者实体 / Attacking entity
     * @param target 目标实体 / Target entity
     * @param absorbDamage 吸收伤害值 / Absorption damage value
     * @return 是否成功造成伤害 / Whether damage was successfully dealt
     */
    public static boolean absorb(World world, LivingEntity attacker, LivingEntity target, float absorbDamage) {
        // 检查目标实体类型 / Check target entity type
        EntityClassification classification = target.getType().getCategory();
        boolean isVillager = target instanceof VillagerEntity;
        boolean isPlayer = target instanceof PlayerEntity;

        // 检查目标是否为创造模式玩家 / Check if target is creative mode player
        boolean isCreativePlayer = isPlayer && ((PlayerEntity) target).isCreative();

        // 验证目标是否可以被吸收 / Validate if target can be absorbed
        if (!(classification == EntityClassification.CREATURE ||
                classification == EntityClassification.MONSTER ||
                isVillager ||
                (isPlayer && !isCreativePlayer))) {
            // 显示无法吸收的提示信息 / Show cannot absorb message
            if (attacker instanceof PlayerEntity && target != attacker) {
                PlayerEntity player = (PlayerEntity) attacker;
                IFormattableTextComponent message = new TranslationTextComponent("action.rotp_yellowtemperance.absorption.cannot_absorb");
                player.displayClientMessage(message, true);
            }
            return false;
        }

        // 应用侵蚀效果 / Apply erosion effect
        if (!target.hasEffect(InitEffects.YELLOW_TEMPERANCE_EROSION.get())) {
            target.addEffect(new EffectInstance(
                    InitEffects.YELLOW_TEMPERANCE_EROSION.get(),
                    200, // 10 seconds (20 ticks/sec * 10 sec = 200 ticks)
                    0,   // Level 1 (levels counted from 0)
                    false,
                    false,
                    true
            ));

            // 记录施法者信息 / Record caster information
            if (attacker instanceof PlayerEntity) {
                CompoundNBT entityData = target.getPersistentData();
                entityData.putUUID(YellowTemperanceErosionEffect.CASTER_UUID_TAG, attacker.getUUID());
            }
        }

        // 创建自定义伤害源 / Create custom damage source
        EntityDamageSource damageSource = new EntityDamageSource("yellow_temperance_erosion", attacker) {
            @Override
            public boolean isMagic() {
                return true; // Uses magic damage reduction mechanics
            }
        };

        if (HamonUtil.preventBlockDamage(target, attacker.level, null, null, damageSource, absorbDamage)) {
            Vector3d userPos = attacker.getEyePosition(1.0F);
            double distanceToTarget = JojoModUtil.getDistance(attacker, target.getEntity().getBoundingBox());
            Vector3d targetPos = attacker.getEyePosition(1.0F).add(attacker.getLookAngle().scale(distanceToTarget));
            Vector3d particlesPos = userPos.add(targetPos.subtract(userPos).scale(0.5));
            if (world.isClientSide()) {
                HamonSparksLoopSound.playSparkSound(attacker, particlesPos, 1.0F, true);
                CustomParticlesHelper.createHamonSparkParticles(null, particlesPos, 1);
            }
            return false;
        }

        // 临时禁用击退效果 / Temporarily disable knockback
        double originalKnockbackResistance = target.getAttribute(net.minecraft.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).getBaseValue();
        target.getAttribute(net.minecraft.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        
        // 执行吸收伤害 / Execute absorption damage
        boolean hurt = DamageUtil.dealPillarmanAbsorptionDamage(target, absorbDamage, attacker);
        
        // 恢复原始击退抗性 / Restore original knockback resistance
        target.getAttribute(net.minecraft.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(originalKnockbackResistance);

        // 应用血液汲取效果 / Apply blood drain effects
        if (hurt) {
            for (Effect effect : BLOOD_DRAIN_EFFECTS) {
                int duration = MathHelper.floor(10F * absorbDamage); // Shortened debuff duration
                EffectInstance effectInstance = target.getEffect(effect);
                
                // 添加虚弱效果但不显示粒子 / Add weakness effect without particles
                EffectInstance newInstance = effectInstance == null ?
                        new EffectInstance(effect, duration, 1, false, false, true)
                        : new EffectInstance(effect, effectInstance.getDuration() + duration, 1, false, false, true);
                target.addEffect(newInstance);
            }
        }
        return hurt;
    }

    /**
     * 检查目标条件 / Check target conditions
     * @param target 目标 / Target
     * @param user 用户实体 / User entity
     * @param standPower 替身能力 / Stand power
     * @return 动作条件结果 / Action condition result
     */
    @Override
    public ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, IStandPower standPower) {
        Entity entityTarget = target.getEntity();
        if (entityTarget instanceof LivingEntity) {
            return super.checkTarget(target, user, standPower);
        }
        return ActionConditionResult.NEGATIVE_CONTINUE_HOLD;
    }

    /**
     * 检查特定条件 / Check specific conditions
     * @param user 用户实体 / User entity
     * @param power 替身能力 / Stand power
     * @param target 目标 / Target
     * @return 动作条件结果 / Action condition result
     */
    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        if (user.level.getDifficulty() == Difficulty.PEACEFUL) {
            return conditionMessage("peaceful");
        }
        return ActionConditionResult.POSITIVE;
    }

    /**
     * 持续执行逻辑 / Hold tick logic
     * @param world 当前世界 / Current world
     * @param user 用户实体 / User entity
     * @param userPower 用户替身能力 / User stand power
     * @param ticksHeld 持续时间 / Ticks held
     * @param target 目标 / Target
     * @param requirementsFulfilled 条件是否满足 / Whether requirements are fulfilled
     */
    @Override
    protected void holdTick(World world, LivingEntity user, IStandPower userPower, int ticksHeld, ActionTarget target, boolean requirementsFulfilled) {
        if (requirementsFulfilled) {
            if (!world.isClientSide() && target.getEntity() instanceof LivingEntity) {
                LivingEntity targetEntity = (LivingEntity) target.getEntity();
                if (!targetEntity.isDeadOrDying()) {
                    boolean hurt = absorb(world, user, targetEntity, 0.5F); // Reduced damage to 0.5 points
                    
                    // 控制饥饿值恢复 / Control hunger restoration
                    if (hurt) {
                        hurtCount++;
                        if (hurtCount >= 3) {
                            if (user instanceof PlayerEntity) {
                                PlayerEntity player = (PlayerEntity) user;
                                player.getFoodData().eat(1, 0.0F); // Restore 1 hunger point every 3 hits
                            }
                            hurtCount = 0; // Reset counter
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取目标需求 / Get target requirement
     * @return 目标需求类型 / Target requirement type
     */
    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ENTITY;
    }

    /**
     * 获取实体目标最大范围 / Get maximum range for entity target
     * @return 最大范围平方值 / Maximum range squared value
     */
    @Override
    public double getMaxRangeSqEntityTarget() {
        return 7.5;
    }

    /**
     * 客户端持续效果 / Client hold tick effect
     * @param user 用户实体 / User entity
     * @param power 替身能力 / Stand power
     * @param ticksHeld 持续时间 / Ticks held
     * @param reqFulfilled 条件是否满足 / Whether requirements fulfilled
     * @param reqStateChanged 条件状态是否改变 / Whether requirement state changed
     */
    @Override
    public void onHoldTickClientEffect(LivingEntity user, IStandPower power, int ticksHeld, boolean reqFulfilled, boolean reqStateChanged) {
        if (reqStateChanged && reqFulfilled) {
            ClientTickingSoundsHelper.playHeldActionSound(ModSounds.PILLAR_MAN_ABSORPTION.get(), 1.25F, 0.8F, true, user, power, this);
        }
    }

    /**
     * 持续状态下是否允许其他动作 / Whether other actions are allowed during hold
     * @param standPower 替身能力 / Stand power
     * @param action 动作 / Action
     * @return 是否允许 / Whether allowed
     */
    @Override
    public boolean heldAllowsOtherAction(IStandPower standPower, Action<IStandPower> action) {
        return true;
    }

}