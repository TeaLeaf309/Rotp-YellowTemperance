package com.TheChaYe.rotp_yellowtemperance.server;

import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.TheChaYe.rotp_yellowtemperance.network.ModNetwork;
import com.TheChaYe.rotp_yellowtemperance.network.packets.client.SyncOtherPlayerPhysicsPacket;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

/**
 * 服务端伪装处理器 / Server Disguise Handler
 * 处理服务端的伪装物理属性同步和更新
 */
@Mod.EventBusSubscriber
public class ServerDisguiseHandler {

    /**
     * 服务端tick事件处理 / Server tick event handler
     * 在服务端定期检查并同步玩家的物理属性
     *
     * @param event 服务器tick事件 / Server tick event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 在服务端定期检查并同步玩家的物理属性 / Periodically check and sync player physics on server
        }
    }

    /**
     * 更新玩家物理属性 / Update player physics
     * 在服务端更新玩家的物理属性，确保与客户端同步
     *
     * @param player 需要更新的玩家 / Player to update
     */
    public static void updatePlayerPhysicsOnServer(PlayerEntity player) {
        // 检查玩家是否处于伪装状态 / Check if player is in disguise state
        // 通过玩家的替身能力检查伪装状态 / Check disguise status through player's stand power
        IStandPower.getStandPowerOptional(player).ifPresent(standPower -> {
            if (standPower.getStandManifestation() instanceof AbstractDisguiseStandEntity) {
                AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity) standPower.getStandManifestation();
                if (disguiseStand.isUserDisguised() && disguiseStand.getUserDisguiseEntity().isPresent()) {
                    EntityType<?> disguiseType = disguiseStand.getUserDisguiseEntity().get();
                    // 创建临时伪装实体用于计算物理属性 / Create temporary disguise entity for physics calculation
                    LivingEntity disguise = (LivingEntity) disguiseType.create(player.level);
                    if (disguise != null) {

                        // 应用伪装实体的物理属性到玩家 / Apply disguise entity physics to player
                        applyDisguisePhysics(player, disguise);
                        disguise.remove();
                    }
                }
            }
        });
    }

    /**
     * 应用伪装物理属性 / Apply disguise physics
     * 应用伪装实体的物理属性到玩家
     *
     * @param player   玩家实体 / Player entity
     * @param disguise 伪装实体 / Disguise entity
     */
    private static void applyDisguisePhysics(PlayerEntity player, LivingEntity disguise) {
        try {
            // 获取伪装实体的尺寸 / Get disguise entity dimensions
            EntitySize disguiseSize = disguise.getDimensions(player.getPose());
            EntitySize playerSize = player.getDimensions(player.getPose());

            // 使用Pehkui API设置碰撞箱宽度 / Use Pehkui API to set hitbox width
            float widthScale = 1.0f;
            if (Math.abs(disguiseSize.width - playerSize.width) > 0.01f) {
                ScaleData hitboxWidthData = ScaleTypes.HITBOX_WIDTH.getScaleData(player);
                widthScale = disguiseSize.width / playerSize.width;
                hitboxWidthData.setBaseScale(widthScale);
                hitboxWidthData.markForSync(true);
            }

            // 使用Pehkui API设置碰撞箱高度 / Use Pehkui API to set hitbox height
            float heightScale = 1.0f;
            if (Math.abs(disguiseSize.height - playerSize.height) > 0.01f) {
                ScaleData hitboxHeightData = ScaleTypes.HITBOX_HEIGHT.getScaleData(player);
                heightScale = disguiseSize.height / playerSize.height;
                hitboxHeightData.setBaseScale(heightScale);
                hitboxHeightData.markForSync(true);
            }

            // 强制更新玩家的边界框以确保碰撞检测正常工作 / Force update player bounding box to ensure collision detection works
            player.setBoundingBox(disguiseSize.makeBoundingBox(player.position()));

            // 广播给所有跟踪该玩家的客户端 / Broadcast to all clients tracking this player
            if (player instanceof ServerPlayerEntity) {
                ModNetwork.sendToAllTracking(
                        new SyncOtherPlayerPhysicsPacket(player.getUUID(), 1.0f, widthScale, heightScale),
                        (ServerPlayerEntity) player
                );
            }

        } catch (Exception e) {
            // 记录错误但不中断执行 / Log error but don't interrupt execution
            e.printStackTrace();
        }
    }

    /**
     * 恢复玩家物理属性 / Restore player physics
     * 恢复玩家的原始物理属性
     *
     * @param player 需要恢复的玩家 / Player to restore
     */
    public static void restorePlayerPhysicsOnServer(PlayerEntity player) {
        try {
            // 恢复眼高 / Restore eye height
            ScaleData eyeHeightData = ScaleTypes.EYE_HEIGHT.getScaleData(player);
            eyeHeightData.setBaseScale(1.0f);
            eyeHeightData.markForSync(true);

            // 恢复碰撞箱宽度 / Restore hitbox width
            ScaleData hitboxWidthData = ScaleTypes.HITBOX_WIDTH.getScaleData(player);
            hitboxWidthData.setBaseScale(1.0f);
            hitboxWidthData.markForSync(true);

            // 恢复碰撞箱高度 / Restore hitbox height
            ScaleData hitboxHeightData = ScaleTypes.HITBOX_HEIGHT.getScaleData(player);
            hitboxHeightData.setBaseScale(1.0f);
            hitboxHeightData.markForSync(true);

            // 广播给所有跟踪该玩家的客户端 / Broadcast to all clients tracking this player
            if (player instanceof ServerPlayerEntity) {
                ModNetwork.sendToAllTracking(
                        new SyncOtherPlayerPhysicsPacket(player.getUUID(), 1.0f, 1.0f, 1.0f),
                        (ServerPlayerEntity) player
                );
            }

        } catch (Exception e) {
            // 记录错误但不中断执行 / Log error but don't interrupt execution
            e.printStackTrace();
        }
    }
}