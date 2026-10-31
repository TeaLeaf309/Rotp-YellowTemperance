package com.TheChaYe.rotp_yellowtemperance.mixin;

import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

/**
 * 访问器：读取/清空索敌 goal 的 target，以及读取目标类型与谓词。
 * 用 @Accessor 而不是 @Shadow：Accessor 的字段名会进 refmap（@Shadow 字段在 0.8.4 的 AP 里
 * 可能不会写入 refmap，正式环境混淆后会找不到字段）。
 */
@Mixin(NearestAttackableTargetGoal.class)
public interface NearestAttackableTargetGoalAccessor {

    @Accessor("targetType")
    Class<?> yt$targetType();

    @Accessor("targetConditions")
    EntityPredicate yt$targetConditions();

    @Accessor("target")
    @Nullable
    LivingEntity yt$target();

    @Accessor("target")
    void yt$setTarget(@Nullable LivingEntity target);
}
