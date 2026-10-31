package com.TheChaYe.rotp_yellowtemperance.server;

import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
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
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端伪装处理器 / Server Disguise Handler
 * 处理服务端的伪装物理属性同步和更新
 */
@Mod.EventBusSubscriber
public class ServerDisguiseHandler {

    /** 进入伪装前玩家自身的 Pehkui 基础缩放（眼高/宽/高），用于恢复而不是粗暴写 1.0 */
    private static final Map<UUID, float[]> ORIGINAL_PHYSICS_SCALES = new HashMap<>();

    /** 解除伪装后额外补偿恢复的 tick 数（防止 Pehkui/客户端在途包把刚恢复的碰撞箱改回去） */
    private static final Map<UUID, Integer> PENDING_RESTORES = new HashMap<>();

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
     * 在伪装状态切换为 true 的那一刻记录玩家原本的 Pehkui 基础缩放。
     * 必须在客户端开始发送伪装体型包之前调用，否则会把伪装值当成原值。
     */
    public static void saveOriginalPhysicsScales(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            ORIGINAL_PHYSICS_SCALES.putIfAbsent(player.getUUID(), new float[]{
                    ScaleTypes.EYE_HEIGHT.getScaleData(player).getBaseScale(),
                    ScaleTypes.HITBOX_WIDTH.getScaleData(player).getBaseScale(),
                    ScaleTypes.HITBOX_HEIGHT.getScaleData(player).getBaseScale()
            });
        }
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
                PacketHandler.sendToAllTracking(
                        new SyncOtherPlayerPhysicsPacket(player.getUUID(), 1.0f, widthScale, heightScale),
                        (ServerPlayerEntity) player
                );
            }

        } catch (Exception e) {
            // 记录错误但不中断执行 / Log error but don't interrupt execution
            e.printStackTrace();
        }
    }

    /** 解除伪装后调度若干 tick 的幂等恢复，确保碰撞箱最终一定回到原值。 */
    public static void scheduleRestore(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            PENDING_RESTORES.put(player.getUUID(), 5);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_RESTORES.isEmpty()) {
            return;
        }
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            PENDING_RESTORES.clear();
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_RESTORES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            restorePlayerPhysicsOnServer(player);
            int left = entry.getValue() - 1;
            if (left <= 0) {
                iterator.remove();
            } else {
                entry.setValue(left);
            }
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
            float[] original = ORIGINAL_PHYSICS_SCALES.remove(player.getUUID());
            float eyeScale = original != null ? original[0] : 1.0f;
            float widthScale = original != null ? original[1] : 1.0f;
            float heightScale = original != null ? original[2] : 1.0f;
            com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon.LOGGER.info(
                    "[YT] restore physics for {}: original={} => eye={}, width={}, height={}",
                    player.getName().getString(), original != null, eyeScale, widthScale, heightScale);

            // 恢复眼高 / Restore eye height
            ScaleData eyeHeightData = ScaleTypes.EYE_HEIGHT.getScaleData(player);
            eyeHeightData.setBaseScale(eyeScale);
            eyeHeightData.setScaleTickDelay(0);
            eyeHeightData.markForSync(true);

            // 恢复碰撞箱宽度 / Restore hitbox width
            ScaleData hitboxWidthData = ScaleTypes.HITBOX_WIDTH.getScaleData(player);
            hitboxWidthData.setBaseScale(widthScale);
            hitboxWidthData.setScaleTickDelay(0);
            hitboxWidthData.markForSync(true);

            // 恢复碰撞箱高度 / Restore hitbox height
            ScaleData hitboxHeightData = ScaleTypes.HITBOX_HEIGHT.getScaleData(player);
            hitboxHeightData.setBaseScale(heightScale);
            hitboxHeightData.setScaleTickDelay(0);
            hitboxHeightData.markForSync(true);

            // 关键：Pehkui 改变 base scale 后不会主动重算当前碰撞箱，必须刷新一次
            player.refreshDimensions();

            // 广播给所有跟踪该玩家的客户端 / Broadcast to all clients tracking this player
            if (player instanceof ServerPlayerEntity) {
                PacketHandler.sendToAllTracking(
                        new SyncOtherPlayerPhysicsPacket(player.getUUID(), eyeScale, widthScale, heightScale),
                        (ServerPlayerEntity) player
                );
            }

        } catch (Exception e) {
            // 记录错误但不中断执行 / Log error but don't interrupt execution
            e.printStackTrace();
        }
    }
}