package com.TheChaYe.rotp_yellowtemperance.init;

import com.TheChaYe.rotp_yellowtemperance.entity.YellowTemperanceEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.init.power.stand.EntityStandRegistryObject.EntityStandSupplier;
import com.github.standobyte.jojo.power.impl.stand.stats.StandStats;
import com.github.standobyte.jojo.power.impl.stand.type.EntityStandType;

/**
 * 插件替身注册 / Addon Stands Registration
 * 管理插件中自定义替身的注册和引用
 */
public class AddonStands {

    /** 黄色节制替身供应器 / Yellow Temperance stand supplier */
    public static final EntityStandSupplier<EntityStandType<StandStats>, StandEntityType<YellowTemperanceEntity>>
            YELLOW_TEMPERANCE = new EntityStandSupplier<>(InitStands.STAND_YELLOW_TEMPERANCE);
}