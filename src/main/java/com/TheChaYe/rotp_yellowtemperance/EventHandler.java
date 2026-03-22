package com.TheChaYe.rotp_yellowtemperance;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.power.impl.stand.StandUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 事件处理器 / Event Handler
 * 处理黄色节制模组的各种游戏事件
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID)
public class EventHandler {
    /**
     * 实体加入世界事件 / Entity join world event
     * 处理实体加入世界时的内存磁盘影响传递
     *
     * @param event 实体加入世界事件 / Entity join world event
     */
    @SubscribeEvent
    public static void EntityJoinWorldEvent(EntityJoinWorldEvent event) {
        final World world = event.getWorld();
        if (event.getEntity() instanceof StandEntity) {
            final StandEntity stand = (StandEntity) event.getEntity();
            final LivingEntity user = StandUtil.getStandUser(stand);
            if (user != null) {
                stand.getPersistentData().putBoolean("MEMORY_DISK_AFFECTED", user.getPersistentData().getBoolean("MEMORY_DISK_AFFECTED"));
            }
        }
    }


    /**
     * 生物攻击事件 / Living attack event
     * 处理受内存磁盘影响的实体攻击事件取消
     *
     * @param event 生物攻击事件 / Living attack event
     */
    @SubscribeEvent
    public static void LivingAttackEvent(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity) {
            final LivingEntity source_living = (LivingEntity) event.getSource().getEntity();
            if (source_living.getPersistentData().getBoolean("MEMORY_DISK_AFFECTED")) {
                event.setCanceled(true);
            }
        }
    }
}
