package com.TheChaYe.rotp_yellowtemperance.entity.stand;

import com.github.standobyte.jojo.entity.stand.StandEntityType;
import net.minecraft.world.World;

/**
 * Khnum替身实体 / Khnum Stand Entity
 * 基于黄色节制的子类替身，继承所有伪装功能
 */
public class KhnumEntity extends AbstractDisguiseStandEntity {
    public KhnumEntity(StandEntityType<KhnumEntity> type, World world) {
        super(type, world);
        unsummonOffset = getDefaultOffsetFromUser().copy();
    }

    /**
     * 移除实体 / Remove entity
     * 实体被销毁时的清理逻辑
     */
    @Override
    public void remove() {
        // 替身被移除时清除效果和伪装状态 / Clear effects and disguise state when stand is removed
        if (getUser() != null) {
            clearBothDisguises();
        }
        super.remove();
    }
}