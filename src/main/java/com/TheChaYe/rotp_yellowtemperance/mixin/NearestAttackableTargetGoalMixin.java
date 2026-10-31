package com.TheChaYe.rotp_yellowtemperance.mixin;

import com.TheChaYe.rotp_yellowtemperance.ai.MobPerception;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 伪装感知核心：在"候选发现"阶段做两件事。
 *
 * <p>1) <b>过滤</b>：候选是玩家、但伪装后不该被这个生物感知时，把 target 置 null。
 * 放在 findTarget 的 TAIL：被过滤时 goal 的 canUse() 返回 false，不占用 TARGET flag，
 * 原版优先级链会继续去找村民/铁傀儡等目标——这是"停止索敌"的根因修复。
 * 禁止在 LivingSetAttackTargetEvent 里清目标（那时 goal 已经抢占 flag，会每 tick 反复抢）。</p>
 *
 * <p>2) <b>补目标</b>：物种 goal（targetType 不是玩家的父类型）原本只按实体 Class 搜索，
 * 永远找不到玩家。比如铁傀儡的"攻击怪物"goal（targetType=MobEntity）不会找玩家，
 * 所以伪装成僵尸时铁傀儡不打你。这里在它没找到真实目标时，把附近伪装成匹配物种的玩家
 * 塞进 target，让"把玩家当成该物种"对这类 goal 也成立。</p>
 */
@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin {

    @Inject(method = "findTarget", at = @At("TAIL"))
    private void yt$adjustDisguiseTargeting(CallbackInfo ci) {
        if (!(this instanceof NearestAttackableTargetGoalAccessor)
                || !(this instanceof TargetGoalAccessor)) {
            return; // 访问器 mixin 未生效：不干预，保持原版
        }
        NearestAttackableTargetGoalAccessor accessor = (NearestAttackableTargetGoalAccessor) this;
        MobEntity mob = ((TargetGoalAccessor) this).yt$mob();
        if (mob == null) {
            return;
        }

        LivingEntity current = accessor.yt$target();
        if (current instanceof PlayerEntity) {
            // 1) 过滤：伪装后不该被感知的玩家从候选里去掉
            if (!MobPerception.canPerceive(mob, (PlayerEntity) current)) {
                accessor.yt$setTarget(null);
            }
            return;
        }
        if (current != null) {
            return; // 已经找到真实目标，不干预
        }

        // 只给"原版普通目标 goal"补玩家目标：部分子类（如 NearestHealableRaiderTargetGoal）
        // 会把 target 强转成自己的泛型类型，塞玩家进去会 CCE。
        if (!NearestAttackableTargetGoal.class.equals(this.getClass())) {
            return;
        }
        Class<?> targetType = accessor.yt$targetType();
        if (targetType == null || targetType.isAssignableFrom(PlayerEntity.class)) {
            return; // "载体"goal（本来就是找玩家的），过滤逻辑已经在上面处理
        }

        // 2) 补目标：物种 goal 没找到真实实体时，尝试把伪装成该物种的玩家作为目标
        PlayerEntity candidate = MobPerception.findDisguisedTargetForGoal(
                mob, targetType, accessor.yt$targetConditions());
        if (candidate != null) {
            accessor.yt$setTarget(candidate);
        }
    }
}