package com.TheChaYe.rotp_yellowtemperance.mixin;

import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问器：读取 TargetGoal 的 mob 字段（发现层过滤用）/ Accessor: TargetGoal.mob
 */
@Mixin(TargetGoal.class)
public interface TargetGoalAccessor {

    @Accessor("mob")
    MobEntity yt$mob();
}
