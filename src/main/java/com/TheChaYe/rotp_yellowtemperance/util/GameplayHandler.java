package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.init.InitStands;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.standobyte.jojo.util.mc.damage.DamageUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/**
 * 游戏玩法事件处理器 / Gameplay Event Handler
 * 处理与黄色节制替身相关的游戏玩法事件和机制
 * Handles gameplay events and mechanics related to Yellow Temperance Stand
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID)
public class GameplayHandler {
    /**
     * 阻挡紫外线伤害 / Block Ultraviolet Damage
     * 当黄色节制替身激活时阻挡紫外线伤害
     *
     * @param event 生物受到攻击事件 / Living entity attack event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void blockSunDamage(LivingAttackEvent event) {
        if (!event.getEntity().level.isClientSide) {
            LivingEntity entity = event.getEntityLiving();
            IStandPower.getStandPowerOptional(entity).ifPresent(power -> {
                StandType<?> yt = InitStands.STAND_YELLOW_TEMPERANCE.getStandType();
                if (power.getType() == yt && power.isActive()) {
                    if (event.getSource() == DamageUtil.ULTRAVIOLET) {
                        event.setCanceled(true);
                    }
                }
            });
        }
    }

}
