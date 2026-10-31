package com.TheChaYe.rotp_yellowtemperance.mixin;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * 访问器：读取 GoalSelector 的全部目标（阶段 0 探针使用）/ Accessor: GoalSelector goals
 * 仅用于读，不改变任何 AI 行为。
 */
@Mixin(GoalSelector.class)
public interface GoalSelectorAccessor {

    @Accessor("availableGoals")
    Set<PrioritizedGoal> yt$availableGoals();
}
