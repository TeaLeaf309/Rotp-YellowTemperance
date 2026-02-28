package com.TheChaYe.rotp_yellowtemperance.events;

import com.TheChaYe.rotp_yellowtemperance.capability.entity.CapabilityHandler;
import com.TheChaYe.rotp_yellowtemperance.effects.YellowTemperanceErosionEffect;
import com.TheChaYe.rotp_yellowtemperance.init.InitEffects;
import com.TheChaYe.rotp_yellowtemperance.init.InitStands;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 黄色节制死亡事件处理器 / Yellow Temperance Death Event Handler
 * 处理黄色节制替身使者死亡时的侵蚀效果清理逻辑
 */
@Mod.EventBusSubscriber
public class YellowTemperanceDeathHandler {

    /**
     * 黄色节制使用者死亡事件处理 / Yellow Temperance user death event handler
     * 清理由死亡玩家施加的侵蚀效果
     */
    @SubscribeEvent
    public static void onYellowTemperanceUserDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity == null || entity.level.isClientSide()) return;

        // 检查死亡实体是否是黄色节制替身使者 / Check if deceased entity is Yellow Temperance stand user
        IStandPower.getStandPowerOptional(entity).ifPresent(power -> {
            StandType<?> YELLOW_TEMPERANCE = InitStands.STAND_YELLOW_TEMPERANCE.getStandType();

            if (power.getType() == YELLOW_TEMPERANCE) {
                // 获取死亡玩家的UUID / Get UUID of deceased player
                UUID deadCasterUUID = entity.getUUID();

                // 遍历所有受影响实体 / Iterate through all affected entities
                ((ServerWorld) entity.level).getAllEntities().forEach(target -> {
                    if (target instanceof LivingEntity) {
                        LivingEntity livingTarget = (LivingEntity) target;
                        // 检查是否有黄色节制的侵蚀效果 / Check if target has Yellow Temperance erosion effect
                        if (livingTarget.hasEffect(InitEffects.YELLOW_TEMPERANCE_EROSION.get())) {
                            // 检查效果是否由死亡的玩家施加 / Check if effect was applied by deceased player
                            CompoundNBT entityData = livingTarget.getPersistentData();
                            if (entityData.contains(YellowTemperanceErosionEffect.CASTER_UUID_TAG)) {
                                UUID effectCasterUUID = entityData.getUUID(YellowTemperanceErosionEffect.CASTER_UUID_TAG);
                                // 如果效果是由死亡的玩家施加的，则移除效果 / If effect was applied by deceased player, remove effect
                                if (effectCasterUUID.equals(deadCasterUUID)) {
                                    livingTarget.removeEffect(InitEffects.YELLOW_TEMPERANCE_EROSION.get());
                                    // 清除PersistentData中的施加者信息 / Clear caster information from PersistentData
                                    entityData.remove(YellowTemperanceErosionEffect.CASTER_UUID_TAG);
                                }
                            }
                        }
                    }
                });

                // 重置使者的侵蚀状态 / Reset user's erosion state
                entity.getCapability(CapabilityHandler.LIVING_DATA_CAPABILITY).ifPresent(data -> {
                    data.setErosionActive(false);
                    data.setErosionLevel(0);
                });
            }
        });
    }
}