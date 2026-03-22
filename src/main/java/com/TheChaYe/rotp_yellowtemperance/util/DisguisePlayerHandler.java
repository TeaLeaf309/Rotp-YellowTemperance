package com.TheChaYe.rotp_yellowtemperance.util;

import com.TheChaYe.rotp_yellowtemperance.entity.stand.AbstractDisguiseStandEntity;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.UUID;

/**
 * 伪装玩家处理器 / Disguise Player Handler
 * 处理玩家伪装状态检查和实体类型识别逻辑
 * Handles player disguise status checking and entity type recognition logic
 */
public class DisguisePlayerHandler {

    /**
     * 获取感知实体类型 / Get Perceived Entity Type
     * 获取实体在玩家伪装状态下生物所认知的实体类型
     *
     * @param entity 目标实体 / Target entity
     * @return 生物感知到的实体类型 / Entity type perceived by mobs
     */
    public static EntityType<?> getPerceivedEntityType(Entity entity) {
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            IStandPower standPower = IStandPower.getStandPowerOptional(player).orElse(null);
            if (standPower != null && standPower.getStandManifestation() instanceof AbstractDisguiseStandEntity) {
                AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity)
                        standPower.getStandManifestation();
                if (disguiseStand.isUserDisguised() && disguiseStand.getUserDisguiseEntity().isPresent()) {
                    return disguiseStand.getUserDisguiseEntity().get();
                }
            }
        }
        return entity.getType();
    }

    /**
     * 检查玩家伪装状态 / Check Player Disguise Status
     * 检查指定玩家是否启用了黄色节制替身的伪装能力
     *
     * @param player 目标玩家 / Target player
     * @return 玩家是否处于伪装状态 / Whether the player is disguised
     */
    public static boolean isPlayerDisguised(PlayerEntity player) {
        IStandPower standPower = IStandPower.getStandPowerOptional(player).orElse(null);
        if (standPower != null && standPower.getStandManifestation() instanceof AbstractDisguiseStandEntity) {
            AbstractDisguiseStandEntity disguiseStand = (AbstractDisguiseStandEntity)
                    standPower.getStandManifestation();
            return disguiseStand.isUserDisguised();
        }
        return false;
    }

    /**
     * 检查玩家是否被生物标记 / Check if Player is Marked by Mob
     * 检查指定生物是否已经标记了该玩家（用于仇恨机制）
     *
     * @param mob    生物实体 / Mob entity
     * @param player 目标玩家 / Target player
     * @return 玩家是否被该生物标记 / Whether the player is marked by this mob
     */
    public static boolean isPlayerMarked(MobEntity mob, PlayerEntity player) {
        CompoundNBT tag = mob.getPersistentData();
        if (tag.contains("DisguiseAttacker")) {
            UUID attackerId = tag.getUUID("DisguiseAttacker");
            long markTime = tag.getLong("DisguiseAttackerTime");

            // 检查标记是否过期（30秒）/ Check if mark has expired (30 seconds)
            if (System.currentTimeMillis() - markTime > 30000) {
                tag.remove("DisguiseAttacker");
                tag.remove("DisguiseAttackerTime");
                return false;
            }

            return player.getUUID().equals(attackerId);
        }
        return false;
    }
}